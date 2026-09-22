package com.ghais.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

        val domainAyah = verse.toAyah(juz = 1, page = 1)
        assertEquals(verse.surahId, domainAyah.surahId)
        assertEquals(verse.ayahNo, domainAyah.ayahNo)
        assertEquals(verse.textUthmani, domainAyah.textUthmani)
        assertEquals(1, domainAyah.juz)
        assertEquals(1, domainAyah.page)
    }

    @Test
    fun testAyahVerseSerialization() {
        val verse = AyahVerse(
            surahId = 112,
            ayahNo = 1,
            textUthmani = "قُلْ هُوَ ٱللَّهُ أَحَدٌ",
            translation = "Say, \"He is Allah, [who is] One,\"",
            transliteration = "Qul huwal laahu ahad"
        )

        val encoded = json.encodeToString(verse)
        val decoded = json.decodeFromString<AyahVerse>(encoded)

        assertEquals(verse, decoded)
    }

    @Test
    fun testBundledSeedsCompleteness() {
        // Surah 1 Al-Fatihah (7 ayahs)
        val fatihah = QuranAyahRepository.SEED_VERSES[1]
        assertNotNull(fatihah)
        assertEquals(7, fatihah.size)
        fatihah.forEachIndexed { index, ayah ->
            assertEquals(1, ayah.surahId)
            assertEquals(index + 1, ayah.ayahNo)
            assertTrue(ayah.textUthmani.isNotBlank())
            assertTrue(ayah.translation.isNotBlank())
            assertTrue(ayah.transliteration.isNotBlank())
        }

        // Surah 112 Al-Ikhlas (4 ayahs)
        val ikhlas = QuranAyahRepository.SEED_VERSES[112]
        assertNotNull(ikhlas)
        assertEquals(4, ikhlas.size)
        ikhlas.forEachIndexed { index, ayah ->
            assertEquals(112, ayah.surahId)
            assertEquals(index + 1, ayah.ayahNo)
            assertTrue(ayah.textUthmani.isNotBlank())
            assertTrue(ayah.translation.isNotBlank())
            assertTrue(ayah.transliteration.isNotBlank())
        }

        // Surah 113 Al-Falaq (5 ayahs)
        val falaq = QuranAyahRepository.SEED_VERSES[113]
        assertNotNull(falaq)
        assertEquals(5, falaq.size)
        falaq.forEachIndexed { index, ayah ->
            assertEquals(113, ayah.surahId)
            assertEquals(index + 1, ayah.ayahNo)
            assertTrue(ayah.textUthmani.isNotBlank())
            assertTrue(ayah.translation.isNotBlank())
            assertTrue(ayah.transliteration.isNotBlank())
        }

        // Surah 114 An-Nas (6 ayahs)
        val nas = QuranAyahRepository.SEED_VERSES[114]
        assertNotNull(nas)
        assertEquals(6, nas.size)
        nas.forEachIndexed { index, ayah ->
            assertEquals(114, ayah.surahId)
            assertEquals(index + 1, ayah.ayahNo)
            assertTrue(ayah.textUthmani.isNotBlank())
            assertTrue(ayah.translation.isNotBlank())
            assertTrue(ayah.transliteration.isNotBlank())
        }

        // Surah 18 Al-Kahf (first 10 ayahs)
        val kahf = QuranAyahRepository.SEED_VERSES[18]
        assertNotNull(kahf)
        assertEquals(10, kahf.size)
        kahf.forEachIndexed { index, ayah ->
            assertEquals(18, ayah.surahId)
            assertEquals(index + 1, ayah.ayahNo)
            assertTrue(ayah.textUthmani.isNotBlank())
            assertTrue(ayah.translation.isNotBlank())
            assertTrue(ayah.transliteration.isNotBlank())
        }
    }

    @Test
    fun testGetAyahImmediate() {
        QuranAyahRepository.clearCache()

        // 1. From seed without cache
        val fatihah1 = QuranAyahRepository.getAyahImmediate(1, 1)
        assertNotNull(fatihah1)
        assertEquals(1, fatihah1.surahId)
        assertEquals(1, fatihah1.ayahNo)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fatihah1.textUthmani)

        val kahf10 = QuranAyahRepository.getAyahImmediate(18, 10)
        assertNotNull(kahf10)
        assertEquals(10, kahf10.ayahNo)

        // 2. Sensible fallback for non-seed surah
        val baqarah1 = QuranAyahRepository.getAyahImmediate(2, 1)
        assertNotNull(baqarah1)
        assertEquals(2, baqarah1.surahId)
        assertEquals(1, baqarah1.ayahNo)
        assertTrue(baqarah1.textUthmani.contains("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))
        assertTrue(baqarah1.translation.contains("Surah Al-Baqarah, Verse 1"))

        val baqarah2 = QuranAyahRepository.getAyahImmediate(2, 2)
        assertNotNull(baqarah2)
        assertEquals(2, baqarah2.ayahNo)
        assertTrue(baqarah2.textUthmani.contains("آية رقم 2 من سورة البقرة"))

        // 3. From cache
        val customVerse = AyahVerse(99, 1, "custom text", "custom translation")
        QuranAyahRepository.setCachedSurah(99, listOf(customVerse))
        val fromCache = QuranAyahRepository.getAyahImmediate(99, 1)
        assertNotNull(fromCache)
        assertEquals("custom text", fromCache.textUthmani)

        // 4. Out of bounds
        assertNull(QuranAyahRepository.getAyahImmediate(999, 1))
        assertNull(QuranAyahRepository.getAyahImmediate(1, 99))
        assertNull(QuranAyahRepository.getAyahImmediate(1, 0))
    }

    @Test
    fun testGetAyahsForSurahOfflineFallbackAndCache() = runBlocking {
        QuranAyahRepository.clearCache()

        // Surah 112 fallback/seed
        val ikhlasList = QuranAyahRepository.getFallbackVerses(112)
        assertEquals(4, ikhlasList.size)
        assertEquals(QuranAyahRepository.SEED_VERSES[112], ikhlasList)

        // Surah 18 fallback (first 10 from seed, 11..110 generated)
        val kahfList = QuranAyahRepository.getFallbackVerses(18)
        assertEquals(110, kahfList.size)
        assertEquals("Alhamdu lillaahil lazeee anzala 'alaa 'abdihil kitaaba wa lam yaj'al lahoo 'iwajaa", kahfList[0].transliteration)
        assertEquals(10, kahfList[9].ayahNo)
        assertEquals(11, kahfList[10].ayahNo)
        assertTrue(kahfList[10].textUthmani.contains("آية رقم 11 من سورة الكهف"))

        // Surah 108 (Al-Kawthar, 3 ayahs) not in seeds
        val kawtharList = QuranAyahRepository.getFallbackVerses(108)
        assertEquals(3, kawtharList.size)
        assertEquals(1, kawtharList[0].ayahNo)
        assertTrue(kawtharList[0].textUthmani.contains("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))
        assertTrue(kawtharList[1].textUthmani.contains("آية رقم 2"))

        // Cache testing
        QuranAyahRepository.setCachedSurah(1, ikhlasList)
        assertTrue(QuranAyahRepository.isCached(1))
        val cached = QuranAyahRepository.getAyahsForSurah(1)
        assertEquals(4, cached.size)
        assertEquals(ikhlasList, cached)

        QuranAyahRepository.clearCache()
        assertEquals(0, QuranAyahRepository.cache.size)
    }

    @Test
    fun testAlQuranCloudApiResponseParsing() {
        val sampleJson = """
        {
          "code": 200,
          "status": "OK",
          "data": [
            {
              "number": 1,
              "name": "سُورَةُ ٱلْفَاتِحَةِ",
              "englishName": "Al-Faatiha",
              "englishNameTranslation": "The Opening",
              "revelationType": "Meccan",
              "numberOfAyahs": 2,
              "ayahs": [
                {
                  "number": 1,
                  "text": "﻿بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                  "numberInSurah": 1
                },
                {
                  "number": 2,
                  "text": "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
                  "numberInSurah": 2
                }
              ],
              "edition": {
                "identifier": "quran-uthmani",
                "language": "ar",
                "type": "quran"
              }
            },
            {
              "number": 1,
              "name": "سُورَةُ ٱلْفَاتِحَةِ",
              "englishName": "Al-Faatiha",
              "englishNameTranslation": "The Opening",
              "revelationType": "Meccan",
              "numberOfAyahs": 2,
              "ayahs": [
                {
                  "number": 1,
                  "text": "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                  "numberInSurah": 1
                },
                {
                  "number": 2,
                  "text": "[All] praise is [due] to Allah, Lord of the worlds -",
                  "numberInSurah": 2
                }
              ],
              "edition": {
                "identifier": "en.sahih",
                "language": "en",
                "type": "translation"
              }
            }
          ]
        }
        """.trimIndent()

        val parsed = json.decodeFromString<AlQuranCloudResponse>(sampleJson)
        assertEquals(200, parsed.code)
        assertEquals("OK", parsed.status)
        assertEquals(2, parsed.data.size)

        val uthmani = parsed.data.find { it.edition?.identifier == "quran-uthmani" }
        assertNotNull(uthmani)
        assertEquals(2, uthmani.ayahs.size)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", uthmani.ayahs[0].text.removePrefix("\uFEFF"))

        val translation = parsed.data.find { it.edition?.identifier == "en.sahih" }
        assertNotNull(translation)
        assertEquals(2, translation.ayahs.size)
        assertEquals("In the name of Allah, the Entirely Merciful, the Especially Merciful.", translation.ayahs[0].text)
    }
}
