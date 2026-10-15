package com.ghais.ui.screens.reciters.deck

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.TasteProfile
import com.ghais.ui.screens.reciters.taste.scoreReciter
import kotlin.random.Random
import kotlin.time.Clock

/**
 * AGENT A4 — "Today's voices" swipe discovery deck: pure deck-building logic.
 * AGENT P7 — personalized deck v2 (affinity-aware recipe, additive).
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
 * P7 v2 recipe (only when [todayDeck]'s `affinities` param is non-empty):
 * - exploitation (up to [ExploitationSlotCount] slots): top-affine unplayed
 *   voices first, ties broken by the day seed.
 * - exploration ([ExplorationSlotCount] slots): low-affinity well-known voices
 *   the user never tried (well-known proxy = Hafs riwayah, the default most
 *   listeners know; no popularity signal exists in the catalog), labelled
 *   [deckReason] "Outside your usual".
 * - fresh anchors (remaining slots): same [AnchorScoreThreshold] rule as v1.
 * Final order is exploit → explore → fresh-anchor.
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

/** P7 v2: exploitation slots (top-affine unplayed first). */
const val ExploitationSlotCount = 6

/** P7 v2: exploration slots (low-affinity well-known voices never tried). */
const val ExplorationSlotCount = 2

/**
 * P7 v2: normalized affinity at or below this counts as low-affinity
 * (exploration candidate + "Outside your usual" [deckReason]).
 */
const val ExplorationAffinityThreshold = 0.35f

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
 * @param affinities P7 v2: normalized-or-raw reciter slug → 0f..1f affinity
 *   (e.g. from P1 SignalStore). Empty (default) = v1 recipe, so existing call
 *   sites compile and behave unchanged. Non-empty = v2 recipe (exploit 6 /
 *   explore 2 / fresh-anchor 2, see file KDoc). Keys are normalized with
 *   [normalizeDeckSlug]; missing slugs score 0f (never tried).
 */
fun todayDeck(
    reciters: List<Reciter>,
    taste: TasteProfile? = null,
    history: Set<String> = emptySet(),
    followedSlugs: Set<String> = emptySet(),
    epochDay: Long = todayEpochDay(),
    maxCards: Int = MaxDeckCards,
    affinities: Map<String, Float> = emptyMap()
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

    val normalizedAffinities = affinities.mapKeys { normalizeDeckSlug(it.key) }
    if (normalizedAffinities.isNotEmpty()) {
        return affinityDeck(pool, taste, normalizedAffinities, rng, maxCards)
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

/**
 * AGENT P7 — v2 affinity-aware deck (exploit → explore → fresh-anchor).
 *
 * Pure and deterministic: every shuffle/sort draws from [rng] (seeded with
 * the epoch day by [todayDeck]) and Kotlin sorts are stable, so ties keep
 * seeded order. Always fills to `min(pool.size, maxCards)` with no
 * duplicates: leftover slots fall back to seeded pool order.
 */
private fun affinityDeck(
    pool: List<Reciter>,
    taste: TasteProfile?,
    affinities: Map<String, Float>,
    rng: Random,
    maxCards: Int
): List<Reciter> {
    fun affinityOf(reciter: Reciter): Float =
        affinities[normalizeDeckSlug(reciter.slug)] ?: 0f
    fun isWellKnown(reciter: Reciter): Boolean =
        reciter.riwayah.trim().equals("Hafs", ignoreCase = true)

    // Seeded shuffle first so every rank below has a deterministic tie-break.
    val shuffled = pool.shuffled(rng)

    // Exploitation: top-affine unplayed first.
    val exploitWant = minOf(ExploitationSlotCount, maxCards)
    val exploit = shuffled.sortedByDescending(::affinityOf).take(exploitWant)
    val exploitKeys = exploit.map { normalizeDeckSlug(it.slug) }.toSet()

    // Exploration: lowest-affinity voices never tried, well-known (Hafs)
    // leading — the "Outside your usual" slots.
    val exploreWant = minOf(ExplorationSlotCount, (maxCards - exploit.size).coerceAtLeast(0))
    val explore = shuffled
        .filter { normalizeDeckSlug(it.slug) !in exploitKeys }
        .sortedWith(compareBy({ if (isWellKnown(it)) 0 else 1 }, ::affinityOf))
        .take(exploreWant)
    val takenKeys = exploitKeys + explore.map { normalizeDeckSlug(it.slug) }.toSet()

    // Fresh anchors: same threshold rule as v1, from whatever remains
    // (seeded order when taste is unknown).
    val anchorWant = (maxCards - exploit.size - explore.size).coerceAtLeast(0)
    val remainder = shuffled.filter { normalizeDeckSlug(it.slug) !in takenKeys }
    val anchors = if (hasTasteSignals(taste)) {
        remainder.filter { scoreReciter(it, taste!!) >= AnchorScoreThreshold }.take(anchorWant)
    } else {
        remainder.take(anchorWant)
    }

    // Fill any shortfall (e.g. thin anchor pool) from the leftover pool in
    // seeded order so the sheet still gets a full lineup.
    val deck = ArrayList<Reciter>(maxCards)
    deck.addAll(exploit)
    deck.addAll(explore)
    deck.addAll(anchors)
    if (deck.size < maxCards) {
        val deckKeys = deck.map { normalizeDeckSlug(it.slug) }.toSet()
        deck.addAll(shuffled.filter { normalizeDeckSlug(it.slug) !in deckKeys }.take(maxCards - deck.size))
    }
    return deck.take(maxCards)
}

/**
 * AGENT P7 — per-card reason line for the v2 deck ("Loved by slow-pace
 * listeners" style, from reciter attributes; low-affinity voices read
 * "Outside your usual"). Pure and never blank, so [DeckCard] and tests share
 * one source of truth.
 */
fun deckReason(reciter: Reciter, affinities: Map<String, Float> = emptyMap()): String {
    val affinity = affinities.mapKeys { normalizeDeckSlug(it.key) }[normalizeDeckSlug(reciter.slug)]
    if (affinity != null && affinity <= ExplorationAffinityThreshold) {
        return "Outside your usual"
    }
    val tempo = reciter.tempo.lowercase()
    val style = reciter.style.lowercase()
    val riwayahRaw = reciter.riwayah.trim()
    val slowHit = tempo.contains("slow") || tempo.contains("deliberate") ||
        tempo.contains("precise") || tempo.contains("calm") ||
        tempo.contains("reflective") || tempo.contains("serene") || tempo.contains("tearful")
    if (slowHit) return "Loved by slow-pace listeners"
    val flowingHit = tempo.contains("flowing") || tempo.contains("brisk") ||
        tempo.contains("fast") || tempo.contains("expressive") ||
        tempo.contains("powerful") || tempo.contains("melodic")
    if (flowingHit) return "Loved by flowing-pace listeners"
    if (style.contains("mujawwad")) return "A mujawwad favourite"
    if (style.contains("muallim") || style.contains("teacher") || style.contains("educational")) {
        return "A study-steady favourite"
    }
    if (style.contains("taraweeh") || style.contains("haramain") || style.contains("maqamat")) {
        return "A taraweeh-night favourite"
    }
    if (riwayahRaw.isNotEmpty() && !riwayahRaw.equals("Hafs", ignoreCase = true)) {
        return "A $riwayahRaw-path rarity"
    }
    val steadyHit = tempo.contains("measured") || tempo.contains("steady") ||
        tempo.contains("medium") || tempo.contains("reverent") ||
        tempo.contains("clean") || tempo.contains("resonant") || tempo.contains("rich")
    if (steadyHit) return "Loved by steady-pace listeners"
    return "A listener favourite"
}

/**
 * AGENT P7 — deck-outcome hook: call on every follow/skip so personalization
 * can learn which voices land. Intentional no-op stub for now.
 *
 * One-line swap once P1 SignalStore lands (coordinator wires later):
 * `SignalStore.recordDeckOutcome(slug, followed)`.
 */
@Suppress("UNUSED_PARAMETER")
fun recordDeckOutcome(slug: String, followed: Boolean) {
    // P1 SignalStore landed: deck verdicts are taste votes.
    try {
        if (followed) com.ghais.personal.SignalStore.recordFollow(slug)
        else com.ghais.personal.SignalStore.recordSkipQuick(slug)
    } catch (_: Exception) {
    }
}
