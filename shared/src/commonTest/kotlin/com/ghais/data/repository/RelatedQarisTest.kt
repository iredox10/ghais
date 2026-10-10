package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the related-qaris similarity engine behind reciter profiles.
 *
 * [RelatedQaris.relatedTo] is a pure in-memory ranker (no network, no
 * platform state), so these tests pin the contract the UI relies on:
 *
 * 1. Relevance: a same-country qari outranks a same-everything-else qari
 *    from another country (country +3 dominates), even across the bundled
 *    flag-suffixed (`"Egypt 🇪🇬"`) vs plain (`"Egypt"`) country spellings.
 * 2. Exclusions: the seed itself never appears, and neither does its
 *    alias-twin (`alafasy` for seed `mishary` — the same human under the
 *    cross-catalog alias table [QuranDataRepository.getBrowseReciters]
 *    groups by).
 * 3. Plumbing: [limit] caps the rows, empty input yields empty output, and
 *    repeat calls (even with reordered input) return the same order, since
 *    ties break deterministically by nameEn then slug.
 */
class RelatedQarisTest {

    private fun qari(
        slug: String,
        nameEn: String,
        country: String = "",
        riwayah: String = "Hafs",
        style: String = "murattal",
        surahs: String = "",
    ): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameEn,
        riwayah = riwayah,
        style = style,
        country = country,
        availableSurahList = surahs,
    )

    private val seed = qari("seed-qari", "Seed Qari", country = "Egypt 🇪🇬")

    // --- 1. Relevance ---

    @Test
    fun testSameCountryRanksFirst() {
        val near = qari("near-qari", "Near Qari", country = "Egypt", riwayah = "Warsh", style = "mujawwad")
        val far = qari("far-qari", "Far Qari", country = "France", riwayah = "Warsh", style = "mujawwad")
        val related = RelatedQaris.relatedTo(seed, listOf(far, near))
        assertEquals(listOf("near-qari", "far-qari"), related.map { it.slug })
    }

    // --- 2. Exclusions ---

    @Test
    fun testSeedItselfIsExcluded() {
        val other = qari("other-qari", "Other Qari", country = "Egypt")
        val related = RelatedQaris.relatedTo(seed, listOf(seed, seed.copy(), other))
        assertTrue(related.none { it.slug == "seed-qari" }, "seed must never suggest itself")
        assertEquals(listOf("other-qari"), related.map { it.slug })
    }

    @Test
    fun testAliasTwinIsExcluded() {
        val mishary = qari("mishary", "Mishary Rashid Alafasy", country = "Kuwait")
        val twin = qari("alafasy", "Mishary Rashid Alafasy", country = "Kuwait")
        val other = qari("other-qari", "Other Qari", country = "Kuwait")
        val related = RelatedQaris.relatedTo(mishary, listOf(twin, other))
        assertTrue(related.none { it.slug == "alafasy" }, "alias-twin is the same human and must be excluded")
        assertEquals(listOf("other-qari"), related.map { it.slug })
    }

    // --- 3. Plumbing ---

    @Test
    fun testLimitIsRespected() {
        val candidates = (1..5).map { qari("qari-$it", "Qari $it", country = "Egypt") }
        val related = RelatedQaris.relatedTo(seed, candidates, limit = 2)
        assertEquals(2, related.size)
        assertEquals(RelatedQaris.relatedTo(seed, candidates).take(2), related)
    }

    @Test
    fun testEmptyCandidatesReturnsEmpty() {
        assertEquals(emptyList(), RelatedQaris.relatedTo(seed, emptyList()))
        assertTrue(RelatedQaris.relatedTo(seed, listOf()).isEmpty())
    }

    @Test
    fun testRepeatCallsAreDeterministic() {
        val candidates = (1..5).map { qari("qari-$it", "Qari $it", country = "Egypt") }
        val first = RelatedQaris.relatedTo(seed, candidates)
        val second = RelatedQaris.relatedTo(seed, candidates)
        assertEquals(first.map { it.slug }, second.map { it.slug })
        assertEquals(first.map { it.slug }, RelatedQaris.relatedTo(seed, candidates.reversed()).map { it.slug })
    }
}
