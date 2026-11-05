package com.ghais.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackInterruptionTest {
    @Test fun transientInterruptionResumesOnce() {
        val state = PlaybackInterruption()
        state.pause(wasPlaying = true, resumable = true)
        assertTrue(state.consumeResume())
        assertFalse(state.consumeResume())
    }

    @Test fun ringingThenAnsweredCallKeepsResumeIntent() {
        val state = PlaybackInterruption()
        state.pause(wasPlaying = true, resumable = true)
        state.pause(wasPlaying = false, resumable = false)
        assertTrue(state.consumeResume())
    }

    @Test fun permanentMediaTakeoverStaysPaused() {
        val state = PlaybackInterruption()
        state.pause(wasPlaying = true, resumable = false)
        assertTrue(state.pausedByFocus)
        assertFalse(state.consumeResume())
    }

    @Test fun outgoingCallDetectedAfterPermanentLossCanResume() {
        val state = PlaybackInterruption()
        state.pause(wasPlaying = true, resumable = false)
        state.markCall()
        assertTrue(state.consumeResume())
    }

    @Test fun userPauseDuringCallCancelsResumeAndLateCallChecks() {
        val state = PlaybackInterruption()
        state.pause(wasPlaying = true, resumable = true)
        state.cancel()
        state.markCall()
        assertFalse(state.consumeResume())
    }

    @Test fun alreadyPausedPlaybackNeverStartsAfterACall() {
        val state = PlaybackInterruption()
        state.pause(wasPlaying = false, resumable = true)
        state.markCall()
        assertFalse(state.consumeResume())
    }
}
