package com.ghais.ui.screens.reciters.personal

import com.ghais.data.repository.RelatedQaris
import com.ghais.domain.model.Reciter
import kotlin.random.Random

/**
 * AGENT P8 — "New for you" weekly ritual shelf (Discover-Weekly-style appointment dynamic).
 *
 * Pure shelf-building logic: no repository, no I/O, no clock, no platform APIs.
 * The coordinator (RecitersScreen/Library) wires the inputs from the P1/P2
 * affinity source and calls [weeklyPicks] inside `remember(weekSeed, …)` so the
 * lineup is stable all week and refreshes every Monday.
 *
 * Mix recipe (`limit` = 10 by default):
 * - **Unplayed high-affinity core** (`limit − 2` slots): eligible reciters
 *   ranked by [affinities] (missing slug = 0.5 neutral, the TasteScoring
 *   baseline), led by a rotating anchor plus its [RelatedQaris.relatedTo]
 *   neighbourhood.
 * - **Exploration tail** (up to 2 slots): seeded shuffle of whatever eligible
 *   voices missed the core — the discovery payoff.
 *
 * Coordinator wiring (all params are plain data so this file stays testable):
 * - `reciters` = `QuranDataRepository.getBrowseReciters()`.
 * - `affinities: Map<String, Float>` = P1/P2 source. To make the related leg
 *   track the charts, blend `ChartsRepository.snapshot.topQaris7d` slugs into
 *   the map (e.g. +0.1) before calling — the anchor then follows the weekly
 *   top without this file touching the network.
 * - `historySlugs` = `UserUsageRepository.history.map { it.reciterSlug }`
 *   ([JumpBackInItem.reciterSlug]); `followedSlugs` = `FollowStore.followedSlugs`.
 * - `weekSeed` = [weekSeed] of the current epoch day.
 *
 * Exclusions mirror [todayDeck][com.ghais.ui.screens.reciters.deck.todayDeck]:
 * played + followed slugs are dropped (slug match is case-insensitive,
 * `_`/`-` agnostic); when exclusion would starve the shelf below [limit], the
 * remainder is topped up from the excluded pool in seeded order instead of
 * rendering an empty rail.
 */
const val WeeklyRitualMaxPicks = 10

/** Exploration-tail size inside [weeklyPicks] (Discover-Weekly discovery payoff). */
const val WeeklyRitualExplorationCount = 2

private const val EXPLORATION_XOR = 0x9E3779B9L

/**
 * Monday-anchored week seed for an [epochDay] (whole days since epoch).
 *
 * `epochDay - ((dow + 6) % 7)` in Monday-first terms, computed with epoch math
 * only — no timezone, no platform clock. The Unix epoch (1970-01-01) was a
 * Thursday, so Monday-first day-of-week is `floorMod(epochDay + 3, 7)` and the
 * seed is that week's Monday epoch-day. Every day Mon–Sun of one calendar week
 * maps to the same seed, so the shelf is stable all week and flips on Monday.
 *
 * **Assumption:** [epochDay] counts whole UTC days since 1970-01-01 (the same
 * convention as [todayEpochDay][com.ghais.ui.screens.reciters.deck.todayEpochDay]).
 * Local-midnight callers must convert to UTC days first, or the flip lands on
 * Sunday/Monday local depending on offset.
 */
fun weekSeed(epochDay: Long): Long {
    val mondayFirstDow = floorModLong(epochDay + 3L, 7L)
    return epochDay - mondayFirstDow
}

/**
 * Shelf title. [weekSeed] is intentionally part of the signature (call-site
 * symmetry + future week-stamped titles, e.g. "Week of …") while the current
 * copy stays constant per the ritual contract.
 */
fun ritualTitle(weekSeed: Long): String = "New for you • this week"

/**
 * Builds the weekly ritual lineup.
 *
 * @param reciters full browse catalog (any order; never mutated).
 * @param affinities per-slug 0f..1f taste scores from the P1/P2 source; keys
 *   are matched case-insensitively (`_` ≡ `-`); unknown slugs score 0.5.
 * @param historySlugs played reciter slugs (unplayed-only rule); empty = no data.
 * @param followedSlugs followed reciter slugs (fresh-voices rule); empty = no data.
 * @param weekSeed Monday-anchored seed from [weekSeed]; same seed + same inputs
 *   always yields the same lineup.
 * @param limit shelf cap; the last [WeeklyRitualExplorationCount] slots go to
 *   exploration (capped by [limit] via the final take).
 */
fun weeklyPicks(
    reciters: List<Reciter>,
    affinities: Map<String, Float>,
    historySlugs: Set<String> = emptySet(),
    followedSlugs: Set<String> = emptySet(),
    weekSeed: Long,
    limit: Int = WeeklyRitualMaxPicks,
): List<Reciter> {
    if (reciters.isEmpty() || limit <= 0) return emptyList()

    val excluded = historySlugs.map(::normalizeRitualSlug).toSet() +
        followedSlugs.map(::normalizeRitualSlug).toSet()
    val fresh = reciters.filter { normalizeRitualSlug(it.slug) !in excluded }

    // Starvation refill: top up from the excluded pool in seeded order so the
    // shelf still fills up to `limit` instead of collapsing to empty.
    val pool = if (fresh.size >= limit) {
        fresh
    } else {
        val rng = Random(weekSeed)
        val refill = reciters
            .filter { normalizeRitualSlug(it.slug) in excluded }
            .shuffled(rng)
        (fresh + refill).distinctBy { normalizeRitualSlug(it.slug) }
    }
    if (pool.isEmpty()) return emptyList()

    val scores = affinities.mapKeys { normalizeRitualSlug(it.key) }
    val ranked = pool.sortedWith(
        compareByDescending<Reciter> { scores[normalizeRitualSlug(it.slug)] ?: 0.5f }
            .thenBy { normalizeRitualSlug(it.slug) }
            .thenBy { it.slug },
    )

    val mainCount = (limit - WeeklyRitualExplorationCount).coerceAtLeast(0)

    // Appointment dynamic: the related leg expands from an anchor that rotates
    // among the top-3 affinity voices by week, so consecutive weeks surface
    // different neighbourhoods even when taste and history are unchanged.
    val anchorWindow = ranked.take(3)
    val anchor = anchorWindow[floorModLong(weekSeed, anchorWindow.size.toLong()).toInt()]
    val anchorKey = normalizeRitualSlug(anchor.slug)
    val related = try {
        RelatedQaris.relatedTo(
            seed = anchor,
            candidates = pool.filter { normalizeRitualSlug(it.slug) != anchorKey },
            limit = limit,
        )
    } catch (_: Exception) {
        emptyList()
    }
    val core = (listOf(anchor) + related + ranked)
        .distinctBy { normalizeRitualSlug(it.slug) }
        .take(mainCount)

    val coreKeys = core.map { normalizeRitualSlug(it.slug) }.toSet()
    val exploration = pool
        .filter { normalizeRitualSlug(it.slug) !in coreKeys }
        .shuffled(Random(weekSeed xor EXPLORATION_XOR))
        .take(WeeklyRitualExplorationCount)

    return (core + exploration).take(limit)
}

fun normalizeRitualSlug(slug: String): String =
    slug.trim().lowercase().replace('_', '-')

private fun floorModLong(a: Long, b: Long): Long = ((a % b) + b) % b
