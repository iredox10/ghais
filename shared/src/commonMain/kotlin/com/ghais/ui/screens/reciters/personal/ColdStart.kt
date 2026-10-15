package com.ghais.ui.screens.reciters.personal

import com.ghais.domain.model.DEVOTIONAL_TRACK_SLUG
import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.Mood
import com.ghais.ui.screens.reciters.taste.Pace
import com.ghais.ui.screens.reciters.taste.TasteProfile

/**
 * AGENT P9 — cold-start upgrade: returning users skip the taste quiz via history import.
 *
 * Pure logic (no store, no I/O, safe to call from any thread and trivially
 * unit-testable). The Compose side lives in [ColdStartGate]; this file only
 * decides "warm enough?" and derives a [TasteProfile] from past listening.
 *
 * ## Sources (merged by the caller, see [ColdStartGate])
 * - listening history slugs (`UserUsageRepository.history.map { it.reciterSlug }`,
 *   most-recent-first, repeats included — repeats weight the majority votes),
 * - followed slugs (`FollowStore.followedSlugs`),
 * - recent reciter-search slugs (`ReciterSearchHistoryStore.recentSlugs`),
 * - the browse catalog (`QuranDataRepository.getBrowseReciters`) for
 *   tempo/style resolution.
 *
 * ## Warmth rule
 * >= [COLD_START_WARM_THRESHOLD] distinct reciter slugs across ANY source
 * (union, not sum — one slug followed, listened, and searched still counts
 * once). Below that the quiz still shows.
 *
 * ## Import rule ([importTasteFromHistory])
 * - `pace` = frequency-weighted majority of [tempoFamilyOf] over the
 *   catalog-resolved slugs (unresolvable/stale slugs abstain);
 * - `mood` = frequency-weighted majority of [moodOf] over the same rows;
 * - `likedSlugs` = top slugs by frequency (ties → first-seen) capped at
 *   [COLD_START_LIKED_CAP], spelled as the catalog spells them so A1's
 *   `scoreReciter` exact-matches them (falls back to the raw trimmed slug
 *   when the slug is no longer in the catalog);
 * - `completed = true`, `skippedSlugs` empty (an import never fabricates a
 *   rejection).
 */
const val COLD_START_WARM_THRESHOLD: Int = 3

/** Cap on imported [TasteProfile.likedSlugs] — mirrors the deck's 10-card lineup. */
const val COLD_START_LIKED_CAP: Int = 10

/**
 * Normalizes one raw slug into the identity key used for counting, or null
 * when it carries no reciter identity (blank, or the devotional sentinel,
 * which is a supplication — not a reciter — and must never warm the gate).
 */
fun cleanColdStartSlug(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed == DEVOTIONAL_TRACK_SLUG) return null
    val key = trimmed.lowercase().replace('_', '-')
    if (key.isEmpty() || key == DEVOTIONAL_TRACK_SLUG.lowercase().replace('_', '-')) return null
    return key
}

/**
 * Union of distinct reciter identities across the three sources, in
 * first-seen order (history → followed → recents, as passed in).
 */
fun distinctColdStartSlugs(
    historySlugs: Collection<String>,
    followed: Set<String>,
    searchRecents: List<String>
): Set<String> {
    val out = LinkedHashSet<String>()
    for (raw in historySlugs) cleanColdStartSlug(raw)?.let(out::add)
    for (raw in followed) cleanColdStartSlug(raw)?.let(out::add)
    for (raw in searchRecents) cleanColdStartSlug(raw)?.let(out::add)
    return out
}

/** True when the union of sources holds >= [COLD_START_WARM_THRESHOLD] distinct reciters. */
fun hasUsableHistory(
    historySlugs: Collection<String>,
    followed: Set<String>,
    searchRecents: List<String>
): Boolean =
    distinctColdStartSlugs(historySlugs, followed, searchRecents).size >= COLD_START_WARM_THRESHOLD

/**
 * Classifies a free-form tempo string into a [Pace] family, or null when
 * unrecognised.
 *
 * NOTE — intentional duplication of A1's `tempoFamily` in
 * `ui/screens/reciters/taste/TasteScoring.kt` (internal there, so it cannot
 * be reused without widening its visibility; widening A1's internals for P9
 * would couple the quiz scorer to the import path). Keep the two in sync:
 * SLOW stems first (so `"slow & …"` strings win before flowing/melodic
 * checks), then FLOWING, then MEASURED. One deliberate difference: this
 * takes the RAW tempo and lowercases it (A1's takes a pre-lowercased
 * string), so callers pass `reciter.tempo` straight through.
 */
fun tempoFamilyOf(tempo: String): Pace? {
    val t = tempo.trim().lowercase()
    if (t.isEmpty()) return null
    if (
        t.contains("slow") || t.contains("deliberate") || t.contains("precise") ||
        t.contains("calm") || t.contains("reflective") || t.contains("serene") ||
        t.contains("tearful")
    ) return Pace.SLOW
    if (
        t.contains("flowing") || t.contains("brisk") || t.contains("fast") ||
        t.contains("expressive") || t.contains("powerful") || t.contains("melodic")
    ) return Pace.FLOWING
    if (
        t.contains("measured") || t.contains("steady") || t.contains("medium") ||
        t.contains("reverent") || t.contains("clean") || t.contains("resonant") ||
        t.contains("rich") || t.contains("vocal") || t.contains("emotive")
    ) return Pace.MEASURED
    return null
}

/**
 * Maps one catalog row to the [Mood] whose school it belongs to, or null
 * when it matches none.
 *
 * NOTE — intentional duplication of A1's `moodFit` keyword sets in
 * `TasteScoring.kt` (same reason as [tempoFamilyOf]; keep in sync). `moodFit`
 * scores one mood at a time, so the inverse needs a tie-break: distinctive
 * schools win over the generic one in CALM → UPLIFTING → FOCUS order. That
 * order matters because FOCUS matches bare `"murattal"` (nearly the whole
 * seed catalog) — checking it first would flatten every import to FOCUS.
 */
fun moodOf(reciter: Reciter): Mood? {
    val s = reciter.style.lowercase()
    val c = reciter.country.lowercase()
    val t = reciter.tempo.lowercase()
    val calmStyle = s.contains("mujawwad") || s.contains("murattal classic") ||
        s.contains("tadabbur") || s.contains("egyptian") || s.contains("madinah") ||
        s.contains("hijazi")
    val calmHeritage = c.contains("egypt") && (calmStyle || s.contains("murattal"))
    val calmTempo = t.contains("slow") || t.contains("calm") || t.contains("serene") ||
        t.contains("reflective")
    if (calmStyle || calmHeritage || calmTempo) return Mood.CALM
    val upliftingStyle = s.contains("haramain") || s.contains("modern") || s.contains("maqamat") ||
        s.contains("taraweeh") || s.contains("melodic")
    val upliftingHeritage = c.contains("saudi")
    val upliftingTempo = t.contains("flowing") || t.contains("brisk") || t.contains("fast") ||
        t.contains("powerful") || t.contains("expressive")
    if (upliftingStyle || upliftingHeritage || upliftingTempo) return Mood.UPLIFTING
    val focusStyle = s.contains("muallim") || s.contains("teacher") || s.contains("educational") ||
        s.contains("tajweed") || s.contains("murattal")
    val focusTempo = t.contains("measured") || t.contains("steady") || t.contains("precise") ||
        t.contains("medium") || t.contains("clean")
    if (focusStyle || focusTempo) return Mood.FOCUS
    return null
}

/**
 * Derives a completed [TasteProfile] from past-listening slugs.
 *
 * @param slugs merged sources in signal order (history first — repeats act
 *   as extra majority votes — then followed, then recents). Callers that
 *   only have the union (no repeats) still get a correct result; frequency
 *   just degrades to first-seen order.
 * @param catalog tempo/style/country resolution (browse list); first row
 *   wins per slug. An empty catalog still imports `likedSlugs` (raw
 *   spellings) with null pace/mood rather than dropping the user's history.
 */
fun importTasteFromHistory(slugs: List<String>, catalog: List<Reciter>): TasteProfile {
    val counts = LinkedHashMap<String, Int>()
    val firstRaw = LinkedHashMap<String, String>()
    for (raw in slugs) {
        val key = cleanColdStartSlug(raw) ?: continue
        counts[key] = (counts[key] ?: 0) + 1
        if (!firstRaw.containsKey(key)) {
            val trimmed = raw.trim()
            if (trimmed.isNotEmpty()) firstRaw[key] = trimmed
        }
    }
    if (counts.isEmpty()) return TasteProfile(completed = true)

    // Frequency desc, first-seen asc — deterministic, history-ordered.
    val seenIndex = counts.keys.withIndex().associate { it.value to it.index }
    val ranked = counts.keys.sortedWith(
        compareByDescending<String> { counts[it] ?: 0 }
            .thenBy { seenIndex[it] ?: Int.MAX_VALUE }
    )

    val bySlug = LinkedHashMap<String, Reciter>()
    for (reciter in catalog) {
        val key = reciter.slug.trim().lowercase().replace('_', '-')
        if (key.isEmpty()) continue
        if (!bySlug.containsKey(key)) bySlug[key] = reciter
    }

    // Frequency-weighted majorities; LinkedHashMap insertion order is ranked
    // order, and maxByOrNull keeps the FIRST max, so ties break toward the
    // most-listened / most-recent family.
    val paceVotes = LinkedHashMap<Pace, Int>()
    val moodVotes = LinkedHashMap<Mood, Int>()
    for (key in ranked) {
        val reciter = bySlug[key] ?: continue
        val weight = counts[key] ?: 1
        tempoFamilyOf(reciter.tempo)?.let { paceVotes[it] = (paceVotes[it] ?: 0) + weight }
        moodOf(reciter)?.let { moodVotes[it] = (moodVotes[it] ?: 0) + weight }
    }
    val pace = paceVotes.maxByOrNull { it.value }?.key
    val mood = moodVotes.maxByOrNull { it.value }?.key

    // Catalog spelling wins so A1's exact-match scoring hits; stale slugs
    // keep their raw trimmed spelling rather than being dropped.
    val liked = ranked.take(COLD_START_LIKED_CAP).map { key ->
        bySlug[key]?.slug?.trim()?.takeIf { it.isNotEmpty() } ?: firstRaw[key].orEmpty()
    }.filter { it.isNotEmpty() }.toSet()

    return TasteProfile(
        pace = pace,
        mood = mood,
        likedSlugs = liked,
        skippedSlugs = emptySet(),
        completed = true
    )
}
