package com.ghais.ui.screens.reciters.deck

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeckDoneStoreTest {

    @Test
    fun testInitialNullIsNotDone() {
        assertFalse(isDone(null, 100L))
    }

    @Test
    fun testSameDayIsDoneNextDayIsNot() {
        assertTrue(isDone(100L, 100L))
        assertFalse(isDone(100L, 101L))
    }

    @Test
    fun testStaleStoredDayIsNotDone() {
        assertFalse(isDone(50L, 100L))
    }
}
