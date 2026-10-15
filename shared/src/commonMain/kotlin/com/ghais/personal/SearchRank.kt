package com.ghais.personal

import com.ghais.domain.model.Reciter

/**
 * AGENT P6 — taste-ranked reciter search ordering.
 *
 * Pure ranking for the already-matched search candidates owned by
 * `ReciterSearchScreen` (debounced contains + A6 normalized matching stays
 * the filter; this file owns only the order). Total order:
 *
 * 1. affinity desc (slug → score, higher = more loved; missing reads as 0),
 * 2. followed first (follows beat unfollowed on affinity ties),
 * 3. alphabetical (`nameEn` case-insensitive, then slug for determinism).
 *
 * Conventions mirror AGENT P3 (`Shelves.kt`): slug matching is
 * trim + case-insensitive on both the [affinities] and [followed] sides, and
 * NaN affinities read as 0 so a corrupt map entry can never float to the
 * top. Affinities are intended to come from P1/P2
 * (`scoreSignals(...)` decayed totals, or P2 `affinityFor(reciter, vector)`
 * mapped by slug); this file deliberately takes a plain map so it stays
 * pure and unit-testable.
 *
 * Pure function: no I/O, no clock, never throws on ordinary input, never
 * mutates [candidates].
 */
fun rankReciters(
    candidates: List<Reciter>,
    affinities: Map<String, Float>,
    followed: Set<String>,
): List<Reciter> {
    if (candidates.isEmpty()) return emptyList()
    if (candidates.size == 1) return candidates.toList()

    val followedNorms = followed.mapTo(HashSet()) { normSearchSlug(it) }

    fun affinityOf(slug: String): Float {
        val exact = affinities[slug]
        if (exact != null && !exact.isNaN()) return exact
        val norm = affinities[normSearchSlug(slug)]
        if (norm != null && !norm.isNaN()) return norm
        return 0f
    }

    fun isFollowed(reciter: Reciter): Boolean = normSearchSlug(reciter.slug) in followedNorms

    return candidates.sortedWith(
        compareByDescending<Reciter> { affinityOf(it.slug) }
            .thenByDescending { isFollowed(it) }
            .thenBy { it.nameEn.trim().lowercase() }
            .thenBy { it.nameEn }
            .thenBy { it.slug.trim().lowercase() }
            .thenBy { it.slug },
    )
}

private fun normSearchSlug(slug: String): String = slug.trim().lowercase()
