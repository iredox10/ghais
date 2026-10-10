package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the user-playlist CRUD store: the exact contract the playlist UI
 * codes against (create/rename/delete/add/remove/move/get + blank-name,
 * unknown-id and clamp rules).
 *
 * [UserPlaylists] is a process-wide `object`, so every test binds a random
 * owner first: a fresh namespace is provably empty on any backend, which
 * keeps each test independent of run order (same isolation trick as
 * `OnboardingStoreTest`). [tearDown] unbinds back to `local` so later test
 * classes never inherit a throwaway namespace.
 *
 * Persistence IS asserted here (unlike `TranslationPrefsTest`, where the
 * no-arg `Settings()` factory throws on the JVM classpath and writes
 * no-op): [UserPlaylists] falls back to a process-local in-memory Settings
 * there — the same fallback `ChartsRepository` uses — so an owner rebind
 * genuinely round-trips the JSON through storage.
 */
class UserPlaylistsTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    private fun track(
        slug: String = "mishary",
        surahId: Int = 1,
        ayahNo: Int = 0,
    ): TrackItem = TrackItem(
        reciterSlug = slug,
        reciterName = "Mishary Rashid Alafasy",
        surahId = surahId,
        surahNameEn = "Al-Fatiha",
        surahNameAr = "الفاتحة",
        ayahNo = ayahNo,
        audioUrl = "https://server8.mp3quran.net/afs/001.mp3",
    )

    @BeforeTest
    fun setUp() {
        UserPlaylists.setOwner(newOwnerId("playlists-test"))
    }

    @AfterTest
    fun tearDown() {
        UserPlaylists.setOwner("local")
    }

    // --- 1. create ---

    @Test
    fun testCreateAddsPlaylistWithTrimmedName() {
        val id = UserPlaylists.create("  My recitation  ")
        assertTrue(id.isNotEmpty(), "create must return a non-blank id")

        val playlist = UserPlaylists.get(id)
        assertNotNull(playlist, "created playlist must be readable via get")
        assertEquals("My recitation", playlist.name, "surrounding whitespace must be trimmed")
        assertTrue(playlist.tracks.isEmpty(), "a new playlist starts empty")
        assertTrue(playlist.createdAt > 0L, "createdAt must be stamped")
        assertEquals(listOf(id), UserPlaylists.playlists.value.map { it.id })
    }

    @Test
    fun testCreateWithBlankNameIsANoOp() {
        val before = UserPlaylists.playlists.value

        assertEquals("", UserPlaylists.create(""), "blank create must return the rejection sentinel")
        assertEquals("", UserPlaylists.create("   "), "whitespace-only create must return the rejection sentinel")
        assertEquals(before, UserPlaylists.playlists.value, "blank create must store nothing")
    }

    // --- 2. rename ---

    @Test
    fun testRenameUpdatesName() {
        val id = UserPlaylists.create("Old")
        UserPlaylists.rename(id, "New")
        assertEquals("New", UserPlaylists.get(id)?.name)
    }

    @Test
    fun testRenameWithBlankNameIsANoOp() {
        val id = UserPlaylists.create("Keep me")
        UserPlaylists.rename(id, "")
        UserPlaylists.rename(id, "   ")
        assertEquals("Keep me", UserPlaylists.get(id)?.name, "blank rename must leave the name untouched")
    }

    @Test
    fun testRenameWithUnknownIdIsANoOp() {
        val id = UserPlaylists.create("Mine")
        val before = UserPlaylists.playlists.value
        UserPlaylists.rename("no-such-id", "Other")
        assertEquals(before, UserPlaylists.playlists.value, "rename of an unknown id must change nothing")
        assertEquals("Mine", UserPlaylists.get(id)?.name)
    }

    // --- 3. delete / get ---

    @Test
    fun testDeleteRemovesPlaylist() {
        val first = UserPlaylists.create("First")
        val second = UserPlaylists.create("Second")
        UserPlaylists.delete(first)
        assertNull(UserPlaylists.get(first), "deleted playlist must be gone")
        assertNotNull(UserPlaylists.get(second), "sibling playlist must survive")
        assertEquals(listOf(second), UserPlaylists.playlists.value.map { it.id })
    }

    @Test
    fun testDeleteWithUnknownIdIsANoOp() {
        val id = UserPlaylists.create("Mine")
        val before = UserPlaylists.playlists.value
        UserPlaylists.delete("no-such-id")
        assertEquals(before, UserPlaylists.playlists.value)
        assertNotNull(UserPlaylists.get(id))
    }

    @Test
    fun testGetWithUnknownIdReturnsNull() {
        assertNull(UserPlaylists.get("no-such-id"))
    }

    // --- 4. add / remove tracks ---

    @Test
    fun testAddTrackAppendsInOrder() {
        val id = UserPlaylists.create("Queue")
        val first = track(surahId = 1)
        val second = track(surahId = 2)
        UserPlaylists.addTrack(id, first)
        UserPlaylists.addTrack(id, second)
        assertEquals(listOf(first, second), UserPlaylists.get(id)?.tracks)
    }

    @Test
    fun testAddTrackWithUnknownIdIsANoOp() {
        val before = UserPlaylists.playlists.value
        UserPlaylists.addTrack("no-such-id", track())
        assertEquals(before, UserPlaylists.playlists.value)
    }

    @Test
    fun testRemoveTrackDropsFirstMatchByKey() {
        val id = UserPlaylists.create("Queue")
        val dup = track(surahId = 1, ayahNo = 2)
        val other = track(surahId = 114)
        UserPlaylists.addTrack(id, dup)
        UserPlaylists.addTrack(id, other)
        UserPlaylists.addTrack(id, dup)

        UserPlaylists.removeTrack(id, UserPlaylists.trackKeyOf(dup))
        assertEquals(
            listOf(other, dup),
            UserPlaylists.get(id)?.tracks,
            "remove must drop only the first matching entry so intentional duplicates survive",
        )
    }

    @Test
    fun testRemoveTrackWithUnknownKeyOrIdIsANoOp() {
        val id = UserPlaylists.create("Queue")
        val kept = track()
        UserPlaylists.addTrack(id, kept)

        UserPlaylists.removeTrack(id, "nobody:0:0")
        assertEquals(listOf(kept), UserPlaylists.get(id)?.tracks, "unmatched key must change nothing")

        UserPlaylists.removeTrack("no-such-id", UserPlaylists.trackKeyOf(kept))
        assertEquals(listOf(kept), UserPlaylists.get(id)?.tracks, "unknown id must change nothing")
    }

    @Test
    fun testTrackKeyFormat() {
        assertEquals("mishary:1:2", UserPlaylists.trackKeyOf(track(slug = "mishary", surahId = 1, ayahNo = 2)))
    }

    // --- 5. move ---

    @Test
    fun testMoveTrackReorders() {
        val id = UserPlaylists.create("Queue")
        val a = track(surahId = 1)
        val b = track(surahId = 2)
        val c = track(surahId = 3)
        UserPlaylists.addTrack(id, a)
        UserPlaylists.addTrack(id, b)
        UserPlaylists.addTrack(id, c)

        UserPlaylists.moveTrack(id, 0, 2)
        assertEquals(listOf(b, c, a), UserPlaylists.get(id)?.tracks)

        UserPlaylists.moveTrack(id, 2, 0)
        assertEquals(listOf(a, b, c), UserPlaylists.get(id)?.tracks)
    }

    @Test
    fun testMoveTrackClampsOutOfRangeIndices() {
        val id = UserPlaylists.create("Queue")
        val a = track(surahId = 1)
        val b = track(surahId = 2)
        UserPlaylists.addTrack(id, a)
        UserPlaylists.addTrack(id, b)

        UserPlaylists.moveTrack(id, -99, 99)
        assertEquals(
            listOf(b, a),
            UserPlaylists.get(id)?.tracks,
            "out-of-range indices must clamp to the list ends, not throw or drop the track",
        )
    }

    @Test
    fun testMoveTrackSameSlotIsANoOp() {
        val id = UserPlaylists.create("Queue")
        val a = track(surahId = 1)
        val b = track(surahId = 2)
        UserPlaylists.addTrack(id, a)
        UserPlaylists.addTrack(id, b)
        val before = UserPlaylists.get(id)?.tracks

        UserPlaylists.moveTrack(id, 0, 0)
        UserPlaylists.moveTrack("no-such-id", 0, 1)
        assertEquals(before, UserPlaylists.get(id)?.tracks)
    }

    // --- 6. persistence round-trip ---

    @Test
    fun testMutationsSurviveAnOwnerRebind() {
        val owner = newOwnerId("roundtrip-owner")
        UserPlaylists.setOwner(owner)
        val id = UserPlaylists.create("Persisted")
        val added = track(surahId = 36, ayahNo = 5)
        UserPlaylists.addTrack(id, added)
        UserPlaylists.rename(id, "Persisted renamed")

        // Binding away evicts the in-memory state; binding back reloads it
        // from the owner-namespaced Settings JSON — a genuine
        // encode -> storage -> decode round-trip.
        UserPlaylists.setOwner(newOwnerId("other-owner"))
        assertTrue(UserPlaylists.playlists.value.isEmpty(), "another owner must see an empty list")
        UserPlaylists.setOwner(owner)

        val reloaded = UserPlaylists.get(id)
        assertNotNull(reloaded, "playlist must survive the reload")
        assertEquals("Persisted renamed", reloaded.name)
        assertEquals(listOf(added), reloaded.tracks)
    }

    @Test
    fun testOwnersDoNotSeeEachOthersPlaylists() {
        val ownerA = newOwnerId("owner-a")
        val ownerB = newOwnerId("owner-b")
        UserPlaylists.setOwner(ownerA)
        UserPlaylists.create("A's list")

        UserPlaylists.setOwner(ownerB)
        assertTrue(UserPlaylists.playlists.value.isEmpty(), "owner B must not see owner A's playlists")
        assertNull(UserPlaylists.get("whatever"), "get in an empty namespace must be null")
    }
}
