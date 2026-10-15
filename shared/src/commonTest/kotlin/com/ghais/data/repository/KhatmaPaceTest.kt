package com.ghais.data.repository

import com.ghais.domain.model.KhatmaPlan
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the additive AGENT A10 pace helpers ([paceStatus], [catchUpPages]).
 *
 * All plans start at 114:1 (An-Nas, 6 ayahs on a single Madinah page), so
 * the plan-aware total is always 6 and every expectation is hand-computable:
 * with `targetDays = 6`, one day's share is exactly 1 ayah.
 */
class KhatmaPaceTest {

    private fun plan(
        id: String = "pace",
        startDateMs: Long = 0L,
        targetDays: Int = 6,
        currentSurahId: Int = 114,
        currentAyahNo: Int = 1,
        streak: Int = 0,
        lastProgressMs: Long = 0L,
        totalAyahsRead: Int = 0,
    ) = KhatmaPlan(
        id = id,
        title = "Plan $id",
        targetDays = targetDays,
        currentSurahId = currentSurahId,
        currentAyahNo = currentAyahNo,
        streak = streak,
        startDateMs = startDateMs,
        lastProgressMs = lastProgressMs,
        totalAyahsRead = totalAyahsRead,
    )

    @Test
    fun freshPlanIsOnTrack() {
        val p = plan()
        assertEquals(PaceStatus.ON_TRACK, paceStatus(p, 0L))
        assertEquals(0, catchUpPages(p, 0L))
    }

    @Test
    fun onTrackWhenExactlyOnSchedule() {
        // Day 3 of 6: 3 of 6 ayahs read matches the even-split expectation.
        val p = plan(currentAyahNo = 3, lastProgressMs = 1_000L, totalAyahsRead = 3)
        assertEquals(3, khatmaRemainingAyahs(p))
        assertEquals(PaceStatus.ON_TRACK, paceStatus(p, 3 * KHATMA_DAY_MS))
        assertEquals(0, catchUpPages(p, 3 * KHATMA_DAY_MS))
    }

    @Test
    fun aheadWhenADayOrMoreAhead() {
        // Day 3 of 6 with 5 read: +2 against a 1-ayah day share.
        val p = plan(currentAyahNo = 5, lastProgressMs = 1_000L, totalAyahsRead = 5)
        assertEquals(1, khatmaRemainingAyahs(p))
        assertEquals(PaceStatus.AHEAD, paceStatus(p, 3 * KHATMA_DAY_MS))
        assertEquals(0, catchUpPages(p, 3 * KHATMA_DAY_MS))
    }

    @Test
    fun behindWhenADayOrMoreBehind() {
        // Day 3 of 6 with 1 read: -2 against a 1-ayah day share.
        // Deficit is 2 ayahs of a 6-ayah / 1-page plan: ceil(2/6) = 1 page.
        val p = plan(currentAyahNo = 1, lastProgressMs = 1_000L, totalAyahsRead = 1)
        assertEquals(5, khatmaRemainingAyahs(p))
        assertEquals(PaceStatus.BEHIND, paceStatus(p, 3 * KHATMA_DAY_MS))
        assertEquals(1, catchUpPages(p, 3 * KHATMA_DAY_MS))
    }

    @Test
    fun earlyFinishReadsAhead() {
        val p = plan(currentAyahNo = 6, lastProgressMs = 1_000L, totalAyahsRead = 6)
        assertEquals(PaceStatus.AHEAD, paceStatus(p, 2 * KHATMA_DAY_MS))
        assertEquals(0, catchUpPages(p, 2 * KHATMA_DAY_MS))
    }

    @Test
    fun pastTargetClampsExpectationToPlanTotal() {
        // Day 10 of a 6-day plan with nothing read: the raw expectation
        // (10 ayahs) clamps to the 6-ayah total, so the deficit is the
        // whole plan — ceil(6/6) = 1 page, never more than the plan itself.
        val p = plan()
        assertEquals(PaceStatus.BEHIND, paceStatus(p, 10 * KHATMA_DAY_MS))
        assertEquals(1, catchUpPages(p, 10 * KHATMA_DAY_MS))
    }

    @Test
    fun unreadablePlanIsOnTrack() {
        val p = plan(currentSurahId = 200)
        assertEquals(PaceStatus.ON_TRACK, paceStatus(p, 3 * KHATMA_DAY_MS))
        assertEquals(0, catchUpPages(p, 3 * KHATMA_DAY_MS))
    }

    @Test
    fun combinedEqualsTodayWhenOnTrack() {
        val p = plan(currentAyahNo = 3, lastProgressMs = 1_000L, totalAyahsRead = 3)
        val nowMs = 3 * KHATMA_DAY_MS
        assertEquals(0, khatmaDeficitAyahs(p, nowMs))
        assertEquals(khatmaTodayPortion(p, nowMs), khatmaCombinedPortion(p, nowMs))
    }

    @Test
    fun combinedAddsDeficitWhenBehind() {
        // Day 3 of 6 with 1 read: expected 3, deficit 2.
        val p = plan(currentAyahNo = 1, lastProgressMs = 1_000L, totalAyahsRead = 1)
        val nowMs = 3 * KHATMA_DAY_MS
        assertEquals(2, khatmaDeficitAyahs(p, nowMs))
        val today = khatmaTodayPortion(p, nowMs)!!
        val combined = khatmaCombinedPortion(p, nowMs)!!
        assertEquals(today.ayahCount + 2, combined.ayahCount)
        assertEquals(today.startSurahId to today.startAyahNo, combined.startSurahId to combined.startAyahNo)
    }

    @Test
    fun combinedIsWholeRemainderPastTarget() {
        val p = plan()
        val nowMs = 10 * KHATMA_DAY_MS
        val combined = khatmaCombinedPortion(p, nowMs)!!
        assertEquals(6, combined.ayahCount)
    }
}
