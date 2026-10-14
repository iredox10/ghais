package com.ghais.ui.screens.reciters

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Agent A6 — pins the transliteration-tolerant search contract in
 * [normalizeReciterSearchText] / [reciterMatchesNormalizedQuery]:
 * variant spellings match, separators/case fold away, diacritics + Arabic
 * harakat strip, and blank queries never match.
 */
class TransliterationTest {

    private fun qari(slug: String, nameEn: String, country: String = ""): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameEn,
        country = country,
    )

    @Test
    fun testAhmedMatchesAhmad() {
        val reciter = qari("ahmad-al-nufais", "Ahmad Al Nufais")
        assertTrue(reciterMatchesNormalizedQuery(reciter, "Ahmed"))
        assertEquals(normalizeReciterSearchText("Ahmed"), normalizeReciterSearchText("Ahmad"))
    }

    @Test
    fun testSpaceMatchesHyphenSlug() {
        val reciter = qari("abdul-basit", "Abdul-Basit Abdul-Samad")
        assertTrue(reciterMatchesNormalizedQuery(reciter, "abdul basit"))
        assertTrue(reciterMatchesNormalizedQuery(reciter, "AbdulBasit"))
    }

    @Test
    fun testBasetMatchesBasit() {
        val reciter = qari("abdul-basit", "Abdul-Basit Abdul-Samad")
        assertTrue(reciterMatchesNormalizedQuery(reciter, "Abdulbaset"))
    }

    @Test
    fun testOuMatchesU() {
        val mahmud = qari("mahmud-khalil", "Mahmud Khalil Al-Hussary")
        assertTrue(reciterMatchesNormalizedQuery(mahmud, "Mahmoud"))
        val yusuf = qari("yusuf-qari", "Yusuf Qari")
        assertTrue(reciterMatchesNormalizedQuery(yusuf, "Yousuf"))
    }

    @Test
    fun testEeMatchesI() {
        val reciter = qari("qari-ahmad", "Qari Ahmad")
        assertTrue(reciterMatchesNormalizedQuery(reciter, "Qaree"))
    }

    @Test
    fun testDiacriticsAndHarakatStrip() {
        assertEquals("cafe", normalizeReciterSearchText("café"))
        // U+0645 U+064F U+062D U+064E U+0645 U+064E U+062F == "مُحَمَّد" -> "محمد"
        assertEquals(normalizeReciterSearchText("محمد"), normalizeReciterSearchText("مُحَمَّد"))
    }

    @Test
    fun testBlankQueryNeverMatches() {
        val reciter = qari("mishary", "Mishary Rashid Alafasy", country = "Kuwait")
        assertFalse(reciterMatchesNormalizedQuery(reciter, ""))
        assertFalse(reciterMatchesNormalizedQuery(reciter, "   "))
    }

    @Test
    fun testCountryStillMatches() {
        val reciter = qari("mishary", "Mishary Rashid Alafasy", country = "Kuwait")
        assertTrue(reciterMatchesNormalizedQuery(reciter, "kuwait"))
    }
}
