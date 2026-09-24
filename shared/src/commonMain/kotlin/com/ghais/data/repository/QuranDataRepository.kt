package com.ghais.data.repository

import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.ReciterCatalog
import com.ghais.domain.model.Surah

/**
 * Repository providing access to Quranic data such as reciters and surahs.
 */
object QuranDataRepository {

    /**
     * Look up a reciter by slug with smart normalization and fallback to Mishary Rashid Alafasy.
     *
     * Resolution order: (0) the cloud-merged catalog from [getReciters] first, so
     * admin-curated metadata (and cloud-only rows) resolve without a sync-time
     * rewrite; exact-[catalog] match when the caller knows it, otherwise
     * MP3Quran-first then any-catalog (variants coexist via
     * [Reciter.catalogKey], never bare slug). (1) Legacy bundled fallback chain
     * below (EveryAyah-first fuzzy match) is kept verbatim for offline parity.
     */
    fun getReciterBySlug(slug: String?, catalog: ReciterCatalog? = null): Reciter {
        if (slug.isNullOrBlank()) {
            return getFallbackReciter()
        }
        val cleanSlug = slug.trim().lowercase()

        // 0. Cloud-merged catalog first (bundled + Appwrite overlay).
        runCatching { getReciters() }.getOrNull()?.let { merged ->
            if (catalog != null) {
                merged.find {
                    it.slug.equals(cleanSlug, ignoreCase = true) && it.catalog == catalog
                }?.let { return it }
            } else {
                merged.find {
                    it.slug.equals(cleanSlug, ignoreCase = true) &&
                        it.catalog == ReciterCatalog.MP3QURAN
                }?.let { return it }
                merged.find { it.slug.equals(cleanSlug, ignoreCase = true) }?.let { return it }
            }
        }

        // 1. Check verified EveryAyah catalog first (contains exact EveryAyah audio folder mapping)
        com.ghais.data.seed.EveryAyahReciters.findBySlug(cleanSlug)?.let {
            return it.toReciter()
        }

        // 2. Check full MP3Quran reciter catalog
        return QuranData.RECITERS.find { reciter ->
            val rSlug = reciter.slug.lowercase()
            rSlug == cleanSlug ||
            (cleanSlug == "mishary" && rSlug == "alafasy") ||
            (cleanSlug == "al-sudais" && rSlug == "sudais") ||
            (cleanSlug == "al-muaiqly" && rSlug == "muaiqly") ||
            (cleanSlug == "al-dossari" && rSlug == "dossari") ||
            (cleanSlug == "abdul-basit" && rSlug.startsWith("abdulbaset")) ||
            (cleanSlug == "abdulbasit" && rSlug.startsWith("abdulbaset")) ||
            (cleanSlug == "shuraim" && rSlug == "shuraym") ||
            (cleanSlug == "islam-sobhi" && rSlug.contains("islam")) ||
            rSlug.replace("_", "-") == cleanSlug ||
            rSlug.replace("-", "_") == cleanSlug ||
            reciter.nameEn.lowercase().contains(cleanSlug)
        } ?: getFallbackReciter()
    }

    /**
     * Look up a reciter by stable catalog key (`"<catalog>:<slug>"`, see
     * [Reciter.catalogKey]) — the collision-proof identity for the 11 slugs
     * that exist in both catalogs. Falls back to [getFallbackReciter] on
     * blank/malformed keys, mirroring [getReciterBySlug].
     */
    fun getReciterByCatalogKey(catalogKey: String?): Reciter {
        val raw = catalogKey?.trim()?.takeIf { it.isNotEmpty() } ?: return getFallbackReciter()
        val sep = raw.indexOf(':')
        if (sep <= 0 || sep >= raw.length - 1) {
            // Bare slug passed as a key: degrade to slug resolution.
            return getReciterBySlug(raw)
        }
        val catalog = when (raw.substring(0, sep).trim().lowercase()) {
            ReciterCatalog.EVERYAYAH.wire -> ReciterCatalog.EVERYAYAH
            else -> ReciterCatalog.MP3QURAN
        }
        return getReciterBySlug(raw.substring(sep + 1), catalog)
    }

    /**
     * Fallback reciter: Mishary Rashid Alafasy.
     */
    fun getFallbackReciter(): Reciter {
        return com.ghais.data.seed.EveryAyahReciters.findBySlug("mishary")?.toReciter()
            ?: QuranData.RECITERS.firstOrNull { it.slug == "alafasy" }
            ?: QuranData.RECITERS.first()
    }

    /**
     * Returns verified EveryAyah reciters for memorization (Hifz).
     */
    fun getEveryAyahReciters(): List<Reciter> =
        com.ghais.data.seed.EveryAyahReciters.ALL.map { it.toReciter() }

    /**
     * Returns all reciters: bundled seeds with Appwrite cloud fields
     * overlaid (images, descriptions, flags) once [ReciterCloudCache] has
     * synced. Offline behavior is unchanged (bundled only).
     *
     * This is the CATALOG-COMPLETE list: it intentionally keeps both audio
     * variants (MP3Quran + EveryAyah) for the same human, keyed apart by
     * [Reciter.catalogKey]. Use [getBrowseReciters] for anything that lists
     * or searches reciters for a human.
     */
    fun getReciters(): List<Reciter> =
        ReciterCloudCache.mergedWith(QuranData.RECITERS)

    /**
     * Human-facing reciter list: ONE row per person, for browse/search and
     * reciter pickers.
     *
     * The catalog legitimately stores two rows for one reciter (MP3Quran
     * full-surah + EveryAyah per-ayah, see [Reciter.catalogKey]) and the two
     * catalogs also use different slugs for the same human (`mishary` vs
     * `alafasy`, `al-muiqly` vs `muaiqly`, ...). Listing the raw catalog
     * therefore produced duplicate people — sometimes with different photos —
     * and duplicate Compose LazyColumn keys, which is a hard crash.
     *
     * Rows are grouped by [personKeyFor] and the survivor is chosen by
     * [browseWinner]: the variant carrying an admin-uploaded photo wins, then
     * MP3Quran (canonical full-surah identity, matching [getReciterBySlug]),
     * then whatever came first. Nothing is deleted — [getReciters] and
     * [getReciterByCatalogKey] still resolve every audio variant, and Hifz
     * keeps its own EveryAyah list via [getEveryAyahReciters].
     */
    fun getBrowseReciters(): List<Reciter> =
        getReciters()
            .groupBy { personKeyFor(it) }
            .values
            .mapNotNull { variants -> variants.takeIf { it.isNotEmpty() }?.let(::browseWinner) }

    /**
     * Identity of the human behind a reciter row. Same-slug cross-catalog
     * rows collapse for free; the alias table folds the cross-catalog naming
     * differences into one group.
     *
     * Deliberately excludes recording *editions* (teacher / mujawwad /
     * haramain slugs): those are distinct audio folders and stay separate
     * rows so their playback is never merged away.
     */
    private fun personKeyFor(reciter: Reciter): String {
        val slug = reciter.slug.trim().lowercase()
        return RECITER_PERSON_ALIASES[slug] ?: slug
    }

    /**
     * The single [getBrowseReciters] row for the human behind [slug],
     * whichever catalog spelling was stored. Returns null when the slug is
     * unknown. Used by search history so `mishary` and `alafasy` collapse to
     * one reciter row instead of two profiles for the same person.
     */
    fun getBrowseReciterBySlug(slug: String?): Reciter? {
        val clean = slug?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val personKey = RECITER_PERSON_ALIASES[clean] ?: clean
        return getBrowseReciters().firstOrNull { personKeyFor(it) == personKey }
    }

    /** Survivor rule for one human's variants — see [getBrowseReciters]. */
    private fun browseWinner(variants: List<Reciter>): Reciter =
        variants.firstOrNull { !it.imageUrl.isNullOrBlank() }
            ?: variants.firstOrNull { it.catalog == ReciterCatalog.MP3QURAN }
            ?: variants.first()

    /**
     * Returns all surahs.
     */
    fun getSurahs(): List<Surah> = QuranData.SURAHS

    /**
     * Returns a surah by its 1-indexed ID.
     */
    fun getSurahById(id: Int): Surah? = QuranData.SURAHS.find { it.id == id }

    /**
     * Returns the list of surahs recorded and available for this reciter.
     */
    fun getSurahsForReciter(reciter: Reciter): List<Surah> {
        val availableIds = reciter.getAvailableSurahIds()
        return QuranData.SURAHS.filter { availableIds.contains(it.id) }
    }

    /**
     * Cross-catalog slug aliases for the SAME human, keyed by the alias slug
     * and valued with a stable group id (the EveryAyah spelling).
     *
     * Both catalogs index several reciters under different slugs
     * (`mishary` vs `alafasy`, `al-sudais` vs `abdulrahman-al-sudais`, ...),
     * which surfaced as two "different" profiles with two different photos in
     * browse/search. Audio identity stays per-slug — this table is display
     * grouping only (see [getBrowseReciters]).
     *
     * Edition slugs (teacher / mujawwad / haramain) are intentionally absent:
     * they are separate recordings with separate audio folders.
     */
    private val RECITER_PERSON_ALIASES: Map<String, String> = mapOf(
        "alafasy" to "mishary",
        "mahmoud-khalil-al-hussary" to "al-husary",
        "shuraym" to "saud-shuraim",
        "abdulbasit-abdulsamad" to "abdul-basit",
        "muaiqly" to "al-muaiqly",
        "dossari" to "al-dossari",
        "abdulrahman-al-sudais" to "al-sudais",
        "mohamed-siddiq-el-minshawi" to "al-minshawi",
        "ghamdi" to "saad-alghamdi",
        "ali-bin-abdulrahman-al-huthaify" to "hudhaify",
        "abdullah-awad-al-juhany" to "abdullah-al-juhany",
        "ahmad-bin-ali-al-ajmi" to "ahmed-alajamy",
        "hani-al-rifai" to "hani-ar-rifai",
        "nasser-al-qatami" to "nasser-alqatami",
        "mohammad-mahmoud-al-tablawi" to "mohammad-al-tablawi",
        "yasser-salamah" to "yasser-salama",
    )
}
