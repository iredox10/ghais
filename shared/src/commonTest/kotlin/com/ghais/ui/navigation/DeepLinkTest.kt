package com.ghais.ui.navigation

import com.ghais.ui.screens.memorization.MemorizationScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.search.SearchScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Guards the deep-link parser + screen mapping: valid custom-scheme and
 * https app links, invalid URIs -> null, surah clamping, ayah validation.
 * Pure common code, no platform involved.
 */
class DeepLinkTest {

    // ---- valid custom-scheme links ----

    @Test
    fun surahCustomScheme() {
        assertEquals(AppLink.Surah(2), parseAppLink("quranify://surah/2"))
    }

    @Test
    fun ayahCustomScheme() {
        assertEquals(AppLink.Ayah(2, 255), parseAppLink("quranify://ayah/2/255"))
    }

    @Test
    fun reciterCustomScheme() {
        assertEquals(AppLink.Reciter("mishary"), parseAppLink("quranify://reciter/mishary"))
    }

    @Test
    fun homeCustomScheme() {
        assertEquals(AppLink.Home, parseAppLink("quranify://home"))
    }

    @Test
    fun hifzCustomScheme() {
        assertEquals(AppLink.Hifz, parseAppLink("quranify://hifz"))
    }

    @Test
    fun searchCustomScheme() {
        assertEquals(AppLink.Search, parseAppLink("quranify://search"))
    }

    // ---- valid https app links ----

    @Test
    fun surahHttps() {
        assertEquals(AppLink.Surah(2), parseAppLink("https://ghaith.app/surah/2"))
    }

    @Test
    fun ayahHttps() {
        assertEquals(AppLink.Ayah(2, 255), parseAppLink("https://ghaith.app/ayah/2/255"))
    }

    @Test
    fun reciterHttps() {
        assertEquals(AppLink.Reciter("mishary"), parseAppLink("https://ghaith.app/reciter/mishary"))
    }

    @Test
    fun homeHifzSearchHttps() {
        assertEquals(AppLink.Home, parseAppLink("https://ghaith.app/home"))
        assertEquals(AppLink.Hifz, parseAppLink("https://ghaith.app/hifz"))
        assertEquals(AppLink.Search, parseAppLink("https://ghaith.app/search"))
    }

    // ---- tolerance: case, trailing slash, query/fragment ----

    @Test
    fun schemeAndRouteAreCaseInsensitive() {
        assertEquals(AppLink.Surah(2), parseAppLink("QURANIFY://SURAH/2"))
        assertEquals(AppLink.Surah(2), parseAppLink("https://GHAITH.APP/surah/2"))
    }

    @Test
    fun trailingSlashQueryAndFragmentAreIgnored() {
        assertEquals(AppLink.Surah(2), parseAppLink("quranify://surah/2/"))
        assertEquals(AppLink.Surah(2), parseAppLink("quranify://surah/2?utm_source=share"))
        assertEquals(AppLink.Surah(2), parseAppLink("https://ghaith.app/surah/2#verse-5"))
    }

    // ---- clamping ----

    @Test
    fun surahIdIsClampedTo1Through114() {
        assertEquals(AppLink.Surah(1), parseAppLink("quranify://surah/0"))
        assertEquals(AppLink.Surah(114), parseAppLink("quranify://surah/200"))
        assertEquals(AppLink.Surah(1), parseAppLink("https://ghaith.app/surah/-7"))
    }

    @Test
    fun ayahSurahIdIsClampedButAyahNoIsNot() {
        assertEquals(AppLink.Ayah(114, 6), parseAppLink("quranify://ayah/999/6"))
    }

    // ---- invalid -> null ----

    @Test
    fun blankAndMalformedUrisAreNull() {
        assertNull(parseAppLink(""))
        assertNull(parseAppLink("   "))
        assertNull(parseAppLink("not a uri"))
        assertNull(parseAppLink("quranify://"))
        assertNull(parseAppLink("quranify://surah"))
    }

    @Test
    fun unsupportedSchemesAreNull() {
        assertNull(parseAppLink("http://ghaith.app/surah/2"))
        assertNull(parseAppLink("appwrite-callback-tikitakatalk://auth?userId=x&secret=y"))
        assertNull(parseAppLink("https://example.com/surah/2"))
    }

    @Test
    fun wrongHttpsHostIsNull() {
        assertNull(parseAppLink("https://evil.com/surah/2"))
        assertNull(parseAppLink("https://ghaith.app.evil.com/surah/2"))
    }

    @Test
    fun unknownRouteIsNull() {
        assertNull(parseAppLink("quranify://player"))
        assertNull(parseAppLink("https://ghaith.app/player"))
    }

    @Test
    fun nonNumericIdsAreNull() {
        assertNull(parseAppLink("quranify://surah/abc"))
        assertNull(parseAppLink("quranify://ayah/2/abc"))
        assertNull(parseAppLink("quranify://ayah/abc/5"))
        assertNull(parseAppLink("quranify://ayah/2"))
    }

    @Test
    fun ayahNumberBelowOneIsNull() {
        assertNull(parseAppLink("quranify://ayah/2/0"))
        assertNull(parseAppLink("quranify://ayah/2/-3"))
    }

    @Test
    fun blankReciterSlugIsNull() {
        assertNull(parseAppLink("quranify://reciter/"))
        assertNull(parseAppLink("quranify://reciter"))
        assertNull(parseAppLink("https://ghaith.app/reciter"))
    }

    // ---- screen mapping ----

    @Test
    fun surahMapsToSurahDetail() {
        assertEquals(SurahDetailScreen(2), AppLink.Surah(2).toScreen())
    }

    @Test
    fun ayahMapsToSurahDetailWithoutScrollTarget() {
        // Scroll-to-ayah is unsupported (SurahDetailScreen takes only surahId),
        // so an ayah link lands at the top of the surah.
        assertEquals(SurahDetailScreen(2), AppLink.Ayah(2, 255).toScreen())
    }

    @Test
    fun reciterMapsToReciterProfile() {
        assertEquals(ReciterProfileScreen("mishary"), AppLink.Reciter("mishary").toScreen())
    }

    @Test
    fun hifzMapsToMemorizationScreen() {
        assertEquals(MemorizationScreen, AppLink.Hifz.toScreen())
    }

    @Test
    fun searchMapsToSearchScreen() {
        assertIs<SearchScreen>(AppLink.Search.toScreen())
    }

    @Test
    fun homeMapsToNullMeaningPopToRoot() {
        assertNull(AppLink.Home.toScreen())
    }

    // ---- bus handoff ----

    @Test
    fun busPostAndConsume() {
        DeepLinkBus.consume()
        DeepLinkBus.post(AppLink.Surah(36))
        assertEquals(AppLink.Surah(36), DeepLinkBus.consume())
        assertNull(DeepLinkBus.consume())
    }
}
