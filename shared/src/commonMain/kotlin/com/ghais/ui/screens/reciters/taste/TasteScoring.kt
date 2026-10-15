package com.ghais.ui.screens.reciters.taste

import com.ghais.domain.model.Reciter

/**
 * Pure reciter-vs-taste scorer (AGENT A1). No repository, no I/O, no
 * randomness — safe to call from any thread and trivially unit-testable.
 *
 * Returns a 0f..1f affinity: higher = better match. An empty taste
 * ([TasteProfile] defaults) scores a neutral 0.5 baseline so unranked
 * catalogs keep their existing order.
 *
 * ## Mapping
 * - **Explicit voice verdict ± strongest signal.** `likedSlugs` +0.30,
 *   `skippedSlugs` −0.35. A slug in both (should not happen via
 *   [TasteStore.recordVoicePick]) resolves as liked.
 * - **Pace ↔ tempo (+0.25 match / +0.08 neighbour).** [Pace.SLOW] matches
 *   tempos containing slow/deliberate/precise/calm/reflective/serene/tearful;
 *   [Pace.FLOWING] matches flowing/brisk/fast/expressive/powerful/melodic;
 *   [Pace.MEASURED] matches measured/steady/medium/reverent/clean/resonant/rich.
 *   (Seed reality: `Mp3QuranReciters` is almost all `"medium"` → MEASURED;
 *   `EveryAyahReciters` carries the descriptive tempos above; `tempo` match
 *   is case-insensitive `contains`.) Adjacent paces (SLOW↔MEASURED,
 *   MEASURED↔FLOWING) earn a +0.08 partial instead of the full +0.25.
 * - **Mood ↔ style/country (+0.20).** [Mood.CALM] → mujawwad/murattal-classic/
 *   tadabbur/egyptian-classic/madinah + Egyptian heritage voices;
 *   [Mood.UPLIFTING] → haramain/modern-murattal/maqamat/taraweeh/melodic +
 *   Saudi Haramain voices; [Mood.FOCUS] → teacher/muallim/educational/tajweed/
 *   murattal (study-steady voices, any nation). Style match is
 *   case-insensitive `contains`; country is a lightweight heritage proxy
 *   (Egypt ↔ CALM mujawwad school, Saudi Arabia ↔ UPLIFTING Haramain school).
 * - **Style↔pace coherence (+0.06).** mujawwad leans SLOW, muallim/teacher
 *   leans MEASURED, taraweeh/modern-murattal leans FLOWING — a small bonus
 *   when the reciter's school agrees with the requested pace even if the
 *   tempo string itself is the generic `"medium"`.
 * - **Riwayah familiarity (+0.04).** Hafs (the default most listeners know)
 *   earns a whisper of a bonus; non-Hafs (Warsh/Qaloon/Al-Duri/…) earns the
 *   same whisper only under FOCUS (study curiosity), otherwise nothing —
 *   riwayah must never outrank a voice verdict.
 *
 * Weights are ordered verdict > pace > mood > coherence > riwayah so an
 * explicit A/B pick always beats any heuristic guess.
 */
fun scoreReciter(reciter: Reciter, taste: TasteProfile): Float {
    var score = 0.5f

    val slug = reciter.slug.trim()
    if (slug.isNotEmpty()) {
        if (slug in taste.likedSlugs) score += 0.30f
        else if (slug in taste.skippedSlugs) score -= 0.35f
    }

    val tempo = reciter.tempo.lowercase()
    val style = reciter.style.lowercase()
    val country = reciter.country.lowercase()
    val riwayah = reciter.riwayah.lowercase()

    if (taste.pace != null) {
        val family = tempoFamily(tempo)
        score += when {
            family == taste.pace -> 0.25f
            family != null && isNeighbourPace(family, taste.pace) -> 0.08f
            else -> 0f
        }
        score += stylePaceCoherence(style, taste.pace)
    }

    if (taste.mood != null) {
        score += moodFit(style, country, tempo, taste.mood)
    }

    score += riwayahFit(riwayah, taste.mood)

    return score.coerceIn(0f, 1f)
}

/** Classifies a free-form tempo string into a [Pace] family, or null when unrecognised. */
internal fun tempoFamily(tempoLower: String): Pace? {
    val t = tempoLower.trim()
    if (t.isEmpty()) return null
    // SLOW first: "slow & ..." strings also contain emotive words, so the
    // slow stems must win before the flowing/melodic checks below.
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
    return null
}

internal fun isNeighbourPace(actual: Pace, wanted: Pace): Boolean =
    (actual == Pace.SLOW && wanted == Pace.MEASURED) ||
        (actual == Pace.MEASURED && (wanted == Pace.SLOW || wanted == Pace.FLOWING)) ||
        (actual == Pace.FLOWING && wanted == Pace.MEASURED)

/** Small school↔pace agreement bonus for generic-tempo (`"medium"`) rows. */
internal fun stylePaceCoherence(styleLower: String, pace: Pace): Float {
    val s = styleLower.trim()
    if (s.isEmpty()) return 0f
    val isMujawwad = s.contains("mujawwad")
    val isTeacher = s.contains("muallim") || s.contains("teacher") || s.contains("educational")
    val isFlowingSchool = s.contains("taraweeh") || s.contains("modern")
    return when (pace) {
        Pace.SLOW -> if (isMujawwad) 0.06f else 0f
        Pace.MEASURED -> if (isTeacher || (s.contains("murattal") && !isMujawwad)) 0.06f else 0f
        Pace.FLOWING -> if (isFlowingSchool) 0.06f else 0f
    }
}

internal fun moodFit(styleLower: String, countryLower: String, tempoLower: String, mood: Mood): Float {
    val s = styleLower
    val c = countryLower
    val t = tempoLower
    return when (mood) {
        Mood.CALM -> {
            val styleHit = s.contains("mujawwad") || s.contains("murattal classic") ||
                s.contains("tadabbur") || s.contains("egyptian") || s.contains("madinah") ||
                s.contains("hijazi")
            // Heritage counts only for calm-school voices: a bare Egypt tag
            // must not lift the teacher/muallim school above mujawwad.
            val heritageHit = c.contains("egypt") &&
                (styleHit || s.contains("murattal"))
            val tempoHit = t.contains("slow") || t.contains("calm") || t.contains("serene") ||
                t.contains("reflective")
            if (styleHit || heritageHit || tempoHit) 0.20f else 0f
        }
        Mood.UPLIFTING -> {
            val styleHit = s.contains("haramain") || s.contains("modern") || s.contains("maqamat") ||
                s.contains("taraweeh") || s.contains("melodic")
            val heritageHit = c.contains("saudi")
            val tempoHit = t.contains("flowing") || t.contains("brisk") || t.contains("fast") ||
                t.contains("powerful") || t.contains("expressive")
            if (styleHit || heritageHit || tempoHit) 0.20f else 0f
        }
        Mood.FOCUS -> {
            val styleHit = s.contains("muallim") || s.contains("teacher") || s.contains("educational") ||
                s.contains("tajweed") || s.contains("murattal")
            val tempoHit = t.contains("measured") || t.contains("steady") || t.contains("precise") ||
                t.contains("medium") || t.contains("clean")
            if (styleHit || tempoHit) 0.20f else 0f
        }
    }
}

internal fun riwayahFit(riwayahLower: String, mood: Mood?): Float {
    val r = riwayahLower.trim()
    if (r.isEmpty()) return 0f
    val isHafs = r.contains("hafs")
    if (isHafs) return 0.04f
    // Non-Hafs variety is a curiosity bonus for studious listeners only.
    return if (mood == Mood.FOCUS) 0.04f else 0f
}
