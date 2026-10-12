package com.ghais.data.search

import com.ghais.data.repository.AyahVerse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards [AyahSearchIndex]'s contracts: Arabic normalization (tashkeel +
 * alef folding, taa marbuta pinned DISTINCT), script routing, ranking order,
 * translation/transliteration matching, limit/blank handling, surah-granular
 * re-indexing, snippet content + cap, and edition scoping.
 *
 * Hermetic by construction: the index takes `editionId` as a plain param, so
 * no test touches `TranslationPrefs` or `QuranAyahRepository` singletons —
 * only [AyahSearchIndex.clear] in setUp/tearDown, since the object outlives a
 * single test.
 */
class AyahSearchIndexTest {

    private fun verse(
        surah: Int,
        ayah: Int,
        ar: String,
        en: String = "",
        tr: String = ""
    ): AyahVerse = AyahVerse(
        surahId = surah,
        ayahNo = ayah,
        textUthmani = ar,
        translation = en,
        transliteration = tr
    )

    @BeforeTest
    fun setUp() {
        AyahSearchIndex.clear()
    }

    @AfterTest
    fun tearDown() {
        AyahSearchIndex.clear()
    }

    @Test
    fun tashkeelInsensitiveMatchBothDirections() {
        AyahSearchIndex.indexSurah(
            surahId = 112,
            verses = listOf(
                verse(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", "Say, \"He is Allah, [who is] One,")
            ),
            editionId = "en.sahih"
        )
        // Bare query matches vocalized text …
        val bare = AyahSearchIndex.search("قل هو الله احد")
        assertEquals(1, bare.size)
        assertEquals(112, bare[0].surahId)
        assertEquals(1, bare[0].ayahNo)
        // … and a vocalized query matches too (both sides normalize).
        val vocalized = AyahSearchIndex.search("قُلْ هُوَ")
        assertEquals(1, vocalized.size)
        assertEquals(1, vocalized[0].ayahNo)
    }

    @Test
    fun alefFormsFoldToBareAlef() {
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(
                verse(1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help."),
                verse(1, 1, "آمَنَ الرَّسُولُ بِمَا أُنزِلَ", "The Messenger has believed.")
            ),
            editionId = "en.sahih"
        )
        // إ → ا
        val hamzaBelow = AyahSearchIndex.search("اياك نعبد")
        assertEquals(1, hamzaBelow.size)
        assertEquals(5, hamzaBelow[0].ayahNo)
        // آ → ا (maddah folded like hamza-above/below).
        val maddah = AyahSearchIndex.search("امن الرسول")
        assertEquals(1, maddah.size)
        assertEquals(1, maddah[0].ayahNo)
    }

    @Test
    fun taaMarbutaStaysDistinctFromHaa() {
        AyahSearchIndex.indexSurah(
            surahId = 114,
            verses = listOf(
                verse(114, 6, "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ", "From among the jinn and mankind.")
            ),
            editionId = "en.sahih"
        )
        // ة (marbuta) must NOT match a ه (haa) query — pinned documented choice.
        assertTrue(AyahSearchIndex.search("الجنه").isEmpty())
        // Positive control: the marbuta query itself matches.
        val hits = AyahSearchIndex.search("الجنة")
        assertEquals(1, hits.size)
        assertEquals(6, hits[0].ayahNo)
    }

    @Test
    fun rankingIsExactThenPrefixThenSubstringWithMushafTiebreak() {
        AyahSearchIndex.indexSurah(
            surahId = 9,
            verses = listOf(
                verse(9, 1, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ"),
                verse(9, 2, "وَلَهُ ٱلْحَمْدُ كُلُّهُ"),
                verse(9, 3, "ٱلْحَمْدُ")
            ),
            editionId = "en.sahih"
        )
        val hits = AyahSearchIndex.search("الحمد")
        assertEquals(listOf(3, 1, 2), hits.map { it.ayahNo })
        // Tie-break across surahs: equal-kind substring hits sort by (surah, ayah).
        AyahSearchIndex.indexSurah(
            surahId = 2,
            verses = listOf(verse(2, 7, "وَلَهُ ٱلْحَمْدُ فِيهَا")),
            editionId = "en.sahih"
        )
        val cross = AyahSearchIndex.search("الحمد")
        assertEquals(listOf(Pair(9, 3), Pair(9, 1), Pair(2, 7), Pair(9, 2)), cross.map { it.surahId to it.ayahNo })
    }

    @Test
    fun latinQueryMatchesTranslationCaseInsensitively() {
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(
                verse(1, 3, "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,")
            ),
            editionId = "en.sahih"
        )
        val lower = AyahSearchIndex.search("merciful")
        assertEquals(1, lower.size)
        val upper = AyahSearchIndex.search("MERCIFUL")
        assertEquals(1, upper.size)
        assertEquals(lower[0], upper[0])
    }

    @Test
    fun arabicQueryNeverMatchesTranslationText() {
        AyahSearchIndex.indexSurah(
            surahId = 113,
            verses = listOf(
                verse(113, 2, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which Allah created")
            ),
            editionId = "en.sahih"
        )
        // "Allah" appears ONLY in the English half; the Arabic query routes to
        // the Arabic field and must miss.
        assertTrue(AyahSearchIndex.search("الله").isEmpty())
        // Latin routing finds the same verse through the translation.
        val hits = AyahSearchIndex.search("allah")
        assertEquals(1, hits.size)
        assertEquals(2, hits[0].ayahNo)
    }

    @Test
    fun transliterationIsBonusFieldWithItsOwnSnippet() {
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(
                verse(
                    1, 1,
                    "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                    en = "",
                    tr = "Bismillahir-Rahmanir-Rahim"
                )
            ),
            editionId = "en.sahih"
        )
        val hits = AyahSearchIndex.search("bismillah")
        assertEquals(1, hits.size)
        // Snippet comes from the MATCHED field — the transliteration here.
        assertEquals("Bismillahir-Rahmanir-Rahim", hits[0].snippet)
    }

    @Test
    fun limitCapsResultsInRankOrder() {
        AyahSearchIndex.indexSurah(
            surahId = 2,
            verses = (1..5).map { n -> verse(2, n, "آية رقم $n", "mercy upon mercy $n") },
            editionId = "en.sahih"
        )
        val hits = AyahSearchIndex.search("mercy", limit = 2)
        assertEquals(2, hits.size)
        assertEquals(listOf(1, 2), hits.map { it.ayahNo })
    }

    @Test
    fun blankQueriesAndEmptyIndexYieldEmptyList() {
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(verse(1, 1, "بِسْمِ ٱللَّهِ", "In the name of Allah")),
            editionId = "en.sahih"
        )
        assertTrue(AyahSearchIndex.search("").isEmpty())
        assertTrue(AyahSearchIndex.search("   ").isEmpty())
        // Tashkeel marks alone normalize to nothing — no crash, no hits.
        assertTrue(AyahSearchIndex.search("ًٌٍ").isEmpty())
        assertTrue(AyahSearchIndex.search("mercy", limit = 0).isEmpty())
        AyahSearchIndex.clear()
        assertTrue(AyahSearchIndex.search("الله").isEmpty())
        assertEquals(0, AyahSearchIndex.indexedSurahCount())
    }

    @Test
    fun reindexReplacesOneSurahOnly() {
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(verse(1, 1, "بِسْمِ ٱللَّهِ", "In the name of Allah")),
            editionId = "en.sahih"
        )
        AyahSearchIndex.indexSurah(
            surahId = 112,
            verses = listOf(verse(112, 2, "ٱللَّهُ ٱلصَّمَدُ", "Allah, the Eternal Refuge.")),
            editionId = "en.sahih"
        )
        assertEquals(2, AyahSearchIndex.indexedSurahCount())
        // Replace surah 1 with unrelated content.
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(verse(1, 1, "نُورٌ عَلَىٰ نُورٍ", "Light upon light")),
            editionId = "en.sahih"
        )
        assertTrue(AyahSearchIndex.search("In the name").isEmpty())
        val light = AyahSearchIndex.search("light upon")
        assertEquals(1, light.size)
        assertEquals(1, light[0].surahId)
        // Surah 112 untouched.
        assertEquals(1, AyahSearchIndex.search("Eternal Refuge").size)
        assertEquals(2, AyahSearchIndex.indexedSurahCount())
    }

    @Test
    fun placeholdersAreNeverIndexed() {
        AyahSearchIndex.indexSurah(
            surahId = 3,
            verses = listOf(
                AyahVerse(
                    surahId = 3,
                    ayahNo = 5,
                    textUthmani = "آية رقم 5 من سورة test",
                    translation = "Ayah 5 of Surah test",
                    isPlaceholder = true
                )
            ),
            editionId = "en.sahih"
        )
        assertEquals(0, AyahSearchIndex.indexedSurahCount())
        assertTrue(AyahSearchIndex.search("آية").isEmpty())
    }

    @Test
    fun snippetIsCappedExcerptContainingTheMatch() {
        val body = "word ".repeat(60) + "needle" + " word".repeat(60)
        AyahSearchIndex.indexSurah(
            surahId = 4,
            verses = listOf(verse(4, 1, "بِسْمِ ٱللَّهِ", body)),
            editionId = "en.sahih"
        )
        val hits = AyahSearchIndex.search("needle")
        assertEquals(1, hits.size)
        assertTrue(hits[0].snippet.contains("needle"))
        assertTrue(hits[0].snippet.length <= AyahSearchIndex.SNIPPET_MAX_CHARS + 2)
        // Short fields pass through whole, with no ellipsis adornment.
        AyahSearchIndex.indexSurah(
            surahId = 5,
            verses = listOf(verse(5, 1, "بِسْمِ ٱللَّهِ", "Short mercy text")),
            editionId = "en.sahih"
        )
        val short = AyahSearchIndex.search("short mercy")
        assertEquals(1, short.size)
        assertEquals("Short mercy text", short[0].snippet)
    }

    @Test
    fun editionChangeRescopesIndexAndClearResets() {
        AyahSearchIndex.indexSurah(
            surahId = 1,
            verses = listOf(verse(1, 1, "بِسْمِ ٱللَّهِ", "In the name of Allah")),
            editionId = "en.sahih"
        )
        assertEquals("en.sahih", AyahSearchIndex.editionId())
        assertEquals(1, AyahSearchIndex.indexedSurahCount())
        // First surah of a new edition clears the stale one (backstop; callers
        // must still clear() explicitly on a TranslationPrefs switch).
        AyahSearchIndex.indexSurah(
            surahId = 2,
            verses = listOf(verse(2, 1, "الم", "Alif Lam Mim")),
            editionId = "fr.hamidullah"
        )
        assertEquals("fr.hamidullah", AyahSearchIndex.editionId())
        assertEquals(1, AyahSearchIndex.indexedSurahCount())
        assertTrue(AyahSearchIndex.search("In the name").isEmpty())
        AyahSearchIndex.clear()
        assertEquals(null, AyahSearchIndex.editionId())
        assertEquals(0, AyahSearchIndex.indexedSurahCount())
    }
}
