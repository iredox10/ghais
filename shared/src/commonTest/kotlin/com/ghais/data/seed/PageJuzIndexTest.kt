package com.ghais.data.seed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards [PageJuzIndex]: the 30 hardcoded juz starts, Madinah page
 * endpoints, whole-Quran monotonicity, pageCount sanity, and OOB defaults.
 */
class PageJuzIndexTest {

    private val expectedJuzStarts = listOf(
        1 to 1,
        2 to 142,
        2 to 253,
        3 to 93,
        4 to 24,
        4 to 148,
        5 to 82,
        6 to 111,
        7 to 88,
        8 to 41,
        9 to 93,
        11 to 6,
        12 to 53,
        15 to 1,
        17 to 1,
        18 to 75,
        21 to 1,
        23 to 1,
        25 to 21,
        27 to 56,
        29 to 46,
        33 to 31,
        36 to 28,
        39 to 32,
        41 to 47,
        46 to 1,
        51 to 31,
        58 to 1,
        67 to 1,
        78 to 1,
    )

    @Test
    fun juzStarts_matchStandardThirty() {
        assertEquals(30, PageJuzIndex.JUZ_STARTS.size)
        assertEquals(expectedJuzStarts, PageJuzIndex.JUZ_STARTS)
    }

    @Test
    fun pageEndpoints_firstAndLastAyah() {
        assertEquals(1, PageJuzIndex.pageOf(1, 1))
        assertEquals(PageJuzIndex.TOTAL_PAGES, PageJuzIndex.pageOf(114, 6))
        assertEquals(604, PageJuzIndex.TOTAL_PAGES)
    }

    @Test
    fun pages_monotonicNonDecreasingAcrossWholeQuran() {
        var total = 0
        var prev = 0
        for (surah in QuranData.SURAHS) {
            total += surah.ayahsCount
            for (ayah in 1..surah.ayahsCount) {
                val page = PageJuzIndex.pageOf(surah.id, ayah)
                assertTrue(page in 1..PageJuzIndex.TOTAL_PAGES,
                    "page out of range at ${surah.id}:$ayah -> $page")
                assertTrue(page >= prev,
                    "page decreased at ${surah.id}:$ayah ($prev -> $page)")
                prev = page
            }
        }
        assertEquals(6236, total)
    }

    @Test
    fun pageCount_fullQuranCoversAllPages() {
        assertEquals(604, PageJuzIndex.pageCount(1, 1, 114, 6))
    }

    @Test
    fun pageCount_singleAyahIsOnePage() {
        assertEquals(1, PageJuzIndex.pageCount(1, 1, 1, 1))
        assertEquals(1, PageJuzIndex.pageCount(2, 255, 2, 255))
    }

    @Test
    fun pageCount_reversedOrOobIsZero() {
        assertEquals(0, PageJuzIndex.pageCount(114, 6, 1, 1))
        assertEquals(0, PageJuzIndex.pageCount(2, 2, 2, 1))
        assertEquals(0, PageJuzIndex.pageCount(0, 1, 1, 1))
        assertEquals(0, PageJuzIndex.pageCount(1, 1, 115, 1))
        assertEquals(0, PageJuzIndex.pageCount(1, 0, 1, 1))
        assertEquals(0, PageJuzIndex.pageCount(1, 1, 1, 8))
    }

    @Test
    fun oobInputs_returnSaneDefaults() {
        // pageOf / juzOf defaults
        assertEquals(1, PageJuzIndex.pageOf(0, 1))
        assertEquals(1, PageJuzIndex.pageOf(115, 1))
        assertEquals(1, PageJuzIndex.pageOf(-3, -5))
        assertEquals(1, PageJuzIndex.pageOf(1, 0))
        assertEquals(1, PageJuzIndex.pageOf(1, 8))
        assertEquals(1, PageJuzIndex.pageOf(2, 287))
        assertEquals(1, PageJuzIndex.pageOf(114, 7))
        assertEquals(1, PageJuzIndex.juzOf(0, 1))
        assertEquals(1, PageJuzIndex.juzOf(115, 1))
        assertEquals(1, PageJuzIndex.juzOf(1, 0))
        assertEquals(1, PageJuzIndex.juzOf(1, 8))
        assertEquals(1, PageJuzIndex.juzOf(114, 7))
        // juzStart defaults
        assertEquals(1 to 1, PageJuzIndex.juzStart(0))
        assertEquals(1 to 1, PageJuzIndex.juzStart(31))
        assertEquals(1 to 1, PageJuzIndex.juzStart(-1))
    }

    @Test
    fun juzStart_firstAndLast() {
        assertEquals(1 to 1, PageJuzIndex.juzStart(1))
        assertEquals(78 to 1, PageJuzIndex.juzStart(30))
    }

    @Test
    fun juzOf_spotChecks() {
        assertEquals(1, PageJuzIndex.juzOf(1, 1))
        assertEquals(1, PageJuzIndex.juzOf(2, 141))
        assertEquals(2, PageJuzIndex.juzOf(2, 142))
        assertEquals(2, PageJuzIndex.juzOf(2, 252))
        assertEquals(3, PageJuzIndex.juzOf(2, 253))
        assertEquals(29, PageJuzIndex.juzOf(77, 50))
        assertEquals(30, PageJuzIndex.juzOf(78, 1))
        assertEquals(30, PageJuzIndex.juzOf(114, 6))
    }

    @Test
    fun juzStart_roundTripsThroughJuzOf() {
        val counts = QuranData.SURAHS.associate { it.id to it.ayahsCount }
        for (j in 1..30) {
            val (s, a) = PageJuzIndex.juzStart(j)
            assertEquals(j, PageJuzIndex.juzOf(s, a), "juzOf(juzStart($j))")
            if (j > 1) {
                // The ayah immediately before a juz start belongs to juz j-1.
                val (ps, pa) = if (a > 1) s to a - 1 else s - 1 to counts.getValue(s - 1)
                assertEquals(j - 1, PageJuzIndex.juzOf(ps, pa),
                    "ayah before juzStart($j)")
            }
        }
    }
}
