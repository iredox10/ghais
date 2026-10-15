package com.ghais.ui.screens.reciters.personal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the [orderSections] contract for the Reciters tab: fixed editorial
 * top (SEARCH → DECK → VOICE → CHARTS), personalized rails by signal,
 * standalone RITUAL, NATIONS last. Filters live on the search screen.
 */
class SectionOrderTest {

    private fun order(
        hasFollows: Boolean = false,
        hasHistory: Boolean = false,
        hasTaste: Boolean = false,
    ) = orderSections(
        hasFollows = hasFollows,
        hasHistory = hasHistory,
        hasTaste = hasTaste,
    )

    @Test
    fun testColdStartOrderIsExact() {
        assertEquals(
            listOf(
                SectionId.SEARCH,
                SectionId.DECK,
                SectionId.VOICE_OF_DAY,
                SectionId.CHARTS,
                SectionId.RITUAL,
                SectionId.NATIONS,
            ),
            order(),
        )
    }

    @Test
    fun testColdStartOmitsPersonalizedRailsUntilThereIsSignal() {
        val cold = order()
        assertTrue(SectionId.YOUR_VOICES !in cold, "no follows -> no voices rail: $cold")
        assertTrue(SectionId.FOR_YOU !in cold, "no confidence -> no for-you rail: $cold")
    }

    @Test
    fun testHeroBlockIsAlwaysSearchDeckVoiceCharts() {
        val full = order(hasFollows = true, hasHistory = true, hasTaste = true)
        assertEquals(
            listOf(
                SectionId.SEARCH,
                SectionId.DECK,
                SectionId.VOICE_OF_DAY,
                SectionId.CHARTS,
            ),
            full.take(4),
            "hero block fixed: $full",
        )
    }

    @Test
    fun testVoicesBeforeForYouBeforeRitual() {
        val full = order(hasFollows = true, hasHistory = true, hasTaste = true)
        assertTrue(
            full.indexOf(SectionId.YOUR_VOICES) < full.indexOf(SectionId.FOR_YOU) &&
                full.indexOf(SectionId.FOR_YOU) < full.indexOf(SectionId.RITUAL) &&
                full.indexOf(SectionId.RITUAL) < full.indexOf(SectionId.NATIONS),
            "rails then ritual then nations: $full",
        )
    }

    @Test
    fun testTasteAloneUnlocksForYouWithoutFollows() {
        val tasteOnly = order(hasTaste = true)
        assertTrue(SectionId.FOR_YOU in tasteOnly, "completed quiz is confidence too: $tasteOnly")
        assertTrue(SectionId.YOUR_VOICES !in tasteOnly, "no follows -> no voices rail: $tasteOnly")
    }

    @Test
    fun testFiltersEntryDoesNotExist() {
        // Filters live on the search screen; the enum must not carry a
        // FILTERS entry that a future branch could render here.
        assertTrue(
            SectionId.entries.none { it.name == "FILTERS" },
            "no FILTERS entry: ${SectionId.entries}",
        )
    }

    @Test
    fun testSearchFirstNationsLastAndNoDuplicatesForAllSignalCombinations() {
        val booleans = listOf(false, true)
        for (hasFollows in booleans) {
            for (hasHistory in booleans) {
                for (hasTaste in booleans) {
                    val result = order(
                        hasFollows = hasFollows,
                        hasHistory = hasHistory,
                        hasTaste = hasTaste,
                    )
                    val signals =
                        "follows=$hasFollows history=$hasHistory taste=$hasTaste"
                    assertEquals(
                        SectionId.SEARCH,
                        result.first(),
                        "SEARCH must be first ($signals): $result",
                    )
                    assertEquals(
                        SectionId.NATIONS,
                        result.last(),
                        "NATIONS must be last ($signals): $result",
                    )
                    assertEquals(
                        result.size,
                        result.toSet().size,
                        "no section may repeat ($signals): $result",
                    )
                    assertEquals(
                        hasFollows,
                        SectionId.YOUR_VOICES in result,
                        "voices rail tracks follows ($signals): $result",
                    )
                    assertEquals(
                        hasHistory || hasTaste,
                        SectionId.FOR_YOU in result,
                        "for-you tracks confidence ($signals): $result",
                    )
                }
            }
        }
    }
}
