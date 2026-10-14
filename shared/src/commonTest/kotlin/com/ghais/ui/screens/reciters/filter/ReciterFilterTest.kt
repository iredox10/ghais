package com.ghais.ui.screens.reciters.filter

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AGENT A2 — pure-logic tests for the taste-axis reciter filters.
 * No Compose, no platform APIs: every test runs on commonTest.
 */
class ReciterFilterTest {

    private fun reciter(
        slug: String,
        riwayah: String,
        style: String,
        tempo: String,
        country: String,
    ) = Reciter(
        slug = slug,
        nameEn = slug,
        nameAr = slug,
        riwayah = riwayah,
        style = style,
        tempo = tempo,
        country = country,
    )

    private val catalog = listOf(
        reciter("mishary", "Hafs", "Murattal", "Medium", "Kuwait"),
        reciter("husary-teacher", "Hafs", "Teacher (Mu'allim)", "Slow & Measured", "Egypt 🇪🇬"),
        reciter("sudais", "Hafs", "Haramain", "Brisk & Resonant", "Saudi Arabia"),
        reciter("warsh-reader", "Warsh", "Murattal", "Medium", "Morocco"),
        reciter("qaloon-reader", "Qaloon", "Murattal", "Slow", "Libya"),
        reciter("duri-reader", "Al-Duri", "Mujawwad", "Slow & Resonant", "Egypt"),
    )

    @Test
    fun filterByRiwayahAxis() {
        val result = applyFilters(catalog, ReciterFilterState(riwayah = "Warsh"))
        assertEquals(listOf("warsh-reader"), result.map { it.slug })
    }

    @Test
    fun filterByStyleAxis() {
        val result = applyFilters(catalog, ReciterFilterState(style = "Haramain"))
        assertEquals(listOf("sudais"), result.map { it.slug })
    }

    @Test
    fun filterByTempoAxis() {
        val result = applyFilters(catalog, ReciterFilterState(tempo = "Medium"))
        assertEquals(setOf("mishary", "warsh-reader"), result.map { it.slug }.toSet())
    }

    @Test
    fun filterByMoodAxis() {
        // Slow/measured reciters sleep; medium Murattal does not.
        val result = applyFilters(catalog, ReciterFilterState(mood = MoodFilter.SLEEP))
        val slugs = result.map { it.slug }.toSet()
        assertTrue("husary-teacher" in slugs)
        assertTrue("qaloon-reader" in slugs)
        assertTrue("mishary" !in slugs)
    }

    @Test
    fun filterByNationIsFlagInsensitive() {
        // Bundled seed carries "Egypt 🇪🇬", admin rows carry "Egypt" — one key.
        val result = applyFilters(catalog, ReciterFilterState(nation = "egypt"))
        assertEquals(setOf("husary-teacher", "duri-reader"), result.map { it.slug }.toSet())
    }

    @Test
    fun combinedAxesNarrowToSingleReciter() {
        val result = applyFilters(
            catalog,
            ReciterFilterState(style = "Murattal", nation = "morocco")
        )
        assertEquals(listOf("warsh-reader"), result.map { it.slug })
    }

    @Test
    fun impossibleCombinationYieldsEmptyResult() {
        val result = applyFilters(
            catalog,
            ReciterFilterState(riwayah = "Warsh", tempo = "Slow")
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun emptyStateReturnsWholeCatalog() {
        val result = applyFilters(catalog, ReciterFilterState())
        assertEquals(catalog, result)
    }

    @Test
    fun optionsDerivedFromCatalog() {
        val options = deriveFilterOptions(catalog)
        assertTrue("Hafs" in options.riwayahs)
        assertTrue("Warsh" in options.riwayahs)
        assertTrue("Murattal" in options.styles)
        // Nations dedupe flag variants: one Egypt entry, richest spelling kept.
        assertEquals(1, options.nations.count { it.trim().startsWith("Egypt") })
        assertTrue(options.nations.any { it.contains("🇪🇬") })
    }
}
