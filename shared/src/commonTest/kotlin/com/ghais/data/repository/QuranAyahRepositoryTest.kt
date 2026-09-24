package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuranAyahRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testAyahVerseDataClassAndDefaults() {
        val verse = AyahVerse(
            surahId = 1,
            ayahNo = 1,
            textUthmani = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
        )

        assertEquals(1, verse.surahId)
        assertEquals(1, verse.ayahNo)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", verse.textUthmani)
        assertEquals("In the name of Allah, the Entirely Merciful, the Especially Merciful.", verse.translation)
        assertEquals("", verse.transliteration, "Transliteration should default to empty string")
        assertEquals("", AyahVerse(2, 2, "text").translation, "Translation should default to empty string")
    }

    @Test
    fun testAyahVerseSerialization() {
        val verse = AyahVerse(
            surahId = 112,
            ayahNo = 1,
            textUthmani = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            translation = "Say, \"He is Allah, [who is] One,\"",
            transliteration = "Qul Huwallāhu Aḥad"
        )

        val encoded = json.encodeToString(verse)
        val decoded = json.decodeFromString<AyahVerse>(encoded)

        assertEquals(verse, decoded)
    }

    @Test
    fun testBundledSeedsPresentWithCorrectVerseOneTexts() {
        // Surah 1 verse 1 IS the basmalah; full 7-ayah seed is bundled.
        val fatihah1 = QuranAyahRepository.getAyahImmediate(1, 1)
        assertEquals(1, fatihah1.surahId)
        assertEquals(1, fatihah1.ayahNo)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fatihah1.textUthmani)
        val fatihah7 = QuranAyahRepository.getAyahImmediate(1, 7)
        assertEquals(7, fatihah7.ayahNo)
        assertTrue(fatihah7.textUthmani.isNotBlank())
        assertFalse(fatihah7.textUthmani.contains("آية رقم"))

        // Surah 112 (4 ayahs bundled).
        val ikhlas1 = QuranAyahRepository.getAyahImmediate(112, 1)
        assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", ikhlas1.textUthmani)
        val ikhlas4 = QuranAyahRepository.getAyahImmediate(112, 4)
        assertEquals(4, ikhlas4.ayahNo)
        assertFalse(ikhlas4.textUthmani.contains("آية رقم"))

        // Surah 113 (5 ayahs bundled).
        val falaq1 = QuranAyahRepository.getAyahImmediate(113, 1)
        assertEquals("قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ", falaq1.textUthmani)
        val falaq5 = QuranAyahRepository.getAyahImmediate(113, 5)
        assertEquals(5, falaq5.ayahNo)
        assertFalse(falaq5.textUthmani.contains("آية رقم"))

        // Surah 114 (6 ayahs bundled).
        val nas1 = QuranAyahRepository.getAyahImmediate(114, 1)
        assertEquals("قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ", nas1.textUthmani)
        val nas6 = QuranAyahRepository.getAyahImmediate(114, 6)
        assertEquals(6, nas6.ayahNo)
        assertFalse(nas6.textUthmani.contains("آية رقم"))

        // Surah 108 (3 ayahs bundled).
        val kawthar1 = QuranAyahRepository.getAyahImmediate(108, 1)
        assertEquals("إِنَّآ أَعْطَيْنَٰكَ ٱلْكَوْثَرَ", kawthar1.textUthmani)
        val kawthar3 = QuranAyahRepository.getAyahImmediate(108, 3)
        assertEquals(3, kawthar3.ayahNo)
        assertFalse(kawthar3.textUthmani.contains("آية رقم"))

        // Surah 18 is a partial seed; verse 1 has NO basmalah prefix.
        val kahf1 = QuranAyahRepository.getAyahImmediate(18, 1)
        assertEquals(18, kahf1.surahId)
        assertEquals(1, kahf1.ayahNo)
        assertTrue(kahf1.textUthmani.startsWith("ٱلْحَمْدُ لِلَّهِ"))
        assertFalse(kahf1.textUthmani.contains("بِسْمِ"))
        // Seeded through ayah 5; ayah 6+ falls back to a placeholder.
        val kahf5 = QuranAyahRepository.getAyahImmediate(18, 5)
        assertEquals(5, kahf5.ayahNo)
        assertFalse(kahf5.textUthmani.contains("آية رقم"))
        val kahf6 = QuranAyahRepository.getAyahImmediate(18, 6)
        assertEquals(6, kahf6.ayahNo)
        assertTrue(kahf6.textUthmani.contains("آية رقم"))
    }

    @Test
    fun testGetAyahImmediatePlaceholderShape() {
        // Never null: unseeded surahs return a clean baseline placeholder.
        val baqarah2: AyahVerse = QuranAyahRepository.getAyahImmediate(2, 2)
        assertEquals(2, baqarah2.surahId)
        assertEquals(2, baqarah2.ayahNo)
        assertTrue(baqarah2.textUthmani.contains("آية رقم"))
        assertTrue(baqarah2.textUthmani.contains("2"))
    }

    @Test
    fun testCleanQuranicText() {
        // Strips U+25CC (dotted circle), U+200B (zero-width space), U+FEFF (BOM).
        assertEquals("ab", QuranAyahRepository.cleanQuranicText("a\u25CCb"))
        assertEquals("ab", QuranAyahRepository.cleanQuranicText("a\u200Bb"))
        assertEquals("ab", QuranAyahRepository.cleanQuranicText("\uFEFFab"))
        // Collapses repeated spaces and trims.
        assertEquals("a b", QuranAyahRepository.cleanQuranicText("a  b"))
        assertEquals("a b", QuranAyahRepository.cleanQuranicText("  a b  "))
        assertEquals("a b", QuranAyahRepository.cleanQuranicText("\uFEFF  a\u200B   b\u25CC  "))
        // Rejoins hamza splits (lam-sukun + hamza, hamza + alef).
        assertEquals("لْء", QuranAyahRepository.cleanQuranicText("لْ ء"))
        assertEquals("لْء", QuranAyahRepository.cleanQuranicText("لْ  ء"))
        assertEquals("ءا", QuranAyahRepository.cleanQuranicText("ء ا"))
        // Strips spaces before orphan combining marks, keeps word spaces.
        assertEquals("نَصْرًا", QuranAyahRepository.cleanQuranicText("نَصْر ًا"))
        assertEquals("a b c", QuranAyahRepository.cleanQuranicText("a b c"))
    }

    @Test
    fun testHasBasmalahHeader() {
        assertTrue(QuranAyahRepository.BASMALAH.isNotBlank())
        assertFalse(QuranAyahRepository.hasBasmalahHeader(1))
        assertFalse(QuranAyahRepository.hasBasmalahHeader(9))
        assertTrue(QuranAyahRepository.hasBasmalahHeader(2))
        assertTrue(QuranAyahRepository.hasBasmalahHeader(18))
        assertTrue(QuranAyahRepository.hasBasmalahHeader(114))
    }

    @Test
    fun testSanitizeVerseTextStripBehavior() {
        val withPrefix = "بسم الله الرحمن الرحيم الْحَمْدُ لِلَّهِ"
        val stripped = QuranAyahRepository.sanitizeVerseText(2, 1, withPrefix)
        assertTrue(stripped.contains("الْحَمْدُ"))
        assertFalse(stripped.contains("بسم"))
        // Surah 1 keeps its verse-1 text untouched.
        assertEquals(withPrefix, QuranAyahRepository.sanitizeVerseText(1, 1, withPrefix))
        // Non-first ayahs are untouched.
        assertEquals(withPrefix, QuranAyahRepository.sanitizeVerseText(2, 2, withPrefix))
    }

    @Test
    fun testCacheGenNonDecreasingAfterGetAyahsForSurah() = runBlocking {
        val before = QuranAyahRepository.cacheGen.value
        // Seeded surah: served from cache, no network needed (offline-safe).
        val verses = QuranAyahRepository.getAyahsForSurah(112)
        assertTrue(verses.isNotEmpty())
        assertTrue(QuranAyahRepository.cacheGen.value >= before)
        // Prefetch smoke test (fire-and-forget, must not throw).
        QuranAyahRepository.prefetchSurah(112)
    }
}
