package com.ghais.data.repository

import com.ghais.domain.model.Reciter

/**
 * Similarity engine behind "related qaris": ranks [candidates] by closeness
 * to [seed] so a reciter profile can suggest "more like this".
 *
 * Weight table (higher wins):
 *
 * | Signal | Weight |
 * |---|---|
 * | Same country ([reciterCountryKey]-normalized, so `"Egypt 🇪🇬"` matches `"Egypt"`) | +3 |
 * | Same riwayah (trimmed, case-insensitive) | +2 |
 * | Shared style tag (lowercased word-token overlap, so `"Madinah Murattal"` shares `"murattal"`) | +1 |
 * | Shared available-surah overlap | + up to 0.5 (`sharedCount / 114 * 0.5`) |
 *
 * The surah bonus is deliberately capped below every integer step so it only
 * breaks ties inside a country/riwayah/style tier instead of outranking one.
 * Blank [Reciter.availableSurahList] means "all 114" (see
 * [Reciter.getAvailableSurahIds]), so two full-coverage reciters share the
 * maximum bonus.
 *
 * Exclusions: the seed itself and its alias-twins are dropped via
 * [QuranDataRepository.personKeyFor] — the exact grouping [QuranDataRepository.getBrowseReciters]
 * uses — so `mishary` never suggests `alafasy` (same human, different
 * catalog spelling). Country/riwayah points require both sides non-blank, so
 * missing metadata scores nothing instead of clustering every blank row.
 *
 * Deterministic: ties break by `nameEn` (case-insensitive, then raw) and
 * then `slug` (lowercased, then raw), so repeated calls and reordered inputs
 * produce the same order.
 *
 * Pure function: no I/O, no platform APIs, never throws on ordinary input.
 */
object RelatedQaris {

    /** Number of surahs in the mushaf; normalizes the overlap bonus. */
    private const val SURAH_COUNT = 114

    /** Ceiling of the surah-overlap bonus: below one style point. */
    private const val MAX_SURAH_BONUS = 0.5

    private val STYLE_SPLIT = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Returns up to [limit] candidates most similar to [seed], best first.
     * Returns an empty list when [candidates] is empty or [limit] <= 0.
     */
    fun relatedTo(seed: Reciter, candidates: List<Reciter>, limit: Int = 10): List<Reciter> {
        if (candidates.isEmpty() || limit <= 0) return emptyList()
        val seedPerson = QuranDataRepository.personKeyFor(seed)
        val seedCountry = reciterCountryKey(seed.country)
        val seedRiwayah = seed.riwayah.trim().lowercase()
        val seedStyleTags = styleTags(seed.style)
        val seedSurahs = seed.getAvailableSurahIds().toSet()

        return candidates
            .filter { QuranDataRepository.personKeyFor(it) != seedPerson }
            .map { candidate -> candidate to score(seedCountry, seedRiwayah, seedStyleTags, seedSurahs, candidate) }
            .sortedWith(
                compareByDescending<Pair<Reciter, Double>> { it.second }
                    .thenBy { it.first.nameEn.lowercase() }
                    .thenBy { it.first.nameEn }
                    .thenBy { it.first.slug.lowercase() }
                    .thenBy { it.first.slug },
            )
            .take(limit)
            .map { it.first }
    }

    private fun score(
        seedCountry: String,
        seedRiwayah: String,
        seedStyleTags: Set<String>,
        seedSurahs: Set<Int>,
        candidate: Reciter,
    ): Double {
        var total = 0.0
        val candidateCountry = reciterCountryKey(candidate.country)
        if (seedCountry.isNotBlank() && candidateCountry.isNotBlank() && candidateCountry == seedCountry) {
            total += 3.0
        }
        val candidateRiwayah = candidate.riwayah.trim().lowercase()
        if (seedRiwayah.isNotBlank() && candidateRiwayah.isNotBlank() && candidateRiwayah == seedRiwayah) {
            total += 2.0
        }
        if (seedStyleTags.isNotEmpty() && styleTags(candidate.style).any { it in seedStyleTags }) {
            total += 1.0
        }
        val shared = seedSurahs.intersect(candidate.getAvailableSurahIds().toSet()).size
        total += (shared.toDouble() / SURAH_COUNT) * MAX_SURAH_BONUS
        return total
    }

    private fun styleTags(style: String): Set<String> =
        style.lowercase().split(STYLE_SPLIT).filter { it.isNotBlank() }.toSet()
}
