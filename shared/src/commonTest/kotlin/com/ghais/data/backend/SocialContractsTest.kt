package com.ghais.data.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the A13 shared semantics: devotion day-boundary (including the
 * timezone edge), deterministic league pooling sizes, and friend-streak
 * folding. Pure common tests — no clock, no platform APIs.
 */
class SocialContractsTest {

    // ---- devotion day boundary ----

    @Test
    fun testFirstActiveDayStartsStreakAtOne() {
        val result = SocialContracts.evaluateDevotionDay(
            SocialContracts.DevotionState(userId = "u1"),
            "2026-10-01",
        )

        assertEquals(1, result.state.streakCount)
        assertEquals(1, result.state.longestStreak)
        assertEquals("2026-10-01", result.state.lastActiveDay)
        assertTrue(result.streakAdvanced)
        assertFalse(result.graceUsed)
    }

    @Test
    fun testConsecutiveDayIncrementsAndTracksLongest() {
        val state = SocialContracts.DevotionState(
            userId = "u1", streakCount = 4, longestStreak = 6,
            lastActiveDay = "2026-10-01",
        )

        val result = SocialContracts.evaluateDevotionDay(state, "2026-10-02")

        assertEquals(5, result.state.streakCount)
        assertEquals(6, result.state.longestStreak)
        assertTrue(result.streakAdvanced)
        assertFalse(result.graceUsed)
    }

    @Test
    fun testSameDayRepeatIsANoOp() {
        val state = SocialContracts.DevotionState(
            userId = "u1", streakCount = 3, longestStreak = 3,
            lastActiveDay = "2026-10-02",
        )

        val result = SocialContracts.evaluateDevotionDay(state, "2026-10-02")

        assertEquals(state, result.state)
        assertFalse(result.streakAdvanced)
    }

    @Test
    fun testOneMissedDayConsumesGraceAndContinues() {
        val state = SocialContracts.DevotionState(
            userId = "u1", streakCount = 5, longestStreak = 5,
            lastActiveDay = "2026-10-01", graceDays = 1,
        )

        // Skips 10-02, active again 10-03: gap of 2 with 1 grace day.
        val result = SocialContracts.evaluateDevotionDay(state, "2026-10-03")

        assertEquals(6, result.state.streakCount)
        assertEquals(0, result.state.graceDays)
        assertTrue(result.streakAdvanced)
        assertTrue(result.graceUsed)
    }

    @Test
    fun testGapBeyondGraceResetsStreakToOne() {
        val state = SocialContracts.DevotionState(
            userId = "u1", streakCount = 9, longestStreak = 9,
            lastActiveDay = "2026-10-01", graceDays = 1,
        )

        val result = SocialContracts.evaluateDevotionDay(state, "2026-10-05")

        assertEquals(1, result.state.streakCount)
        assertEquals(9, result.state.longestStreak)
        assertEquals("2026-10-05", result.state.lastActiveDay)
        assertTrue(result.streakAdvanced)
        assertFalse(result.graceUsed)
    }

    @Test
    fun testMalformedTodayNeverThrowsAndLeavesStateUntouched() {
        val state = SocialContracts.DevotionState(
            userId = "u1", streakCount = 2, lastActiveDay = "2026-10-01",
        )

        val result = SocialContracts.evaluateDevotionDay(state, "not-a-day")

        assertEquals(state, result.state)
        assertFalse(result.streakAdvanced)
    }

    // ---- timezone edge ----

    @Test
    fun testSameInstantFallsOnDifferentDaysUnderDifferentOffsets() {
        // 2026-01-01T00:30:00Z derived from the day math itself (no hardcoded ms).
        val instantMs = SocialContracts.dayToEpochDay("2026-01-01")!! * 86_400_000L + 1_800_000L

        val utcDay = SocialContracts.epochDayToString(SocialContracts.epochDay(instantMs, 0))
        val minusThreeDay = SocialContracts.epochDayToString(SocialContracts.epochDay(instantMs, -180))
        val plusFiveHalfDay = SocialContracts.epochDayToString(SocialContracts.epochDay(instantMs, 330))

        assertEquals("2026-01-01", utcDay)
        assertEquals(
            "2025-12-31", minusThreeDay,
            "00:30Z is still the previous local day at UTC-3 — the boundary both sides must agree on",
        )
        assertEquals("2026-01-01", plusFiveHalfDay)
    }

    @Test
    fun testDayStringsRoundTripThroughEpochDays() {
        listOf("2026-01-01", "2025-12-31", "2024-02-29", "2026-10-14").forEach { day ->
            val epoch = SocialContracts.dayToEpochDay(day)!!
            assertEquals(day, SocialContracts.epochDayToString(epoch.toLong()))
        }
        assertNull(SocialContracts.dayToEpochDay("2026-13-01"))
        assertNull(SocialContracts.dayToEpochDay("2026-02-30"))
        assertNull(SocialContracts.dayToEpochDay("10-14"))
    }

    // ---- league pooling ----

    @Test
    fun testSixtyFiveMembersPoolIntoThirtyThirtyAndFive() {
        val members = (1..65).map { "user-$it" }

        val pools = SocialContracts.poolLeague(members, "2026-10-12")

        assertEquals(3, pools.size)
        assertEquals(listOf(30, 30, 5), pools.map { it.memberIds.size })
        assertEquals(65, pools.flatMap { it.memberIds }.toSet().size)
        assertTrue(pools.all { it.weekStart == "2026-10-12" })
    }

    @Test
    fun testPoolingIsDeterministicAndWeekScoped() {
        val members = (1..40).map { "user-$it" }

        val first = SocialContracts.poolLeague(members, "2026-10-12")
        val second = SocialContracts.poolLeague(members, "2026-10-12")
        val otherWeek = SocialContracts.poolLeague(members, "2026-10-19")

        assertEquals(first, second)
        assertEquals(
            first.flatMap { it.memberIds }.toSet(),
            otherWeek.flatMap { it.memberIds }.toSet(),
            "Same members, reshuffled — nobody is lost across weeks",
        )
    }

    @Test
    fun testPoolingEdgeSizes() {
        assertTrue(SocialContracts.poolLeague(emptyList(), "2026-10-12").isEmpty())

        val exact = SocialContracts.poolLeague((1..30).map { "u$it" }, "2026-10-12")
        assertEquals(1, exact.size)
        assertEquals(30, exact.single().memberIds.size)

        val overflow = SocialContracts.poolLeague((1..31).map { "u$it" }, "2026-10-12")
        assertEquals(listOf(30, 1), overflow.map { it.memberIds.size })
    }

    // ---- friend streaks ----

    @Test
    fun testFirstSharedDayStartsFriendStreakAtOne() {
        val result = SocialContracts.evaluateFriendStreak(
            lastBothActiveDay = null, aDay = "2026-10-14", bDay = "2026-10-14",
            todayDay = "2026-10-14", currentStreak = 0,
        )

        assertEquals(1, result.sharedStreak)
        assertEquals("2026-10-14", result.lastBothActiveDay)
        assertTrue(result.bothActiveToday)
    }

    @Test
    fun testConsecutiveSharedDayIncrements() {
        val result = SocialContracts.evaluateFriendStreak(
            lastBothActiveDay = "2026-10-13", aDay = "2026-10-14", bDay = "2026-10-14",
            todayDay = "2026-10-14", currentStreak = 4,
        )

        assertEquals(5, result.sharedStreak)
        assertTrue(result.bothActiveToday)
    }

    @Test
    fun testOneSidedDayNeitherAdvancesNorResets() {
        val result = SocialContracts.evaluateFriendStreak(
            lastBothActiveDay = "2026-10-13", aDay = "2026-10-14", bDay = "2026-10-13",
            todayDay = "2026-10-14", currentStreak = 4,
        )

        assertEquals(4, result.sharedStreak)
        assertEquals("2026-10-13", result.lastBothActiveDay)
        assertFalse(result.bothActiveToday)
    }

    @Test
    fun testSharedDayAfterGapRestartsAtOne() {
        val result = SocialContracts.evaluateFriendStreak(
            lastBothActiveDay = "2026-10-10", aDay = "2026-10-14", bDay = "2026-10-14",
            todayDay = "2026-10-14", currentStreak = 7,
        )

        assertEquals(1, result.sharedStreak)
        assertEquals("2026-10-14", result.lastBothActiveDay)
        assertTrue(result.bothActiveToday)
    }

    @Test
    fun testFriendPairKeyIsOrderIndependent() {
        assertEquals(
            BackendSchema.Friendships.pairKey("b-user", "a-user"),
            BackendSchema.Friendships.pairKey("a-user", "b-user"),
        )
    }
}
