package com.ghais.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * N-times ayah repeat counter tests: the pure [consumeAyahRepeat] decision
 * helper used by [AudioEngine.handleAyahTrackEnd] (step 0 — consumes before
 * the hifz-range / rep-target / RepeatMode logic).
 *
 * Like [AudioEngineHifzTest], these run WITHOUT booting the [AudioEngine]
 * singleton (its init touches PlayerBridge, which needs a platform player),
 * so the StateFlow wiring ([AudioEngine.setAyahRepeatCount] /
 * [AudioEngine.clearAyahRepeat] / [AudioEngine.ayahRepeatRemaining] plus the
 * handleAyahTrackEnd replay branch and the manual-nav/track-change clearing
 * call sites) is compile-checked here but behaviour-verified only on-device.
 */
class AyahRepeatTest {

    @Test
    fun countThreeReplaysThreeTimesThenAdvances() {
        // setAyahRepeatCount(3): the next 3 ayah-track ends replay, 4th advances.
        var remaining = 3

        var step = consumeAyahRepeat(remaining)
        assertTrue(step.second, "1st end replays")
        assertEquals(2, step.first)
        remaining = step.first

        step = consumeAyahRepeat(remaining)
        assertTrue(step.second, "2nd end replays")
        assertEquals(1, step.first)
        remaining = step.first

        step = consumeAyahRepeat(remaining)
        assertTrue(step.second, "3rd end replays")
        assertEquals(0, step.first)
        remaining = step.first

        step = consumeAyahRepeat(remaining)
        assertFalse(step.second, "4th end advances to normal logic")
        assertEquals(0, step.first)
    }

    @Test
    fun countOneReplaysExactlyOnce() {
        val first = consumeAyahRepeat(1)
        assertTrue(first.second, "single repeat replays once")
        assertEquals(0, first.first)

        val second = consumeAyahRepeat(first.first)
        assertFalse(second.second, "next end advances")
        assertEquals(0, second.first)
    }

    @Test
    fun zeroNeverReplays() {
        val step = consumeAyahRepeat(0)
        assertFalse(step.second, "inactive counter advances")
        assertEquals(0, step.first)
    }

    @Test
    fun negativeNeverReplaysAndNormalizesToZero() {
        // setAyahRepeatCount(count <= 0) clears, so negatives never reach the
        // StateFlow — but the helper stays total over them regardless.
        assertEquals(Pair(0, false), consumeAyahRepeat(-1))
        assertEquals(Pair(0, false), consumeAyahRepeat(-50))
        assertEquals(Pair(0, false), consumeAyahRepeat(Int.MIN_VALUE))
    }

    @Test
    fun largeCountDecrementsByExactlyOne() {
        val step = consumeAyahRepeat(50)
        assertTrue(step.second)
        assertEquals(49, step.first)

        val max = consumeAyahRepeat(Int.MAX_VALUE)
        assertTrue(max.second)
        assertEquals(Int.MAX_VALUE - 1, max.first)
    }

    @Test
    fun replaySequenceLengthMatchesCount() {
        // For every N in 1..5, simulating consecutive ayah-track ends yields
        // exactly N replays, lands on 0, then advances.
        for (n in 1..5) {
            var remaining = n
            var replays = 0
            var step = consumeAyahRepeat(remaining)
            while (step.second) {
                replays++
                remaining = step.first
                step = consumeAyahRepeat(remaining)
            }
            assertEquals(n, replays, "count=$n replays exactly $n times")
            assertEquals(0, remaining, "count=$n lands on 0")
            assertEquals(0, step.first)
        }
    }
}
