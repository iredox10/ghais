package com.ghais.personal

import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * Implicit-feedback weight per signal kind (Spotify-style, on-device).
 *
 * Positive kinds mean affinity, negative kinds mean aversion. The numeric
 * values ARE the default [ReciterSignal.weight] recorded for that kind, so
 * changing a value here only affects signals recorded AFTER the change —
 * already-stored signals keep the weight they were recorded with.
 */
@Serializable
enum class SignalKind(val weight: Float) {
    PLAY_STARTED(1f),
    PLAY_COMPLETED(3f),
    PREVIEW(1f),
    FOLLOW(4f),
    SEARCH_OPEN(2f),
    DOWNLOAD(2f),
    SKIP_QUICK(-2f),
    UNFOLLOW(-4f),
}

/**
 * One implicit-feedback event for a reciter.
 *
 * @param slug reciter slug, trimmed, never blank (stores drop blanks on write).
 * @param kind what happened; kept alongside [weight] so future weight retunes
 *   stay interpretable (kind = cause, weight = effect at record time).
 * @param epochDay day the event happened on (`epochMs / 86_400_000`), same
 *   math as `UserUsageRepository`. May lie in the future on clock-skewed
 *   devices — [scoreSignals] clamps those to full weight.
 * @param weight scoring contribution before time decay; defaults to
 *   [SignalKind.weight] but is stored explicitly so scoring stays stable even
 *   if the enum values are retuned later. Callers may override it for
 *   experiments; [scoreSignals] always honors the stored value, never the
 *   enum's current value.
 */
@Serializable
data class ReciterSignal(
    val slug: String,
    val kind: SignalKind,
    val epochDay: Long,
    val weight: Float = kind.weight,
)

/** Default half-life used by [scoreSignals] when the caller passes none. */
const val DEFAULT_HALF_LIFE_DAYS = 30

/**
 * Scores signals into per-reciter affinity with exponential time decay.
 *
 * Pure function: no I/O, no clock, deterministic on its inputs — the seam
 * ranking agents call.
 *
 * `score(slug) = Σ weight_i * 0.5 ^ (ageDays_i / halfLifeDays)`,
 * where `ageDays = max(0, todayDay - epochDay)` (future-dated signals score
 * at full weight). Negative weights (skip/unfollow) subtract, so a slug's
 * total can be negative — callers that need non-negative input must clamp.
 *
 * @param halfLifeDays days until a signal counts half; coerced to >= 1, so
 *   `0`/negatives mean "almost instant decay", never divide-by-zero.
 */
fun scoreSignals(
    signals: List<ReciterSignal>,
    todayDay: Long,
    halfLifeDays: Int = DEFAULT_HALF_LIFE_DAYS,
): Map<String, Float> {
    if (signals.isEmpty()) return emptyMap()
    val halfLife = halfLifeDays.coerceAtLeast(1).toDouble()
    val totals = HashMap<String, Double>()
    for (signal in signals) {
        val slug = signal.slug.trim()
        if (slug.isEmpty()) continue
        val weight = signal.weight.toDouble()
        if (weight == 0.0) continue
        val ageDays = (todayDay - signal.epochDay).coerceAtLeast(0L).toDouble()
        val decay = 0.5.pow(ageDays / halfLife)
        totals[slug] = (totals[slug] ?: 0.0) + weight * decay
    }
    return totals.mapValues { it.value.toFloat() }
}
