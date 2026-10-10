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
 * Guards the khatma plan list: bound normalization, newest-first ordering,
 * progress recording, remove/complete, and bulk restore.
 *
 * `KhatmaStore` is an `object` singleton whose `Settings` is a private lazy
 * no-arg factory with no injection hook (`KhatmaStore.kt:37`), which throws
 * on the unit-test classpath, so persistence is a silent no-op here (same
 * limitation as `OnboardingStoreTest`). These tests assert only in-memory
 * list transitions on provably fresh random owners; persistence roundtrip is
 * deliberately NOT asserted.
 *
 * Time note: `recordProgress` stamps `Clock.System.now()` directly
 * (`KhatmaStore.kt:97`) with no injected clock, so streak day-rollover paths
 * cannot be forced — the tests below pin the deterministic core (position,
 * totals, first-day streak) and use fixed epoch millis everywhere else.
 */
class KhatmaStoreTest {

    private companion object {
        const val DAY_MS = 86_400_000L
    }

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    private fun plan(
        id: String,
        startDateMs: Long = 3 * DAY_MS,
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

    @BeforeTest
    fun setUp() {
        KhatmaStore.setOwner(newOwnerId("khatma-owner"))
        KhatmaStore.clear()
    }

    @AfterTest
    fun tearDown() {
        KhatmaStore.clear()
        KhatmaStore.setOwner("local")
    }

    @Test
    fun testAddNormalizesBounds() {
        val stored = KhatmaStore.add(
            plan(
                "bounds",
                targetDays = 0,
                currentSurahId = 200,
                currentAyahNo = 0,
                streak = -3,
                totalAyahsRead = -5,
            )
        )

        assertEquals(1, stored.targetDays)
        assertEquals(114, stored.currentSurahId)
        assertEquals(1, stored.currentAyahNo)
        assertEquals(0, stored.streak)
        assertEquals(0, stored.totalAyahsRead)
    }

    @Test
    fun testPlansAreNewestFirstAndActivePlanIsTheMostRecent() {
        KhatmaStore.add(plan("old", startDateMs = DAY_MS))
        KhatmaStore.add(plan("new", startDateMs = 3 * DAY_MS))
        KhatmaStore.add(plan("mid", startDateMs = 2 * DAY_MS))

        assertEquals(listOf("new", "mid", "old"), KhatmaStore.plans.value.map { it.id })
        assertEquals("new", KhatmaStore.activePlan()?.id)
    }

    @Test
    fun testActivePlanIsNullWhenEmpty() {
        assertEquals(null, KhatmaStore.activePlan())
        assertNull(KhatmaStore.get("missing"))
    }

    @Test
    fun testAddWithAnExistingIdReplaces() {
        KhatmaStore.add(plan("same", startDateMs = DAY_MS, targetDays = 30))
        KhatmaStore.add(plan("same", startDateMs = DAY_MS, targetDays = 60))

        assertEquals(1, KhatmaStore.plans.value.size)
        assertEquals(60, KhatmaStore.get("same")?.targetDays)
    }

    @Test
    fun testUpdateReplacesWithNormalization() {
        KhatmaStore.add(plan("u", targetDays = 30))
        KhatmaStore.update(plan("u", targetDays = 0))

        assertEquals(1, KhatmaStore.plans.value.single().targetDays)
    }

    @Test
    fun testRemoveUnknownIdIsANoOp() {
        KhatmaStore.add(plan("a"))
        KhatmaStore.remove("missing")

        assertEquals(listOf("a"), KhatmaStore.plans.value.map { it.id })
    }

    @Test
    fun testRemoveAndCompleteDeleteThePlan() {
        KhatmaStore.add(plan("a"))
        KhatmaStore.add(plan("b"))

        KhatmaStore.remove("a")
        assertEquals(listOf("b"), KhatmaStore.plans.value.map { it.id })

        KhatmaStore.complete("b")
        assertTrue(KhatmaStore.plans.value.isEmpty())
        assertEquals(null, KhatmaStore.activePlan())
    }

    @Test
    fun testRestoreAllNormalizesSortsAndReplaces() {
        KhatmaStore.add(plan("stale", startDateMs = 9 * DAY_MS))
        KhatmaStore.restoreAll(
            listOf(
                plan("new", startDateMs = 3 * DAY_MS, targetDays = 0),
                plan("old", startDateMs = DAY_MS),
            )
        )

        assertEquals(listOf("new", "old"), KhatmaStore.plans.value.map { it.id })
        assertEquals(1, KhatmaStore.get("new")?.targetDays)
    }

    @Test
    fun testRestoreAllEmptyClears() {
        KhatmaStore.add(plan("a"))
        KhatmaStore.restoreAll(emptyList())

        assertTrue(KhatmaStore.plans.value.isEmpty())
    }

    @Test
    fun testRecordProgressUnknownIdIsANoOp() {
        KhatmaStore.add(plan("a", totalAyahsRead = 4))
        KhatmaStore.recordProgress("missing", surahId = 2, ayahNo = 10, ayahsRead = 5)

        assertEquals(4, KhatmaStore.get("a")?.totalAyahsRead)
        assertEquals(1, KhatmaStore.plans.value.size)
    }

    @Test
    fun testRecordProgressAdvancesPositionTotalsAndFirstDayStreak() {
        KhatmaStore.add(plan("p", streak = 0, lastProgressMs = 0L, totalAyahsRead = 0))

        KhatmaStore.recordProgress("p", surahId = 2, ayahNo = 10, ayahsRead = 5)

        val stored = KhatmaStore.get("p")
        assertEquals(2, stored?.currentSurahId)
        assertEquals(10, stored?.currentAyahNo)
        assertEquals(5, stored?.totalAyahsRead)
        assertTrue((stored?.lastProgressMs ?: 0L) > 0L)
        // `lastProgressMs <= 0` counts as a new day, so the first record
        // always starts the streak at 1.
        assertEquals(1, stored?.streak)
    }

    @Test
    fun testRecordProgressCoercesNegativeAyahsReadToZero() {
        KhatmaStore.add(plan("p", totalAyahsRead = 3))

        KhatmaStore.recordProgress("p", surahId = 2, ayahNo = 10, ayahsRead = -4)

        val stored = KhatmaStore.get("p")
        assertEquals(3, stored?.totalAyahsRead)
        assertEquals(2, stored?.currentSurahId)
    }

    @Test
    fun testRecordProgressAccumulatesTotals() {
        KhatmaStore.add(plan("p"))

        KhatmaStore.recordProgress("p", surahId = 2, ayahNo = 10, ayahsRead = 5)
        KhatmaStore.recordProgress("p", surahId = 2, ayahNo = 20, ayahsRead = 10)

        assertEquals(15, KhatmaStore.get("p")?.totalAyahsRead)
        assertEquals(20, KhatmaStore.get("p")?.currentAyahNo)
    }

    @Test
    fun testDaysRemainingUsesFixedEpochMillis() {
        val stored = KhatmaStore.add(
            plan("d", targetDays = 30, startDateMs = 0L, lastProgressMs = 10 * DAY_MS)
        )

        assertEquals(20, stored.daysRemaining)
    }

    @Test
    fun testProgressPercentageClampsToUnitRange() {
        assertEquals(0f, plan("z", totalAyahsRead = 0).progressPercentage)
        assertEquals(1f, plan("full", totalAyahsRead = 6236).progressPercentage)
        assertEquals(1f, plan("over", totalAyahsRead = 99_999).progressPercentage)
    }

    @Test
    fun testNewIdHasTheKhatmaPrefixAndIsUnique() {
        val first = KhatmaStore.newId()
        val second = KhatmaStore.newId()

        assertTrue(first.startsWith("kht-"), "Unexpected id shape: $first")
        assertTrue(first != second, "Two ids minted in the same millisecond collided")
    }
}
