package com.ghais.hifz

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaqfAndMutashabihatTest {

    @Test
    fun splitAyahByWaqf_returnsEmptyForBlankInput() {
        assertEquals(emptyList(), splitAyahByWaqf(""))
        assertEquals(emptyList(), splitAyahByWaqf("   "))
    }

    @Test
    fun splitAyahByWaqf_returnsSingleElementWhenNoWaqfMarks() {
        val ayah = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        assertEquals(listOf(ayah), splitAyahByWaqf(ayah))
    }

    @Test
    fun splitAyahByWaqf_returnsSingleElementForShortAyahEvenWithWaqf() {
        val shortAyah = "يس ۚ وَالْقُرْآنِ"
        assertEquals(listOf(shortAyah), splitAyahByWaqf(shortAyah))
    }

    @Test
    fun splitAyahByWaqf_splitsAtWaqfMarksKeepingMarkAttachedToPrecedingPhrase() {
        val ayah = "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ"
        val phrases = splitAyahByWaqf(ayah)

        assertEquals(3, phrases.size)
        assertTrue(phrases[0].endsWith("ۛ"))
        assertTrue(phrases[1].endsWith("ۛ"))
        assertEquals("هُدًى لِّلْمُتَّقِينَ", phrases[2])
    }

    @Test
    fun splitAyahByWaqf_handlesAllSupportedWaqfMarks() {
        val marks = listOf('ۖ', 'ۗ', 'ۘ', 'ۙ', 'ۚ', 'ۛ', 'ۜ')
        val text = marks.mapIndexed { idx, mark ->
            "عبارة قرآنية رقم ${idx + 1} $mark"
        }.joinToString(" ")

        val phrases = splitAyahByWaqf(text)
        assertEquals(marks.size, phrases.size)
        marks.forEachIndexed { idx, mark ->
            assertTrue(phrases[idx].endsWith(mark), "Expected phrase $idx to end with $mark")
        }
    }

    @Test
    fun mutashabihatFor_returnsSymmetricPairs() {
        val baqarah35 = mutashabihatFor(2, 35)
        assertTrue(baqarah35.any { it.otherSurahId == 7 && it.otherAyahNo == 19 })

        val araf19 = mutashabihatFor(7, 19)
        assertTrue(araf19.any { it.otherSurahId == 2 && it.otherAyahNo == 35 })

        val baqarah58 = mutashabihatFor(2, 58)
        assertTrue(baqarah58.any { it.otherSurahId == 7 && it.otherAyahNo == 161 })

        val baqarah134 = mutashabihatFor(2, 134)
        assertTrue(baqarah134.any { it.otherSurahId == 2 && it.otherAyahNo == 141 })

        val kafirun3 = mutashabihatFor(109, 3)
        assertTrue(kafirun3.any { it.otherSurahId == 109 && it.otherAyahNo == 5 })
    }

    @Test
    fun mutashabihatFor_returnsAshShuaraRefrainsWithoutSelf() {
        val matches = mutashabihatFor(26, 8)
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.none { it.otherSurahId == 26 && it.otherAyahNo == 8 })
        assertTrue(matches.any { it.otherSurahId == 26 && it.otherAyahNo == 67 })
    }

    @Test
    fun mutashabihatFor_returnsEmptyForNonIndexedAyah() {
        assertEquals(emptyList(), mutashabihatFor(1, 1))
        assertEquals(emptyList(), mutashabihatFor(999, 999))
    }
}
