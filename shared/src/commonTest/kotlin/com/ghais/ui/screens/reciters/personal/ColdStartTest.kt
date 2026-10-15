package com.ghais.ui.screens.reciters.personal

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.Mood
import com.ghais.ui.screens.reciters.taste.Pace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AGENT P9 — guards the pure cold-start import ([hasUsableHistory] /
 * [importTasteFromHistory]). Fully offline: synthetic reciters only, no
 * store, no catalog seeds, so no seed-size hardcoding can rot.
 */
class ColdStartTest {

    private fun voice(
        slug: String,
        tempo: String = "Medium",
        style: String = "Murattal",
        country: String = "Kuwait"
    ): Reciter = Reciter(
        slug = slug,
        nameEn = slug,
        nameAr = "اختبار",
        style = style,
        tempo = tempo,
        country = country
    )

    private fun slowMujawwad(slug: String): Reciter = voice(
        slug = slug,
        tempo = "Slow & Tearful",
        style = "Mujawwad",
        country = "Egypt"
    )

    private fun fastModern(slug: String): Reciter = voice(
        slug = slug,
        tempo = "Fast & Expressive",
        style = "Modern Murattal",
        country = "Saudi Arabia"
    )

    @Test
    fun belowThresholdIsNotWarm() {
        assertFalse(
            hasUsableHistory(
                historySlugs = listOf("mishary", "abdul-basit"),
                followed = emptySet(),
                searchRecents = emptyList()
            )
        )
    }

    @Test
    fun exactlyThreeDistinctIsWarm() {
        assertTrue(
            hasUsableHistory(
                historySlugs = listOf("mishary"),
                followed = setOf("abdul-basit"),
                searchRecents = listOf("husary")
            )
        )
    }

    @Test
    fun duplicatesAcrossSourcesCountOnce() {
        // One slug present in all three sources is still one identity: cold.
        assertFalse(
            hasUsableHistory(
                historySlugs = listOf("mishary", "mishary"),
                followed = setOf("mishary"),
                searchRecents = listOf("mishary")
            )
        )
    }

    @Test
    fun blanksAndDevotionalNeverWarm() {
        assertFalse(hasUsableHistory(listOf("", "   "), emptySet(), emptyList()))
        assertFalse(
            hasUsableHistory(
                historySlugs = listOf("__devotional", "__devotional", "mishary"),
                followed = emptySet(),
                searchRecents = emptyList()
            )
        )
    }

    @Test
    fun emptySourcesAreColdNullPath() {
        // The gate maps this to content(null) → the quiz still shows.
        assertFalse(hasUsableHistory(emptyList(), emptySet(), emptyList()))
        val imported = importTasteFromHistory(emptyList(), emptyList())
        assertTrue(imported.completed)
        assertNull(imported.pace)
        assertNull(imported.mood)
        assertTrue(imported.likedSlugs.isEmpty())
    }

    @Test
    fun importTakesMajorityPace() {
        val catalog = listOf(
            slowMujawwad("abdul-basit"),
            slowMujawwad("minshawi"),
            fastModern("brisk-voice")
        )
        val imported = importTasteFromHistory(
            slugs = listOf("abdul-basit", "minshawi", "brisk-voice"),
            catalog = catalog
        )
        assertEquals(Pace.SLOW, imported.pace)
        assertTrue(imported.completed)
        assertTrue(imported.skippedSlugs.isEmpty())
    }

    @Test
    fun importTakesMajorityMood() {
        val catalog = listOf(
            slowMujawwad("abdul-basit"),
            slowMujawwad("minshawi"),
            fastModern("brisk-voice")
        )
        val imported = importTasteFromHistory(
            slugs = listOf("abdul-basit", "minshawi", "brisk-voice"),
            catalog = catalog
        )
        assertEquals(Mood.CALM, imported.mood)
    }

    @Test
    fun importRepeatsWeightTheMajority() {
        val catalog = listOf(slowMujawwad("slow-voice"), fastModern("fast-voice"))
        val imported = importTasteFromHistory(
            slugs = listOf("fast-voice", "slow-voice", "slow-voice", "slow-voice"),
            catalog = catalog
        )
        assertEquals(Pace.SLOW, imported.pace)
    }

    @Test
    fun likedSlugsCappedAtTenInFrequencyOrder() {
        val catalog = List(12) { i -> voice("voice-$i") }
        val slugs = List(12) { i -> "voice-$i" }
        val imported = importTasteFromHistory(slugs, catalog)
        assertEquals(10, imported.likedSlugs.size)
        assertEquals((0 until 10).map { "voice-$it" }.toSet(), imported.likedSlugs)
    }

    @Test
    fun likedSlugsUseCatalogSpelling() {
        val catalog = listOf(voice("Mishary"))
        val imported = importTasteFromHistory(listOf("mishary"), catalog)
        assertTrue("Mishary" in imported.likedSlugs)
    }
}
