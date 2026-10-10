package com.ghais.data.repository

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the reciter-pick history: most-recent-first ordering, normalization
 * (trimmed, non-empty, exactly unique), the 15-entry cap, removal, and the
 * export/import snapshot seam.
 *
 * `ReciterSearchHistoryStore` is an `object` singleton whose `Settings` is a
 * private lazy no-arg factory with no injection hook
 * (`ReciterSearchHistoryStore.kt:52`), which throws on the unit-test
 * classpath, so persistence is a silent no-op here (same limitation as
 * `OnboardingStoreTest`). These tests assert only in-memory list transitions.
 * Setup/teardown scrub both the working owner and the `local` namespace (the
 * only namespace `setOwner` ever adopts out of) through the ordinary writers,
 * so the adoption path can never carry a previous test's slugs into a fresh
 * owner.
 *
 * Persistence roundtrip and cross-owner adoption are deliberately NOT
 * asserted: without a `Settings` seam no stored value can be observed on this
 * classpath.
 *
 * Sibling note: unlike [SearchHistoryStore] (typed queries, minimum 2
 * characters, case-insensitive dedupe), reciter slugs dedupe exactly and a
 * single character is a keepable entry — both differences are pinned below.
 */
class ReciterSearchHistoryStoreTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    @BeforeTest
    fun setUp() {
        ReciterSearchHistoryStore.clear()
        ReciterSearchHistoryStore.setOwner("local")
        ReciterSearchHistoryStore.clear()
        ReciterSearchHistoryStore.setOwner(newOwnerId("reciter-search-owner"))
        ReciterSearchHistoryStore.clear()
    }

    @AfterTest
    fun tearDown() {
        ReciterSearchHistoryStore.clear()
        ReciterSearchHistoryStore.setOwner("local")
        ReciterSearchHistoryStore.clear()
    }

    @Test
    fun testRecordAddsMostRecentFirst() {
        ReciterSearchHistoryStore.record("alafasy")
        ReciterSearchHistoryStore.record("husary")

        assertEquals(listOf("husary", "alafasy"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testRecordTrimsWhitespaceAndDropsBlanks() {
        ReciterSearchHistoryStore.record("   alafasy   ")
        ReciterSearchHistoryStore.record("")
        ReciterSearchHistoryStore.record("   ")

        assertEquals(listOf("alafasy"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testRecordKeepsSingleCharacterSlugs() {
        // Reciter picks have no 2-character minimum: a one-letter slug is a
        // real pick, unlike a one-letter typed query.
        ReciterSearchHistoryStore.record("a")

        assertEquals(listOf("a"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testRecordMovesAnExistingSlugToTheFront() {
        ReciterSearchHistoryStore.record("alafasy")
        ReciterSearchHistoryStore.record("husary")
        ReciterSearchHistoryStore.record("alafasy")

        assertEquals(listOf("alafasy", "husary"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testRecordDedupesExactlyNotCaseInsensitively() {
        ReciterSearchHistoryStore.record("Alafasy")
        ReciterSearchHistoryStore.record("alafasy")

        assertEquals(
            listOf("alafasy", "Alafasy"),
            ReciterSearchHistoryStore.recentSlugs.value,
        )
    }

    @Test
    fun testRecordCapsAtFifteenDroppingTheOldest() {
        for (i in 0 until 20) {
            ReciterSearchHistoryStore.record("reciter-$i")
        }

        val slugs = ReciterSearchHistoryStore.recentSlugs.value
        assertEquals(15, slugs.size)
        assertEquals("reciter-19", slugs.first())
        assertEquals("reciter-5", slugs.last())
        assertFalse(slugs.contains("reciter-4"))
    }

    @Test
    fun testRemoveDeletesTheExactTrimmedSlug() {
        ReciterSearchHistoryStore.record("alafasy")
        ReciterSearchHistoryStore.record("husary")

        ReciterSearchHistoryStore.remove("   alafasy   ")

        assertEquals(listOf("husary"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testRemoveBlankIsANoOp() {
        ReciterSearchHistoryStore.record("alafasy")
        ReciterSearchHistoryStore.remove("")
        ReciterSearchHistoryStore.remove("   ")

        assertEquals(listOf("alafasy"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testClearEmptiesTheHistory() {
        ReciterSearchHistoryStore.record("alafasy")
        ReciterSearchHistoryStore.clear()

        assertTrue(ReciterSearchHistoryStore.recentSlugs.value.isEmpty())
    }

    @Test
    fun testExportSnapshotReturnsACopy() {
        ReciterSearchHistoryStore.record("alafasy")

        val snapshot = ReciterSearchHistoryStore.exportSnapshot().toMutableList()
        snapshot.add("injected")

        assertEquals(listOf("alafasy"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testImportSnapshotNormalizesLikeALoadedList() {
        ReciterSearchHistoryStore.importSnapshot(
            listOf("  alafasy  ", "", "   ", "husary", "alafasy", "  husary ")
        )

        // Trimmed, blanks dropped, exact first occurrence wins, order kept.
        assertEquals(listOf("alafasy", "husary"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testImportSnapshotCapsAtFifteen() {
        ReciterSearchHistoryStore.importSnapshot((0 until 30).map { "reciter-$it" })

        val slugs = ReciterSearchHistoryStore.recentSlugs.value
        assertEquals(15, slugs.size)
        assertEquals("reciter-0", slugs.first())
        assertEquals("reciter-14", slugs.last())
    }

    @Test
    fun testImportSnapshotReplacesRatherThanMerges() {
        ReciterSearchHistoryStore.record("stale-slug")
        ReciterSearchHistoryStore.importSnapshot(listOf("fresh-slug"))

        assertEquals(listOf("fresh-slug"), ReciterSearchHistoryStore.recentSlugs.value)
    }

    @Test
    fun testFreshOwnerStartsWithAnEmptyHistory() {
        ReciterSearchHistoryStore.record("alafasy")

        ReciterSearchHistoryStore.setOwner(newOwnerId("other-owner"))

        assertTrue(ReciterSearchHistoryStore.recentSlugs.value.isEmpty())
    }
}
