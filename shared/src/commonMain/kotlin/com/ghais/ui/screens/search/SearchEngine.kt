package com.ghais.ui.screens.search

import com.ghais.data.repository.DevotionalRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.DevotionKind
import com.ghais.domain.model.DevotionalItem
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah

data class AyahReference(val surahId: Int, val ayahNo: Int)

data class SearchResults(
    val query: String,
    val surahs: List<Surah> = emptyList(),
    val reciters: List<Reciter> = emptyList(),
    val ayahReference: AyahReference? = null,
    /**
     * Duas and Ruqiyah, both kinds in ONE relevance-ordered list rather than
     * two blocks the caller has to merge (which is where a "Duas" and a "Ruqiyah"
     * group would end up in an arbitrary, kind-derived order).
     *
     * Defaults to empty so `SearchResults(query)` — the call
     * `CloudReciterVisibilityTest` makes through `search()` for a blank query —
     * stays source-compatible. A non-default field here breaks that test.
     */
    val devotionals: List<DevotionalItem> = emptyList()
)

/**
 * Combined devotional hits shown by the search screen, both kinds together.
 *
 * 40 is the same per-call ceiling [DevotionalRepository.search] defaults to, and
 * it is comfortably above the number of rows the group is meant to show: a
 * query that matches more than 40 devotionals is a browse, not a lookup, and the
 * Duas tab in the devotional list screen is one tap away for that.
 */
private const val DEVOTIONAL_RESULT_LIMIT: Int = 40

/**
 * Per-kind ceiling handed to [DevotionalRepository.search] BEFORE ranking.
 *
 * Deliberately above the whole catalogue (97 duas + 20 ruqiyah = 117): the
 * repository truncates in seed order, so a low per-kind cap would drop
 * high-ranking rows before the ranking ever ran. Ranking sees every hit; the
 * cap that actually bites is [DEVOTIONAL_RESULT_LIMIT] on the combined list.
 */
private const val DEVOTIONAL_FETCH_LIMIT: Int = 200

object SearchEngine {
    private val famousAyahAliases = mapOf(
        "ayat al-kursi" to AyahReference(2, 255),
        "ayat al kursi" to AyahReference(2, 255),
        "ayatal kursi" to AyahReference(2, 255),
        "amanar rasul" to AyahReference(2, 285),
        "amanar-rasul" to AyahReference(2, 285),
    )

    private val referenceRegex = Regex("""^(\d{1,3})\s*[:\-]\s*(\d{1,3})$""")

    fun search(query: String, reciters: List<Reciter> = QuranDataRepository.getBrowseReciters()): SearchResults {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return SearchResults(query)

        val lowerQuery = trimmed.lowercase()

        val ayahRef = parseAyahReference(lowerQuery)

        val surahs = QuranData.SURAHS.filter {
            it.nameEn.lowercase().contains(lowerQuery) ||
            it.nameAr.contains(trimmed) ||
            it.transliteration.lowercase().contains(lowerQuery) ||
            it.meaning.lowercase().contains(lowerQuery) ||
            it.id.toString() == trimmed
        }

        val matchedReciters = reciters.filter {
            it.nameEn.lowercase().contains(lowerQuery) ||
            it.nameAr.contains(trimmed) ||
            it.slug.lowercase().contains(lowerQuery)
        }

        val devotionals = devotionalHits(trimmed, lowerQuery)

        return SearchResults(
            query = query,
            surahs = surahs,
            reciters = matchedReciters,
            ayahReference = ayahRef,
            devotionals = devotionals
        )
    }

    /**
     * Both devotional collections, one relevance-ordered list.
     *
     * Matching itself is NOT re-implemented here: each kind goes through
     * [DevotionalRepository.search], which owns the field list and the casing
     * rules (Latin lowercased on both sides, Arabic matched case-sensitively
     * against the trimmed raw query because Arabic has no case and a lowercased
     * query loses its diacritics). This function only RANK what that returned.
     *
     * Order of preference, best first:
     * 0. title prefix  — "Ruq" before "Ar-Ruqiyah before "…Ar-Ruqiyah"
     * 1. title substring
     * 2. category — a typed category should surface its own entries above an
     *    incidental mention in a translation
     * 3. translation
     * 4. reference — a citation hit is a weaker signal than prose
     * 5. Arabic-only match, last: it means the user pasted script, and the seed
     *    stores several near-identical passages, so those rows are the least
     *    discriminating and must not crowd the top of the list.
     *
     * [List.sortedBy] is stable, so equal scores keep the flattened order —
     * duas before ruqiyah, catalogue order within each — rather than an
     * arbitrary reshuffle between recompositions.
     */
    private fun devotionalHits(trimmed: String, lowerQuery: String): List<DevotionalItem> {
        if (trimmed.isEmpty()) return emptyList()
        return DevotionKind.entries
            .flatMap { kind ->
                DevotionalRepository.search(kind, trimmed, DEVOTIONAL_FETCH_LIMIT)
            }
            .sortedBy { devotionalRank(it, trimmed, lowerQuery) }
            .take(DEVOTIONAL_RESULT_LIMIT)
    }

    /**
     * Lower is better. Mirrors [DevotionalRepository.search]'s match
     * conditions in the same order so every returned hit scores below
     * [RANK_UNMATCHED]; the last branch exists only so the total order is
     * defined for a row that matched a field this ranking does not test.
     */
    private fun devotionalRank(
        item: DevotionalItem,
        rawQuery: String,
        lowerQuery: String
    ): Int {
        val title = item.title.trim().lowercase()
        return when {
            title.startsWith(lowerQuery) -> 0
            title.contains(lowerQuery) -> 1
            item.category.trim().lowercase().contains(lowerQuery) -> 2
            item.translation.lowercase().contains(lowerQuery) -> 3
            item.reference.lowercase().contains(lowerQuery) -> 4
            item.arabic.contains(rawQuery) -> 5
            else -> RANK_UNMATCHED
        }
    }

    private const val RANK_UNMATCHED: Int = 6

    private fun parseAyahReference(query: String): AyahReference? {
        val match = referenceRegex.find(query)
        if (match != null) {
            val sId = match.groupValues[1].toIntOrNull()
            val aId = match.groupValues[2].toIntOrNull()
            if (sId != null && aId != null && sId in 1..114) {
                val surah = QuranData.SURAHS.find { it.id == sId }
                if (surah != null && aId in 1..surah.ayahsCount) {
                    return AyahReference(sId, aId)
                }
            }
        }
        for ((alias, ref) in famousAyahAliases) {
            if (query.contains(alias)) {
                return ref
            }
        }
        return famousAyahAliases[query]
    }
}
