package com.ghais.hifz.quests

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HifzXpLevelsTest {

    @Test
    fun testThresholds() {
        assertEquals(HifzXpLevel.BRONZE, hifzXpLevel(0))
        assertEquals(HifzXpLevel.BRONZE, hifzXpLevel(199))
        assertEquals(HifzXpLevel.SILVER, hifzXpLevel(200))
        assertEquals(HifzXpLevel.GOLD, hifzXpLevel(600))
        assertEquals(HifzXpLevel.PLATINUM, hifzXpLevel(1200))
        assertEquals(HifzXpLevel.PLATINUM, hifzXpLevel(9999))
    }

    @Test
    fun testNegativesClampToBronze() {
        assertEquals(HifzXpLevel.BRONZE, hifzXpLevel(-50))
    }

    @Test
    fun testNextAndProgress() {
        assertEquals(HifzXpLevel.SILVER, nextHifzXpLevel(0))
        assertNull(nextHifzXpLevel(1200))
        assertEquals(1f, hifzXpProgress(5000))
        val mid = hifzXpProgress(100)
        assertTrue(mid > 0f && mid < 1f, "mid=$mid")
    }
}
