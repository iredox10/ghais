package com.ghais.ui.screens.reciters.deck

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.Mood
import com.ghais.ui.screens.reciters.taste.Pace
import com.ghais.ui.screens.reciters.taste.TasteProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * AGENT A4 — guards the pure deck builder ([todayDeck]).
 *
 * Fully offline and deterministic: synthetic reciters only, fixed epoch-days
 * injected (never the wall clock), so no seed or catalog-size hardcoding can
 * rot. Mirrors the [DailyMixTest] idiom (seeded-shuffle inequality over a
 * large pool is stable: 25! permutations).
 */
class DiscoveryDeckTest {

    private fun voice(
        i: Int,
        tempo: String = "medium",
        style: String = "murattal",
        riwayah: String = "Hafs"
    ): Reciter = Reciter(
        slug = "deck-voice-$i",
        nameEn = "Voice $i",
        nameAr = "صوت $i",
        riwayah = riwayah,
        style = style,
        tempo = tempo
    )

    private fun catalog(size: Int): List<Reciter> = List(size) { i ->
        voice(
            i,
            tempo = listOf("slow", "medium", "fast")[i % 3],
            style = listOf("murattal", "mujawwad", "muallim")[i % 3],
            riwayah = if (i % 5 == 0) "Warsh" else "Hafs"
        )
    }

    private val taste = TasteProfile(
        pace = Pace.SLOW,
        mood = Mood.CALM,
        likedSlugs = setOf("deck-voice-7"),
        completed = true
    )

    @Test
    fun testSameDayGivesSameDeck() {
        val reciters = catalog(25)
        val first = todayDeck(reciters, taste, setOf("deck-voice-1"), setOf("deck-voice-2"), epochDay = 20_375L)
        val second = todayDeck(reciters, taste, setOf("deck-voice-1"), setOf("deck-voice-2"), epochDay = 20_375L)
        assertTrue(first.isNotEmpty())
        assertEquals(first.map { it.slug }, second.map { it.slug })
    }

    @Test
    fun testDeckCapsAtTen() {
        val deck = todayDeck(catalog(30), taste, epochDay = 20_375L)
        assertEquals(MaxDeckCards, deck.size)
        assertEquals(10, deck.size)
    }

    @Test
    fun testExcludesFollowedAndListened() {
        val reciters = catalog(25)
        val history = setOf("deck-voice-1", "deck-voice-2", "deck-voice-3")
        val followed = setOf("deck-voice-4", "deck-voice-5", "deck-voice-6")
        // 19 eligible > 10 cap, so every excluded slug must be fully absent.
        val deck = todayDeck(reciters, taste, history, followed, epochDay = 20_375L)
        assertEquals(10, deck.size)
        val slugs = deck.map { it.slug }.toSet()
        assertTrue(slugs.none { it in history })
        assertTrue(slugs.none { it in followed })
    }

    @Test
    fun testDifferentDayLikelyDifferentOrder() {
        val reciters = catalog(25)
        val a = todayDeck(reciters, taste, epochDay = 111L)
        val b = todayDeck(reciters, taste, epochDay = 222L)
        assertTrue(a.isNotEmpty())
        assertTrue(b.isNotEmpty())
        assertNotEquals(a.map { it.slug }, b.map { it.slug })
    }

    @Test
    fun testSkippedSlugsExcluded() {
        val reciters = catalog(25)
        val quiz = taste.copy(skippedSlugs = setOf("deck-voice-8", "deck-voice-9"))
        val deck = todayDeck(reciters, quiz, epochDay = 20_375L)
        val slugs = deck.map { it.slug }.toSet()
        assertTrue("deck-voice-8" !in slugs)
        assertTrue("deck-voice-9" !in slugs)
    }

    @Test
    fun testLikedVoiceAnchorsFirst() {
        // Liked-only taste: the liked voice scores 0.80 (0.5 + 0.30 verdict +
        // 0.04 Hafs whisper) while every other voice sits at 0.54 or below,
        // so it is the sole anchor and must lead the deck at position 0.
        val reciters = catalog(25)
        val quiz = TasteProfile(likedSlugs = setOf("deck-voice-7"))
        val deck = todayDeck(reciters, quiz, epochDay = 20_375L)
        assertEquals(10, deck.size)
        assertEquals("deck-voice-7", deck.first().slug)
    }

    @Test
    fun testRefillsWhenEverythingExcluded() {
        // Starvation fallback: the whole catalog is followed, but the sheet
        // must still deliver a full lineup rather than an empty state.
        val reciters = catalog(20)
        val followed = reciters.map { it.slug }.toSet()
        val deck = todayDeck(reciters, taste, emptySet(), followed, epochDay = 20_375L)
        assertEquals(10, deck.size)
    }

    @Test
    fun testEmptyCatalogGivesEmptyDeck() {
        assertTrue(todayDeck(emptyList(), taste, epochDay = 20_375L).isEmpty())
    }

    @Test
    fun testNoDuplicatesInDeck() {
        val deck = todayDeck(catalog(30), taste, epochDay = 20_375L)
        val slugs = deck.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size)
    }

    @Test
    fun testWhyLineShape() {
        assertEquals("Measured pace · Hafs", whyLineFor(voice(0, tempo = "medium", riwayah = "Hafs")))
        assertEquals("Unhurried pace · Warsh", whyLineFor(voice(1, tempo = "slow", riwayah = "Warsh")))
    }
}
