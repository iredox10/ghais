package com.ghais.personal

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.Mood
import com.ghais.ui.screens.reciters.taste.Pace
import com.ghais.ui.screens.reciters.taste.TasteProfile

/**
 * Implicit taste vector v2 (AGENT P2).
 *
 * A dense, attribute-level summary of what a listener likes, built from
 * implicit behavioural evidence plus optional explicit quiz priors. Pure
 * data + pure functions: no repository, no I/O, no clock, no randomness —
 * safe to call from any thread and trivially unit-testable.
 *
 * ## Input contract (P1 assumption)
 * [buildTasteVector] takes `signals` as the **output** of P1's
 * `scoreSignals` (`slug -> affinity`, decayed per-reciter totals). Only
 * positive affinities spread weight; zero/negative affinities (skips,
 * unfollows) are ignored so aversion never creates positive attribute
 * mass. Slugs with no match in [catalog] (after trim + case-insensitive
 * fallback) are ignored.
 *
 * NOTE on the P1 API as actually built (`com.ghais.personal.SignalsKt`):
 * `scoreSignals(signals: List<ReciterSignal>, todayDay: Long,
 * halfLifeDays: Int = ...): Map<String, Float>` — i.e. it consumes raw
 * events and *returns* the `Map<String, Float>` this file consumes. Wire
 * them as `buildTasteVector(scoreSignals(events, today), catalog, quiz)`.
 *
 * ## Spreading rule
 * Each positive affinity `w` for a known reciter adds `w` to exactly one
 * bucket per dimension: its [paceOf] pace (tempo stems, style-school
 * fallback), every mood in [moodsOf], and its normalized style / riwayah /
 * country keys. Attribute keys are `lowercase().trim()`; blank style or
 * country contributes to no bucket in that dimension.
 *
 * ## Quiz blend
 * Only a **completed** quiz contributes priors: `quiz.pace` and
 * `quiz.mood` each add [QUIZ_PRIOR_WEIGHT] to their bucket. An absent,
 * unfinished, or answered-but-not-completed quiz contributes nothing.
 * Explicit voice verdicts (`likedSlugs`/`skippedSlugs`) intentionally stay
 * in v1 scoring ([com.ghais.ui.screens.reciters.taste.scoreReciter]) and
 * are NOT re-spread here, so this vector remains purely implicit + priors.
 *
 * ## Confidence
 * `confidence = min(1, totalWeight / 20)`, where `totalWeight` = sum of
 * attributed positive signal affinities + applied quiz prior weight.
 *
 * Predicate stems mirror v1
 * (`com.ghais.ui.screens.reciters.taste.TasteScoringKt`) semantics but are
 * duplicated locally so this file has no cross-package `internal`
 * dependency.
 */
data class TasteVectorV2(
    val paceW: Map<Pace, Float> = emptyMap(),
    val moodW: Map<Mood, Float> = emptyMap(),
    val styleW: Map<String, Float> = emptyMap(),
    val riwayahW: Map<String, Float> = emptyMap(),
    val countryW: Map<String, Float> = emptyMap(),
    val confidence: Float = 0f,
)

/** Weight added per quiz prior (pace, mood) when the quiz is completed. */
const val QUIZ_PRIOR_WEIGHT: Float = 2.0f

/** Total evidence weight at which [TasteVectorV2.confidence] saturates to 1. */
const val CONFIDENCE_FULL_WEIGHT: Float = 20f

/** Per-dimension weights used by [affinityFor]; ordered pace/mood > style > country > riwayah. */
private const val W_PACE: Float = 1.0f
private const val W_MOOD: Float = 1.0f
private const val W_STYLE: Float = 0.75f
private const val W_COUNTRY: Float = 0.5f
private const val W_RIWAYAH: Float = 0.25f

/**
 * Builds a [TasteVectorV2] from decayed per-reciter affinities.
 *
 * Pure and deterministic: equal inputs always yield equal vectors (map
 * equality is order-insensitive, and every accumulation is a commutative
 * float sum keyed by attribute, never by iteration order).
 */
fun buildTasteVector(
    signals: Map<String, Float>,
    catalog: List<Reciter>,
    quiz: TasteProfile?,
): TasteVectorV2 {
    val paceW = HashMap<Pace, Float>()
    val moodW = HashMap<Mood, Float>()
    val styleW = HashMap<String, Float>()
    val riwayahW = HashMap<String, Float>()
    val countryW = HashMap<String, Float>()

    val bySlug = HashMap<String, Reciter>(catalog.size * 2)
    val bySlugLower = HashMap<String, Reciter>(catalog.size * 2)
    for (reciter in catalog) {
        val slug = reciter.slug.trim()
        if (slug.isEmpty()) continue
        bySlug[slug] = reciter
        bySlugLower[slug.lowercase()] = reciter
    }

    var signalWeight = 0f
    for ((rawSlug, affinity) in signals) {
        if (affinity <= 0f || affinity.isNaN()) continue
        val slug = rawSlug.trim()
        if (slug.isEmpty()) continue
        val reciter = bySlug[slug] ?: bySlugLower[slug.lowercase()] ?: continue
        signalWeight += affinity
        paceOf(reciter)?.let { paceW[it] = (paceW[it] ?: 0f) + affinity }
        for (mood in moodsOf(reciter)) {
            moodW[mood] = (moodW[mood] ?: 0f) + affinity
        }
        styleKeyOf(reciter)?.let { styleW[it] = (styleW[it] ?: 0f) + affinity }
        riwayahKeyOf(reciter)?.let { riwayahW[it] = (riwayahW[it] ?: 0f) + affinity }
        countryKeyOf(reciter)?.let { countryW[it] = (countryW[it] ?: 0f) + affinity }
    }

    var quizWeight = 0f
    if (quiz?.completed == true) {
        if (quiz.pace != null) {
            paceW[quiz.pace] = (paceW[quiz.pace] ?: 0f) + QUIZ_PRIOR_WEIGHT
            quizWeight += QUIZ_PRIOR_WEIGHT
        }
        if (quiz.mood != null) {
            moodW[quiz.mood] = (moodW[quiz.mood] ?: 0f) + QUIZ_PRIOR_WEIGHT
            quizWeight += QUIZ_PRIOR_WEIGHT
        }
    }

    val confidence = ((signalWeight + quizWeight) / CONFIDENCE_FULL_WEIGHT).coerceIn(0f, 1f)
    return TasteVectorV2(
        paceW = paceW.toMap(),
        moodW = moodW.toMap(),
        styleW = styleW.toMap(),
        riwayahW = riwayahW.toMap(),
        countryW = countryW.toMap(),
        confidence = confidence,
    )
}

/**
 * Ranks a [reciter] against a [vector]: weighted sum of the reciter's
 * attribute buckets (pace + best-fitting mood + style + country +
 * riwayah). Missing buckets count 0. Unbounded (grows with evidence mass)
 * — compare relatively for ranking, not as a 0..1 score.
 */
fun affinityFor(reciter: Reciter, vector: TasteVectorV2): Float {
    val pace = paceOf(reciter)?.let { vector.paceW[it] } ?: 0f
    var mood = 0f
    for (m in moodsOf(reciter)) {
        val w = vector.moodW[m] ?: 0f
        if (w > mood) mood = w
    }
    val style = styleKeyOf(reciter)?.let { vector.styleW[it] } ?: 0f
    val country = countryKeyOf(reciter)?.let { vector.countryW[it] } ?: 0f
    val riwayah = riwayahKeyOf(reciter)?.let { vector.riwayahW[it] } ?: 0f
    return pace * W_PACE + mood * W_MOOD + style * W_STYLE + country * W_COUNTRY + riwayah * W_RIWAYAH
}

/** Classifies a reciter into a [Pace] via tempo stems, falling back to style school. */
private fun paceOf(reciter: Reciter): Pace? {
    val t = reciter.tempo.trim().lowercase()
    if (t.isNotEmpty()) {
        if (t.contains("slow") || t.contains("deliberate") || t.contains("precise") ||
            t.contains("calm") || t.contains("reflective") || t.contains("serene") ||
            t.contains("tearful")
        ) return Pace.SLOW
        if (t.contains("flowing") || t.contains("brisk") || t.contains("fast") ||
            t.contains("expressive") || t.contains("powerful") || t.contains("melodic")
        ) return Pace.FLOWING
        if (t.contains("measured") || t.contains("steady") || t.contains("medium") ||
            t.contains("reverent") || t.contains("clean") || t.contains("resonant") ||
            t.contains("rich") || t.contains("vocal") || t.contains("emotive")
        ) return Pace.MEASURED
    }
    // Generic/unknown tempo ("Eclectic", blank, …): fall back to style school
    // so medium-by-default catalog rows still carry a pace signal.
    val s = reciter.style.trim().lowercase()
    if (s.isEmpty()) return null
    if (s.contains("mujawwad")) return Pace.SLOW
    if (s.contains("taraweeh") || s.contains("modern")) return Pace.FLOWING
    if (s.contains("muallim") || s.contains("teacher") || s.contains("educational") ||
        s.contains("murattal")
    ) return Pace.MEASURED
    return null
}

/** Infers every mood a reciter's style/country/tempo fits (mirrors v1 moodFit predicates). */
private fun moodsOf(reciter: Reciter): Set<Mood> {
    val s = reciter.style.trim().lowercase()
    val c = reciter.country.trim().lowercase()
    val t = reciter.tempo.trim().lowercase()
    val out = HashSet<Mood>(3)
    val calmStyle = s.contains("mujawwad") || s.contains("murattal classic") ||
        s.contains("tadabbur") || s.contains("egyptian") || s.contains("madinah") ||
        s.contains("hijazi")
    val calmHeritage = c.contains("egypt") && (calmStyle || s.contains("murattal"))
    val calmTempo = t.contains("slow") || t.contains("calm") || t.contains("serene") ||
        t.contains("reflective")
    if (calmStyle || calmHeritage || calmTempo) out.add(Mood.CALM)
    val upliftingStyle = s.contains("haramain") || s.contains("modern") ||
        s.contains("maqamat") || s.contains("taraweeh") || s.contains("melodic")
    val upliftingTempo = t.contains("flowing") || t.contains("brisk") || t.contains("fast") ||
        t.contains("powerful") || t.contains("expressive")
    if (upliftingStyle || c.contains("saudi") || upliftingTempo) out.add(Mood.UPLIFTING)
    val focusStyle = s.contains("muallim") || s.contains("teacher") ||
        s.contains("educational") || s.contains("tajweed") || s.contains("murattal")
    val focusTempo = t.contains("measured") || t.contains("steady") ||
        t.contains("precise") || t.contains("medium") || t.contains("clean")
    if (focusStyle || focusTempo) out.add(Mood.FOCUS)
    return out
}

private fun styleKeyOf(reciter: Reciter): String? =
    reciter.style.trim().lowercase().takeIf { it.isNotEmpty() }

private fun riwayahKeyOf(reciter: Reciter): String? =
    reciter.riwayah.trim().lowercase().takeIf { it.isNotEmpty() }

private fun countryKeyOf(reciter: Reciter): String? =
    reciter.country.trim().lowercase().takeIf { it.isNotEmpty() }
