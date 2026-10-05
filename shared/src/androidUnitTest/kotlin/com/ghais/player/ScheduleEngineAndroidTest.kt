package com.ghais.data.repository

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the alarm trigger computation in [ScheduleEngine.nextTriggerFor],
 * the arithmetic that decides when the next `SetAlarmClock` fires.
 *
 * Contracts covered:
 *
 * 1. Future time today -> today, past time today -> tomorrow.
 * 2. The equality boundary: an exact match rolls forward (the alarm must
 *    never re-fire the moment it already fired).
 * 3. Seconds and milliseconds are zeroed onto the minute.
 * 4. Midnight rollover uses `DAY_OF_MONTH` add, not +24h (DST safe).
 *
 * Expected values are built with the same `Calendar` idiom as the
 * implementation so the tests are timezone-agnostic.
 */
class ScheduleEngineAndroidTest {

    private fun at(hour: Int, minute: Int, dayOffset: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, dayOffset)
        }.timeInMillis

    @Test
    fun futureTimeTodayFiresToday() {
        assertEquals(at(17, 5), ScheduleEngine.nextTriggerFor(17, 5, at(8, 0)))
    }

    @Test
    fun pastTimeTodayFiresTomorrow() {
        assertEquals(at(17, 5, 1), ScheduleEngine.nextTriggerFor(17, 5, at(18, 0)))
    }

    @Test
    fun exactMatchRollsToTomorrow() {
        assertEquals(at(17, 5, 1), ScheduleEngine.nextTriggerFor(17, 5, at(17, 5)))
    }

    @Test
    fun oneMillisecondBeforeFiresToday() {
        val target = at(17, 5)
        assertEquals(target, ScheduleEngine.nextTriggerFor(17, 5, target - 1))
    }

    @Test
    fun resultIsZeroedOntoTheMinute() {
        val now = at(17, 4) + 30_000 // 17:04:30
        val result = ScheduleEngine.nextTriggerFor(17, 5, now)
        assertEquals(at(17, 5), result)
        assertEquals(0, (result / 1000) % 60)
        assertEquals(5, (result / 60_000) % 60)
    }

    @Test
    fun midnightTargetAfterNowRollsToTomorrowMidnight() {
        assertEquals(at(0, 0, 1), ScheduleEngine.nextTriggerFor(0, 0, at(23, 30)))
    }

    @Test
    fun midnightTargetBeforeNowRollsToTomorrowMidnight() {
        assertEquals(at(0, 0, 1), ScheduleEngine.nextTriggerFor(0, 0, at(0, 30)))
    }

    @Test
    fun nextOccurrenceMillisIsInTheFutureOnTheMinute() {
        val now = System.currentTimeMillis()
        val result = ScheduleEngine.nextOccurrenceMillis(17, 5)
        assertTrue(result > now, "must be in the future")
        assertTrue(result <= now + 25 * 3600_000L, "must be within a day")
        assertEquals(0, (result / 1000) % 60)
        assertEquals(5, (result / 60_000) % 60)
    }
}
