package com.ghais.player

import com.ghais.domain.model.HifzLoopMode
import com.ghais.domain.model.HifzRange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HifzLoopEngineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --- Progressive Chaining (Accumulation) ---

    @Test
    fun accumulationFollowsProgressiveChainingSequence() {
        // Range 1..3, repeatPerAyah = 2, targetLoops = 1:
        // Expected play sequence: V1, V1, V2, V2, V1, V2, V3, V3, V1, V2, V3 -> CompleteRange
        val played = mutableListOf<Int>()
        var anchor = 1
        var cursor = 1
        var isChain = false
        var rep = 1
        var currentAyah = 1

        played += currentAyah
        repeat(20) {
            when (
                val step = nextAccumulationStep(
                    surahId = 112,
                    startAyah = 1,
                    endAyah = 3,
                    repeatPerAyah = 2,
                    anchorAyah = anchor,
                    chainCursor = cursor,
                    isChainPhase = isChain,
                    currentRepetition = rep,
                    targetLoops = 1,
                    currentLoop = 1
                )
            ) {
                is AccumulationStep.PlayAyah -> {
                    currentAyah = step.ayahNo
                    anchor = step.anchorAyah
                    cursor = step.chainCursor
                    isChain = step.isChainPhase
                    rep = step.nextRepetition
                    played += currentAyah
                }
                is AccumulationStep.RestartRange -> {
                    error("Did not expect RestartRange for targetLoops = 1")
                }
                AccumulationStep.CompleteRange -> {
                    assertEquals(listOf(1, 1, 2, 2, 1, 2, 3, 3, 1, 2, 3), played)
                    return
                }
            }
        }
        error("Accumulation sequence did not complete")
    }

    @Test
    fun accumulationSingleAyahRangeCompletesAfterRepeats() {
        val step1 = nextAccumulationStep(
            surahId = 1,
            startAyah = 5,
            endAyah = 5,
            repeatPerAyah = 2,
            anchorAyah = 5,
            chainCursor = 5,
            isChainPhase = false,
            currentRepetition = 1,
            targetLoops = 1,
            currentLoop = 1
        )
        val play2 = assertIs<AccumulationStep.PlayAyah>(step1)
        assertEquals(5, play2.ayahNo)
        assertEquals(2, play2.nextRepetition)

        val step2 = nextAccumulationStep(
            surahId = 1,
            startAyah = 5,
            endAyah = 5,
            repeatPerAyah = 2,
            anchorAyah = play2.anchorAyah,
            chainCursor = play2.chainCursor,
            isChainPhase = play2.isChainPhase,
            currentRepetition = play2.nextRepetition,
            targetLoops = 1,
            currentLoop = 1
        )
        assertIs<AccumulationStep.CompleteRange>(step2)
    }

    @Test
    fun accumulationRestartsWhenMoreLoopsRemain() {
        val step = nextAccumulationStep(
            surahId = 2,
            startAyah = 1,
            endAyah = 2,
            repeatPerAyah = 1,
            anchorAyah = 2,
            chainCursor = 2,
            isChainPhase = true,
            currentRepetition = 1,
            targetLoops = 3,
            currentLoop = 1
        )
        val restart = assertIs<AccumulationStep.RestartRange>(step)
        assertEquals(1, restart.startAyah)
        assertEquals(2, restart.nextLoop)
    }

    // --- Adaptive Pause Calculation ---

    @Test
    fun adaptivePauseUsesFixedWhenMultiplierZeroOrDurationInvalid() {
        assertEquals(3_000L, computeAdaptivePauseMs(10_000L, 3_000L, 0f))
        assertEquals(3_000L, computeAdaptivePauseMs(10_000L, 3_000L, -1f))
        assertEquals(3_000L, computeAdaptivePauseMs(0L, 3_000L, 1.5f))
        assertEquals(0L, computeAdaptivePauseMs(-500L, 0L, 2.0f))
    }

    @Test
    fun adaptivePauseScalesAndClampsBetween2sAnd45s() {
        assertEquals(7_500L, computeAdaptivePauseMs(5_000L, 1_000L, 1.5f))
        assertEquals(2_000L, computeAdaptivePauseMs(1_000L, 0L, 0.5f))
        assertEquals(45_000L, computeAdaptivePauseMs(40_000L, 0L, 2.0f))
    }

    // --- Bridge & First-Word Prompt Window Helpers ---

    @Test
    fun bridgeStartPositionReturnsLast4SecondsCoercedNonNegative() {
        assertEquals(6_000L, bridgeStartPositionMs(10_000L))
        assertEquals(0L, bridgeStartPositionMs(4_000L))
        assertEquals(0L, bridgeStartPositionMs(2_500L))
        assertEquals(0L, bridgeStartPositionMs(0L))
        assertEquals(0L, bridgeStartPositionMs(-1_000L))
    }

    @Test
    fun firstWordPromptCutoffCapsAt2200MsOrDuration() {
        assertEquals(2_200L, firstWordPromptCutoffMs(10_000L))
        assertEquals(2_200L, firstWordPromptCutoffMs(2_200L))
        assertEquals(1_400L, firstWordPromptCutoffMs(1_400L))
        assertEquals(0L, firstWordPromptCutoffMs(0L))
        assertEquals(0L, firstWordPromptCutoffMs(-100L))
    }

    // --- Models Compatibility ---

    @Test
    fun hifzRangeWithLoopModeAndMultiplierRoundTrips() {
        val range = HifzRange(
            surahId = 18,
            startAyah = 1,
            endAyah = 10,
            targetLoops = 2,
            repeatPerAyah = 3,
            pauseBetweenMs = 2_000L,
            loopMode = HifzLoopMode.ACCUMULATION,
            pauseMultiplier = 1.5f
        )
        val decoded = json.decodeFromString<HifzRange>(json.encodeToString(range))
        assertEquals(range, decoded)
    }
}
