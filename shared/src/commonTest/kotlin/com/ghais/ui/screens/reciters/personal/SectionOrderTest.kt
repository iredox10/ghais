package com.ghais.ui.screens.reciters.personal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AGENT P4 — pins the [orderSections] contract for the Reciters tab:
 * cold-start order, followed-first, deck-done demotion, and the structural
 * invariants (SEARCH first, NATIONS last, no duplicates) across the full
 * signal matrix.
 */
class SectionOrderTest {

    // --- Cold start ---

    @Test
    fun testColdStartOrderIsExact() {
        assertEquals(
            listOf(
                SectionId.SEARCH,
                SectionId.DECK,
                SectionId.VOICE_OF_DAY,
                SectionId.CHARTS,
                SectionId.FILTERS,
                SectionId.NATIONS,
            ),
            orderSections(
                hasFollows = false,
                hasHistory = false,
                hasTaste = false,
                isNewDay = true,
                deckDoneToday = false,
            ),
        )
    }

    @Test
    fun testColdStartOmitsPersonalizedRailsUntilThereIsSignal() {
        val cold = orderSections(
            hasFollows = false,
            hasHistory = false,
            hasTaste = false,
            isNewDay = true,
            deckDoneToday = false,
        )
        assertTrue(SectionId.YOUR_VOICES !in cold, "no follows -> no voices rail: $cold")
        assertTrue(SectionId.FOR_YOU !in cold, "no confidence -> no for-you rail: $cold")
    }

    // --- Followed-first / for-you-second ---

    @Test
    fun testFollowedVoicesComeFirstWhenFollowsExist() {
        val order = orderSections(
            hasFollows = true,
            hasHistory = false,
            hasTaste = false,
            isNewDay = true,
            deckDoneToday = false,
        )
        assertEquals(SectionId.SEARCH, order.first())
        assertEquals(SectionId.YOUR_VOICES, order[1], "followed voices first after search: $order")
    }

    @Test
    fun testForYouIsSecondWhenFollowsAndHistoryPresent() {
        val order = orderSections(
            hasFollows = true,
            hasHistory = true,
            hasTaste = false,
            isNewDay = true,
            deckDoneToday = false,
        )
        assertEquals(
            listOf(SectionId.SEARCH, SectionId.YOUR_VOICES, SectionId.FOR_YOU),
            order.take(3),
            "expected SEARCH, YOUR_VOICES, FOR_YOU head: $order",
        )
    }

    @Test
    fun testTasteAloneUnlocksForYouWithoutFollows() {
        val order = orderSections(
            hasFollows = false,
            hasHistory = false,
            hasTaste = true,
            isNewDay = true,
            deckDoneToday = false,
        )
        assertTrue(SectionId.FOR_YOU in order, "completed quiz is confidence too: $order")
        assertTrue(SectionId.YOUR_VOICES !in order, "no follows -> no voices rail: $order")
        // With no voices rail, FOR_YOU is the first content section after search.
        assertEquals(SectionId.FOR_YOU, order[1], "for-you leads without follows: $order")
    }

    // --- Deck promotion / demotion ---

    @Test
    fun testDeckDoneDemotesDeckBelowEditorialBlock() {
        val todo = orderSections(
            hasFollows = true,
            hasHistory = true,
            hasTaste = true,
            isNewDay = true,
            deckDoneToday = false,
        )
        val done = orderSections(
            hasFollows = true,
            hasHistory = true,
            hasTaste = true,
            isNewDay = true,
            deckDoneToday = true,
        )
        assertTrue(
            todo.indexOf(SectionId.DECK) < done.indexOf(SectionId.DECK),
            "done deck must sit lower than todo deck: todo=$todo done=$done",
        )
        // Todo deck is promoted above voice + charts ...
        assertTrue(
            todo.indexOf(SectionId.DECK) < todo.indexOf(SectionId.VOICE_OF_DAY) &&
                todo.indexOf(SectionId.DECK) < todo.indexOf(SectionId.CHARTS),
            "todo deck promoted above editorial: $todo",
        )
        // ... while a done deck rests below them but above the filter tail,
        // keeping FILTERS adjacent to the closing NATIONS reels.
        assertTrue(
            done.indexOf(SectionId.DECK) > done.indexOf(SectionId.CHARTS) &&
                done.indexOf(SectionId.DECK) > done.indexOf(SectionId.VOICE_OF_DAY) &&
                done.indexOf(SectionId.DECK) < done.indexOf(SectionId.FILTERS),
            "done deck demoted below editorial, above filters: $done",
        )
    }

    // --- Freshness ---

    @Test
    fun testNewDayPromotesVoiceOfDayAboveCharts() {
        val fresh = orderSections(
            hasFollows = false,
            hasHistory = false,
            hasTaste = false,
            isNewDay = true,
            deckDoneToday = false,
        )
        val stale = orderSections(
            hasFollows = false,
            hasHistory = false,
            hasTaste = false,
            isNewDay = false,
            deckDoneToday = false,
        )
        assertTrue(
            fresh.indexOf(SectionId.VOICE_OF_DAY) < fresh.indexOf(SectionId.CHARTS),
            "fresh voice outranks charts: $fresh",
        )
        assertTrue(
            stale.indexOf(SectionId.VOICE_OF_DAY) > stale.indexOf(SectionId.CHARTS),
            "stale voice sinks below charts: $stale",
        )
    }

    // --- Structural invariants over the full signal matrix ---

    @Test
    fun testSearchFirstNationsLastAndNoDuplicatesForAllSignalCombinations() {
        val booleans = listOf(false, true)
        for (hasFollows in booleans) {
            for (hasHistory in booleans) {
                for (hasTaste in booleans) {
                    for (isNewDay in booleans) {
                        for (deckDoneToday in booleans) {
                            val order = orderSections(
                                hasFollows = hasFollows,
                                hasHistory = hasHistory,
                                hasTaste = hasTaste,
                                isNewDay = isNewDay,
                                deckDoneToday = deckDoneToday,
                            )
                            val signals =
                                "follows=$hasFollows history=$hasHistory taste=$hasTaste " +
                                    "newDay=$isNewDay deckDone=$deckDoneToday"
                            assertEquals(
                                SectionId.SEARCH,
                                order.first(),
                                "SEARCH must be first ($signals): $order",
                            )
                            assertEquals(
                                SectionId.NATIONS,
                                order.last(),
                                "NATIONS must be last ($signals): $order",
                            )
                            assertEquals(
                                order.size,
                                order.toSet().size,
                                "no section may repeat ($signals): $order",
                            )
                            // Personalized rails appear exactly when their signal exists.
                            assertEquals(
                                hasFollows,
                                SectionId.YOUR_VOICES in order,
                                "voices rail tracks follows ($signals): $order",
                            )
                            assertEquals(
                                hasHistory || hasTaste,
                                SectionId.FOR_YOU in order,
                                "for-you tracks confidence ($signals): $order",
                            )
                        }
                    }
                }
            }
        }
    }
}
