package com.ghais.ui.screens.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AGENT A11 — guards the pure curator scoring ([curatorLevel]).
 * Score = playlists * 10 + tracks (negatives clamp to 0).
 * Thresholds: BRONZE 0 / SILVER 25 / GOLD 75 / PLATINUM 150.
 */
class CuratorLevelsTest {

    @Test
    fun freshUserIsBronzeCuratorOne() {
        val level = curatorLevel(0, 0)
        assertEquals(CuratorLevel.BRONZE, level)
        assertEquals("Curator I", level.title)
    }

    @Test
    fun silverAtTwentyFive() {
        // 2 playlists (20) + 5 tracks = 25 → SILVER.
        val level = curatorLevel(2, 5)
        assertEquals(CuratorLevel.SILVER, level)
        assertEquals("Curator II", level.title)
        // One point short stays BRONZE.
        assertEquals(CuratorLevel.BRONZE, curatorLevel(2, 4))
    }

    @Test
    fun goldAtSeventyFive() {
        assertEquals(CuratorLevel.GOLD, curatorLevel(5, 25))
        assertEquals("Curator III", CuratorLevel.GOLD.title)
        assertEquals(CuratorLevel.SILVER, curatorLevel(5, 24))
    }

    @Test
    fun platinumAtOneFifty() {
        assertEquals(CuratorLevel.PLATINUM, curatorLevel(10, 50))
        assertEquals("Curator IV", CuratorLevel.PLATINUM.title)
        assertEquals(CuratorLevel.GOLD, curatorLevel(10, 49))
    }

    @Test
    fun negativesClampToBronze() {
        assertEquals(CuratorLevel.BRONZE, curatorLevel(-3, -99))
        assertEquals(0, curatorScore(-3, -99))
    }

    @Test
    fun nextLevelIsNullAtPlatinumAndProgressIsOne() {
        assertNull(nextCuratorLevel(10, 50))
        assertEquals(1f, curatorProgress(10, 50))
        assertTrue(curatorProgress(0, 0) in 0f..1f)
    }
}
