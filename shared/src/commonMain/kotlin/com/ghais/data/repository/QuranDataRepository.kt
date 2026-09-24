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
     */
    fun getReciters(): List<Reciter> =
        ReciterCloudCache.mergedWith(QuranData.RECITERS)

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
}
