package com.ghais.player

import com.ghais.domain.model.HifzRange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Phase 1.1 hifz loop engine tests: per-ayah repeat count + inter-repeat pause.
 *
 * These tests exercise the pure range/loop decision layer
 * ([buildHifzRange]/[decideHifzTransition]/[hifzPauseCountdownSeconds] plus the
 * [HifzRange] model) WITHOUT booting the [AudioEngine] singleton, whose init
 * touches PlayerBridge and needs a platform player. [AudioEngine.setHifzRange]
 * and [AudioEngine.handleAyahTrackEnd] delegate to exactly this logic.
 */
class AudioEngineHifzTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --- HifzRange model ---

    @Test
    fun hifzRangeDefaultsToSingleRepeatAndZeroPause() {
        val range = HifzRange(surahId = 1, startAyah = 1, endAyah = 5)
        assertEquals(1, range.targetLoops)
        assertEquals(1, range.currentLoop)
        assertEquals(1, range.repeatPerAyah)
        assertEquals(0L, range.pauseBetweenMs)
    }

    @Test
    fun hifzRangeKeepsExplicitRepeatAndPause() {
        val range = HifzRange(112, 1, 4, targetLoops = 2, currentLoop = 1, repeatPerAyah = 3, pauseBetweenMs = 5_000L)
        assertEquals(3, range.repeatPerAyah)
        assertEquals(5_000L, range.pauseBetweenMs)
    }

    @Test
    fun hifzRangeRoundTripSerialization() {
        val range = HifzRange(112, 1, 4, targetLoops = 2, currentLoop = 1, repeatPerAyah = 3, pauseBetweenMs = 5_000L)
        val decoded = json.decodeFromString<HifzRange>(json.encodeToString(range))
        assertEquals(range, decoded)
    }

    @Test
    fun hifzRangeDecodesLegacyPayloadWithDefaults() {
        // Pre-1.1 persisted payloads carry no repeat/pause keys: decode-compat
        // must yield repeatPerAyah=1, pauseBetweenMs=0L.
        val legacy = """{"surahId":1,"startAyah":1,"endAyah":5,"targetLoops":2,"currentLoop":1}"""
        val decoded = json.decodeFromString<HifzRange>(legacy)
        assertEquals(HifzRange(1, 1, 5, targetLoops = 2, currentLoop = 1), decoded)
        assertEquals(1, decoded.repeatPerAyah)
        assertEquals(0L, decoded.pauseBetweenMs)
    }

    // --- buildHifzRange: normalization, clamping, overload defaults ---

    @Test
    fun buildHifzRangeDefaultsToSingleRepeatAndZeroPause() {
        // Mirrors the existing setHifzRange overloads delegating with (1, 0L).
        val range = buildHifzRange(surahId = 2, startAyah = 1, endAyah = 3, targetLoops = 1)
        assertEquals(HifzRange(2, 1, 3, targetLoops = 1, currentLoop = 1), range)
        assertEquals(1, range.repeatPerAyah)
        assertEquals(0L, range.pauseBetweenMs)
    }

    @Test
    fun buildHifzRangeNormalizesSwappedBounds() {
        val range = buildHifzRange(surahId = 2, startAyah = 5, endAyah = 2, targetLoops = 1)
        assertEquals(2, range.startAyah)
        assertEquals(5, range.endAyah)
    }

    @Test
    fun buildHifzRangeClampsRepeatPerAyah() {
        assertEquals(1, buildHifzRange(2, 1, 3, 1, repeatPerAyah = 0).repeatPerAyah)
        assertEquals(1, buildHifzRange(2, 1, 3, 1, repeatPerAyah = -5).repeatPerAyah)
        assertEquals(1, buildHifzRange(2, 1, 3, 1, repeatPerAyah = 1).repeatPerAyah)
        assertEquals(50, buildHifzRange(2, 1, 3, 1, repeatPerAyah = 50).repeatPerAyah)
        assertEquals(50, buildHifzRange(2, 1, 3, 1, repeatPerAyah = 51).repeatPerAyah)
        assertEquals(50, buildHifzRange(2, 1, 3, 1, repeatPerAyah = Int.MAX_VALUE).repeatPerAyah)
    }

    @Test
    fun buildHifzRangeClampsPauseBetweenMs() {
        assertEquals(0L, buildHifzRange(2, 1, 3, 1, pauseBetweenMs = -1L).pauseBetweenMs)
        assertEquals(0L, buildHifzRange(2, 1, 3, 1, pauseBetweenMs = 0L).pauseBetweenMs)
        assertEquals(5_000L, buildHifzRange(2, 1, 3, 1, pauseBetweenMs = 5_000L).pauseBetweenMs)
        assertEquals(30_000L, buildHifzRange(2, 1, 3, 1, pauseBetweenMs = 30_000L).pauseBetweenMs)
        assertEquals(30_000L, buildHifzRange(2, 1, 3, 1, pauseBetweenMs = 30_001L).pauseBetweenMs)
    }

    @Test
    fun buildHifzRangeStoresPauseValue() {
        val range = buildHifzRange(112, 1, 4, targetLoops = 2, repeatPerAyah = 3, pauseBetweenMs = 5_000L)
        assertEquals(3, range.repeatPerAyah)
        assertEquals(5_000L, range.pauseBetweenMs)
        assertEquals(1, range.currentLoop)
    }

    // --- decideHifzTransition: rep counting ---

    @Test
    fun repeatThreeReplaysSameAyahThenAdvances() {
        val range = buildHifzRange(112, 1, 4, targetLoops = 1, repeatPerAyah = 3)

        // Completing rep 1 of 3 -> replay same ayah as rep 2.
        val first = decideHifzTransition(range, surahId = 112, currentAyah = 2, currentRepetition = 1)
        assertIs<HifzTransition.ReplayAyah>(first)
        assertEquals(112, first.surahId)
        assertEquals(2, first.ayahNo)
        assertEquals(2, first.repetition)

        // Completing rep 2 of 3 -> replay same ayah as rep 3.
        val second = decideHifzTransition(range, surahId = 112, currentAyah = 2, currentRepetition = 2)
        assertIs<HifzTransition.ReplayAyah>(second)
        assertEquals(2, second.ayahNo)
        assertEquals(3, second.repetition)

        // Completing rep 3 of 3 -> advance to the next ayah.
        val third = decideHifzTransition(range, surahId = 112, currentAyah = 2, currentRepetition = 3)
        assertIs<HifzTransition.AdvanceAyah>(third)
        assertEquals(3, third.ayahNo)
    }

    @Test
    fun singleRepeatAdvancesImmediately() {
        // Default repeatPerAyah=1 preserves the pre-1.1 advance flow.
        val range = buildHifzRange(112, 1, 4, targetLoops = 1)
        val step = decideHifzTransition(range, surahId = 112, currentAyah = 2, currentRepetition = 1)
        assertIs<HifzTransition.AdvanceAyah>(step)
        assertEquals(3, step.ayahNo)
    }

    @Test
    fun advanceCoercesUpToRangeStart() {
        val range = buildHifzRange(2, 3, 5, targetLoops = 1)
        val step = decideHifzTransition(range, surahId = 2, currentAyah = 1, currentRepetition = 1)
        assertIs<HifzTransition.AdvanceAyah>(step)
        assertEquals(3, step.ayahNo)
    }

    // --- decideHifzTransition: loops + completion ---

    @Test
    fun rangeEndWithLoopsLeftRestartsAtStart() {
        val range = buildHifzRange(112, 1, 4, targetLoops = 3).copy(currentLoop = 1)
        val step = decideHifzTransition(range, surahId = 112, currentAyah = 4, currentRepetition = 1)
        assertIs<HifzTransition.RestartRange>(step)
        assertEquals(1, step.startAyah)
        assertEquals(2, step.nextLoop)
    }

    @Test
    fun rangeCompletionWhenLoopsExhausted() {
        // Last loop done at endAyah -> CompleteRange (engine clears/advances).
        val range = buildHifzRange(112, 1, 4, targetLoops = 2).copy(currentLoop = 2)
        val step = decideHifzTransition(range, surahId = 112, currentAyah = 4, currentRepetition = 1)
        assertIs<HifzTransition.CompleteRange>(step)
    }

    @Test
    fun singleLoopRangeCompletesAtEnd() {
        val range = buildHifzRange(112, 1, 4, targetLoops = 1)
        val step = decideHifzTransition(range, surahId = 112, currentAyah = 4, currentRepetition = 1)
        assertIs<HifzTransition.CompleteRange>(step)
    }

    @Test
    fun infiniteLoopsNeverComplete() {
        val range = buildHifzRange(112, 1, 4, targetLoops = -1).copy(currentLoop = 99)
        val step = decideHifzTransition(range, surahId = 112, currentAyah = 4, currentRepetition = 1)
        assertIs<HifzTransition.RestartRange>(step)
        assertEquals(100, step.nextLoop)
    }

    @Test
    fun overshootPastEndAyahLoopsOrCompletes() {
        val looping = buildHifzRange(112, 1, 4, targetLoops = -1)
        assertIs<HifzTransition.RestartRange>(
            decideHifzTransition(looping, surahId = 112, currentAyah = 6, currentRepetition = 1)
        )
        val done = buildHifzRange(112, 1, 4, targetLoops = 1)
        assertIs<HifzTransition.CompleteRange>(
            decideHifzTransition(done, surahId = 112, currentAyah = 6, currentRepetition = 1)
        )
    }

    // --- decideHifzTransition: fall-through ---

    @Test
    fun noRangeFallsThroughToStandardLogic() {
        assertIs<HifzTransition.FallThrough>(
            decideHifzTransition(null, surahId = 112, currentAyah = 2, currentRepetition = 1)
        )
    }

    @Test
    fun mismatchedSurahFallsThroughToStandardLogic() {
        val range = buildHifzRange(112, 1, 4, targetLoops = 2, repeatPerAyah = 3)
        assertIs<HifzTransition.FallThrough>(
            decideHifzTransition(range, surahId = 113, currentAyah = 2, currentRepetition = 1)
        )
    }

    // --- pause countdown ---

    @Test
    fun hifzPauseCountdownSecondsMapping() {
        assertEquals(0, hifzPauseCountdownSeconds(-100L))
        assertEquals(0, hifzPauseCountdownSeconds(0L))
        assertEquals(1, hifzPauseCountdownSeconds(1L))
        assertEquals(1, hifzPauseCountdownSeconds(1_000L))
        assertEquals(2, hifzPauseCountdownSeconds(1_001L))
        assertEquals(5, hifzPauseCountdownSeconds(5_000L))
        assertEquals(30, hifzPauseCountdownSeconds(30_000L))
    }

    @Test
    fun clampHelpersMatchEngineBounds() {
        assertEquals(1, clampRepeatPerAyah(0))
        assertEquals(50, clampRepeatPerAyah(99))
        assertEquals(0L, clampPauseBetweenMs(-1L))
        assertEquals(30_000L, clampPauseBetweenMs(60_000L))
        assertTrue(HIFZ_MAX_REPEAT_PER_AYAH == 50)
        assertTrue(HIFZ_MAX_PAUSE_BETWEEN_MS == 30_000L)
    }
}
