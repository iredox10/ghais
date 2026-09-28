package com.ghais.data.repository

import com.ghais.data.seed.DevotionalData
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.domain.model.DevotionKind
import com.ghais.domain.model.DevotionalItem
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.ayahSpan

/**
 * Read-only accessor over the bundled devotional catalogue
 * ([DevotionalData.duas] / [DevotionalData.ruqiyah]).
 *
 * There is no cloud layer and no fetch: the catalogue is a compile-time
 * constant, so every function here is synchronous, side-effect free and
 * offline-identical. Callers that need Compose state should hold the result
 * with `remember(kind) { DevotionalRepository.all(kind) }` at the call site —
 * there is nothing to observe, so no composable accessor ships here (contrast
 * `rememberMergedReciters`, which exists only because the reciter overlay
 * changes when a cloud sync lands).
 *
 * Audio resolution goes through [Reciter.getAyahAudioUrl] for ruqiyah — the
 * one existing everyayah builder, so devotional audio is byte-identical to a
 * Mushaf ayah row — and through each dua's explicit [DevotionalItem.audioUrl]
 * otherwise (open IslamicAPI clips, HisnMuslim per-dua clips, everyayah for
 * Quranic duas). That is why every accessor that touches audio returns
 * nullable and why [playableTracks] keeps unresolvable items in the queue
 * instead of dropping them.
 */
object DevotionalRepository {

    private const val RUQIYAH_AUDIO_SLUG = "mishary"

    /**
     * Separator between per-ayah URLs in the [audioUrlFor] join.
     *
     * Exposed because a consumer that accepts a multi-URL value must split on
     * the same character; the current player takes a single URL (see
     * [playableTracks] for what the queue does instead).
     */
    const val MULTI_AUDIO_SEPARATOR = "|"

    /**
     * Reciter row used to build ruqiyah URLs — the same EveryAyah entry the
     * Quran player resolves for `mishary`, so the folder never drifts from
     * `Alafasy_128kbps`. Falls back to the seed's declared folder rather than
     * failing, because a missing catalogue row must not cost a working URL.
     */
    private val ruqiyahAudio: Reciter by lazy {
        EveryAyahReciters.findBySlug(RUQIYAH_AUDIO_SLUG)?.toReciter()
            ?: Reciter(
                slug = RUQIYAH_AUDIO_SLUG,
                nameEn = DevotionalData.RUQIYAH_RECITER,
                nameAr = "",
                audioFolder = DevotionalData.DEFAULT_EVERAYAH_FOLDER,
            )
    }

    /**
     * Quranic entries are re-cleaned on the way out, duas are passed through.
     *
     * A ruqiyah `arabic` string is Uthmani text fetched from the same edition the
     * Quran reader uses, so it arrives carrying the same annotation marks. Ten
     * of the twenty carry `U+06DF`, which `QCF_Hafs` draws as a large filled
     * disc dropped into the middle of the verse — the exact defect the Quran
     * reader already fixed in `QuranAyahRepository.cleanQuranicText`. Running
     * the same cleaner here means a devotional passage and a Mushaf ayah render
     * from one rule set instead of two that drift.
     *
     * The basmalah is stripped per-ayah for surahs other than 1 and 9, so
     * `Al-Ikhlas` reads `قُلْ هُوَ ٱللَّهُ أَحَدٌ…` rather than opening with a
     * basmalah the player would then recite twice.
     */
    private val cleaned: Map<DevotionKind, List<DevotionalItem>> by lazy {
        DevotionKind.entries.associateWith { kind ->
            seed(kind).map { item ->
                if (!item.isQuranic) return@map item
                val cleanedArabic = QuranAyahRepository.cleanQuranicText(item.arabic)
                val withoutBasmalah = if (
                    item.startAyah == 1 && QuranAyahRepository.hasBasmalahHeader(item.surahId)
                ) {
                    QuranAyahRepository.stripBasmalahPrefix(item.surahId, 1, cleanedArabic)
                } else {
                    cleanedArabic
                }
                item.copy(arabic = withoutBasmalah)
            }
        }
    }

    private fun seed(kind: DevotionKind): List<DevotionalItem> = when (kind) {
        DevotionKind.DUA -> DevotionalData.duas
        DevotionKind.RUQIYAH -> DevotionalData.ruqiyah
    }

    /** Every entry of one kind, in seed order (97 duas, 20 ruqiyah). */
    fun all(kind: DevotionKind): List<DevotionalItem> = cleaned.getValue(kind)

    /**
     * Both catalogues flattened, duas first. Used for the kind-agnostic
     * [byId]; the two id namespaces (`dua-*` / `ruq-*`) do not overlap.
     */
    private val everything: List<DevotionalItem> by lazy {
        all(DevotionKind.DUA) + all(DevotionKind.RUQIYAH)
    }

    /**
     * Distinct non-blank categories per kind, in first-appearance order, so a
     * filter row renders in the catalogue's own grouping rather than
     * alphabetically. Computed once per kind.
     */
    private val categoryIndex: Map<DevotionKind, List<String>> by lazy {
        DevotionKind.entries.associateWith { kind ->
            seed(kind)
                .mapNotNull { it.category.trim().takeIf { name -> name.isNotEmpty() } }
                .distinct()
        }
    }

    /** Category names for one kind, for a filter row. */
    fun categories(kind: DevotionKind): List<String> =
        categoryIndex[kind].orEmpty()

    /**
     * First seed entry with this id, or null.
     *
     * First match wins, and the seed does contain duplicate dua ids (`dua-1`
     * is used by five different supplications), so this resolves to the
     * earliest of them. That ambiguity is a seed bug, not a contract here:
     * until the ids are unique, an id is a display key, not a deep link.
     */
    fun byId(id: String): DevotionalItem? {
        val clean = id.trim()
        if (clean.isEmpty()) return null
        return everything.firstOrNull { it.id == clean }
    }

    /** Entries of one kind in one category; null/blank [category] means all. */
    fun byKindAndCategory(kind: DevotionKind, category: String?): List<DevotionalItem> {
        val clean = category?.trim().orEmpty()
        if (clean.isEmpty()) return all(kind)
        return all(kind).filter { it.category.trim().equals(clean, ignoreCase = true) }
    }

    /**
     * Substring search over title, arabic, translation and reference.
     *
     * Latin fields are lowercased on both sides; arabic is matched with a plain
     * case-sensitive `contains`, exactly as `SearchEngine` does, since Arabic
     * has no case and a lowercased query can no longer match diacritics
     * reliably. A blank query returns nothing rather than the whole catalogue,
     * so a search box does not read as "no filter" the moment it is empty.
     */
    fun search(kind: DevotionKind, query: String, limit: Int = 40): List<DevotionalItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || limit <= 0) return emptyList()
        val lower = trimmed.lowercase()
        return all(kind)
            .filter {
                it.title.lowercase().contains(lower) ||
                    it.arabic.contains(trimmed) ||
                    it.translation.lowercase().contains(lower) ||
                    it.reference.lowercase().contains(lower)
            }
            .take(limit)
    }

    /**
     * Every everyayah URL for one item, in passage order — one entry per ayah
     * via [Reciter.getAyahAudioUrl]. Empty for a dua, which has no Quran
     * coordinates: dua audio arrives as an explicit [DevotionalItem.audioUrl],
     * never through this builder.
     */
    fun ayahUrlsFor(item: DevotionalItem): List<String> {
        if (!item.isQuranic) return emptyList()
        val span = item.ayahSpan
        if (span <= 0) return emptyList()
        return List(span) { offset ->
            ruqiyahAudio.getAyahAudioUrl(item.surahId, item.startAyah + offset)
        }
    }

    /**
     * The item's audio as a single string: its own [DevotionalItem.audioUrl]
     * when set, else the everyayah URL(s) for its ayah span.
     *
     * A single-ayah item (16 of 20 ruqiyah entries, and every single-clip dua)
     * yields exactly one fetchable URL. A multi-part item yields its per-part
     * URLs joined with [MULTI_AUDIO_SEPARATOR] so the passage stays in order;
     * treat that form as a queue description, not a fetchable URL —
     * [playableTracks] is what hands the player real URLs. Null only when the
     * item resolves to no URL at all.
     */
    fun audioUrlFor(item: DevotionalItem): String? {
        val explicit = item.audioUrl
        if (!explicit.isNullOrBlank()) return explicit
        val urls = ayahUrlsFor(item)
        return when {
            urls.isEmpty() -> null
            urls.size == 1 -> urls.first()
            else -> urls.joinToString(MULTI_AUDIO_SEPARATOR)
        }
    }

    /**
     * The kind's queue: [DevotionalItem.asTrackItem] per entry, with audio
     * resolved. Nothing is filtered out — an entry with no audio still gets a
     * track with a blank URL and the player hides its transport, which is
     * cheaper to explain than a queue that silently drops rows.
     *
     * A ruqiyah entry spanning several ayahs expands to one track per ayah
     * ([ayahUrlsFor]) rather than one joined URL per item: `PlayerBridge.play`
     * takes a single URL, so a joined string would fail to load. An explicit
     * multi-part dua URL (parts joined with [MULTI_AUDIO_SEPARATOR], e.g. the
     * three after-wudu clips) expands the same way. Each expanded track keeps
     * the entry's title and full text and stays at `ayahNo = 0` /
     * `surahId = 0`, so the queue still never routes a devotional into the
     * Quran ayah pipeline.
     */
    fun playableTracks(kind: DevotionKind): List<TrackItem> =
        all(kind).flatMap { item ->
            val urls = explicitUrls(item).ifEmpty { ayahUrlsFor(item) }
            if (urls.isEmpty()) {
                listOf(item.asTrackItem())
            } else {
                urls.map { url -> item.asTrackItem().copy(audioUrl = url) }
            }
        }

    /**
     * The entry's own [DevotionalItem.audioUrl], split into playable parts.
     *
     * A single URL yields a one-element list; a multi-part dua joins its clips
     * with [MULTI_AUDIO_SEPARATOR]. Blank segments are dropped, so a stray
     * separator can never hand the player an empty URL.
     */
    fun explicitUrls(item: DevotionalItem): List<String> =
        item.audioUrl
            ?.split(MULTI_AUDIO_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

    /**
     * Queue rows one entry occupies: one per ayah or clip, at least one.
     *
     * Silent entries still occupy exactly one row (their blank-URL track), so
     * this doubles as the stride for index arithmetic over [playableTracks].
     */
    fun queueWidth(item: DevotionalItem): Int =
        maxOf(1, item.ayahSpan, explicitUrls(item).size)

    /**
     * Start index of [item]'s rows in [playableTracks] of its kind, or -1.
     *
     * Matched by id, never by title-and-text: three "Al-Ikhlas" duas share
     * both, so content matching can only ever find the first twin. The queue
     * mirrors catalogue order, so summing the [queueWidth] strides of the
     * preceding entries lands on exactly this entry's rows.
     */
    fun queueStartIndex(kind: DevotionKind, item: DevotionalItem): Int {
        var cursor = 0
        for (entry in all(kind)) {
            if (entry.id == item.id) return cursor
            cursor += queueWidth(entry)
        }
        return -1
    }

    /**
     * The contiguous slice of [playableTracks] that belongs to [item].
     *
     * Empty when the item is not in the catalogue. Callers must only use this
     * against the queue this repository built — the arithmetic assumes the
     * queue mirrors [all(kind)] exactly.
     */
    fun queueRange(kind: DevotionKind, item: DevotionalItem): IntRange {
        val first = queueStartIndex(kind, item)
        if (first < 0) return first until first
        return first..(first + queueWidth(item) - 1)
    }

    /**
     * Catalogue index of the entry owning queue row [queueIndex], or -1.
     *
     * The inverse of [queueStartIndex]: what turns an engine `currentIndex`
     * back into the entry being recited, twin-safe where content matching is
     * not.
     */
    fun collectionIndexForQueueIndex(kind: DevotionKind, queueIndex: Int): Int {
        if (queueIndex < 0) return -1
        var cursor = 0
        for ((index, entry) in all(kind).withIndex()) {
            val width = queueWidth(entry)
            if (queueIndex in cursor until cursor + width) return index
            cursor += width
        }
        return -1
    }
}
