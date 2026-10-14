package com.ghais.ui.screens.reciters.deck

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.screens.reciters.taste.scoreReciter
import kotlin.random.Random
import kotlin.time.Clock

/**
 * AGENT A4 — "Today's voices" swipe discovery deck: pure deck-building logic.
 *
 * UI-free on purpose so it stays unit-testable in commonTest. The deck is
 * deterministic per calendar day: the same epoch-day + the same inputs always
 * yields the same 10 cards, so every user gets one stable daily lineup.
 *
 * Taste comes from the canonical A1 quiz model
 * ([TasteProfile][com.ghais.ui.screens.reciters.taste.TasteProfile]) ranked
 * through A1's [scoreReciter] — this file owns only the deck recipe on top:
 *
 * Mix recipe (max 10 cards):
 * - familiar anchors (up to [FamiliarAnchorCount]): pool members scoring at
 *   or above [AnchorScoreThreshold] — the recognition hook.
 * - edge-of-taste picks: everything else below the threshold, for discovery.
 * Anchors lead (position 0) then interleave every third slot (3, 6, 9).
 *
 * Already-followed / already-listened slugs are excluded *where the data is
 * available* (non-empty sets), plus the quiz's explicit [TasteProfile.skippedSlugs].
 * When exclusion would starve the deck below [MaxDeckCards], the remainder is
 * topped up from the excluded pool in seeded order so the sheet still
 * delivers a full lineup instead of an empty state.
 */

const val MaxDeckCards = 10
const val FamiliarAnchorCount = 4

/** Minimum [scoreReciter] affinity for a familiar anchor (see recipe above). */
const val AnchorScoreThreshold = 0.65f

const val MillisPerDay = 86_400_000L

/** Calendar-day seed: whole days since epoch (UTC), mirroring HomeDiscovery. */
fun todayEpochDay(nowMs: Long = Clock.System.now().toEpochMilliseconds()): Long =
    nowMs / MillisPerDay

fun normalizeDeckSlug(slug: String): String =
    slug.trim().lowercase().replace('_', '-')

/**
 * Builds today's discovery deck.
 *
 * @param reciters full browse catalog (any order; never mutated).
 * @param taste quiz taste, or null when unknown (deck is all edge picks).
 * @param history listened reciter slugs (any casing); empty = no data, no exclusion.
 * @param followedSlugs followed reciter slugs; empty = no data, no exclusion.
 * @param epochDay calendar-day seed; defaults to today. Tests inject fixed days.
 * @param maxCards deck cap; defaults to [MaxDeckCards].
 */
fun todayDeck(
    reciters: List<Reciter>,
    taste: TasteProfile? = null,
    history: Set<String> = emptySet(),
    followedSlugs: Set<String> = emptySet(),
    epochDay: Long = todayEpochDay(),
    maxCards: Int = MaxDeckCards
): List<Reciter> {
    if (reciters.isEmpty() || maxCards <= 0) return emptyList()
    val rng = Random(epochDay)
    val excluded = history.map(::normalizeDeckSlug).toSet() +
        followedSlugs.map(::normalizeDeckSlug).toSet() +
        (taste?.skippedSlugs?.map(::normalizeDeckSlug).orEmpty().toSet())

    val fresh = reciters.filter { normalizeDeckSlug(it.slug) !in excluded }
    // Starvation refill: top up from the excluded pool (seeded order) so the
    // deck still fills up to maxCards instead of collapsing to empty.
    val pool = if (fresh.size >= maxCards) {
        fresh
    } else {
        val excludedReciters = reciters
            .filter { normalizeDeckSlug(it.slug) in excluded }
            .shuffled(rng)
        (fresh + excludedReciters).distinctBy { normalizeDeckSlug(it.slug) }
    }

    if (!hasTasteSignals(taste)) {
        return pool.shuffled(rng).take(maxCards)
    }
    val quiz = taste!!

    // Seeded shuffle FIRST, then split by affinity: the day seed drives which
    // voices surface while affinity drives the anchor/edge split. Stable and
    // deterministic for a fixed (epochDay, inputs) pair.
    val shuffled = pool.shuffled(rng)
    val anchors = shuffled
        .filter { scoreReciter(it, quiz) >= AnchorScoreThreshold }
        .take(FamiliarAnchorCount)
    val anchorKeys = anchors.map { normalizeDeckSlug(it.slug) }.toSet()
    val edges = shuffled
        .filter { normalizeDeckSlug(it.slug) !in anchorKeys }
        .take((maxCards - anchors.size).coerceAtLeast(0))

    // Interleave: anchor leads, then an anchor every third slot (3, 6, 9).
    val deck = ArrayList<Reciter>(maxCards)
    var ai = 0
    var ei = 0
    while (deck.size < maxCards && (ai < anchors.size || ei < edges.size)) {
        if (ai < anchors.size && (deck.isEmpty() || deck.size % 3 == 0 || ei >= edges.size)) {
            deck.add(anchors[ai++])
        } else if (ei < edges.size) {
            deck.add(edges[ei++])
        } else {
            deck.add(anchors[ai++])
        }
    }
    return deck
}

/** True when the quiz taste carries at least one anchor-usable signal. */
fun hasTasteSignals(taste: TasteProfile?): Boolean =
    taste != null && (taste.pace != null || taste.mood != null || taste.likedSlugs.isNotEmpty())

/**
 * Card why-line in the reel language: "Measured pace · Hafs".
 * Pure so [DeckCard] and tests share one source of truth.
 */
fun whyLineFor(reciter: Reciter): String {
    val pace = when (reciter.tempo.trim().lowercase()) {
        "slow" -> "Unhurried pace"
        "fast", "brisk" -> "Brisk pace"
        "medium" -> "Measured pace"
        else -> {
            val raw = reciter.tempo.trim()
            if (raw.isBlank()) "Steady pace" else raw + " pace"
        }
    }
    val riwayah = reciter.riwayah.trim().ifBlank { "Hafs" }
    return "$pace · $riwayah"
}

/**
 * Optional match chip for anchor cards, e.g. "Same measured pace as your pick".
 * Null when there is no taste match — the card then shows only the why-line.
 */
fun matchReasonFor(reciter: Reciter, taste: TasteProfile?): String? {
    if (!hasTasteSignals(taste)) return null
    val quiz = taste!!
    if (normalizeDeckSlug(reciter.slug) in quiz.likedSlugs.map(::normalizeDeckSlug)) {
        return "One of your liked voices"
    }
    if (scoreReciter(reciter, quiz) < AnchorScoreThreshold) return null
    val pace = quiz.pace
    if (pace != null) {
        val label = when (pace) {
            com.ghais.ui.screens.reciters.taste.Pace.SLOW -> "slow"
            com.ghais.ui.screens.reciters.taste.Pace.MEASURED -> "measured"
            com.ghais.ui.screens.reciters.taste.Pace.FLOWING -> "flowing"
        }
        return "Same $label pace as your pick"
    }
    val mood = quiz.mood
    if (mood != null) {
        return "Picked for your ${mood.name.lowercase()} mood"
    }
    return "Matches your taste"
}
