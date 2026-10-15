package com.ghais.player

import com.ghais.domain.model.Reciter
import com.ghais.domain.model.ReciterCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure URL-resolution + cache tests for the reciter preview engine.
 * No [AudioEngine]/[PlayerBridge] involvement (the singleton touches the
 * platform player on init, so play/stop save-restore is intentionally NOT
 * exercised here — see uncertainties in the report).
 */
class ReciterPreviewTest {

    private fun everyAyahReciter(): Reciter = Reciter(
        slug = "husary-muallim",
        nameEn = "Mahmoud Khalil Al-Husary (Teacher)",
        nameAr = "محمود خليل الحصري (المعلم)",
        audioFolder = "Husary_Muallim_128kbps",
        catalog = ReciterCatalog.EVERYAYAH
    )

    private fun mp3quranReciter(): Reciter = Reciter(
        slug = "mishary",
        nameEn = "Mishary Rashid Alafasy",
        nameAr = "مشاري رشيد العفاسي",
        audioFolder = "",
        serverUrl = "https://server8.mp3quran.net/afs/",
        catalog = ReciterCatalog.MP3QURAN
    )

    @Test
    fun everyayahCatalogResolvesAyatKursiClip() {
        PreviewCache.clear()
        val url = ReciterPreview.previewUrl(everyAyahReciter())
        assertNotNull(url)
        assertEquals("https://everyayah.com/data/Husary_Muallim_128kbps/002255.mp3", url)
    }

    @Test
    fun mp3quranCatalogPrefersEveryayahClipOverFullSurah() {
        PreviewCache.clear()
        val reciter = mp3quranReciter()
        val preview = ReciterPreview.previewUrl(reciter)
        assertNotNull(preview)
        // Blank audioFolder falls back to resolveEveryAyahFolder("mishary").
        assertEquals("https://everyayah.com/data/Alafasy_128kbps/002255.mp3", preview)
        // Full-surah fallback note: a DIFFERENT host/shape, never the preview.
        val fullSurah = reciter.getFullSurahUrl(2)
        assertTrue(fullSurah.contains("mp3quran.net"), "full-surah fallback lives on mp3quran, got $fullSurah")
        assertTrue(fullSurah.endsWith("/002.mp3"))
        assertFalse(preview == fullSurah)
    }

    @Test
    fun defaultAyahRefIsAyatKursi() {
        assertEquals("2:255", ReciterPreview.DEFAULT_AYAH_REF)
        PreviewCache.clear()
        val url = ReciterPreview.previewUrl(mp3quranReciter())
        assertNotNull(url)
        assertTrue(url.endsWith("/002255.mp3"), "got $url")
    }

    @Test
    fun customAyahRefResolves() {
        PreviewCache.clear()
        val url = ReciterPreview.previewUrl(everyAyahReciter(), "114:6")
        assertEquals("https://everyayah.com/data/Husary_Muallim_128kbps/114006.mp3", url)
    }

    @Test
    fun invalidAyahRefReturnsNull() {
        PreviewCache.clear()
        assertNull(ReciterPreview.previewUrl(mp3quranReciter(), ""))
        assertNull(ReciterPreview.previewUrl(mp3quranReciter(), "garbage"))
        assertNull(ReciterPreview.previewUrl(mp3quranReciter(), "2:"))
        assertNull(ReciterPreview.previewUrl(mp3quranReciter(), "0:9999"))
        assertNull(ReciterPreview.previewUrl(mp3quranReciter(), "115:1"))
    }

    @Test
    fun cacheKeyIsCatalogScoped() {
        val mp3 = Reciter(slug = "mishary", nameEn = "A", nameAr = "", catalog = ReciterCatalog.MP3QURAN)
        val every = Reciter(slug = "mishary", nameEn = "A", nameAr = "", catalog = ReciterCatalog.EVERYAYAH)
        assertFalse(ReciterPreview.cacheKey(mp3, 2, 255) == ReciterPreview.cacheKey(every, 2, 255))
    }

    @Test
    fun previewUrlHitsCache() {
        PreviewCache.clear()
        val reciter = everyAyahReciter()
        val first = ReciterPreview.previewUrl(reciter)
        assertNotNull(first)
        assertEquals(1, PreviewCache.size)
        val second = ReciterPreview.previewUrl(reciter)
        assertEquals(first, second)
        assertEquals(1, PreviewCache.size)
    }

    @Test
    fun previewTrackShape() {
        PreviewCache.clear()
        val track = ReciterPreview.previewTrack(mp3quranReciter())
        assertNotNull(track)
        assertEquals(2, track.surahId)
        assertEquals(255, track.ayahNo)
        assertEquals("mishary", track.reciterSlug)
        assertTrue(track.audioUrl.endsWith("/002255.mp3"))
    }

    @Test
    fun cacheEvictsEldestBeyond50() {
        PreviewCache.clear()
        for (i in 0 until 51) {
            PreviewCache.put("k$i", "u$i")
        }
        assertEquals(PreviewCache.MAX_SIZE, PreviewCache.size)
        assertFalse(PreviewCache.contains("k0"), "eldest must be evicted")
        assertTrue(PreviewCache.contains("k50"))
        assertTrue(PreviewCache.contains("k1"))
    }

    @Test
    fun cacheAccessRefreshesLruOrder() {
        PreviewCache.clear()
        for (i in 0 until PreviewCache.MAX_SIZE) {
            PreviewCache.put("k$i", "u$i")
        }
        // Touch k0 so k1 becomes eldest.
        assertEquals("u0", PreviewCache.get("k0"))
        PreviewCache.put("fresh", "uFresh")
        assertEquals(PreviewCache.MAX_SIZE, PreviewCache.size)
        assertTrue(PreviewCache.contains("k0"), "recently-read entry must survive")
        assertFalse(PreviewCache.contains("k1"), "un-touched eldest must go")
        assertTrue(PreviewCache.contains("fresh"))
    }
}
