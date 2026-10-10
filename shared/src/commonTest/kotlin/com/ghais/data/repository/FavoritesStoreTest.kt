package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the favorites list: identity, toggle/remove semantics, the
 * placeholder-upgrade rule, and owner isolation.
 *
 * `FavoritesStore` is an `object` singleton whose `Settings` is a private
 * lazy no-arg factory with no injection hook (`FavoritesStore.kt:90`), which
 * throws on the unit-test classpath, so every persistence call is a silent
 * no-op here (same limitation as `OnboardingStoreTest`). These tests therefore
 * assert only in-memory `StateFlow` transitions — never a value loaded back
 * out of storage — which makes them identical on a no-op backend and on a
 * real one. Each test binds a provably fresh random owner, and setup/teardown
 * scrub both the working owner and the `local` namespace through the ordinary
 * writers, so no test can inherit another's tracks (including the pending
 * adoption buffer, which `clear` releases).
 *
 * Scope note — persistence roundtrip is deliberately NOT asserted: without a
 * `Settings` seam there is no way to observe a stored value on this
 * classpath, and `setOwner` back to a previous owner cannot restore anything.
 */
class FavoritesStoreTest {

    private fun newOwnerId(tag: String): String = "$tag-${Random.nextLong().toString(16)}"

    private fun track(
        slug: String = "alafasy",
        surahId: Int = 1,
        ayahNo: Int = 1,
        url: String = "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
    ) = TrackItem(
        reciterSlug = slug,
        reciterName = "Mishary Alafasy",
        surahId = surahId,
        surahNameEn = "Al-Fatihah",
        surahNameAr = "الفاتحة",
        ayahNo = ayahNo,
        audioUrl = url,
    )

    private val trackA = track()
    private val trackB = track(
        slug = "husary",
        surahId = 2,
        ayahNo = 5,
        url = "https://everyayah.com/data/Husary_128kbps/002005.mp3",
    )

    @BeforeTest
    fun setUp() {
        // Fully normalise: scrub whatever owner the previous test left, scrub
        // `local` (the only namespace `setOwner` ever adopts out of), then bind
        // a fresh owner whose `load` resets the restore-attempted flag.
        FavoritesStore.clear()
        FavoritesStore.setOwner("local")
        FavoritesStore.clear()
        FavoritesStore.setOwner(newOwnerId("favorites-owner"))
        FavoritesStore.clear()
    }

    @AfterTest
    fun tearDown() {
        FavoritesStore.clear()
        FavoritesStore.setOwner("local")
        FavoritesStore.clear()
    }

    @Test
    fun testAddMakesTrackFavorite() {
        FavoritesStore.add(trackA)

        assertTrue(FavoritesStore.isFavorite(trackA))
        assertEquals(listOf(trackA), FavoritesStore.favoriteTracks.value)
    }

    @Test
    fun testRepeatAddKeepsSingleEntry() {
        FavoritesStore.add(trackA)
        FavoritesStore.add(trackA)

        assertEquals(1, FavoritesStore.favoriteTracks.value.size)
        assertTrue(FavoritesStore.isFavorite(trackA))
    }

    @Test
    fun testSameUrlWithDifferentTupleIsOneFavorite() {
        // Identity is the audio URL first: a re-fetched entry for the same
        // bytes under a different slug tuple must not double the row.
        val alias = track(slug = "mishary", surahId = 9, ayahNo = 9, url = trackA.audioUrl)
        FavoritesStore.add(trackA)
        FavoritesStore.add(alias)

        assertEquals(1, FavoritesStore.favoriteTracks.value.size)
        assertTrue(FavoritesStore.isFavorite(alias))
    }

    @Test
    fun testSameTupleWithDifferentUrlIsOneFavorite() {
        // ... widened to the cloud doc-id tuple second: a playable pull result
        // and its `appwrite://likes/` placeholder share one favourite even
        // though their URLs differ (covered more precisely below).
        val twin = track(url = "https://everyayah.com/data/Alafasy_128kbps/001001_v2.mp3")
        FavoritesStore.add(trackA)
        FavoritesStore.add(twin)

        assertEquals(1, FavoritesStore.favoriteTracks.value.size)
        assertTrue(FavoritesStore.isFavorite(twin))
    }

    @Test
    fun testToggleAddsAnAbsentTrackAndRemovesAPresentOne() {
        FavoritesStore.toggle(trackA)
        assertTrue(FavoritesStore.isFavorite(trackA))

        FavoritesStore.toggle(trackA)
        assertFalse(FavoritesStore.isFavorite(trackA))
        assertTrue(FavoritesStore.favoriteTracks.value.isEmpty())
    }

    @Test
    fun testRemoveDeletesOnlyTheMatchingTrack() {
        FavoritesStore.add(trackA)
        FavoritesStore.add(trackB)
        FavoritesStore.remove(trackA)

        assertFalse(FavoritesStore.isFavorite(trackA))
        assertTrue(FavoritesStore.isFavorite(trackB))
        assertEquals(listOf(trackB), FavoritesStore.favoriteTracks.value)
    }

    @Test
    fun testRemoveUnknownTrackIsANoOp() {
        FavoritesStore.add(trackA)
        FavoritesStore.remove(trackB)

        assertEquals(listOf(trackA), FavoritesStore.favoriteTracks.value)
    }

    @Test
    fun testPlayableTrackUpgradesItsPlaceholder() {
        // The cloud pull stages an unplayable `appwrite://likes/<docId>`
        // placeholder for a like it could not rebuild; the rebuilt playable
        // entry for the same doc-id tuple must replace it, not sit beside it.
        val placeholder = track(url = "appwrite://likes/doc-1")
        val playable = track(url = "https://everyayah.com/data/Alafasy_128kbps/001001.mp3")
        FavoritesStore.add(placeholder)
        FavoritesStore.add(playable)

        assertEquals(1, FavoritesStore.favoriteTracks.value.size)
        assertEquals(playable.audioUrl, FavoritesStore.favoriteTracks.value.single().audioUrl)
    }

    @Test
    fun testPlaceholderNeverDowngradesAPlayableTrack() {
        val playable = track(url = "https://everyayah.com/data/Alafasy_128kbps/001001.mp3")
        val placeholder = track(url = "appwrite://likes/doc-1")
        FavoritesStore.add(playable)
        FavoritesStore.add(placeholder)

        assertEquals(1, FavoritesStore.favoriteTracks.value.size)
        assertEquals(playable.audioUrl, FavoritesStore.favoriteTracks.value.single().audioUrl)
    }

    @Test
    fun testClearEmptiesTheList() {
        FavoritesStore.add(trackA)
        FavoritesStore.add(trackB)
        FavoritesStore.clear()

        assertTrue(FavoritesStore.favoriteTracks.value.isEmpty())
        assertFalse(FavoritesStore.isFavorite(trackA))
        assertFalse(FavoritesStore.isFavorite(trackB))
    }

    @Test
    fun testTracksAreInvisibleUnderAnotherOwner() {
        FavoritesStore.add(trackA)
        FavoritesStore.setOwner(newOwnerId("other-owner"))

        assertFalse(
            FavoritesStore.isFavorite(trackA),
            "Switching owners must not leak one account's favourites into another",
        )
        assertTrue(FavoritesStore.favoriteTracks.value.isEmpty())
    }

    @Test
    fun testPreAuthFavoritesAreHeldAsPendingOnLogin() {
        // The documented pre-auth adoption: tracks tapped while signed out are
        // carried out of `local` but HELD out of the visible list (so the
        // cloud pull still runs first), while `isFavorite` already reports
        // them so hearts render correctly.
        FavoritesStore.setOwner("local")
        FavoritesStore.clear()
        FavoritesStore.add(trackA)
        assertTrue(FavoritesStore.isFavorite(trackA))

        FavoritesStore.setOwner(newOwnerId("fresh-login"))

        assertTrue(
            FavoritesStore.isFavorite(trackA),
            "A pre-auth favourite must stay reported while pending",
        )
        assertTrue(
            FavoritesStore.favoriteTracks.value.isEmpty(),
            "A pending pre-auth favourite must not enter the visible list before the cloud restore",
        )
    }
}
