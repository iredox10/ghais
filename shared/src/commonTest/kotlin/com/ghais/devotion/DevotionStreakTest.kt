package com.ghais.devotion

import com.russhwolf.settings.Settings
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AGENT A7 — guards the gentle devotion streak:
 * - pure day-rollover math ([evaluateStreak]);
 * - grace consumption, banking and the cap of 2;
 * - restore math (3 gentle days pick up where you left off);
 * - the persisted store path ([DevotionStreak.recordDevotionDay]) on an
 *   in-memory Settings with explicit epoch-days (no real clock).
 */
class DevotionStreakTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    @BeforeTest
    fun setUp() {
        DevotionStreak.settingsFactory = { TestDevotionSettings() }
        DevotionStreak.resetSettingsCache()
        DevotionStreak.clearAll()
        DevotionStreak.setOwner("local")
        DevotionStreak.clearAll()
        DevotionStreak.setOwner(newOwnerId("devotion-owner"))
        DevotionStreak.clearAll()
    }

    @AfterTest
    fun tearDown() {
        DevotionStreak.clearAll()
        DevotionStreak.setOwner("local")
        DevotionStreak.clearAll()
    }

    // --- Pure day math ---

    @Test
    fun testFirstDayIsFreshStart() {
        assertEquals(StreakResult.FRESH_START, evaluateStreak(lastDay = 0L, today = 100L, graceDays = 0))
    }

    @Test
    fun testSameDayIsNoOp() {
        assertEquals(StreakResult.SAME_DAY, evaluateStreak(lastDay = 100L, today = 100L, graceDays = 2))
    }

    @Test
    fun testClockSkewIsNoOp() {
        assertEquals(StreakResult.SAME_DAY, evaluateStreak(lastDay = 105L, today = 100L, graceDays = 0))
    }

    @Test
    fun testConsecutiveDayIncrements() {
        assertEquals(StreakResult.INCREMENT, evaluateStreak(lastDay = 100L, today = 101L, graceDays = 0))
    }

    @Test
    fun testOneMissedDayConsumesGrace() {
        assertEquals(StreakResult.GRACE_USED, evaluateStreak(lastDay = 100L, today = 102L, graceDays = 1))
    }

    @Test
    fun testOneMissedDayWithoutGraceIsRecoverableBreak() {
        assertEquals(
            StreakResult.BROKEN_RECOVERABLE,
            evaluateStreak(lastDay = 100L, today = 102L, graceDays = 0),
        )
    }

    @Test
    fun testLongerGapIsRecoverableEvenWithGrace() {
        // A grace day covers exactly one missed day — never a longer absence.
        assertEquals(
            StreakResult.BROKEN_RECOVERABLE,
            evaluateStreak(lastDay = 100L, today = 103L, graceDays = 2),
        )
        assertEquals(
            StreakResult.BROKEN_RECOVERABLE,
            evaluateStreak(lastDay = 100L, today = 200L, graceDays = 2),
        )
    }

    // --- Grace economy ---

    @Test
    fun testGraceEarnedEverySeventhDayAndCappedAtTwo() {
        assertEquals(0, earnGraceForWeek(currentGrace = 0, streakCount = 6))
        assertEquals(1, earnGraceForWeek(currentGrace = 0, streakCount = 7))
        assertEquals(1, earnGraceForWeek(currentGrace = 0, streakCount = 14))
        assertEquals(2, earnGraceForWeek(currentGrace = 1, streakCount = 7))
        // Cap holds: a 7-day boundary at a full balance stays at 2.
        assertEquals(2, earnGraceForWeek(currentGrace = 2, streakCount = 21))
        assertEquals(2, earnGraceForWeek(currentGrace = 9, streakCount = 7))
    }

    // --- Restore math ---

    @Test
    fun testRestoreProgressIsClampedFraction() {
        assertEquals(0f, restoreProgress(done = 0))
        assertEquals(1f / 3f, restoreProgress(done = 1))
        assertEquals(2f / 3f, restoreProgress(done = 2))
        assertEquals(1f, restoreProgress(done = 3))
        assertEquals(1f, restoreProgress(done = 9))
        assertEquals(0f, restoreProgress(done = -2))
    }

    @Test
    fun testRestoredCountPicksUpWhereItLeftOff() {
        assertEquals(13, restoredDevotionCount(restoreTarget = 10, gentleDone = 3))
        assertEquals(10, restoredDevotionCount(restoreTarget = 10, gentleDone = 0))
        assertFalse(isRestoreComplete(done = 2))
        assertTrue(isRestoreComplete(done = 3))
    }

    @Test
    fun testMilestoneShareTextNamesTheCount() {
        val text = devotionMilestoneShareText(30)
        assertTrue(text.contains("30"))
        assertTrue(text.contains("Quran", ignoreCase = true))
    }

    @Test
    fun testGraceIndicatorCopyIsAlwaysReassuring() {
        assertTrue(graceIndicatorText(days = 10, grace = 1).contains("your 10 days are safe"))
        assertTrue(graceIndicatorText(days = 5, grace = 0).contains("Every day counts"))
        assertTrue(graceIndicatorText(days = 0, grace = 0).contains("Start"))
        // Never guilt/freeze language.
        for (copy in listOf(
            graceIndicatorText(10, 1),
            graceIndicatorText(5, 0),
            restoreHeadlineText(10),
        )) {
            assertFalse(copy.contains("freeze", ignoreCase = true))
            assertFalse(copy.contains("broken", ignoreCase = true))
            assertFalse(copy.contains("lost", ignoreCase = true))
        }
    }

    // --- Store path (explicit epoch-days, in-memory Settings) ---

    @Test
    fun testStoreFreshStartThenIncrement() {
        assertEquals(StreakResult.FRESH_START, DevotionStreak.recordDevotionDay(500L))
        assertEquals(1, DevotionStreak.devotionDays.value)

        // Same day: no-op, stays at 1.
        assertEquals(StreakResult.SAME_DAY, DevotionStreak.recordDevotionDay(500L))
        assertEquals(1, DevotionStreak.devotionDays.value)

        assertEquals(StreakResult.INCREMENT, DevotionStreak.recordDevotionDay(501L))
        assertEquals(2, DevotionStreak.devotionDays.value)
    }

    @Test
    fun testStoreBanksGraceAtSevenAndConsumesItOnASkippedDay() {
        // Days 600..606 build a 7-day streak -> 1 grace day banked.
        for (day in 600L..606L) DevotionStreak.recordDevotionDay(day)
        assertEquals(7, DevotionStreak.devotionDays.value)
        assertEquals(1, DevotionStreak.graceDays.value)

        // Skip 607, record 608: grace covers the single missed day.
        assertEquals(StreakResult.GRACE_USED, DevotionStreak.recordDevotionDay(608L))
        assertEquals(8, DevotionStreak.devotionDays.value)
        assertEquals(0, DevotionStreak.graceDays.value)
    }

    @Test
    fun testStoreBreakKeepsTargetAndRestoresAfterThreeGentleDays() {
        for (day in 700L..704L) DevotionStreak.recordDevotionDay(day)
        assertEquals(5, DevotionStreak.devotionDays.value)

        // Long gap with no grace banked: recoverable break, restarts at 1,
        // keeps the 5-day target with the first gentle day done.
        assertEquals(StreakResult.BROKEN_RECOVERABLE, DevotionStreak.recordDevotionDay(710L))
        assertEquals(1, DevotionStreak.devotionDays.value)
        assertEquals(5, DevotionStreak.restoreTarget.value)
        assertEquals(1, DevotionStreak.restoreDone.value)
        assertTrue(DevotionStreak.isRestoring())

        // Two more gentle days complete the restore: 5 + 3 = 8.
        DevotionStreak.recordDevotionDay(711L)
        assertTrue(DevotionStreak.isRestoring())
        DevotionStreak.recordDevotionDay(712L)
        assertEquals(8, DevotionStreak.devotionDays.value)
        assertEquals(0, DevotionStreak.restoreTarget.value)
        assertFalse(DevotionStreak.isRestoring())
    }

    @Test
    fun testStoreMilestonePendsOnceAndClearsOnCelebrate() {
        for (day in 800L..806L) DevotionStreak.recordDevotionDay(day)
        assertEquals(7, DevotionStreak.pendingMilestone())

        DevotionStreak.markMilestoneCelebrated(7)
        assertNull(DevotionStreak.pendingMilestone())
    }
}

/**
 * In-memory [Settings] installed via [DevotionStreak.settingsFactory].
 * Shape copied from `HifzMasteryStoreTest.TestSettings`.
 */
private class TestDevotionSettings : Settings {
    private val values = mutableMapOf<String, Any?>()

    override val keys: Set<String> get() = values.keys
    override val size: Int get() = values.size

    override fun clear() {
        values.clear()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String): String =
        values[key] as? String ?: defaultValue

    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        values[key] as? Double ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}
