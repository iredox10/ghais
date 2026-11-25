package com.ghais.player

/**
 * Pure state-machine and timing helpers for the Hifz Audio-First Loop Engine.
 *
 * Kept top-level and `internal` so `commonTest` can exhaustively verify
 * progressive chaining, adaptive pauses, and bridge / first-word prompt
 * windows without booting [AudioEngine].
 */

internal const val HIFZ_ADAPTIVE_MIN_PAUSE_MS: Long = 2_000L
internal const val HIFZ_ADAPTIVE_MAX_PAUSE_MS: Long = 45_000L
internal const val HIFZ_BRIDGE_TAIL_MS: Long = 4_000L
internal const val HIFZ_FIRST_WORD_PROMPT_MS: Long = 2_200L

/**
 * Result of advancing one step in Progressive Chaining (Accumulation) mode:
 * `V1 xN -> V2 xN -> V1..V2 -> V3 xN -> V1..V3 ...`
 */
internal sealed interface AccumulationStep {
    /** Play [ayahNo] with updated accumulation cursor/phase state. */
    data class PlayAyah(
        val surahId: Int,
        val ayahNo: Int,
        val anchorAyah: Int,
        val chainCursor: Int,
        val isChainPhase: Boolean,
        val nextRepetition: Int
    ) : AccumulationStep

    /** Full accumulation pass reached `endAyah` and more range loops remain. */
    data class RestartRange(
        val surahId: Int,
        val startAyah: Int,
        val nextLoop: Int
    ) : AccumulationStep

    /** Full accumulation pass reached `endAyah` and all loops are exhausted. */
    data object CompleteRange : AccumulationStep
}

/**
 * Computes the next [AccumulationStep] for Progressive Chaining:
 * - While `!isChainPhase`: repeats [anchorAyah] up to [repeatPerAyah] times.
 *   Once complete, if `anchorAyah > startAyah`, enters chain phase starting at
 *   `startAyah`; otherwise advances to `startAyah + 1` (or finishes if single-ayah).
 * - While `isChainPhase`: plays `startAyah..anchorAyah` once each via [chainCursor].
 *   Once `chainCursor == anchorAyah`, advances `anchorAyah` (or finishes/restarts
 *   the range if `anchorAyah == endAyah`).
 */
internal fun nextAccumulationStep(
    surahId: Int,
    startAyah: Int,
    endAyah: Int,
    repeatPerAyah: Int,
    anchorAyah: Int,
    chainCursor: Int,
    isChainPhase: Boolean,
    currentRepetition: Int = 1,
    targetLoops: Int = 1,
    currentLoop: Int = 1
): AccumulationStep {
    val lo = minOf(startAyah, endAyah).coerceAtLeast(1)
    val hi = maxOf(startAyah, endAyah).coerceAtLeast(1)
    val reps = repeatPerAyah.coerceAtLeast(1)
    val anchor = anchorAyah.coerceIn(lo, hi)

    if (!isChainPhase) {
        if (currentRepetition < reps) {
            return AccumulationStep.PlayAyah(
                surahId = surahId,
                ayahNo = anchor,
                anchorAyah = anchor,
                chainCursor = anchor,
                isChainPhase = false,
                nextRepetition = currentRepetition + 1
            )
        }
        if (anchor > lo) {
            return AccumulationStep.PlayAyah(
                surahId = surahId,
                ayahNo = lo,
                anchorAyah = anchor,
                chainCursor = lo,
                isChainPhase = true,
                nextRepetition = 1
            )
        }
        if (anchor < hi) {
            val nextAnchor = anchor + 1
            return AccumulationStep.PlayAyah(
                surahId = surahId,
                ayahNo = nextAnchor,
                anchorAyah = nextAnchor,
                chainCursor = nextAnchor,
                isChainPhase = false,
                nextRepetition = 1
            )
        }
        return if (targetLoops == -1 || currentLoop < targetLoops) {
            AccumulationStep.RestartRange(surahId, lo, currentLoop + 1)
        } else {
            AccumulationStep.CompleteRange
        }
    }

    val cursor = chainCursor.coerceIn(lo, anchor)
    if (cursor < anchor) {
        val nextCursor = cursor + 1
        return AccumulationStep.PlayAyah(
            surahId = surahId,
            ayahNo = nextCursor,
            anchorAyah = anchor,
            chainCursor = nextCursor,
            isChainPhase = true,
            nextRepetition = 1
        )
    }

    if (anchor < hi) {
        val nextAnchor = anchor + 1
        return AccumulationStep.PlayAyah(
            surahId = surahId,
            ayahNo = nextAnchor,
            anchorAyah = nextAnchor,
            chainCursor = nextAnchor,
            isChainPhase = false,
            nextRepetition = 1
        )
    }

    return if (targetLoops == -1 || currentLoop < targetLoops) {
        AccumulationStep.RestartRange(surahId, lo, currentLoop + 1)
    } else {
        AccumulationStep.CompleteRange
    }
}

/**
 * Computes the inter-repeat pause in milliseconds.
 * When [multiplier] > 0f and [ayahDurationMs] > 0L, scales the ayah duration
 * and clamps to `2_000L..45_000L`; otherwise returns [fixedPauseMs].
 */
internal fun computeAdaptivePauseMs(
    ayahDurationMs: Long,
    fixedPauseMs: Long,
    multiplier: Float
): Long =
    if (multiplier > 0f && ayahDurationMs > 0L) {
        (ayahDurationMs * multiplier).toLong().coerceIn(HIFZ_ADAPTIVE_MIN_PAUSE_MS, HIFZ_ADAPTIVE_MAX_PAUSE_MS)
    } else {
        fixedPauseMs
    }

/**
 * Start position (ms) for Bridge mode: the last 4 seconds of the ayah,
 * clamped to `>= 0L`.
 */
internal fun bridgeStartPositionMs(durationMs: Long): Long =
    if (durationMs <= 0L) 0L else (durationMs - HIFZ_BRIDGE_TAIL_MS).coerceAtLeast(0L)

/**
 * Cutoff position (ms) for First-Word Prompt mode: ~2200ms into the ayah,
 * clamped to `<= durationMs` (and `>= 0L`).
 */
internal fun firstWordPromptCutoffMs(durationMs: Long): Long =
    HIFZ_FIRST_WORD_PROMPT_MS.coerceAtMost(durationMs.coerceAtLeast(0L))
