package com.ghais.data.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the date editorial playlist contracts:
 *
 * 1. Legacy docs without `publishedAt`/`qariSlugs` default to `0` / `[]`
 *    (constructor and JSON decode), so old admin rows keep working.
 * 2. [EditorialRepository.newSince] returns dated playlists at/after the
 *    cutoff newest-first, always excluding undated legacy docs.
 * 3. [EditorialRepository.featuring] filters by qari slug, newest first.
 *
 * Everything here is offline: the repository is an in-memory snapshot and
 * every function under test is synchronous.
 */
class EditorialRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @AfterTest
    fun tearDown() {
        EditorialRepository.setPlaylists(emptyList())
    }

    // ---------------------------------------------------------------------
    // 1. legacy defaults
    // ---------------------------------------------------------------------

    @Test
    fun testLegacyConstructorDefaultsToUndatedAndUnattributed() {
        val playlist = EditorialPlaylist(id = "legacy-1", title = "Legacy")
        assertEquals(0L, playlist.publishedAt)
        assertEquals(emptyList(), playlist.qariSlugs)
    }

    @Test
    fun testLegacyJsonWithoutNewKeysDecodesToDefaults() {
        val decoded = json.decodeFromString<EditorialPlaylist>("""{"id":"a","title":"T"}""")
        assertEquals(0L, decoded.publishedAt)
        assertEquals(emptyList(), decoded.qariSlugs)
    }

    @Test
    fun testNewFieldsSurviveSerializationRoundTrip() {
        val playlist = EditorialPlaylist(
            id = "dated-1",
            title = "Dated",
            publishedAt = 1_700_000_000_000L,
            qariSlugs = listOf("mishary", "husary"),
        )
        assertEquals(playlist, json.decodeFromString<EditorialPlaylist>(json.encodeToString(playlist)))
    }

    @Test
    fun testSetPlaylistsPassesNewFieldsThrough() {
        val playlist = EditorialPlaylist(
            id = "p1",
            title = "P",
            publishedAt = 1234L,
            qariSlugs = listOf("mishary"),
        )
        EditorialRepository.setPlaylists(listOf(playlist))
        assertEquals(playlist, EditorialRepository.get("p1"))
    }

    // ---------------------------------------------------------------------
    // 2. newSince
    // ---------------------------------------------------------------------

    @Test
    fun testNewSinceExcludesUndatedAndSortsNewestFirst() {
        EditorialRepository.setPlaylists(
            listOf(
                EditorialPlaylist(id = "undated", title = "U"),
                EditorialPlaylist(id = "old", title = "O", publishedAt = 1000L),
                EditorialPlaylist(id = "new", title = "N", publishedAt = 3000L),
                EditorialPlaylist(id = "mid", title = "M", publishedAt = 2000L),
            ),
        )
        assertEquals(
            listOf("new", "mid", "old"),
            EditorialRepository.newSince(1000L).map { it.id },
        )
    }

    @Test
    fun testNewSinceExcludesUndatedEvenWithZeroCutoff() {
        EditorialRepository.setPlaylists(
            listOf(
                EditorialPlaylist(id = "undated", title = "U"),
                EditorialPlaylist(id = "dated", title = "D", publishedAt = 1000L),
            ),
        )
        val ids = EditorialRepository.newSince(0L).map { it.id }
        assertEquals(listOf("dated"), ids)
    }

    @Test
    fun testNewSinceRespectsCutoff() {
        EditorialRepository.setPlaylists(
            listOf(
                EditorialPlaylist(id = "old", title = "O", publishedAt = 1000L),
                EditorialPlaylist(id = "mid", title = "M", publishedAt = 2000L),
                EditorialPlaylist(id = "new", title = "N", publishedAt = 3000L),
            ),
        )
        assertEquals(
            listOf("new", "mid"),
            EditorialRepository.newSince(2000L).map { it.id },
        )
        assertEquals(emptyList(), EditorialRepository.newSince(9999L))
    }

    // ---------------------------------------------------------------------
    // 3. featuring
    // ---------------------------------------------------------------------

    @Test
    fun testFeaturingFiltersBySlugNewestFirst() {
        EditorialRepository.setPlaylists(
            listOf(
                EditorialPlaylist(id = "a", title = "A", publishedAt = 1000L, qariSlugs = listOf("mishary", "husary")),
                EditorialPlaylist(id = "b", title = "B", publishedAt = 3000L, qariSlugs = listOf("husary")),
                EditorialPlaylist(id = "c", title = "C", publishedAt = 2000L, qariSlugs = listOf("mishary")),
                EditorialPlaylist(id = "d", title = "D", publishedAt = 4000L),
            ),
        )
        assertEquals(listOf("b", "a"), EditorialRepository.featuring("husary").map { it.id })
        assertEquals(listOf("c", "a"), EditorialRepository.featuring("mishary").map { it.id })
        assertEquals(emptyList(), EditorialRepository.featuring("unknown-slug"))
        assertEquals(emptyList(), EditorialRepository.featuring("   "))
    }

    @Test
    fun testFeaturingSortsUndatedLast() {
        EditorialRepository.setPlaylists(
            listOf(
                EditorialPlaylist(id = "undated", title = "U", qariSlugs = listOf("mishary")),
                EditorialPlaylist(id = "dated", title = "D", publishedAt = 5000L, qariSlugs = listOf("mishary")),
            ),
        )
        assertEquals(
            listOf("dated", "undated"),
            EditorialRepository.featuring("mishary").map { it.id },
        )
    }

    @Test
    fun testHelpersAreEmptyWhenCatalogIsEmpty() {
        assertEquals(emptyList(), EditorialRepository.newSince(0L))
        assertEquals(emptyList(), EditorialRepository.featuring("mishary"))
        assertTrue(EditorialRepository.playlists.value.isEmpty())
    }
}
