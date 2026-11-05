package com.ghais.player

/** Remembers playback intent across repeated focus losses, independently of UI state. */
internal class PlaybackInterruption {
    var pausedByFocus = false
        private set
    var resumeOnGain = false
        private set

    fun pause(wasPlaying: Boolean, resumable: Boolean) {
        pausedByFocus = pausedByFocus || wasPlaying
        resumeOnGain = pausedByFocus && (resumeOnGain || resumable)
    }

    fun markCall() {
        if (pausedByFocus) resumeOnGain = true
    }

    fun consumeResume(): Boolean {
        val resume = pausedByFocus && resumeOnGain
        cancel()
        return resume
    }

    fun cancel() {
        pausedByFocus = false
        resumeOnGain = false
    }
}
