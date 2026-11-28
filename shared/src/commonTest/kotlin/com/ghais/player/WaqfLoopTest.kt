package com.ghais.player

import kotlin.test.Test
import kotlin.test.assertEquals

class WaqfLoopTest {
    @Test
    fun computeWaqfWindowMsPartitionsDurationProportionally() {
        val weights = listOf(10, 20, 30) // total = 60
        val duration = 12_000L

        assertEquals(0L to 2_000L, computeWaqfWindowMs(duration, weights, 0))
        assertEquals(2_000L to 6_000L, computeWaqfWindowMs(duration, weights, 1))
        assertEquals(6_000L to 12_000L, computeWaqfWindowMs(duration, weights, 2))
    }

    @Test
    fun computeWaqfWindowMsHandlesZeroDurationAndClampsIndex() {
        assertEquals(0L to 0L, computeWaqfWindowMs(0L, listOf(10, 20), 0))
        assertEquals(0L to 0L, computeWaqfWindowMs(10_000L, emptyList(), 0))
        assertEquals(6_000L to 10_000L, computeWaqfWindowMs(10_000L, listOf(6, 4), 99))
    }
}
