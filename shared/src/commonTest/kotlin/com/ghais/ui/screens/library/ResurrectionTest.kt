package com.ghais.ui.screens.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AGENT A11 — guards the pure resurrection core ([findUnplayed]).
 * Offline, deterministic, no platform APIs.
 */
class ResurrectionTest {

    @Test
    fun emptySavedReturnsEmpty() {
        assertTrue(findUnplayed(emptyList(), listOf("a", "b")).isEmpty())
    }

    @Test
    fun allPlayedReturnsEmpty() {
        val saved = listOf("a", "b", "c")
        assertTrue(findUnplayed(saved, saved.toSet()).isEmpty())
    }

    @Test
    fun preservesSavedOrderSkippingPlayed() {
        val out = findUnplayed(
            savedIds = listOf("a", "b", "c", "d"),
            playHistory = setOf("b", "d")
        )
        assertEquals(listOf("a", "c"), out)
    }

    @Test
    fun limitDefaultsToEightAndIsRespected() {
        val saved = (1..20).map { "id-$it" }
        assertEquals(8, findUnplayed(saved, emptySet()).size)
        assertEquals(
            (1..3).map { "id-$it" },
            findUnplayed(saved, emptySet(), limit = 3)
        )
    }

    @Test
    fun duplicatesCollapseToFirstOccurrence() {
        val out = findUnplayed(
            savedIds = listOf("a", "a", "b", "b", "c"),
            playHistory = emptySet(),
            limit = 8
        )
        assertEquals(listOf("a", "b", "c"), out)
    }

    @Test
    fun blanksAreDropped() {
        val out = findUnplayed(
            savedIds = listOf("", "  ", "a"),
            playHistory = emptySet()
        )
        assertEquals(listOf("a"), out)
    }

    @Test
    fun nonPositiveLimitReturnsEmpty() {
        assertTrue(findUnplayed(listOf("a"), emptySet(), limit = 0).isEmpty())
        assertTrue(findUnplayed(listOf("a"), emptySet(), limit = -2).isEmpty())
    }
}
