package com.ghais.ui.screens.reciters.personal

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AGENT P8 — guards the pure weekly-ritual builder ([weeklyPicks]) and its
 * Monday-anchored seed ([weekSeed]).
 *
 * Fully offline and deterministic: synthetic reciters only, fixed seeds
 * injected (never the wall clock), so no catalog-size hardcoding can rot.
 *
 * Contract pinned here:
 * 1. Monday anchoring: every day Mon–Sun of one week maps to the same seed,
 *    and the seed itself is a Monday (fixed-point).
 * 2. Unplayed-only: history/followed slugs are excluded while the pool suffices.
 * 3. Determinism: same inputs + same seed always yield the same lineup.
 * 4. Appointment dynamic: adjacent weeks rotate the anchor, so the lineup flips.
 * 5. Plumbing: [limit] caps the rows, exploration tails the shelf, starvation
 *    refills instead of collapsing, empty input yields empty output.
 */
class WeeklyRitualTest {

    private fun voice(i: Int, slug: String = "ritual-voice-$i"): Reciter = Reciter(
        slug = slug,
        nameEn = "Voice $i",
        nameAr = "صوت $i",
        country = "Country ${i % 4}",
        riwayah = if (i % 5 == 0) "Warsh" else "Hafs",
        style = listOf("murattal", "mujawwad", "muallim", "haramain")[i % 4],
    )

    private fun catalog(size: Int): List<Reciter> = List(size) { voice(it) }

    // --- 1. Monday anchoring ---

    @Test
    fun testEpochThursdayAnchorsToMonday() {
        // 1970-01-01 (epoch day 0) was a Thursday -> Monday 1969-12-29 (day -3).
        assertEquals(-3L, weekSeed(0L))
        // 1970-01-05 was a Monday -> fixed point.
        assertEquals(4L, weekSeed(4L))
    }

    @Test
    fun testWholeWeekSharesOneMondaySeed() {
        // weekSeed is a fixed point (a Monday seeds to itself) and every day
        // of its Mon..Sun week maps back to it.
        val monday = weekSeed(20_375L)
        assertEquals(monday, weekSeed(monday))
        for (offset in 0L..6L) {
            assertEquals(monday, weekSeed(monday + offset), "day +$offset must share the Monday seed")
        }
        assertTrue(weekSeed(monday - 1) != monday, "Sunday before must belong to the prior week")
        assertTrue(weekSeed(monday + 7) != monday, "next Monday must start a new week")
    }

    @Test
    fun testSameWeekDifferentDaysGiveSamePicks() {
        val reciters = catalog(25)
        val affinities = reciters.associate { it.slug to (0.3f + (it.slug.hashCode() % 50) / 100f) }
        val monday = weekSeed(20_375L)
        val expected = weeklyPicks(reciters, affinities, weekSeed = monday)
        for (offset in 1L..6L) {
            val sameWeek = weeklyPicks(reciters, affinities, weekSeed = weekSeed(monday + offset))
            assertEquals(expected.map { it.slug }, sameWeek.map { it.slug })
        }
    }

    // --- 2. Unplayed-only ---

    @Test
    fun testHistoryAndFollowedAreExcluded() {
        val reciters = catalog(25)
        val history = setOf("ritual-voice-1", "ritual-voice-2", "ritual-voice-3")
        val followed = setOf("ritual-voice-4", "ritual-voice-5")
        // 20 eligible > 10 cap, so every excluded slug must be fully absent.
        val picks = weeklyPicks(reciters, emptyMap(), history, followed, weekSeed = 99L)
        assertEquals(10, picks.size)
        val slugs = picks.map { it.slug }.toSet()
        assertTrue(slugs.none { it in history }, "played voices must not resurface")
        assertTrue(slugs.none { it in followed }, "followed voices must not resurface")
    }

    @Test
    fun testTopAffinityAnchorIsAlwaysIncluded() {
        val reciters = catalog(15)
        val affinities = mapOf("ritual-voice-7" to 1.0f)
        val picks = weeklyPicks(reciters, affinities, weekSeed = 42L)
        assertTrue(picks.any { it.slug == "ritual-voice-7" }, "highest-affinity voice must lead the ritual")
    }

    // --- 3. Determinism + appointment dynamic ---

    @Test
    fun testSameSeedGivesSameLineup() {
        val reciters = catalog(25)
        val affinities = reciters.associate { it.slug to ((it.slug.hashCode() % 100) / 100f) }
        val first = weeklyPicks(reciters, affinities, setOf("ritual-voice-1"), emptySet(), weekSeed = 7L)
        val second = weeklyPicks(reciters, affinities, setOf("ritual-voice-1"), emptySet(), weekSeed = 7L)
        assertTrue(first.isNotEmpty())
        assertEquals(first.map { it.slug }, second.map { it.slug })
    }

    @Test
    fun testAdjacentWeeksRotateTheAnchor() {
        // Seeds 0 and 1 pick anchorWindow[0] vs anchorWindow[1] (floorMod),
        // so the lineup lead is guaranteed to flip week over week.
        val reciters = catalog(12)
        val a = weeklyPicks(reciters, emptyMap(), weekSeed = 0L)
        val b = weeklyPicks(reciters, emptyMap(), weekSeed = 1L)
        assertTrue(a.isNotEmpty() && b.isNotEmpty())
        assertTrue(a.first().slug != b.first().slug, "adjacent weeks must rotate the anchor")
    }

    // --- 4. Plumbing ---

    @Test
    fun testLimitIsRespected() {
        val picks = weeklyPicks(catalog(30), emptyMap(), weekSeed = 5L, limit = 4)
        assertEquals(4, picks.size)
        assertEquals(
            weeklyPicks(catalog(30), emptyMap(), weekSeed = 5L).take(4).size,
            picks.size,
        )
    }

    @Test
    fun testNoDuplicatesInPicks() {
        val picks = weeklyPicks(catalog(30), emptyMap(), weekSeed = 5L)
        val slugs = picks.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size)
    }

    @Test
    fun testRefillsWhenEverythingExcluded() {
        // Starvation fallback: the whole catalog is played, but the ritual
        // must still deliver a full shelf rather than an empty state.
        val reciters = catalog(20)
        val history = reciters.map { it.slug }.toSet()
        val picks = weeklyPicks(reciters, emptyMap(), history, emptySet(), weekSeed = 11L)
        assertEquals(10, picks.size)
    }

    @Test
    fun testEmptyCatalogGivesEmptyPicks() {
        assertTrue(weeklyPicks(emptyList(), emptyMap(), weekSeed = 1L).isEmpty())
        assertTrue(weeklyPicks(catalog(10), emptyMap(), weekSeed = 1L, limit = 0).isEmpty())
    }

    @Test
    fun testRitualTitleShape() {
        assertEquals("New for you • this week", ritualTitle(0L))
        assertEquals("New for you • this week", ritualTitle(weekSeed(20_375L)))
    }
}
