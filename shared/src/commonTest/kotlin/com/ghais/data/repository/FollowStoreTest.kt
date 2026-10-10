package com.ghais.data.repository

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Guards the follow set, the follower-count cache rules, and the compact
 * count labels.
 *
 * `FollowStore` is an `object` singleton whose `Settings` is a private lazy
 * no-arg factory with no injection hook (`FollowStore.kt:48`), which throws
 * on the unit-test classpath, so persistence is a silent no-op here (same
 * limitation as `OnboardingStoreTest`). These tests assert only in-memory
 * `StateFlow` transitions, which behave identically on a no-op backend and on
 * a real one — every test binds a provably fresh random owner, so "empty"
 * holds on both. Persistence roundtrip is deliberately NOT asserted: without
 * a `Settings` seam no stored value can be observed on this classpath.
 *
 * The count fetcher IS injectable (`FollowStore.countFetcher`), so the
 * refresh/eviction paths are covered through it; every test that installs a
 * fake resets it to null so the fire-and-forget refresh behind
 * `follow`/`unfollow`/`toggle` can never leak a coroutine into another test.
 */
class FollowStoreTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    @BeforeTest
    fun setUp() {
        FollowStore.countFetcher = null
        FollowStore.setOwner(newOwnerId("follow-owner"))
        FollowStore.clear()
    }

    @AfterTest
    fun tearDown() {
        FollowStore.countFetcher = null
        FollowStore.clear()
        FollowStore.setOwner("local")
    }

    // --- Follow set ---

    @Test
    fun testFollowAddsAndUnfollowRemoves() {
        FollowStore.follow("alafasy")
        assertTrue(FollowStore.isFollowing("alafasy"))

        FollowStore.unfollow("alafasy")
        assertFalse(FollowStore.isFollowing("alafasy"))
    }

    @Test
    fun testFollowIsIdempotent() {
        FollowStore.follow("alafasy")
        FollowStore.follow("alafasy")

        assertEquals(setOf("alafasy"), FollowStore.followedSlugs.value)
    }

    @Test
    fun testUnfollowUnknownSlugIsANoOp() {
        FollowStore.follow("alafasy")
        FollowStore.unfollow("husary")

        assertEquals(setOf("alafasy"), FollowStore.followedSlugs.value)
    }

    @Test
    fun testToggleFlipsBothWays() {
        FollowStore.toggle("alafasy")
        assertTrue(FollowStore.isFollowing("alafasy"))

        FollowStore.toggle("alafasy")
        assertFalse(FollowStore.isFollowing("alafasy"))
    }

    @Test
    fun testSetAllReplacesTheWholeSet() {
        FollowStore.follow("alafasy")
        FollowStore.follow("husary")

        FollowStore.setAll(setOf("minshawi", "ajamy"))

        assertEquals(setOf("minshawi", "ajamy"), FollowStore.followedSlugs.value)
    }

    @Test
    fun testSetAllDoesNotTouchTheCountMap() {
        // Deliberate: the login pull calls `setAll` mid-sync, and refreshing
        // counts there would fan out reads at the worst moment — while slugs
        // that left the set keep their counts until a read evicts them.
        FollowStore.setCounts(mapOf("alafasy" to 5L))
        FollowStore.setAll(setOf("husary"))

        assertEquals(mapOf("alafasy" to 5L), FollowStore.followerCounts.value)
    }

    @Test
    fun testClearEmptiesSlugsAndCounts() {
        FollowStore.follow("alafasy")
        FollowStore.setCounts(mapOf("alafasy" to 5L))
        FollowStore.clear()

        assertTrue(FollowStore.followedSlugs.value.isEmpty())
        assertTrue(FollowStore.followerCounts.value.isEmpty())
        assertFalse(FollowStore.isFollowing("alafasy"))
    }

    @Test
    fun testSwitchingOwnersStartsFromEmptySlugsAndCounts() {
        FollowStore.follow("alafasy")
        FollowStore.setCounts(mapOf("alafasy" to 5L))

        FollowStore.setOwner(newOwnerId("other-owner"))

        assertTrue(FollowStore.followedSlugs.value.isEmpty())
        assertTrue(
            FollowStore.followerCounts.value.isEmpty(),
            "Counts belong to the previous owner and must never leak across accounts",
        )
    }

    // --- Count cache rules ---

    @Test
    fun testSetCountsDropsBlankSlugsAndNegativeValues() {
        // Absent key = unknown (renders as nothing); only a real 0 is stored
        // as 0, and negatives are unknown rather than rendered.
        FollowStore.setCounts(
            mapOf(
                "" to 3L,
                "   " to 4L,
                "negative" to -1L,
                "zero" to 0L,
                "  alafasy  " to 7L,
            )
        )

        assertEquals(mapOf("zero" to 0L, "alafasy" to 7L), FollowStore.followerCounts.value)
    }

    @Test
    fun testSetCountsReplaceSwapsButMergeKeepsUnmentioned() {
        FollowStore.setCounts(mapOf("alafasy" to 1L))
        FollowStore.setCounts(mapOf("husary" to 2L), replace = false)
        assertEquals(
            mapOf("alafasy" to 1L, "husary" to 2L),
            FollowStore.followerCounts.value,
            "Merge must keep counts a partial snapshot omitted",
        )

        FollowStore.setCounts(mapOf("husary" to 2L), replace = true)
        assertEquals(
            mapOf("husary" to 2L),
            FollowStore.followerCounts.value,
            "Replace must drop counts that are no longer reported",
        )
    }

    @Test
    fun testRefreshCountsWritesHitsAndEvictsMisses() = runBlocking {
        FollowStore.countFetcher = { slug -> if (slug == "known") 42L else null }
        FollowStore.setCounts(mapOf("gone" to 9L))
        try {
            FollowStore.refreshCounts(listOf("known", "gone", "   "))

            assertEquals(
                mapOf("known" to 42L),
                FollowStore.followerCounts.value,
                "A read that yields nothing must evict, never leave a stale count",
            )
        } finally {
            FollowStore.countFetcher = null
        }
    }

    @Test
    fun testRefreshCountsEvictsOnFetcherFailure() = runBlocking {
        FollowStore.countFetcher = { throw IllegalStateException("offline") }
        FollowStore.setCounts(mapOf("alafasy" to 9L))
        try {
            FollowStore.refreshCounts(listOf("alafasy"))

            assertTrue(
                FollowStore.followerCounts.value.isEmpty(),
                "A failed read must evict rather than throw or go stale",
            )
        } finally {
            FollowStore.countFetcher = null
        }
    }

    @Test
    fun testRefreshCountsWithoutFetcherIsANoOp() = runBlocking {
        FollowStore.setCounts(mapOf("alafasy" to 9L))

        FollowStore.refreshCounts(listOf("alafasy", "husary"))

        assertEquals(mapOf("alafasy" to 9L), FollowStore.followerCounts.value)
    }

    @Test
    fun testRefreshCountResolvesASingleSlug() = runBlocking {
        FollowStore.countFetcher = { slug -> if (slug == "alafasy") 100L else null }
        try {
            FollowStore.refreshCount("alafasy")

            assertEquals(100L, FollowStore.followerCounts.value["alafasy"])
        } finally {
            FollowStore.countFetcher = null
        }
    }

    @Test
    fun testUnknownCountIsAbsentNeverZero() {
        assertNull(
            FollowStore.followerCounts.value["never-seen"],
            "An unknown count must be an absent key, not a 0",
        )
    }

    // --- Labels ---

    @Test
    fun testFormatFollowerCountBelowOneThousand() {
        assertEquals("0 followers", FollowStore.formatFollowerCount(0L))
        assertEquals("1 followers", FollowStore.formatFollowerCount(1L))
        assertEquals("999 followers", FollowStore.formatFollowerCount(999L))
    }

    @Test
    fun testFormatFollowerCountThousands() {
        assertEquals("1k followers", FollowStore.formatFollowerCount(1000L))
        assertEquals("1.5k followers", FollowStore.formatFollowerCount(1500L))
        // Truncation, not rounding: 1999 reads as 1.9k.
        assertEquals("1.9k followers", FollowStore.formatFollowerCount(1999L))
    }

    @Test
    fun testFormatFollowerCountMillions() {
        assertEquals("1M followers", FollowStore.formatFollowerCount(1_000_000L))
        assertEquals("2.5M followers", FollowStore.formatFollowerCount(2_500_000L))
    }
}
