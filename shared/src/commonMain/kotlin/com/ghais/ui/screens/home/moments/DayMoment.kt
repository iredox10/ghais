package com.ghais.ui.screens.home.moments

/**
 * Agent A8 — time-aware Home hero model (pure, no Compose, no I/O).
 *
 * Maps the local hour (+ an optional prayer hint) to one of four day
 * [Moment]s, each with suggested copy and a suggested surah id. The surah
 * picks are thematic, not prescriptive:
 * - MORNING → Al-Fatihah (1): the opening, a beginning-of-day read.
 * - EVENING → Ya-Sin (36): a reflective afternoon/evening read.
 * - NIGHT → Al-Mulk (67): the well-known night recitation.
 * - DEEP_NIGHT → Al-Muzzammil (73): the night-vigil surah.
 *
 * Display names are resolved at render time from
 * `QuranDataRepository.getSurahById` (see [MomentHero.kt]); the ids here
 * are the stable 1..114 catalog ids and never change.
 *
 * Streak ownership: NONE here. Agent A7 owns the streak store
 * (`DevotionStreak.recordDevotionDay/stats`); this file takes no streak
 * input and writes no streak state.
 */

/** The four day-parts the Home hero distinguishes. */
enum class Moment {
    MORNING,
    EVENING,
    NIGHT,
    DEEP_NIGHT,
}

/**
 * What the hero shows for one [Moment].
 *
 * @param moment the resolved day-part.
 * @param greeting short eyebrow-style greeting ("Good morning").
 * @param copy one gentle line under the greeting — invitational, never
 *   guilt-laden.
 * @param surahId suggested surah (1..114 catalog id).
 * @param playAction reciter-neutral descriptor of the one-tap action; the
 *   hero resolves the actual reciter (default/fallback) at tap time.
 */
data class MomentSuggestion(
    val moment: Moment,
    val greeting: String,
    val copy: String,
    val surahId: Int,
    val playAction: MomentPlayAction,
)

/**
 * Reciter-neutral descriptor of the hero's one-tap play action.
 *
 * Carries no reciter slug on purpose: [MomentHero.kt] resolves the default
 * reciter (`QuranDataRepository.getFallbackReciter()`) at tap time, so a
 * reciter change or catalog update never leaves a stale slug baked into
 * the Home hero.
 */
data class MomentPlayAction(
    val surahId: Int,
    /** Button label fragment, e.g. "Play Al-Mulk". Resolved with the real surah name at render. */
    val labelPrefix: String = "Play",
    /** Accessibility / subtitle line. */
    val description: String = "Plays with your default reciter",
)

/**
 * Pure mapping of hour (+ optional prayer hint) to a [MomentSuggestion].
 *
 * Hour buckets (24h local hour):
 * - 5..11 → MORNING
 * - 12..17 → EVENING
 * - 18..22 → NIGHT
 * - 23, 0..4 → DEEP_NIGHT
 *
 * Out-of-range hours are normalized with `floorMod(24)` rather than
 * rejected, so platform clock quirks can never crash Home.
 *
 * @param prayerHint optional lowercase-or-mixed hint such as "fajr",
 *   "dhuhr", "asr", "maghrib", "isha" or "tahajjud". When it names a
 *   prayer whose moment disagrees with the hour bucket, the hint wins —
 *   prayer time is the stronger signal of the user's rhythm. Null/blank
 *   (the common case) keeps the hour bucket.
 */
fun momentFor(hour: Int, prayerHint: String?): MomentSuggestion {
    val hintMoment = prayerHint?.trim()?.lowercase()?.let(::momentForPrayerHint)
    val moment = hintMoment ?: momentForHour(floorMod24(hour))
    return suggestionFor(moment)
}

/** Hour → moment with no hint involved. Public for tests. */
fun momentForHour(hour: Int): Moment = when (floorMod24(hour)) {
    in 5..11 -> Moment.MORNING
    in 12..17 -> Moment.EVENING
    in 18..22 -> Moment.NIGHT
    else -> Moment.DEEP_NIGHT
}

private fun momentForPrayerHint(hint: String): Moment? = when {
    hint.contains("tahajjud") || hint.contains("qiyam") -> Moment.DEEP_NIGHT
    hint.contains("fajr") || hint.contains("sunrise") || hint.contains("shur") -> Moment.MORNING
    hint.contains("dhuhr") || hint.contains("asr") -> Moment.EVENING
    hint.contains("maghrib") -> Moment.NIGHT
    hint.contains("isha") -> Moment.NIGHT
    else -> null
}

private fun suggestionFor(moment: Moment): MomentSuggestion = when (moment) {
    Moment.MORNING -> MomentSuggestion(
        moment = moment,
        greeting = "Good morning",
        copy = "Begin the day with the Opening — a minute of calm before the noise.",
        surahId = 1, // Al-Fatihah
        playAction = MomentPlayAction(surahId = 1),
    )
    Moment.EVENING -> MomentSuggestion(
        moment = moment,
        greeting = "Good afternoon",
        copy = "A reflective pause for the middle of your day.",
        surahId = 36, // Ya-Sin
        playAction = MomentPlayAction(surahId = 36),
    )
    Moment.NIGHT -> MomentSuggestion(
        moment = moment,
        greeting = "Good evening",
        copy = "Unwind with the night recitation — gentle, unhurried.",
        surahId = 67, // Al-Mulk
        playAction = MomentPlayAction(surahId = 67),
    )
    Moment.DEEP_NIGHT -> MomentSuggestion(
        moment = moment,
        greeting = "A quiet night",
        copy = "The night is still. A few verses, at your own pace.",
        surahId = 73, // Al-Muzzammil
        playAction = MomentPlayAction(surahId = 73),
    )
}

private fun floorMod24(hour: Int): Int = ((hour % 24) + 24) % 24
