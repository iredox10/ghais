package com.ghais.hifz.quests

import com.russhwolf.settings.Settings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AGENT A9 — guards the XP rate card, the pure Monday helpers, and the
 * Settings-backed ledger (weekly Monday reset included, driven through the
 * explicit-day seam so no real clock is involved).
 */
class HifzXpTest {

    private val backing = InMemoryXpSettings()

    @BeforeTest
    fun setUp() {
        backing.clear()
        HifzXpStore.settingsFactory = { backing }
        HifzXpStore.resetSettingsCache()
        HifzXpStore.clearAll()
    }

    @AfterTest
    fun tearDown() {
        HifzXpStore.clearAll()
    }

    @Test
    fun testXpRateCard() {
        assertEquals(10, xpFor(HifzEvent.REVIEW_DONE))
        assertEquals(20, xpFor(HifzEvent.AYAH_MASTERED))
        assertEquals(30, xpFor(HifzEvent.QUEST_DONE))
        assertEquals(200, xpFor(HifzEvent.SURAH_DONE))
    }

    @Test
    fun testMondayOfAlwaysLandsOnMonday() {
        for (day in 19_990L..20_010L) {
            val monday = mondayOf(day)
            assertEquals(0L, (monday + 3) % 7, "mondayOf($day) must be a Monday")
            assertTrue(monday <= day && day - monday < 7)
        }
    }

    @Test
    fun testWholeWeekSharesOneMonday() {
        val monday = mondayOf(20_000L)
        for (offset in 0L..6L) {
            assertEquals(monday, mondayOf(monday + offset))
        }
        assertEquals(monday + 7, mondayOf(monday + 7), "next Monday starts a new week")
    }

    @Test
    fun testNeedsWeeklyReset() {
        val monday = mondayOf(20_000L)
        assertFalse(needsWeeklyReset(monday, monday + 3))
        assertTrue(needsWeeklyReset(monday, monday + 7))
        assertTrue(needsWeeklyReset(HifzXpStore.NO_WEEK_STORED, monday))
    }

    @Test
    fun testAddXpAccumulatesTotalAndWeekly() {
        val wednesday = mondayOf(20_000L) + 2

        assertEquals(10, HifzXpStore.addXp(HifzEvent.REVIEW_DONE, wednesday))
        assertEquals(20, HifzXpStore.addXp(HifzEvent.AYAH_MASTERED, wednesday))

        assertEquals(30, HifzXpStore.totalXp.value)
        assertEquals(30, HifzXpStore.weeklyXp.value)
    }

    @Test
    fun testWeeklyTotalResetsOnMondayRollover() {
        val monday = mondayOf(20_000L)

        HifzXpStore.addXp(HifzEvent.QUEST_DONE, monday + 2)
        assertEquals(30, HifzXpStore.weeklyXp.value)

        HifzXpStore.addXp(HifzEvent.SURAH_DONE, monday + 7)

        assertEquals(200, HifzXpStore.weeklyXp.value, "new week starts from this event only")
        assertEquals(230, HifzXpStore.totalXp.value, "lifetime total keeps everything")
    }

    @Test
    fun testRefreshAppliesRolloverWithoutAwarding() {
        val monday = mondayOf(20_000L)
        HifzXpStore.addXp(HifzEvent.REVIEW_DONE, monday + 1)
        assertEquals(10, HifzXpStore.weeklyXp.value)

        HifzXpStore.refresh(monday + 8)

        assertEquals(0, HifzXpStore.weeklyXp.value)
        assertEquals(10, HifzXpStore.totalXp.value)
    }

    @Test
    fun testClearAllResetsBothCounters() {
        val monday = mondayOf(20_000L)
        HifzXpStore.addXp(HifzEvent.SURAH_DONE, monday)

        HifzXpStore.clearAll()

        assertEquals(0, HifzXpStore.totalXp.value)
        assertEquals(0, HifzXpStore.weeklyXp.value)
    }
}

/** In-memory [Settings]; shape copied from the HifzMasteryStore test seam. */
private class InMemoryXpSettings : Settings {
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
