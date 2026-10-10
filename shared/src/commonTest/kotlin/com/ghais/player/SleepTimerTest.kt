package com.ghais.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SleepTimer pure-logic tests.
 *
 * Fully pure (zero singleton/clock/engine): [SleepTimerState] defaults and the
 * [SleepTimerState.volumeMultiplier] fade curve.
 *
 * Synchronous singleton boundary tests ([startAtAyahBoundary] /
 * [startAtSurahBoundary] / [startAtQueueBoundary] + onXEnded callbacks) launch
 * no timer job for non-MINUTES conditions, so they need no clock — each resets
 * the singleton before and after.
 *
 * Deliberately NOT covered (needs a clock/seam): [SleepTimer.startTimer] with
 * [StopCondition.MINUTES] launches a real 1s-tick coroutine, so the
 * `durationMinutes * 60L` duration math cannot be asserted without either
 * sleeping the test or injecting a clock.
 */
class SleepTimerTest {

    private fun resetTimer() {
        SleepTimer.cancelTimer()
        SleepTimer.onStopPlayer = null
    }

    // --- SleepTimerState defaults ---

    @Test
    fun stateDefaultsToInactiveWithFullVolume() {
        val state = SleepTimerState()
        assertFalse(state.isActive)
        assertEquals(0L, state.remainingSeconds)
        assertEquals(StopCondition.MINUTES, state.stopCondition)
        assertTrue(state.fadeOutEnabled)
        assertEquals(1f, state.volumeMultiplier)
    }

    // --- volumeMultiplier fade curve (pure) ---

    @Test
    fun volumeMultiplierFadesLinearlyOverLastThirtySeconds() {
        val atThirty = SleepTimerState(isActive = true, remainingSeconds = 30)
        assertEquals(1f, atThirty.volumeMultiplier, 0.0001f)

        val atFifteen = SleepTimerState(isActive = true, remainingSeconds = 15)
        assertEquals(0.5f, atFifteen.volumeMultiplier, 0.0001f)

        val atZero = SleepTimerState(isActive = true, remainingSeconds = 0)
        assertEquals(0f, atZero.volumeMultiplier, 0.0001f)
    }

    @Test
    fun volumeMultiplierStaysFullAboveThirtySeconds() {
        val state = SleepTimerState(isActive = true, remainingSeconds = 31)
        assertEquals(1f, state.volumeMultiplier, 0.0001f)
    }

    @Test
    fun volumeMultiplierStaysFullWhenFadeOutDisabled() {
        val state = SleepTimerState(isActive = true, remainingSeconds = 5, fadeOutEnabled = false)
        assertEquals(1f, state.volumeMultiplier)
    }

    @Test
    fun volumeMultiplierStaysFullForNonMinutesConditions_ReadBehavior() {
        // READ: the fade curve only applies when stopCondition == MINUTES, so
        // boundary stops (which arm with remainingSeconds = 0) never duck.
        for (condition in listOf(StopCondition.END_OF_AYAH, StopCondition.END_OF_SURAH, StopCondition.END_OF_QUEUE)) {
            val state = SleepTimerState(isActive = true, remainingSeconds = 0, stopCondition = condition)
            assertEquals(1f, state.volumeMultiplier, "condition $condition must not fade")
        }
    }

    @Test
    fun volumeMultiplierStaysFullWhenInactive() {
        val state = SleepTimerState(isActive = false, remainingSeconds = 5)
        assertEquals(1f, state.volumeMultiplier)
    }

    // --- boundary arming + callbacks (synchronous, no clock) ---

    @Test
    fun startAtAyahBoundaryArmsAyahStopWithZeroDuration() {
        resetTimer()
        try {
            SleepTimer.startAtAyahBoundary()
            val state = SleepTimer.state.value
            assertTrue(state.isActive)
            assertEquals(StopCondition.END_OF_AYAH, state.stopCondition)
            assertEquals(0L, state.remainingSeconds)
        } finally {
            resetTimer()
        }
    }

    @Test
    fun boundaryStartersArmTheirRespectiveConditions() {
        resetTimer()
        try {
            SleepTimer.startAtSurahBoundary()
            assertEquals(StopCondition.END_OF_SURAH, SleepTimer.state.value.stopCondition)
            assertTrue(SleepTimer.state.value.isActive)

            SleepTimer.startAtQueueBoundary()
            assertEquals(StopCondition.END_OF_QUEUE, SleepTimer.state.value.stopCondition)
            assertTrue(SleepTimer.state.value.isActive)
        } finally {
            resetTimer()
        }
    }

    @Test
    fun onAyahEndedFiresStopOnlyForMatchingCondition() {
        resetTimer()
        try {
            var fired = false
            SleepTimer.onStopPlayer = { fired = true }

            SleepTimer.startAtAyahBoundary()
            SleepTimer.onSurahEnded()
            assertFalse(fired, "surah callback must not fire an ayah-armed timer")
            assertTrue(SleepTimer.state.value.isActive)

            SleepTimer.onAyahEnded()
            assertTrue(fired)
            assertFalse(SleepTimer.state.value.isActive)
        } finally {
            resetTimer()
        }
    }

    @Test
    fun onQueueEndedFiresStopOnlyForMatchingCondition() {
        resetTimer()
        try {
            var fired = false
            SleepTimer.onStopPlayer = { fired = true }

            SleepTimer.startAtQueueBoundary()
            SleepTimer.onAyahEnded()
            assertFalse(fired, "ayah callback must not fire a queue-armed timer")

            SleepTimer.onQueueEnded()
            assertTrue(fired)
            assertFalse(SleepTimer.state.value.isActive)
        } finally {
            resetTimer()
        }
    }
}
