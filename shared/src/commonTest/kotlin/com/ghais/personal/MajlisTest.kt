package com.ghais.personal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MajlisTest {

    @Test
    fun testFatihaOpensEveryGoal() {
        listOf("sleep", "memorize", "work", "study", "consistency", null, "weird").forEach { goal ->
            assertEquals(1, majlisSurahIds(goal).first(), "goal=$goal")
        }
    }

    @Test
    fun testSleepPutsMulkBeforeKahf() {
        val ids = majlisSurahIds("sleep")
        assertTrue(ids.indexOf(67) < ids.indexOf(18), "Mulk before Kahf: $ids")
    }

    @Test
    fun testMemorizeLeadsWithShortSurahs() {
        val ids = majlisSurahIds("memorize")
        assertTrue(ids.indexOf(112) < ids.indexOf(36), "Ikhlas before Ya-Sin: $ids")
    }

    @Test
    fun testMajlisTracksCapMinutes() {
        val reciter = resolveMajlisReciter(emptySet())
        val tracks = buildMajlisTracks(reciter, "consistency", minutesCap = 10)
        assertTrue(tracks.isNotEmpty(), "at least one track")
        assertEquals(1, tracks.first().surahId, "Fatiha first")
        val totalMs = tracks.sumOf { it.durationMs }
        // First track always included even if it alone exceeds cap; the rest
        // must fit inside it.
        assertTrue(totalMs <= 10 * 60_000L + tracks.first().durationMs, "total=$totalMs")
    }

    @Test
    fun testMajlisFallsBackToDefaultVoice() {
        val reciter = resolveMajlisReciter(emptySet())
        assertEquals(DEFAULT_MAJLIS_RECITER_SLUG, reciter.slug)
    }
}
