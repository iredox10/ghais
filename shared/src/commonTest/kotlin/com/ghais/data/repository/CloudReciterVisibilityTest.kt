package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.search.SearchEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the contract that makes admin-panel reciters visible in the app:
 * a reciter that exists ONLY in Appwrite (no bundled seed) must show up in
 * browse, search, and follow resolution.
 */
class CloudReciterVisibilityTest {

    private val archiveReciter = Reciter(
        slug = "ia-archive-smoke",
        nameEn = "Archive Smoke Reciter",
        nameAr = "قارئ اختبار",
        country = "Saudi Arabia",
        style = "Murattal",
        serverUrl = "https://archive.org/download/ia-archive-smoke/",
        availableSurahList = "1,2,3",
        surahFileMap = """{"1":"001 - Al-Fatihah.mp3","2":"002 - Al-Baqarah.mp3","3":"003 - Ali Imran.mp3"}""",
    )

    @AfterTest
    fun tearDown() {
        ReciterCloudCache.setCloudReciters(emptyList())
        ReciterCloudCache.catalogFetcher = null
    }

    @Test
    fun testCloudOnlyReciterIsAbsentBeforeSync() {
        val browse = QuranDataRepository.getBrowseReciters()
        assertNull(browse.firstOrNull { it.slug == archiveReciter.slug })
    }

    @Test
    fun testCloudOnlyReciterAppearsInBrowseAfterSync() {
        ReciterCloudCache.setCloudReciters(listOf(archiveReciter))

        val browse = QuranDataRepository.getBrowseReciters()
        val row = assertNotNull(browse.firstOrNull { it.slug == archiveReciter.slug })
        assertEquals("Archive Smoke Reciter", row.nameEn)
        assertTrue(row.isSurahAvailable(2))
        assertEquals(
            "https://archive.org/download/ia-archive-smoke/002%20-%20Al-Baqarah.mp3",
            row.getFullSurahUrl(2),
        )
    }

    @Test
    fun testCloudOnlyReciterResolvesBySlug() {
        ReciterCloudCache.setCloudReciters(listOf(archiveReciter))
        val resolved = QuranDataRepository.getReciterBySlug("ia-archive-smoke")
        assertEquals("ia-archive-smoke", resolved?.slug)
    }

    @Test
    fun testCloudOnlyReciterIsGloballySearchable() {
        ReciterCloudCache.setCloudReciters(listOf(archiveReciter))
        val results = SearchEngine.search("Archive Smoke")
        assertTrue(results.reciters.any { it.slug == archiveReciter.slug })
    }

    @Test
    fun testFollowedAdminReciterResolves() {
        assertNull(resolveFollowedQari("ia-archive-smoke"))

        ReciterCloudCache.setCloudReciters(listOf(archiveReciter))
        val followed = assertNotNull(resolveFollowedQari("ia-archive-smoke"))
        assertEquals("ia-archive-smoke", followed.reciter.slug)
        assertEquals("Archive Smoke Reciter", followed.reciter.nameEn)
    }

    @Test
    fun testCloudDocOverridesBundledSeed() {
        val bundled = QuranDataRepository.getReciters()
            .first { it.catalogKey() == "mp3quran:alafasy" }
        ReciterCloudCache.setCloudReciters(listOf(bundled.copy(nameEn = "Alafasy Renamed In Admin")))

        val merged = QuranDataRepository.getReciters()
            .first { it.catalogKey() == "mp3quran:alafasy" }
        assertEquals("Alafasy Renamed In Admin", merged.nameEn)
    }

    @Test
    fun testRefreshKeepsPreviousCacheOnEmptyPull() = runBlocking {
        ReciterCloudCache.setCloudReciters(listOf(archiveReciter))
        ReciterCloudCache.catalogFetcher = { emptyList() }

        assertEquals(false, ReciterCloudCache.refresh())
        assertTrue(ReciterCloudCache.cloudReciters.value.any { it.slug == archiveReciter.slug })
    }

    @Test
    fun testRefreshReplacesCacheOnSuccessfulPull() = runBlocking {
        ReciterCloudCache.catalogFetcher = { listOf(archiveReciter) }

        assertEquals(true, ReciterCloudCache.refresh())
        assertEquals(listOf(archiveReciter), ReciterCloudCache.cloudReciters.value)
    }

    @Test
    fun testRefreshIsNoopWithoutFetcher() = runBlocking {
        assertEquals(false, ReciterCloudCache.refresh())
    }

    @Test
    fun testCountryKeyIgnoresFlagSuffix() {
        // Bundled seeds store a flag, admin-created rows do not. They must be
        // one country, or the browse screen grows a duplicate section and a
        // region screen silently drops half its reciters.
        assertEquals(
            reciterCountryKey("Saudi Arabia 🇸🇦"),
            reciterCountryKey("Saudi Arabia"),
        )
        assertEquals(reciterCountryKey("Egypt 🇪🇬"), reciterCountryKey("  egypt  "))
        assertEquals("saudi arabia", reciterCountryKey("Saudi Arabia 🇸🇦"))
        assertEquals("egypt", reciterCountryKey("Egypt 🇪🇬"))
    }

    @Test
    fun testAdminReciterJoinsExistingCountryGroup() {
        val egyptBundled = QuranDataRepository.getReciters().first {
            reciterCountryKey(it.country) == "egypt"
        }
        val adminRow = archiveReciter.copy(slug = "ia-egypt-demo", country = "Egypt")
        ReciterCloudCache.setCloudReciters(listOf(adminRow))

        val egyptRows = QuranDataRepository.getBrowseReciters()
            .filter { reciterCountryKey(it.country) == reciterCountryKey(egyptBundled.country) }
        assertTrue(egyptRows.any { it.slug == "ia-egypt-demo" })
        assertTrue(egyptRows.size > 1)
    }
}
