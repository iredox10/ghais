package com.ghais.ui.screens.reciters.today

import com.ghais.data.seed.JumpBackInItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YesterdayTopTest {

    private val day = YESTERDAY_MILLIS_PER_DAY
    // Tuesday 00:00 UTC-ish anchor: today = day 3, yesterday = [2d, 3d).
    private val nowMs = 3 * day + 12_000L

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
        assertNull(yesterdayTop(emptyList(), nowMs))
    }

    @Test
    fun testTodayOnlyIsNull() {
        assertNull(yesterdayTop(listOf(item("aaa", 3 * day + 1)), nowMs))
    }

    @Test
    fun testOlderThanYesterdayIsNull() {
        assertNull(yesterdayTop(listOf(item("aaa", day)), nowMs))
    }

    @Test
    fun testBoundaries() {
        // from inclusive, to exclusive.
        val rows = listOf(item("aaa", 2 * day), item("bbb", 3 * day))
        assertEquals(listOf("aaa"), yesterdayTop(rows, nowMs))
    }

    @Test
    fun testRanksByCountThenRecency() {
        val rows = listOf(
            item("bbb", 2 * day + 1),
            item("aaa", 2 * day + 2),
            item("aaa", 2 * day + 3),
            item("aaa", 2 * day + 4),
        )
        assertEquals(listOf("aaa", "bbb"), yesterdayTop(rows, nowMs))
    }

    @Test
    fun testTieBreaksByRecency() {
        val rows = listOf(item("aaa", 2 * day + 1), item("bbb", 2 * day + 9))
        assertEquals(listOf("bbb", "aaa"), yesterdayTop(rows, nowMs))
    }

    @Test
    fun testExcludesBlankAndDevotional() {
        val rows = listOf(
            item("", 2 * day + 1),
            item("   ", 2 * day + 2),
            item("__devotional", 2 * day + 3),
            item("aaa", 2 * day + 4),
        )
        assertEquals(listOf("aaa"), yesterdayTop(rows, nowMs))
    }

    @Test
    fun testLimitTruncates() {
        val rows = listOf(item("aaa", 2 * day + 1), item("bbb", 2 * day + 2))
        assertEquals(listOf("bbb"), yesterdayTop(rows, nowMs, limit = 1))
        assertNull(yesterdayTop(rows, nowMs, limit = 0))
        assertNull(yesterdayTop(rows, -5L))
    }
}
