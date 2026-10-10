package com.ghais.data.repository

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the typed-query history: most-recent-first ordering, normalization
 * (trimmed, at least 2 characters, case-insensitively unique), the 15-entry
 * cap, removal, and the export/import snapshot seam.
 *
 * `SearchHistoryStore` is an `object` singleton whose `Settings` is a private
 * lazy no-arg factory with no injection hook (`SearchHistoryStore.kt:51`),
 * which throws on the unit-test classpath, so persistence is a silent no-op
 * here (same limitation as `OnboardingStoreTest`). These tests assert only
 * in-memory list transitions. Setup/teardown scrub both the working owner
 * and the `local` namespace (the only namespace `setOwner` ever adopts out
 * of) through the ordinary writers, so the adoption path can never carry a
 * previous test's queries into a fresh owner.
 *
 * Persistence roundtrip and cross-owner adoption are deliberately NOT
 * asserted: without a `Settings` seam no stored value can be observed on this
 * classpath.
 */
class SearchHistoryStoreTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    @BeforeTest
    fun setUp() {
        SearchHistoryStore.clear()
        SearchHistoryStore.setOwner("local")
        SearchHistoryStore.clear()
        SearchHistoryStore.setOwner(newOwnerId("search-owner"))
        SearchHistoryStore.clear()
    }

    @AfterTest
    fun tearDown() {
        SearchHistoryStore.clear()
        SearchHistoryStore.setOwner("local")
        SearchHistoryStore.clear()
    }

    @Test
    fun testRecordAddsMostRecentFirst() {
        SearchHistoryStore.record("quran")
        SearchHistoryStore.record("tafsir")

        assertEquals(listOf("tafsir", "quran"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testRecordTrimsWhitespace() {
        SearchHistoryStore.record("   quran   ")

        assertEquals(listOf("quran"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testRecordDropsQueriesShorterThanTwoCharacters() {
        SearchHistoryStore.record("")
        SearchHistoryStore.record("   ")
        SearchHistoryStore.record("a")

        assertTrue(SearchHistoryStore.recentQueries.value.isEmpty())
    }

    @Test
    fun testRecordMovesAnExistingQueryToTheFrontCaseInsensitively() {
        SearchHistoryStore.record("Quran")
        SearchHistoryStore.record("tafsir")
        SearchHistoryStore.record("quran")

        assertEquals(listOf("quran", "tafsir"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testRecordCapsAtFifteenDroppingTheOldest() {
        for (i in 0 until 20) {
            SearchHistoryStore.record("query-$i")
        }

        val queries = SearchHistoryStore.recentQueries.value
        assertEquals(15, queries.size)
        assertEquals("query-19", queries.first())
        assertEquals("query-5", queries.last())
        assertFalse(queries.contains("query-4"))
    }

    @Test
    fun testRemoveDeletesTheExactTrimmedQuery() {
        SearchHistoryStore.record("quran")
        SearchHistoryStore.record("tafsir")

        SearchHistoryStore.remove("   quran   ")

        assertEquals(listOf("tafsir"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testRemoveIsCaseSensitiveAndBlankIsANoOp() {
        SearchHistoryStore.record("Quran")

        // Removal matches exactly (unlike `record`, which dedupes ignoring
        // case), and blank input never touches the list.
        SearchHistoryStore.remove("quran")
        SearchHistoryStore.remove("   ")
        SearchHistoryStore.remove("")

        assertEquals(listOf("Quran"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testClearEmptiesTheHistory() {
        SearchHistoryStore.record("quran")
        SearchHistoryStore.clear()

        assertTrue(SearchHistoryStore.recentQueries.value.isEmpty())
    }

    @Test
    fun testExportSnapshotReturnsACopy() {
        SearchHistoryStore.record("quran")

        val snapshot = SearchHistoryStore.exportSnapshot().toMutableList()
        snapshot.add("injected")

        assertEquals(listOf("quran"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testImportSnapshotNormalizesLikeALoadedList() {
        SearchHistoryStore.importSnapshot(
            listOf("  ok  ", "x", "", "   ", "OK", "fine", "FINE  ")
        )

        // Trimmed, single characters and blanks dropped, case-insensitive
        // first occurrence wins.
        assertEquals(listOf("ok", "fine"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testImportSnapshotCapsAtFifteen() {
        SearchHistoryStore.importSnapshot((0 until 30).map { "query-$it" })

        val queries = SearchHistoryStore.recentQueries.value
        assertEquals(15, queries.size)
        assertEquals("query-0", queries.first())
        assertEquals("query-14", queries.last())
    }

    @Test
    fun testImportSnapshotReplacesRatherThanMerges() {
        SearchHistoryStore.record("stale-query")
        SearchHistoryStore.importSnapshot(listOf("fresh-query"))

        assertEquals(listOf("fresh-query"), SearchHistoryStore.recentQueries.value)
    }

    @Test
    fun testFreshOwnerStartsWithAnEmptyHistory() {
        SearchHistoryStore.record("quran")

        SearchHistoryStore.setOwner(newOwnerId("other-owner"))

        assertTrue(SearchHistoryStore.recentQueries.value.isEmpty())
    }
}
