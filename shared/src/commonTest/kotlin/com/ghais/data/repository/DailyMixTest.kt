package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Guards the pure Daily Mix builder ([DailyMix.build]).
 *
 * Everything is offline and deterministic: qari slugs are derived live from
 * the bundled catalog ([QuranDataRepository.getBrowseReciters]) and every
 * expectation is computed from [QuranDataRepository.getSurahsForReciter], so
 * no seed-count hardcoding can rot. No platform player, no Settings.
 */
class DailyMixTest {

    /** Browse reciters with a large available pool (stable, non-flaky shuffles). */
    private fun largePoolSlugs(): List<String> {
        val browse = QuranDataRepository.getBrowseReciters()
        val large = browse
            .filter { QuranDataRepository.getSurahsForReciter(it).size >= 30 }
            .take(2)
            .map { it.slug }
        return large.ifEmpty { browse.take(2).map { it.slug } }
    }

    private fun availableIdsFor(slug: String): Pair<String, List<Int>> {
        val reciter = QuranDataRepository.getBrowseReciterBySlug(slug)
            ?: QuranDataRepository.getReciterBySlug(slug)
        return reciter.slug to QuranDataRepository.getSurahsForReciter(reciter)
            .filter { reciter.isSurahAvailable(it.id) }
            .map { it.id }
    }

    @Test
    fun testSameSeedGivesSameList() {
        val slugs = largePoolSlugs()
        val played = setOf(slugs.first() to 1, slugs.first() to 2)
        val first = DailyMix.build(slugs, played, daySeed = 20261010L)
        val second = DailyMix.build(slugs, played, daySeed = 20261010L)
        assertTrue(first.isNotEmpty())
        assertEquals(first, second)
    }

    @Test
    fun testDifferentSeedLikelyDifferentOrder() {
        val slugs = largePoolSlugs()
        val a = DailyMix.build(slugs, emptySet(), daySeed = 111L)
        val b = DailyMix.build(slugs, emptySet(), daySeed = 222L)
        assertTrue(a.isNotEmpty())
        assertTrue(b.isNotEmpty())
        // Seeded shuffle over a >= 30 pool: identical orders across seeds are
        // vanishingly unlikely (30! permutations), so inequality is stable.
        assertNotEquals(a, b)
    }

    @Test
    fun testUnplayedPreferredOverPlayed() {
        val slugs = largePoolSlugs()
        val (canonical, available) = availableIdsFor(slugs.first())
        val playedIds = available.take(10)
        assertTrue(playedIds.size == 10)
        val played = playedIds.map { canonical to it }.toSet()
        val mix = DailyMix.build(listOf(slugs.first()), played, daySeed = 7L, count = 20)
        assertEquals(20, mix.size)
        // The whole unplayed pool (104) covers the first 20 slots: no played
        // track may appear before the unplayed pool is exhausted.
        assertTrue(mix.none { (it.reciterSlug to it.surahId) in played })
    }

    @Test
    fun testPlayedFallbackAfterExhaustion() {
        val slugs = largePoolSlugs()
        val (canonical, available) = availableIdsFor(slugs.first())
        // Mark all but 2 as played: the mix must start with the 2 unplayed,
        // then fall back to played surahs to fill the requested count.
        val unplayedIds = available.takeLast(2).toSet()
        val played = available.filterNot { it in unplayedIds }.map { canonical to it }.toSet()
        val mix = DailyMix.build(listOf(slugs.first()), played, daySeed = 7L, count = 10)
        assertEquals(10, mix.size)
        assertEquals(unplayedIds, mix.take(2).map { it.surahId }.toSet())
        assertTrue(mix.drop(2).all { (it.reciterSlug to it.surahId) in played })
    }

    @Test
    fun testOnlyAvailableSurahsEmitted() {
        val slugs = largePoolSlugs()
        val mix = DailyMix.build(slugs, emptySet(), daySeed = 99L, count = 20)
        assertTrue(mix.isNotEmpty())
        mix.forEach { track ->
            val reciter = QuranDataRepository.getBrowseReciterBySlug(track.reciterSlug)
                ?: QuranDataRepository.getReciterBySlug(track.reciterSlug)
            assertTrue(reciter.isSurahAvailable(track.surahId))
            assertEquals(0, track.ayahNo)
            assertTrue(track.audioUrl.isNotBlank())
        }
    }

    @Test
    fun testEmptyInputFallbackIsNonEmpty() {
        val mix = DailyMix.build(emptyList(), emptySet(), daySeed = 42L)
        assertTrue(mix.isNotEmpty())
        assertTrue(mix.size <= 20)
        assertTrue(mix.all { it.ayahNo == 0 && it.audioUrl.isNotBlank() })
    }

    @Test
    fun testCountCapRespected() {
        val slugs = largePoolSlugs()
        val union = slugs.flatMap { slug ->
            val (canonical, ids) = availableIdsFor(slug)
            ids.map { canonical to it }
        }.toSet()
        val small = DailyMix.build(slugs, emptySet(), daySeed = 5L, count = 5)
        assertEquals(minOf(5, union.size), small.size)
        // No duplicate (qari, surah) pairs even across alias resolutions.
        val keys = small.map { it.reciterSlug to it.surahId }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun testTrackShapeIsFullSurahPlayable() {
        val slugs = largePoolSlugs()
        val mix = DailyMix.build(slugs, emptySet(), daySeed = 5L, count = 5)
        assertTrue(mix.isNotEmpty())
        mix.forEach { track ->
            val reciter = QuranDataRepository.getBrowseReciterBySlug(track.reciterSlug)
                ?: QuranDataRepository.getReciterBySlug(track.reciterSlug)
            assertEquals(reciter.slug, track.reciterSlug)
            assertEquals(reciter.nameEn, track.reciterName)
            val surah = QuranDataRepository.getSurahById(track.surahId)
            assertTrue(surah != null)
            assertEquals(surah.nameEn, track.surahNameEn)
            assertEquals(surah.nameAr, track.surahNameAr)
            assertEquals(0, track.ayahNo)
            assertEquals("", track.textUthmani)
            assertEquals(surah.ayahsCount * 15_000L, track.durationMs)
            assertEquals(reciter.getFullSurahUrl(track.surahId), track.audioUrl)
        }
    }
}
