package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the Hifz profile: the mastery cycle, per-ayah statuses and their
 * derived flows, the daily goal, review counting, and the snapshot export.
 *
 * `HifzMasteryStore` is an `object` singleton whose `Settings` is a private
 * lazy no-arg factory with no injection hook (`HifzMasteryStore.kt:117`),
 * which throws on the unit-test classpath, so persistence is a silent no-op
 * here (same limitation as `OnboardingStoreTest`). These tests assert only
 * in-memory `StateFlow` transitions on provably fresh random owners.
 *
 * Deliberately NOT covered (untestable without production hooks — see the
 * report): `importSnapshot` (clears in-memory state, then replays through the
 * no-op `Settings`, so it wipes on this classpath;
 * `HifzMasteryStore.kt:352`), `setOwner` first-owner adoption (the carried
 * `readNamespace` always yields null without persistence;
 * `HifzMasteryStore.kt:159-168`), and streak day-rollover in `load`
 * (`HifzMasteryStore.kt:580-605`, real `Clock`, no injected clock).
 */
class HifzMasteryStoreTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    @BeforeTest
    fun setUp() {
        HifzMasteryStore.settingsFactory = { TestSettings() }
        HifzMasteryStore.resetSettingsCache()
        HifzMasteryStore.clearAll()
        HifzMasteryStore.setOwner("local")
        HifzMasteryStore.clearAll()
        HifzMasteryStore.setOwner(newOwnerId("hifz-owner"))
        HifzMasteryStore.clearAll()
    }

    @AfterTest
    fun tearDown() {
        HifzMasteryStore.clearAll()
        HifzMasteryStore.setOwner("local")
        HifzMasteryStore.clearAll()
    }

    // --- The cycle itself (pure, no store) ---

    @Test
    fun testMasteryStatusCyclesNewReviewMasteredNew() {
        assertEquals(MasteryStatus.REVIEW_NEEDED, MasteryStatus.NEW.next())
        assertEquals(MasteryStatus.MASTERED, MasteryStatus.REVIEW_NEEDED.next())
        assertEquals(MasteryStatus.NEW, MasteryStatus.MASTERED.next())
    }

    @Test
    fun testDefaultDailyGoalIsFive() {
        assertEquals(5, DEFAULT_HIFZ_DAILY_GOAL)
        assertEquals(DEFAULT_HIFZ_DAILY_GOAL, HifzMasteryStore.dailyGoalCount.value)
    }

    // --- Statuses ---

    @Test
    fun testUnsetAyahReadsAsNew() {
        assertEquals(MasteryStatus.NEW, HifzMasteryStore.getStatus(1, 1))
    }

    @Test
    fun testSetAndGetStatus() {
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.REVIEW_NEEDED)

        assertEquals(MasteryStatus.REVIEW_NEEDED, HifzMasteryStore.getStatus(1, 1))
        assertEquals(mapOf((1 to 1) to MasteryStatus.REVIEW_NEEDED), HifzMasteryStore.masteryMap.value)
    }

    @Test
    fun testSettingNewRemovesTheEntry() {
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.NEW)

        assertEquals(MasteryStatus.NEW, HifzMasteryStore.getStatus(1, 1))
        assertTrue(HifzMasteryStore.masteryMap.value.isEmpty())
    }

    @Test
    fun testCycleStatusWalksTheWholeRing() {
        assertEquals(MasteryStatus.REVIEW_NEEDED, HifzMasteryStore.cycleStatus(2, 3))
        assertEquals(MasteryStatus.MASTERED, HifzMasteryStore.cycleStatus(2, 3))
        assertEquals(MasteryStatus.NEW, HifzMasteryStore.cycleStatus(2, 3))
        assertEquals(MasteryStatus.NEW, HifzMasteryStore.getStatus(2, 3))
    }

    @Test
    fun testWeakAyahsAreSortedAndFilterableBySurah() {
        HifzMasteryStore.setStatus(2, 5, MasteryStatus.REVIEW_NEEDED)
        HifzMasteryStore.setStatus(1, 7, MasteryStatus.REVIEW_NEEDED)
        HifzMasteryStore.setStatus(1, 3, MasteryStatus.REVIEW_NEEDED)
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)

        assertEquals(
            listOf(1 to 3, 1 to 7, 2 to 5),
            HifzMasteryStore.getWeakAyahs(),
        )
        assertEquals(listOf(1 to 3, 1 to 7), HifzMasteryStore.getWeakAyahs(surahId = 1))
        assertEquals(listOf(2 to 5), HifzMasteryStore.getWeakAyahs(surahId = 2))
        assertTrue(HifzMasteryStore.getWeakAyahs(surahId = 9).isEmpty())
    }

    @Test
    fun testMasteredCountTracksTotalAndPerSurah() {
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(1, 2, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(2, 1, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(2, 2, MasteryStatus.REVIEW_NEEDED)

        assertEquals(3, HifzMasteryStore.getMasteredCount())
        assertEquals(3, HifzMasteryStore.totalMasteredCount.value)
        assertEquals(2, HifzMasteryStore.getMasteredCount(surahId = 1))
        assertEquals(1, HifzMasteryStore.getMasteredCount(surahId = 2))
        assertEquals(0, HifzMasteryStore.getMasteredCount(surahId = 9))
    }

    @Test
    fun testClearingAStatusUpdatesDerivedFlows() {
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(1, 2, MasteryStatus.REVIEW_NEEDED)
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.NEW)
        HifzMasteryStore.setStatus(1, 2, MasteryStatus.NEW)

        assertEquals(0, HifzMasteryStore.getMasteredCount())
        assertTrue(HifzMasteryStore.getWeakAyahs().isEmpty())
    }

    // --- Goal and reviews ---

    @Test
    fun testSetDailyGoalClampsToAtLeastOne() {
        HifzMasteryStore.setDailyGoal(0)
        assertEquals(1, HifzMasteryStore.dailyGoalCount.value)

        HifzMasteryStore.setDailyGoal(-10)
        assertEquals(1, HifzMasteryStore.dailyGoalCount.value)

        HifzMasteryStore.setDailyGoal(10)
        assertEquals(10, HifzMasteryStore.dailyGoalCount.value)
    }

    @Test
    fun testFirstReviewOfTheDayStartsStreakAtOne() {
        HifzMasteryStore.recordAyahReviewed(1, 1)

        assertEquals(1, HifzMasteryStore.todayReviewedCount.value)
        assertEquals(1, HifzMasteryStore.streakDays.value)
    }

    @Test
    fun testDailyGoalReachedComparesCountAgainstGoal() {
        assertFalse(HifzMasteryStore.isDailyGoalReached())

        HifzMasteryStore.setDailyGoal(2)
        HifzMasteryStore.recordAyahReviewed(1, 1)
        assertFalse(HifzMasteryStore.isDailyGoalReached())

        HifzMasteryStore.recordAyahReviewed(1, 2)
        assertTrue(HifzMasteryStore.isDailyGoalReached())
    }

    // --- SM-2-lite SRS (due days + intervals, additive in setStatus) ---

    @Test
    fun testMasteredDoublesIntervalEachTime() {
        HifzMasteryStore.setStatus(3, 7, MasteryStatus.MASTERED)
        assertEquals(2, HifzMasteryStore.getInterval(3, 7))
        val firstDue = HifzMasteryStore.getDueDay(3, 7)

        HifzMasteryStore.setStatus(3, 7, MasteryStatus.MASTERED)
        assertEquals(4, HifzMasteryStore.getInterval(3, 7))
        val secondDue = HifzMasteryStore.getDueDay(3, 7)

        // Same-day grading: due moves from today+2 to today+4.
        assertEquals(2, secondDue - firstDue)
    }

    @Test
    fun testReviewNeededResetsIntervalAndDueTomorrow() {
        HifzMasteryStore.setStatus(3, 7, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(3, 7, MasteryStatus.MASTERED)
        assertEquals(4, HifzMasteryStore.getInterval(3, 7))

        HifzMasteryStore.setStatus(3, 7, MasteryStatus.REVIEW_NEEDED)
        assertEquals(1, HifzMasteryStore.getInterval(3, 7))

        // A fresh REVIEW_NEEDED is also due tomorrow: same-day equality holds
        // without touching the real clock.
        HifzMasteryStore.setStatus(4, 9, MasteryStatus.REVIEW_NEEDED)
        assertEquals(1, HifzMasteryStore.getInterval(4, 9))
        assertEquals(HifzMasteryStore.getDueDay(4, 9), HifzMasteryStore.getDueDay(3, 7))
    }

    @Test
    fun testSettingNewClearsSrsState() {
        HifzMasteryStore.setStatus(5, 5, MasteryStatus.MASTERED)
        assertEquals(2, HifzMasteryStore.getInterval(5, 5))

        HifzMasteryStore.setStatus(5, 5, MasteryStatus.NEW)

        assertEquals(1, HifzMasteryStore.getInterval(5, 5))
        assertTrue(
            HifzMasteryStore.dueToday(Long.MAX_VALUE).none { it == (5 to 5) },
        )
    }

    @Test
    fun testDueTodayOrdersOverdueFirst() {
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.REVIEW_NEEDED) // due T+1
        HifzMasteryStore.setStatus(2, 5, MasteryStatus.MASTERED) // due T+2
        HifzMasteryStore.setStatus(1, 3, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(1, 3, MasteryStatus.MASTERED) // due T+4

        val tomorrow = HifzMasteryStore.getDueDay(1, 1)
        val plusTwo = HifzMasteryStore.getDueDay(2, 5)
        val plusFour = HifzMasteryStore.getDueDay(1, 3)

        assertTrue(HifzMasteryStore.dueToday(tomorrow - 1).isEmpty())
        assertEquals(listOf(1 to 1), HifzMasteryStore.dueToday(tomorrow))
        assertEquals(
            listOf(1 to 1, 2 to 5),
            HifzMasteryStore.dueToday(plusTwo),
        )
        // (1,1) is overdue by T+4 and still heads the queue; ties break by
        // surah/ayah order.
        assertEquals(
            listOf(1 to 1, 2 to 5, 1 to 3),
            HifzMasteryStore.dueToday(plusFour),
        )
        assertEquals(
            listOf(1 to 1, 2 to 5, 1 to 3),
            HifzMasteryStore.dueToday(plusFour + 30),
        )
    }

    @Test
    fun testSrsStateSurvivesSettingsReload() {
        val backing = TestSettings()
        HifzMasteryStore.settingsFactory = { backing }
        HifzMasteryStore.resetSettingsCache()
        HifzMasteryStore.setOwner("srs-roundtrip")
        HifzMasteryStore.clearAll()

        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)
        val due = HifzMasteryStore.getDueDay(1, 1)

        val snapshot = HifzMasteryStore.exportSnapshot()
        assertEquals(4, snapshot.intervals[1 to 1])
        assertEquals(due, snapshot.dueDays[1 to 1])

        // Bounce owners to force load() from the same Settings without
        // clearing it (account -> account carries nothing).
        HifzMasteryStore.setOwner("srs-bounce")
        assertEquals(1, HifzMasteryStore.getInterval(1, 1))
        HifzMasteryStore.setOwner("srs-roundtrip")

        assertEquals(4, HifzMasteryStore.getInterval(1, 1))
        assertEquals(due, HifzMasteryStore.getDueDay(1, 1))
        assertEquals(MasteryStatus.MASTERED, HifzMasteryStore.getStatus(1, 1))
        assertEquals(listOf(1 to 1), HifzMasteryStore.dueToday(due))
        assertTrue(HifzMasteryStore.dueToday(due - 1).none { it == (1 to 1) })
    }

    @Test
    fun testSurahMasteredPercentageUsesSeedAyahCounts() {
        // Al-Fatihah has 7 ayahs in the bundled seed data.
        assertEquals(0f, HifzMasteryStore.getSurahMasteredPercentage(999))
        assertEquals(0f, HifzMasteryStore.getSurahMasteredPercentage(1))

        for (ayah in 1..7) {
            HifzMasteryStore.setStatus(1, ayah, MasteryStatus.MASTERED)
        }
        assertEquals(1f, HifzMasteryStore.getSurahMasteredPercentage(1))
    }

    // --- Snapshot export / reset ---

    @Test
    fun testExportSnapshotReflectsInMemoryStateInSurahOrder() {
        HifzMasteryStore.setStatus(2, 5, MasteryStatus.REVIEW_NEEDED)
        HifzMasteryStore.setStatus(1, 7, MasteryStatus.MASTERED)
        HifzMasteryStore.setDailyGoal(9)

        val snapshot = HifzMasteryStore.exportSnapshot()

        assertEquals(
            listOf((1 to 7), (2 to 5)),
            snapshot.statuses.keys.toList(),
        )
        assertEquals(MasteryStatus.MASTERED, snapshot.statuses[1 to 7])
        assertEquals(9, snapshot.dailyGoalCount)
    }

    @Test
    fun testClearAllResetsStatusesGoalStreakAndCounts() {
        HifzMasteryStore.setStatus(1, 1, MasteryStatus.MASTERED)
        HifzMasteryStore.setStatus(1, 2, MasteryStatus.REVIEW_NEEDED)
        HifzMasteryStore.setDailyGoal(9)
        HifzMasteryStore.recordAyahReviewed(1, 1)

        HifzMasteryStore.clearAll()

        assertTrue(HifzMasteryStore.masteryMap.value.isEmpty())
        assertEquals(0, HifzMasteryStore.totalMasteredCount.value)
        assertTrue(HifzMasteryStore.weakAyahs.value.isEmpty())
        assertEquals(DEFAULT_HIFZ_DAILY_GOAL, HifzMasteryStore.dailyGoalCount.value)
        assertEquals(0, HifzMasteryStore.todayReviewedCount.value)
        assertEquals(0, HifzMasteryStore.streakDays.value)
        assertFalse(HifzMasteryStore.isDailyGoalReached())
    }
}

/**
 * In-memory [Settings] installed via [HifzMasteryStore.settingsFactory] so
 * persistence paths (day-rollover, goal count, streak) execute instead of
 * no-op throwing. Shape copied from ChartsRepository's InMemorySettings.
 */
private class TestSettings : Settings {
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
