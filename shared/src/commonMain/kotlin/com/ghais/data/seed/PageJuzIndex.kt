package com.ghais.data.seed

/**
 * Quran page (Madinah mushaf, 1..604) and juz (1..30) index.
 *
 * Consumers: mushaf reader (ayah -> page), khatma portions (juz ranges).
 *
 * Page data comes from the generated [AYAH_PAGES] table
 * (`PageAyahPages.kt`, produced by `scripts/gen_page_index.py` from
 * Quran.com API v4 `page_number`, i.e. Madani-mushaf pagination in the
 * Tanzil lineage). Index math derives offsets from
 * [QuranData.SURAHS] ayah counts at runtime; no ayah positions are
 * hardcoded here besides the 30 juz starts below.
 *
 * Juz boundaries (hardcoded standard 30 juz starts):
 * 1:1, 2:142, 2:253, 3:93, 4:24, 4:148, 5:82, 6:111, 7:88, 8:41, 9:93,
 * 11:6, 12:53, 15:1, 17:1, 18:75, 21:1, 23:1, 25:21, 27:56, 29:46,
 * 33:31, 36:28, 39:32, 41:47, 46:1, 51:31, 58:1, 67:1, 78:1.
 * Source: the standard Madinah-mushaf juz division (as printed in the
 * Mushaf al-Madinah and served by the Tanzil/Quran.com metadata).
 * Verified 2026-10-10: the juz derived from these starts for every one of
 * the 6236 ayahs exactly matches the per-ayah `juz_number` reported by
 * both Quran.com API v4 and AlQuran Cloud (zero mismatches; see
 * `scripts/gen_page_index.py` verification step).
 *
 * Out-of-bounds inputs never throw: [pageOf] returns 1, [juzOf] returns 1,
 * [juzStart] returns `1 to 1`, and [pageCount] returns 0.
 */
object PageJuzIndex {

    const val TOTAL_PAGES = 604

    /** Juz `j` (1-based) starts at [JUZ_STARTS]`[j - 1]` as (surahId, ayahNo). */
    val JUZ_STARTS: List<Pair<Int, Int>> = listOf(
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

    /**
     * Start offset (0-based canonical ayah index) of each surah, derived
     * from [QuranData.SURAHS] ayah counts. Indexed by surahId (1..114);
     * index 115 holds the total ayah count.
     */
    private val surahOffsets: IntArray by lazy {
        val surahs = QuranData.SURAHS
        val offsets = IntArray(116)
        val n = minOf(114, surahs.size)
        for (s in 2..n) {
            offsets[s] = offsets[s - 1] + surahs[s - 2].ayahsCount
        }
        var total = 0
        for (surah in surahs) total += surah.ayahsCount
        offsets[115] = total
        offsets
    }

    private fun isValid(surahId: Int, ayahNo: Int): Boolean {
        if (surahId !in 1..114) return false
        if (surahId > QuranData.SURAHS.size) return false
        val count = QuranData.SURAHS[surahId - 1].ayahsCount
        return ayahNo in 1..count
    }

    /** 0-based canonical ayah index, or -1 when out of bounds. */
    private fun globalIndex(surahId: Int, ayahNo: Int): Int {
        if (!isValid(surahId, ayahNo)) return -1
        return surahOffsets[surahId] + (ayahNo - 1)
    }

    /** Madinah page (1..[TOTAL_PAGES]) of an ayah; 1 when out of bounds. */
    fun pageOf(surahId: Int, ayahNo: Int): Int {
        val i = globalIndex(surahId, ayahNo)
        if (i < 0 || i >= AYAH_PAGES.size) return 1
        return AYAH_PAGES[i].coerceIn(1, TOTAL_PAGES)
    }

    /** Juz (1..30) of an ayah; 1 when out of bounds. */
    fun juzOf(surahId: Int, ayahNo: Int): Int {
        if (!isValid(surahId, ayahNo)) return 1
        var juz = 1
        for (j in JUZ_STARTS.indices) {
            val (s, a) = JUZ_STARTS[j]
            if (s < surahId || (s == surahId && a <= ayahNo)) {
                juz = j + 1
            } else {
                break
            }
        }
        return juz
    }

    /** Start ayah of a juz as (surahId, ayahNo); `1 to 1` when out of bounds. */
    fun juzStart(juz: Int): Pair<Int, Int> {
        if (juz !in 1..JUZ_STARTS.size) return 1 to 1
        return JUZ_STARTS[juz - 1]
    }

    /**
     * Number of distinct Madinah pages covered by the closed ayah range
     * [fromSurah]:[fromAyah]..[toSurah]:[toAyah]
     * (`pageOf(to) - pageOf(from) + 1`). Returns 0 when either endpoint is
     * out of bounds or the range is empty/reversed.
     */
    fun pageCount(fromSurah: Int, fromAyah: Int, toSurah: Int, toAyah: Int): Int {
        if (!isValid(fromSurah, fromAyah) || !isValid(toSurah, toAyah)) return 0
        if (toSurah < fromSurah || (toSurah == fromSurah && toAyah < fromAyah)) return 0
        return pageOf(toSurah, toAyah) - pageOf(fromSurah, fromAyah) + 1
    }
}
