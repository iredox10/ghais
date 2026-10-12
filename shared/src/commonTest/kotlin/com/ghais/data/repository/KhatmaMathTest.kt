package com.ghais.data.repository

import com.ghais.domain.model.KhatmaPlan
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the pure khatma math in `KhatmaStore.kt` (portion splits, cursor
 * motion, plan-aware progress, pages/day + ETA) plus the additive store
 * wrappers ([KhatmaStore.create], [KhatmaStore.createForTargetDate],
 * [KhatmaStore.recordPortionDone]).
 *
 * All date math uses fixed epoch millis (no injected clock needed — every
 * helper takes `nowMs`). Store tests reuse the random-owner isolation from
 * `KhatmaStoreTest` (persistence itself stays a silent no-op on this
 * classpath; only in-memory transitions are asserted).
 */
class KhatmaMathTest {

    private fun plan(
        id: String = "m",
        startDateMs: Long = 0L,
        targetDays: Int = 30,
        currentSurahId: Int = 1,
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

    // --- Even split -------------------------------------------------------

    @Test
    fun portionSizes_evenSplit() {
        assertEquals(List(10) { 10 }, khatmaPortionSizes(100, 10))
    }

    @Test
    fun portionSizes_sumIsPreserved() {
        assertEquals(6236, khatmaPortionSizes(6236, 30).sum())
        assertEquals(30, khatmaPortionSizes(6236, 30).size)
    }

    // --- Remainder distribution (earliest days get the extra ayah) --------

    @Test
    fun portionSizes_remainderGoesToEarliestDays() {
        assertEquals(listOf(4, 3, 3), khatmaPortionSizes(10, 3))
    }

    @Test
    fun portionSizes_fullQuranRemainder() {
        val sizes = khatmaPortionSizes(6236, 30)
        // 6236 = 207 * 30 + 26: first 26 days read 208, last 4 read 207.
        assertEquals(List(26) { 208 } + List(4) { 207 }, sizes)
    }

    // --- Single-day + degenerate inputs ------------------------------------

    @Test
    fun portionSizes_singleDayTakesAll() {
        assertEquals(listOf(7), khatmaPortionSizes(7, 1))
    }

    @Test
    fun portionSizes_nonPositiveDaysClampToSingleDay() {
        assertEquals(listOf(7), khatmaPortionSizes(7, 0))
        assertEquals(listOf(7), khatmaPortionSizes(7, -3))
    }

    @Test
    fun portionSizes_zeroTotalYieldsNoDays() {
        assertTrue(khatmaPortionSizes(0, 5).isEmpty())
    }

    // --- Past-target clamping ----------------------------------------------

    @Test
    fun todayPortion_pastTargetDueAllRemainder() {
        // Target was 30 days; now is day 40 with nothing read.
        val p = plan(targetDays = 30, startDateMs = 0L)
        val now = 40 * KHATMA_DAY_MS
        assertEquals(0, khatmaDaysLeft(p, now))

        val portion = khatmaTodayPortion(p, now)
        val remaining = khatmaRemainingAyahs(p)
        assertEquals(6236, remaining)
        assertEquals(remaining, portion?.ayahCount)
        assertEquals(1 to 1, portion?.let { it.startSurahId to it.startAyahNo })
        assertEquals(114 to 6, portion?.let { it.endSurahId to it.endAyahNo })
    }

    // --- First-day portion ---------------------------------------------------

    @Test
    fun todayPortion_firstDayStartsAtPlanStart() {
        val p = plan(targetDays = 30, startDateMs = 0L)
        val portion = khatmaTodayPortion(p, 0L)
        // 6236 over 30 days: today (earliest) gets 208.
        assertEquals(208, portion?.ayahCount)
        assertEquals(1 to 1, portion?.let { it.startSurahId to it.startAyahNo })
        // 208 ayahs from 1:1 end at 2:201 (Al-Fatihah's 7 + 201 of Al-Baqarah).
        assertEquals(2 to 201, portion?.let { it.endSurahId to it.endAyahNo })
    }

    @Test
    fun todayPortion_advancesAfterProgress() {
        // Read the first 7 ayahs (all of Al-Fatihah), cursor now at 2:1.
        val p = plan(
            targetDays = 30,
            startDateMs = 0L,
            currentSurahId = 1,
            currentAyahNo = 7,
            lastProgressMs = 1_000L,
            totalAyahsRead = 7,
        )
        assertEquals(2 to 1, khatmaCursorOf(p))
        assertEquals(6236 - 7, khatmaRemainingAyahs(p))
        val portion = khatmaTodayPortion(p, 0L)
        assertEquals(2 to 1, portion?.let { it.startSurahId to it.startAyahNo })
    }

    @Test
    fun todayPortion_completePlanIsNull() {
        val p = plan(
            currentSurahId = 114,
            currentAyahNo = 6,
            lastProgressMs = 1_000L,
            totalAyahsRead = 6236,
        )
        assertTrue(khatmaIsComplete(p))
        assertNull(khatmaTodayPortion(p, 1_000L))
    }

    // --- Cursor motion -------------------------------------------------------

    @Test
    fun cursor_neverProgressedIsTheStart() {
        assertEquals(2 to 142, khatmaCursorOf(plan(currentSurahId = 2, currentAyahNo = 142)))
    }

    @Test
    fun nextAyah_crossesSurahBoundaryAndEndsAtQuranEnd() {
        assertEquals(2 to 1, khatmaNextAyah(1, 7))
        assertEquals(1 to 2, khatmaNextAyah(1, 1))
        assertNull(khatmaNextAyah(114, 6))
        assertNull(khatmaNextAyah(0, 1))
    }

    @Test
    fun advance_spansSurahs() {
        assertEquals(2 to 1, khatmaAdvanceOrNull(1, 7, 2))
        assertNull(khatmaAdvanceOrNull(1, 1, 0))
    }

    @Test
    fun ayahCountBetween_matchesPageCountContract() {
        assertEquals(1, khatmaAyahCountBetween(1, 1, 1, 1))
        assertEquals(6236, khatmaAyahCountBetween(1, 1, 114, 6))
        assertEquals(0, khatmaAyahCountBetween(114, 6, 1, 1))
        assertEquals(0, khatmaAyahCountBetween(0, 1, 1, 1))
    }

    // --- Progress accumulation (pure) ----------------------------------------

    @Test
    fun planProgress_freshPlanIsZero() {
        assertEquals(0f, khatmaPlanProgress(plan()))
    }

    @Test
    fun planProgress_accumulatesTowardOne() {
        val half = plan(
            currentSurahId = 1,
            currentAyahNo = 7,
            lastProgressMs = 1_000L,
            totalAyahsRead = 7,
        )
        // 7 read, 6229 remain.
        assertEquals(6229, khatmaRemainingAyahs(half))
        assertEquals(7f / 6236f, khatmaPlanProgress(half), 1e-6f)

        val mid = plan(
            currentSurahId = 2,
            currentAyahNo = 10,
            lastProgressMs = 2_000L,
            totalAyahsRead = 17,
        )
        assertEquals(17, mid.totalAyahsRead)
        assertTrue(khatmaPlanProgress(mid) > khatmaPlanProgress(half))
    }

    @Test
    fun planProgress_customStartIsPlanAware() {
        // A plan starting at 114:1 has 6 ayahs total; 3 read = half done,
        // while the model's full-Quran percentage would read ~0.
        val p = plan(
            currentSurahId = 114,
            currentAyahNo = 3,
            lastProgressMs = 1_000L,
            totalAyahsRead = 3,
        )
        assertEquals(3, khatmaRemainingAyahs(p))
        assertEquals(0.5f, khatmaPlanProgress(p), 1e-6f)
    }

    @Test
    fun doneToday_tracksEpochDay() {
        val day = 5 * KHATMA_DAY_MS
        val done = plan(lastProgressMs = day + 1_000L)
        assertTrue(khatmaDoneToday(done, day + 9_999L))
        assertTrue(!khatmaDoneToday(done, day + KHATMA_DAY_MS))
        assertTrue(!khatmaDoneToday(plan(), day))
    }

    // --- Pages/day + ETA -----------------------------------------------------

    @Test
    fun pagesPerDay_fullQuranThirtyDays() {
        // 604 pages over 30 days ceilings to 21.
        assertEquals(21, khatmaPagesPerDay(plan(targetDays = 30)))
    }

    @Test
    fun pagesPerDay_singleDayTakesAllPages() {
        assertEquals(604, khatmaPagesPerDay(plan(targetDays = 1)))
    }

    @Test
    fun etaDate_fullQuranThirtyDays() {
        val p = plan(targetDays = 30, startDateMs = 0L)
        // 604 pages remain at 21/day: ceil(604 / 21) = 29 days.
        assertEquals(29 * KHATMA_DAY_MS, khatmaEtaDateMs(p, 0L))
    }

    @Test
    fun etaDate_completePlanIsNow() {
        val p = plan(
            currentSurahId = 114,
            currentAyahNo = 6,
            lastProgressMs = 1_000L,
            totalAyahsRead = 6236,
        )
        assertEquals(9_999L, khatmaEtaDateMs(p, 9_999L))
    }

    @Test
    fun etaLabel_knownDates() {
        assertEquals("Est. Jan 1", khatmaEtaLabel(0L))
        assertEquals("Est. Jan 30", khatmaEtaLabel(29 * KHATMA_DAY_MS))
        // 10^12 ms = 2001-09-09T01:46:40Z.
        assertEquals("Est. Sep 9", khatmaEtaLabel(1_000_000_000_000L))
        assertEquals("Jan 1", khatmaShortDate(0L))
    }

    // --- Additive store wrappers (in-memory transitions only) -----------------

    @BeforeTest
    fun setUp() {
        KhatmaStore.setOwner("khatma-math-${Random.nextLong().toString(16)}")
        KhatmaStore.clear()
    }

    @AfterTest
    fun tearDown() {
        KhatmaStore.clear()
        KhatmaStore.setOwner("local")
    }

    @Test
    fun create_persistsAndBecomesActive() {
        val stored = KhatmaStore.create("Ramadan", startSurahId = 2, startAyahNo = 142, targetDays = 30)

        assertEquals("Ramadan", stored.title)
        assertEquals(2, stored.currentSurahId)
        assertEquals(142, stored.currentAyahNo)
        assertEquals(30, stored.targetDays)
        assertEquals(stored.id, KhatmaStore.activePlan()?.id)
    }

    @Test
    fun create_clampsStartAndBlanks() {
        val stored = KhatmaStore.create("", startSurahId = 999, startAyahNo = -4, targetDays = 0)

        assertEquals("My Khatma", stored.title)
        assertEquals(114, stored.currentSurahId)
        assertEquals(1, stored.currentAyahNo)
        assertEquals(1, stored.targetDays)
    }

    @Test
    fun createForTargetDate_ceilsToWholeDays() {
        val now = 10 * KHATMA_DAY_MS
        val threeDays = KhatmaStore.createForTargetDate("T", targetDateMs = now + 2 * KHATMA_DAY_MS + 1, nowMs = now)
        assertEquals(3, threeDays.targetDays)

        val past = KhatmaStore.createForTargetDate("P", targetDateMs = now - 1, nowMs = now)
        assertEquals(1, past.targetDays)
    }

    @Test
    fun recordPortionDone_advancesCursorAndTotals() {
        val created = KhatmaStore.create("P", targetDays = 30)
        val portion = khatmaTodayPortion(created, created.startDateMs) ?: error("no portion")

        KhatmaStore.recordPortionDone(created.id, portion)

        val stored = KhatmaStore.get(created.id)
        assertEquals(portion.endSurahId, stored?.currentSurahId)
        assertEquals(portion.endAyahNo, stored?.currentAyahNo)
        assertEquals(portion.ayahCount, stored?.totalAyahsRead)
    }
}
