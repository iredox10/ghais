package com.ghais.ui.screens.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class StatsDailyTest {
    @Test fun sumsOnlySeparateDeviceBucketsAndRetainsSilentDays() {
        val result = combinedStatsDays(mapOf(1L to 30L, 2L to -1L), mapOf(1L to 45L, 3L to 60L))
        assertEquals(mapOf(1L to 75L, 2L to 0L, 3L to 60L), result)
        assertEquals(null, result[4L])
    }
    @Test fun doesNotOverflowOnMalformedTotals() {
        assertEquals(Long.MAX_VALUE, combinedStatsDays(mapOf(1L to Long.MAX_VALUE), mapOf(1L to 1L))[1L])
    }
}
