package com.ghais.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ReciterUrlTest {

    private fun archiveReciter(fileMap: String) = Reciter(
        slug = "archive-test",
        nameEn = "Archive Test",
        nameAr = "اختبار",
        serverUrl = "https://archive.org/download/ghamdirecitation_201903/",
        availableSurahList = "1,2,114",
        surahFileMap = fileMap,
    )

    @Test
    fun testCanonicalServerUrlWithoutFileMap() {
        val reciter = Reciter(
            slug = "alaa-aql",
            nameEn = "Alaa Aql",
            nameAr = "علاء عقل",
            serverUrl = "https://archive.org/download/AlaaAql/",
        )
        assertEquals("https://archive.org/download/AlaaAql/001.mp3", reciter.getFullSurahUrl(1))
        assertEquals("https://archive.org/download/AlaaAql/114.mp3", reciter.getFullSurahUrl(114))
    }

    @Test
    fun testNumberedNameFileMapIsUsed() {
        val map = """{"1":"001 - Al-Fatihah.mp3","2":"002 - Al-Baqarah.mp3","114":"114 - An-Nas.mp3"}"""
        val reciter = archiveReciter(map)
        assertEquals(
            "https://archive.org/download/ghamdirecitation_201903/001%20-%20Al-Fatihah.mp3",
            reciter.getFullSurahUrl(1),
        )
        assertEquals(
            "https://archive.org/download/ghamdirecitation_201903/002%20-%20Al-Baqarah.mp3",
            reciter.getFullSurahUrl(2),
        )
        assertEquals(
            "https://archive.org/download/ghamdirecitation_201903/114%20-%20An-Nas.mp3",
            reciter.getFullSurahUrl(114),
        )
    }

    @Test
    fun testFileMapKeyDoesNotMatchPrefixOfOtherKey() {
        val map = """{"1":"001Fatihah.MP3","11":"011Hud.MP3","110":"110Nur.MP3","111":"111Nas.MP3","112":"112Ikhlas.MP3","113":"113Falaq.MP3","114":"114Nas.MP3"}"""
        val reciter = archiveReciter(map)
        assertEquals(
            "https://archive.org/download/ghamdirecitation_201903/011Hud.MP3",
            reciter.getFullSurahUrl(11),
        )
        assertEquals(
            "https://archive.org/download/ghamdirecitation_201903/110Nur.MP3",
            reciter.getFullSurahUrl(110),
        )
        assertEquals(
            "https://archive.org/download/ghamdirecitation_201903/003.mp3",
            reciter.getFullSurahUrl(3),
        )
    }

    @Test
    fun testCanonicalEntriesInMapFallBackToPaddedName() {
        val map = """{"1":"001.mp3","2":"002.mp3"}"""
        val reciter = archiveReciter(map)
        assertEquals("https://archive.org/download/ghamdirecitation_201903/001.mp3", reciter.getFullSurahUrl(1))
    }

    @Test
    fun testLegacyMp3quranFallbackStillApplies() {
        val reciter = Reciter(slug = "mishary", nameEn = "Mishary", nameAr = "مشاري")
        assertEquals("https://server8.mp3quran.net/afs/002.mp3", reciter.getFullSurahUrl(2))
    }
}
