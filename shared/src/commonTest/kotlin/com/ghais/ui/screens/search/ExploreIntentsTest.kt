package com.ghais.ui.screens.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Agent A12 — guards the pure exploratory-intent core.
 *
 * [parseExploreIntent] maps natural phrases to intents (or null for plain
 * lookups); [applyIntent] reorders primitive metadata deterministically and
 * never drops rows. No Compose, no singletons — hermetic by construction.
 */
class ExploreIntentsTest {

    // --- parseExploreIntent: task's four hub chips ---

    @Test
    fun calmNightMapsToCalmSlow() {
        val intent = parseExploreIntent("calm recitation for night")
        assertNotNull(intent)
        assertEquals("calm", intent!!.mood)
        assertEquals("slow", intent.tempo)
        assertEquals(ExploreSort.RELEVANCE, intent.sort)
    }

    @Test
    fun shortPowerfulMapsToShortFirst() {
        val intent = parseExploreIntent("short powerful surahs")
        assertNotNull(intent)
        assertEquals(ExploreSort.SHORT_FIRST, intent!!.sort)
    }

    @Test
    fun newVoicesMapsToNewFirst() {
        val intent = parseExploreIntent("new voices like Mishary")
        assertNotNull(intent)
        assertEquals(ExploreSort.NEW_FIRST, intent!!.sort)
    }

    @Test
    fun rainyMurattalMapsToMurattalMood() {
        val intent = parseExploreIntent("rainy-day murattal")
        assertNotNull(intent)
        assertEquals("murattal", intent!!.mood)
    }

    // --- parseExploreIntent: edges ---

    @Test
    fun plainLookupReturnsNull() {
        assertNull(parseExploreIntent("Mishary"))
        assertNull(parseExploreIntent("Yasin"))
        assertNull(parseExploreIntent("2:255"))
    }

    @Test
    fun blankReturnsNull() {
        assertNull(parseExploreIntent(""))
        assertNull(parseExploreIntent("   "))
    }

    @Test
    fun matchingIsCaseInsensitive() {
        val intent = parseExploreIntent("CALM NIGHT")
        assertNotNull(intent)
        assertEquals("calm", intent!!.mood)
    }

    @Test
    fun moodAndSortCompose() {
        val intent = parseExploreIntent("short calm night recitation")
        assertNotNull(intent)
        assertEquals("calm", intent!!.mood)
        assertEquals(ExploreSort.SHORT_FIRST, intent.sort)
    }

    @Test
    fun focusAndMorningMoods() {
        val focus = parseExploreIntent("quran for deep focus while studying")
        assertNotNull(focus)
        assertEquals("focus", focus!!.mood)
        assertEquals("medium", focus.tempo)

        val morning = parseExploreIntent("morning energy recitation")
        assertNotNull(morning)
        assertEquals("morning", morning!!.mood)
    }

    // --- applyIntent ---

    private fun reciter(
        slug: String,
        style: String = "Murattal",
        tempo: String = "Medium",
        isNew: Boolean = false
    ) = ExploreReciterMeta(
        slug = slug,
        name = slug,
        style = style,
        tempo = tempo,
        isNew = isNew
    )

    private fun surah(id: Int, ayahs: Int) = ExploreSurahMeta(id = id, ayahsCount = ayahs)

    @Test
    fun shortFirstOrdersSurahsByAyahCount() {
        val outcome = applyIntent(
            reciters = emptyList(),
            surahs = listOf(surah(2, 286), surah(112, 4), surah(1, 7)),
            intent = ExploreIntent(sort = ExploreSort.SHORT_FIRST)
        )
        assertEquals(listOf(112, 1, 2), outcome.surahIds)
    }

    @Test
    fun calmIntentRanksSlowTempoFirstWithoutDropping() {
        val reciters = listOf(
            reciter("fast-qari", tempo = "Fast & Powerful"),
            reciter("slow-qari", tempo = "Slow & Emotive"),
            reciter("medium-qari", tempo = "Medium")
        )
        val outcome = applyIntent(
            reciters = reciters,
            surahs = emptyList(),
            intent = ExploreIntent(mood = "calm", tempo = "slow")
        )
        assertEquals(listOf("slow-qari", "fast-qari", "medium-qari"), outcome.reciterSlugs)
    }

    @Test
    fun newFirstRanksFlaggedVoicesFirstStably() {
        val reciters = listOf(
            reciter("old-a"),
            reciter("fresh-b", isNew = true),
            reciter("old-c"),
            reciter("fresh-d", isNew = true)
        )
        val outcome = applyIntent(
            reciters = reciters,
            surahs = emptyList(),
            intent = ExploreIntent(sort = ExploreSort.NEW_FIRST)
        )
        assertEquals(listOf("fresh-b", "fresh-d", "old-a", "old-c"), outcome.reciterSlugs)
    }

    @Test
    fun relevanceIntentWithNoConstraintsPreservesOrder() {
        val reciters = listOf(reciter("b"), reciter("a"), reciter("c"))
        val surahs = listOf(surah(2, 286), surah(112, 4))
        val outcome = applyIntent(
            reciters = reciters,
            surahs = surahs,
            intent = ExploreIntent()
        )
        assertEquals(listOf("b", "a", "c"), outcome.reciterSlugs)
        assertEquals(listOf(2, 112), outcome.surahIds)
    }

    @Test
    fun emptyInputsYieldEmptyOutcome() {
        val outcome = applyIntent(emptyList(), emptyList(), ExploreIntent(sort = ExploreSort.SHORT_FIRST))
        assertTrue(outcome.reciterSlugs.isEmpty())
        assertTrue(outcome.surahIds.isEmpty())
    }
}
