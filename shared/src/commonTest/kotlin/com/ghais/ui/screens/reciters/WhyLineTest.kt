package com.ghais.ui.screens.reciters

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the [whyLine] recommendation-explanation contract behind the
 * "More like {name}" rail on the reciter profile.
 *
 * [whyLine] is a pure function (no I/O, no platform APIs), so these tests
 * cover: full shared-attribute lines, the medium-tempo "measured" wording,
 * per-signal matching (flag-suffixed country, case-insensitive riwayah,
 * style token overlap), and the no-shared-attributes fallback.
 */
class WhyLineTest {

    private fun qari(
        slug: String,
        nameEn: String,
        country: String = "",
        riwayah: String = "Hafs",
        style: String = "murattal",
        tempo: String = "medium",
    ): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameEn,
        riwayah = riwayah,
        style = style,
        tempo = tempo,
        country = country,
    )

    private val seed = qari(
        slug = "seed-qari",
        nameEn = "Seed Qari",
        country = "Egypt",
        riwayah = "Hafs",
        style = "murattal",
        tempo = "medium",
    )

    @Test
    fun testAllSharedProducesFullLine() {
        val candidate = qari(
            slug = "near-qari",
            nameEn = "Near Qari",
            country = "Egypt",
            riwayah = "Hafs",
            style = "murattal",
            tempo = "medium",
        )
        assertEquals(
            "Same measured pace · Hafs · Murattal · Egypt",
            whyLine(seed, candidate),
        )
    }

    @Test
    fun testFallbackWhenNothingShared() {
        val stranger = qari(
            slug = "far-qari",
            nameEn = "Far Qari",
            country = "France",
            riwayah = "Warsh",
            style = "mujawwad",
            tempo = "fast",
        )
        assertEquals("Voices like Seed Qari", whyLine(seed, stranger))
    }

    @Test
    fun testFlagSuffixedCountryStillMatches() {
        val flagged = qari(
            slug = "flagged-qari",
            nameEn = "Flagged Qari",
            country = "Egypt 🇪🇬",
            riwayah = "Warsh",
            style = "mujawwad",
            tempo = "fast",
        )
        val line = whyLine(seed, flagged)
        assertEquals("Egypt", line)
        assertFalse(line.contains("🇪🇬"), "display country must not leak the flag suffix")
    }

    @Test
    fun testRiwayahMatchesCaseInsensitiveAndTrimmed() {
        val candidate = qari(
            slug = "riwayah-qari",
            nameEn = "Riwayah Qari",
            country = "Spain",
            riwayah = " hafs ",
            style = "mujawwad",
            tempo = "slow",
        )
        assertEquals("Hafs", whyLine(seed, candidate))
    }

    @Test
    fun testStyleTokenOverlapMatches() {
        val candidate = qari(
            slug = "style-qari",
            nameEn = "Style Qari",
            country = "Spain",
            riwayah = "Warsh",
            style = "Madinah Murattal",
            tempo = "slow",
        )
        // Seed style is "murattal": only the style tag is shared.
        assertEquals("Murattal", whyLine(seed, candidate))
    }

    @Test
    fun testFastTempoMapsToBrisk() {
        val fastSeed = seed.copy(slug = "fast-seed", tempo = "fast")
        val candidate = qari(
            slug = "brisk-qari",
            nameEn = "Brisk Qari",
            country = "Spain",
            riwayah = "Warsh",
            style = "mujawwad",
            tempo = " Fast ",
        )
        assertEquals("Same brisk pace", whyLine(fastSeed, candidate))
    }

    @Test
    fun testBlankSidesScoreNothing() {
        val blankSeed = seed.copy(slug = "blank-seed", country = "", riwayah = "", style = "", tempo = "")
        val candidate = qari(
            slug = "blank-qari",
            nameEn = "Blank Qari",
            country = "",
            riwayah = "",
            style = "",
            tempo = "",
        )
        assertEquals("Voices like Seed Qari", whyLine(blankSeed, candidate))
        assertTrue(whyLine(seed, candidate).contains("Voices like"))
    }
}
