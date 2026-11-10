package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class StatsDayRolloverTest {
    @Test fun idleDayNeverEarnsAnotherStreakDay() {
        assertEquals(4, streakBeforeListening(101, 100, 4))
        assertEquals(4, streakBeforeListening(100, 100, 4))
        assertEquals(0, streakBeforeListening(102, 100, 4))
        assertEquals(0, streakBeforeListening(100, 0, 4))
    }
}
