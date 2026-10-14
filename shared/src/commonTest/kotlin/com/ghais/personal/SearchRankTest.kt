package com.ghais.personal

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AGENT P6 — guards the pure taste-ranked search ordering ([rankReciters]).
 *
 * Contract pinned here: affinity desc beats everything, followed wins only
 * on affinity ties, and alphabetical (`nameEn` case-insensitive, then slug)
 * breaks the rest. Slug matching is trim + case-insensitive on both the
 * affinities and followed sides; missing/NaN affinity reads as 0.
 */
class SearchRankTest {

    private fun qari(
        slug: String,
        nameEn: String = slug,
    ): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameEn,
    )

    @Test
    fun testAffinityDescBeatsAlphabetical() {
        val candidates = listOf(
            qari("aaa", "Aaa Voice"),
            qari("zzz", "Zzz Voice"),
        )
        val ranked = rankReciters(
            candidates,
            affinities = mapOf("zzz" to 0.9f),
            followed = emptySet(),
        )
        assertEquals(listOf("zzz", "aaa"), ranked.map { it.slug })
    }

    @Test
    fun testFollowedWinsOnAffinityTie() {
        val candidates = listOf(
            qari("aaa", "Aaa Voice"),
            qari("zzz", "Zzz Voice"),
        )
        // Equal (both missing = 0) affinity: followed zzz jumps first even
        // though it sorts last alphabetically.
        val ranked = rankReciters(candidates, emptyMap(), setOf("zzz"))
        assertEquals(listOf("zzz", "aaa"), ranked.map { it.slug })
    }

    @Test
    fun testHigherAffinityBeatsFollowed() {
        // Documents the comparator order: affinity is primary, so an
        // unfollowed high-affinity hit outranks a followed zero-affinity hit.
        val candidates = listOf(
            qari("followed-low", "Followed Low"),
            qari("unfollowed-high", "Unfollowed High"),
        )
        val ranked = rankReciters(
            candidates,
            affinities = mapOf("unfollowed-high" to 0.8f),
            followed = setOf("followed-low"),
        )
        assertEquals(listOf("unfollowed-high", "followed-low"), ranked.map { it.slug })
    }

    @Test
    fun testAlphabeticalBreaksFullTies() {
        val candidates = listOf(
            qari("charlie", "Charlie"),
            qari("alpha", "Alpha"),
            qari("bravo", "Bravo"),
        )
        val ranked = rankReciters(candidates, emptyMap(), emptySet())
        assertEquals(listOf("alpha", "bravo", "charlie"), ranked.map { it.slug })
    }

    @Test
    fun testSlugMatchingIsCaseAndWhitespaceInsensitive() {
        val candidates = listOf(
            qari("Mishary", "Mishary"),
            qari("other", "Other"),
        )
        val ranked = rankReciters(
            candidates,
            affinities = mapOf("  mishary  " to 0.7f),
            followed = setOf("  MISHARY "),
        )
        assertEquals("Mishary", ranked.first().slug)
    }

    @Test
    fun testMissingAndNaNAffinityReadAsZero() {
        val candidates = listOf(
            qari("b", "B Voice"),
            qari("a", "A Voice"),
        )
        // NaN must not float to the top; both read as 0 so alpha order wins.
        val ranked = rankReciters(
            candidates,
            affinities = mapOf("b" to Float.NaN),
            followed = emptySet(),
        )
        assertEquals(listOf("a", "b"), ranked.map { it.slug })
    }

    @Test
    fun testEmptyCandidatesGivesEmpty() {
        assertTrue(rankReciters(emptyList(), mapOf("x" to 1f), setOf("x")).isEmpty())
    }

    @Test
    fun testDoesNotMutateInput() {
        val candidates = listOf(
            qari("b", "B Voice"),
            qari("a", "A Voice"),
        )
        val snapshot = candidates.toList()
        rankReciters(candidates, mapOf("a" to 1f), emptySet())
        assertEquals(snapshot.map { it.slug }, candidates.map { it.slug })
    }
}
