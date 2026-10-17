package com.ghais.ui.screens.reciters.today

import com.ghais.data.seed.JumpBackInItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WeeklyTopTest {

    private val day = YESTERDAY_MILLIS_PER_DAY
    private val week = WEEK_MILLIS
    // Arbitrary positive anchor: window = [nowMs - week, nowMs).
    private val nowMs = 30 * week + 12_000L

    private fun item(slug: String, ts: Long) = JumpBackInItem(
        title = "t",
        subtitle = "s",
        progress = 0f,
        coverUrl = "",
        reciterSlug = slug,
        lastPlayedTimestampMs = ts,
    )

    @Test
    fun testEmptyHistoryIsNull() {
        assertNull(weeklyTop(emptyList(), nowMs))
    }

    @Test
    fun testNonPositiveNowIsNull() {
        val rows = listOf(item("aaa", nowMs - 1))
        assertNull(weeklyTop(rows, 0L))
        assertNull(weeklyTop(rows, -1L))
    }

    @Test
    fun testNonPositiveLimitIsNull() {
        val rows = listOf(item("aaa", nowMs - 1))
        assertNull(weeklyTop(rows, nowMs, limit = 0))
        assertNull(weeklyTop(rows, nowMs, limit = -3))
    }

    @Test
    fun testRowsInsideWindowAreCounted() {
        val rows = listOf(
            item("aaa", nowMs - 1),
            item("aaa", nowMs - day),
            item("bbb", nowMs - 2 * day),
        )
        assertEquals(listOf("aaa", "bbb"), weeklyTop(rows, nowMs))
    }

    @Test
    fun testRowsOlderThanWeekExcluded() {
        // One week old minus one ms is out; inside minus one ms is in.
        val rows = listOf(
            item("aaa", nowMs - week - 1),
            item("bbb", nowMs - week + 1),
        )
        assertEquals(listOf("bbb"), weeklyTop(rows, nowMs))
    }

    @Test
    fun testFutureRowsExcluded() {
        val rows = listOf(
            item("aaa", nowMs + 1),
            item("bbb", nowMs + day),
        )
        assertNull(weeklyTop(rows, nowMs))
    }

    @Test
    fun testBoundaries() {
        // Half-open: from (nowMs - week) inclusive, to (nowMs) exclusive.
        val rows = listOf(item("aaa", nowMs - week), item("bbb", nowMs))
        assertEquals(listOf("aaa"), weeklyTop(rows, nowMs))
    }

    @Test
    fun testRanksByCountThenRecency() {
        val rows = listOf(
            item("bbb", nowMs - week),
            item("aaa", nowMs - day - 2),
            item("aaa", nowMs - day - 1),
            item("aaa", nowMs - day),
        )
        assertEquals(listOf("aaa", "bbb"), weeklyTop(rows, nowMs))
    }

    @Test
    fun testTieBreaksByRecency() {
        val rows = listOf(
            item("aaa", nowMs - 3 * day),
            item("bbb", nowMs - 2 * day),
        )
        assertEquals(listOf("bbb", "aaa"), weeklyTop(rows, nowMs))
    }

    @Test
    fun testLimitTruncates() {
        val rows = listOf(
            item("aaa", nowMs - 1),
            item("bbb", nowMs - 2),
            item("ccc", nowMs - 3),
        )
        assertEquals(listOf("aaa", "bbb"), weeklyTop(rows, nowMs, limit = 2))
        assertEquals(listOf("aaa"), weeklyTop(rows, nowMs, limit = 1))
    }

    @Test
    fun testExcludesBlankAndDevotional() {
        val rows = listOf(
            item("", nowMs - 1),
            item("   ", nowMs - 2),
            item("__devotional", nowMs - 3),
            item("aaa", nowMs - 4),
        )
        assertEquals(listOf("aaa"), weeklyTop(rows, nowMs))
    }

    @Test
    fun testNeverThrowsOnGarbageTimestamps() {
        // Extreme/garbage timestamps must not crash — underflow subtraction,
        // Long.MIN_VALUE / MAX_VALUE values, negative nowMs — result is null
        // or a sane list, never an exception.
        assertNull(weeklyTop(listOf(item("aaa", Long.MIN_VALUE)), nowMs))
        assertNull(weeklyTop(listOf(item("aaa", Long.MAX_VALUE)), nowMs))
        assertNull(weeklyTop(listOf(item("aaa", -1L)), nowMs))
        assertNull(
            weeklyTop(
                listOf(
                    item("", Long.MAX_VALUE),
                    item("__devotional", Long.MIN_VALUE),
                ),
                nowMs,
            )
        )
        // Long.MIN_VALUE timestamp + negative offset can't overflow here
        // (from = nowMs - week stays positive for positive nowMs), but the
        // guard covers any future rework too.
        assertNull(weeklyTop(listOf(item("aaa", Long.MIN_VALUE)), 1L))
        assertNull(weeklyTop(listOf(item("aaa", Long.MAX_VALUE)), 1L))
    }

    @Test
    fun testEntriesCarryPlaysAndLastPlayed() {
        val rows = listOf(
            item("aaa", nowMs - day - 2),
            item("aaa", nowMs - day),
            item("bbb", nowMs - 5 * day),
        )
        val entries = weeklyTopEntries(rows, nowMs)!!
        assertEquals(
            listOf(
                WeeklyTopEntry("aaa", 2, nowMs - day),
                WeeklyTopEntry("bbb", 1, nowMs - 5 * day),
            ),
            entries,
        )
    }

    @Test
    fun testWeeklyTopMirrorsEntriesSlugs() {
        val rows = listOf(
            item("aaa", nowMs - 1),
            item("bbb", nowMs - day),
        )
        assertEquals(
            weeklyTopEntries(rows, nowMs)!!.map { it.slug },
            weeklyTop(rows, nowMs),
        )
    }
}
