package com.ghais.notify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotifyDoctrineTest {

    @Test
    fun testAllChannelsHaveCopyWithoutBannedWords() {
        NotifyChannel.all.forEach { channel ->
            val copy = copyFor(channel)
            assertTrue(copy.title.isNotBlank(), channel)
            assertTrue(copy.body.isNotBlank(), channel)
            listOf("missed", "lost", "broken").forEach { banned ->
                assertFalse(banned in copy.body.lowercase(), "$channel: $banned")
            }
        }
    }

    @Test
    fun testScheduleIdRoundtrip() {
        NotifyChannel.all.forEach { channel ->
            assertEquals(channel, channelForScheduleId(channelScheduleId(channel)))
        }
        assertNull(channelForScheduleId("sch-123"))
        assertNull(channelForScheduleId("notify-nope"))
    }

    @Test
    fun testBestHourHistogramTieAndEmpty() {
        assertEquals(9, bestHour(listOf(9, 9, 21)))
        assertEquals(9, bestHour(listOf(21, 9)))
        assertNull(bestHour(emptyList()))
        assertEquals(5, autoHour(emptyList()))
        assertEquals(21, autoHour(listOf(9, 21, 21)))
    }

    @Test
    fun testMayFireTodayCap() {
        val cap = FrequencyCap(channel = NotifyChannel.STREAK_SAFE, maxPerDay = 1)
        assertTrue(mayFireToday(cap, null, 100L))
        assertFalse(mayFireToday(cap, 100L, 100L))
        assertTrue(mayFireToday(cap, 99L, 100L))
        assertFalse(mayFireToday(cap.copy(maxPerDay = 0), null, 100L))
    }

    @Test
    fun testStreakAtRisk() {
        assertFalse(isStreakAtRisk(100L, 100L, 5))
        assertTrue(isStreakAtRisk(99L, 100L, 5))
        assertFalse(isStreakAtRisk(99L, 100L, 0))
    }
}
