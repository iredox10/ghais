package com.ghais.personal

import com.ghais.data.repository.RelatedQaris
import com.ghais.domain.model.Reciter

/**
 * AGENT P3 — personalized "For you" shelves (Spotify shelf concept).
 *
 * [Shelf.subtitle] is the "why-line": every shelf explains *why* these voices
 * were picked, so the rail reads as a recommendation instead of a dump.
 *
 * Contract:
 * - Pure function: no I/O, no platform APIs, never throws on ordinary input.
 * - Deterministic: same inputs (including [epochDay]) always yield the same
 *   shelves in the same order. All rankings total-order by
 *   (affinity → nameEn → slug), and [RelatedQaris] is itself deterministic.
 * - Priority claiming: shelves are filled in order
 *   your-voices → because-follow → for-you-daily → outside-usual →
 *   new-to-explore, and a voice claimed by an earlier shelf never appears in
 *   a later one (no duplicates across shelves).
 * - Caps: at most [MAX_SHELF_SIZE] slugs per shelf. Empty shelves are omitted,
 *   so callers can render whatever [buildShelves] returns directly.
 * - Affinity source: [affinities] maps reciter slug → score (higher = more
 *   loved). Missing entries read as 0. Intended to be built with P2's
 *   `affinityFor(reciter, vector)`; this file deliberately does NOT call it
 *   so the ranking stays pure and unit-testable with a plain map.
 * - Slug matching is case/whitespace-insensitive on the set side
 *   ([followed]/[history]) and falls back the same way for [affinities] keys.
 * - "Well-known" (exploration quality signal) is intentionally model-local:
 *   [Reciter] carries no play-count/popularity field, so a voice counts as
 *   well-known when it is a teacher ([Reciter.isTeacher]) or carries a
 *   complete mushaf ([Reciter.getAvailableSurahIds] covers all 114 surahs —
 *   blank `availableSurahList` means full coverage).
 */
data class Shelf(
    /** Stable id for LazyColumn keys, e.g. `"for-you-daily"`. */
    val id: String,
    /** Rail title, e.g. `"Because you follow Ahmed"`. */
    val title: String,
    /** Why-line explaining the pick, e.g. `"Same country, riwayah and style as …"`. */
    val subtitle: String,
    /** Reciter slugs, best first, at most [MAX_SHELF_SIZE]. */
    val slugs: List<String>,
)

/** Max voices per shelf. */
const val MAX_SHELF_SIZE = 8

/**
 * Below this affinity a voice counts as "outside your usual".
 * Missing affinity reads as 0, so untried voices qualify by default and the
 * [isWellKnown] gate keeps the shelf to established voices only.
 */
const val LOW_AFFINITY_THRESHOLD = 0.35f

/**
 * Builds the personalized shelves for [reciters].
 *
 * @param reciters full browse catalog (already deduped/grouped upstream).
 * @param affinities slug → affinity score; missing reads as 0.
 * @param followed slugs the user follows.
 * @param history slugs the user has played before.
 * @param epochDay days since epoch (see `currentEpochDay`); rotates
 *   "For you, daily" so the rail feels fresh every day. Negative values are
 *   safe (floor-mod).
 */
fun buildShelves(
    reciters: List<Reciter>,
    affinities: Map<String, Float>,
    followed: Set<String>,
    history: Set<String>,
    epochDay: Long,
): List<Shelf> {
    if (reciters.isEmpty()) return emptyList()

    val followedNorms = followed.mapTo(LinkedHashSet()) { normSlug(it) }
    val playedNorms = history.mapTo(LinkedHashSet()) { normSlug(it) }
    // Normalized slugs already placed on an earlier (higher-priority) shelf.
    val shelved = LinkedHashSet<String>()

    fun affinityOf(slug: String): Float =
        affinities[slug] ?: affinities[normSlug(slug)] ?: 0f

    fun isFollowed(reciter: Reciter): Boolean = normSlug(reciter.slug) in followedNorms
    fun isPlayed(reciter: Reciter): Boolean = normSlug(reciter.slug) in playedNorms
    fun unplayed(): List<Reciter> = reciters.filter { !isFollowed(it) && !isPlayed(it) }

    fun unshelved(candidates: List<Reciter>): List<Reciter> =
        candidates.filter { normSlug(it.slug) !in shelved }

    /** Takes up to [limit] unshelved candidates and claims them. */
    fun pick(candidates: List<Reciter>, limit: Int = MAX_SHELF_SIZE): List<Reciter> {
        if (limit <= 0) return emptyList()
        val fresh = unshelved(candidates).take(limit)
        fresh.forEach { shelved.add(normSlug(it.slug)) }
        return fresh
    }

    // Total order: affinity desc, then name, then slug — deterministic.
    val affinityRank = compareByDescending<Reciter> { affinityOf(it.slug) }
        .thenBy { it.nameEn.lowercase() }
        .thenBy { it.nameEn }
        .thenBy { it.slug.lowercase() }
        .thenBy { it.slug }

    val shelves = mutableListOf<Shelf>()

    // --- 1. Your voices: followed, most-affine first. ---
    val followedRank = reciters.filter(::isFollowed).sortedWith(affinityRank)
    val yourVoices = pick(followedRank)
    if (yourVoices.isNotEmpty()) {
        shelves += Shelf(
            id = "your-voices",
            title = "Your voices",
            subtitle = "Your followed voices, most-loved first",
            slugs = yourVoices.map { it.slug },
        )
    }

    // --- 2. Because you follow X: related to the top followed voice. ---
    val topFollowed = followedRank.firstOrNull()
    if (topFollowed != null) {
        // RelatedQaris drops the seed + alias-twins internally.
        val related = RelatedQaris.relatedTo(topFollowed, unshelved(unplayed()), MAX_SHELF_SIZE)
        val because = pick(related)
        if (because.isNotEmpty()) {
            val first = firstNameOf(topFollowed.nameEn)
            val full = topFollowed.nameEn.trim().ifBlank { "your favorite voice" }
            shelves += Shelf(
                id = "because-follow",
                title = if (first != null) "Because you follow $first" else "More like your favorites",
                subtitle = "Same country, riwayah and style as $full",
                slugs = because.map { it.slug },
            )
        }
    }

    // --- 3. For you, daily: top-affine unplayed, day-rotated. ---
    val daily = pick(rotateForDay(unplayed().sortedWith(affinityRank), epochDay))
    if (daily.isNotEmpty()) {
        shelves += Shelf(
            id = "for-you-daily",
            title = "For you, daily",
            subtitle = "Picked for your taste · fresh every day",
            slugs = daily.map { it.slug },
        )
    }

    // --- 4. Outside your usual: low-affinity but high-quality, never tried. ---
    // Ordered by quality (teachers first) then name — deliberately NOT by
    // affinity, so the slot reads as exploration rather than ranking.
    val explorationRank = compareBy<Reciter> { if (it.isTeacher) 0 else 1 }
        .thenBy { it.nameEn.lowercase() }
        .thenBy { it.nameEn }
        .thenBy { it.slug.lowercase() }
        .thenBy { it.slug }
    val outside = pick(
        unplayed()
            .filter { affinityOf(it.slug) < LOW_AFFINITY_THRESHOLD && isWellKnown(it) }
            .sortedWith(explorationRank),
    )
    if (outside.isNotEmpty()) {
        shelves += Shelf(
            id = "outside-usual",
            title = "Outside your usual",
            subtitle = "Loved voices outside your usual taste",
            slugs = outside.map { it.slug },
        )
    }

    // --- 5. New to explore: unplayed well-known voices, affinity-ranked. ---
    val fresh = pick(unplayed().filter(::isWellKnown).sortedWith(affinityRank))
    if (fresh.isNotEmpty()) {
        shelves += Shelf(
            id = "new-to-explore",
            title = "New to explore",
            subtitle = "Complete voices you haven't played yet",
            slugs = fresh.map { it.slug },
        )
    }

    return shelves
}

private fun normSlug(slug: String): String = slug.trim().lowercase()

private fun isWellKnown(reciter: Reciter): Boolean =
    reciter.isTeacher || reciter.getAvailableSurahIds().size >= 114

private val FIRST_NAME_SPLIT = Regex("\\s+")

private fun firstNameOf(nameEn: String): String? =
    nameEn.trim().split(FIRST_NAME_SPLIT).firstOrNull()?.takeIf { it.isNotBlank() }

/**
 * Deterministic daily rotation: the same [epochDay] always yields the same
 * order, consecutive days start at different offsets. Floor-mod, so negative
 * days are safe.
 */
private fun <T> rotateForDay(list: List<T>, epochDay: Long): List<T> {
    if (list.size <= 1) return list
    val start = (((epochDay % list.size) + list.size) % list.size).toInt()
    if (start == 0) return list
    return list.drop(start) + list.take(start)
}
