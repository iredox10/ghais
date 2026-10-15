package com.ghais.personal

import com.ghais.domain.model.Reciter
import com.ghais.ui.screens.reciters.taste.Mood
import com.ghais.ui.screens.reciters.taste.Pace
import com.ghais.ui.screens.reciters.taste.TasteProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure implicit taste vector v2 ([buildTasteVector] /
 * [affinityFor]): no engine, no Settings, no P1 dependency (raw
 * slug->affinity maps stand in for `scoreSignals` output).
 */
class TasteVectorTest {

    private fun reciter(
        slug: String,
        tempo: String = "Medium",
        style: String = "Murattal",
        riwayah: String = "Hafs",
        country: String = "Kuwait",
    ): Reciter = Reciter(
        slug = slug,
        nameEn = slug,
        nameAr = "اختبار",
        riwayah = riwayah,
        style = style,
        tempo = tempo,
        country = country,
    )

    private val slowMujawwad = reciter(
        slug = "abdul-basit-mujawwad",
        tempo = "Slow & Tearful",
        style = "Mujawwad",
        riwayah = "Hafs",
        country = "Egypt",
    )
    private val fastModern = reciter(
        slug = "fast-voice",
        tempo = "Fast & Expressive",
        style = "Modern Murattal",
        riwayah = "Hafs",
        country = "Saudi Arabia",
    )
    private val catalog = listOf(slowMujawwad, fastModern)

    @Test
    fun emptyInputsYieldZeroVector() {
        val v = buildTasteVector(emptyMap(), catalog, null)
        assertTrue(v.paceW.isEmpty(), "paceW must be empty, got ${v.paceW}")
        assertTrue(v.moodW.isEmpty(), "moodW must be empty, got ${v.moodW}")
        assertTrue(v.styleW.isEmpty(), "styleW must be empty, got ${v.styleW}")
        assertTrue(v.riwayahW.isEmpty(), "riwayahW must be empty, got ${v.riwayahW}")
        assertTrue(v.countryW.isEmpty(), "countryW must be empty, got ${v.countryW}")
        assertEquals(0f, v.confidence, 1e-6f)
    }

    @Test
    fun unknownSlugIgnored() {
        val v = buildTasteVector(mapOf("ghost-voice" to 5f, "  " to 3f), catalog, null)
        assertTrue(v.paceW.isEmpty(), "unknown slugs must not seed paceW, got ${v.paceW}")
        assertTrue(v.moodW.isEmpty(), "unknown slugs must not seed moodW, got ${v.moodW}")
        assertTrue(v.styleW.isEmpty(), "unknown slugs must not seed styleW, got ${v.styleW}")
        assertEquals(0f, v.confidence, 1e-6f)
    }

    @Test
    fun negativeAndZeroAffinityIgnored() {
        val v = buildTasteVector(
            mapOf("abdul-basit-mujawwad" to -3f, "fast-voice" to 0f),
            catalog,
            null,
        )
        assertTrue(v.paceW.isEmpty(), "aversion must not create positive mass, got ${v.paceW}")
        assertTrue(v.styleW.isEmpty(), "zero affinity must contribute nothing, got ${v.styleW}")
        assertEquals(0f, v.confidence, 1e-6f)
    }

    @Test
    fun affinitySpreadsAcrossAttributes() {
        val v = buildTasteVector(mapOf("abdul-basit-mujawwad" to 4f), catalog, null)
        assertEquals(4f, v.paceW[Pace.SLOW] ?: 0f, 1e-6f)
        assertEquals(4f, v.moodW[Mood.CALM] ?: 0f, 1e-6f)
        assertEquals(4f, v.styleW["mujawwad"] ?: 0f, 1e-6f)
        assertEquals(4f, v.riwayahW["hafs"] ?: 0f, 1e-6f)
        assertEquals(4f, v.countryW["egypt"] ?: 0f, 1e-6f)
        // The slow mujawwad voice fits no other mood.
        assertEquals(1, v.moodW.size, "expected only CALM, got ${v.moodW}")
        assertEquals(4f / 20f, v.confidence, 1e-6f)
    }

    @Test
    fun styleSchoolFallbackForGenericTempo() {
        val eclectic = reciter(slug = "eclectic-voice", tempo = "Eclectic", style = "Mujawwad")
        val v = buildTasteVector(mapOf("eclectic-voice" to 2f), listOf(eclectic), null)
        assertEquals(2f, v.paceW[Pace.SLOW] ?: 0f, 1e-6f)
    }

    @Test
    fun confidenceRampsWithWeight() {
        val v = buildTasteVector(mapOf("abdul-basit-mujawwad" to 6f, "fast-voice" to 4f), catalog, null)
        assertEquals(0.5f, v.confidence, 1e-6f)
    }

    @Test
    fun confidenceCapsAtOne() {
        val v = buildTasteVector(mapOf("abdul-basit-mujawwad" to 25f), catalog, null)
        assertEquals(1f, v.confidence, 1e-6f)
    }

    @Test
    fun completedQuizBlendsPriors() {
        val quiz = TasteProfile(pace = Pace.FLOWING, mood = Mood.FOCUS, completed = true)
        val v = buildTasteVector(emptyMap(), catalog, quiz)
        assertEquals(QUIZ_PRIOR_WEIGHT, v.paceW[Pace.FLOWING] ?: 0f, 1e-6f)
        assertEquals(QUIZ_PRIOR_WEIGHT, v.moodW[Mood.FOCUS] ?: 0f, 1e-6f)
        assertEquals(4f / 20f, v.confidence, 1e-6f)
    }

    @Test
    fun incompleteQuizIgnored() {
        val quiz = TasteProfile(pace = Pace.SLOW, mood = Mood.CALM, completed = false)
        val v = buildTasteVector(emptyMap(), catalog, quiz)
        assertTrue(v.paceW.isEmpty(), "unfinished quiz must not seed paceW, got ${v.paceW}")
        assertTrue(v.moodW.isEmpty(), "unfinished quiz must not seed moodW, got ${v.moodW}")
        assertEquals(0f, v.confidence, 1e-6f)
    }

    @Test
    fun affinityForRanksMatchingVoiceHigher() {
        val v = buildTasteVector(mapOf("abdul-basit-mujawwad" to 4f), catalog, null)
        val matching = affinityFor(slowMujawwad, v)
        val other = affinityFor(fastModern, v)
        assertTrue(matching > other, "matching voice ($matching) must outrank other ($other)")
        assertEquals(0f, affinityFor(slowMujawwad, TasteVectorV2()), 1e-6f)
    }

    @Test
    fun buildIsDeterministic() {
        val quiz = TasteProfile(pace = Pace.SLOW, mood = Mood.CALM, completed = true)
        val first = buildTasteVector(
            mapOf("abdul-basit-mujawwad" to 4f, "fast-voice" to 2f),
            catalog,
            quiz,
        )
        val reordered = buildTasteVector(
            mapOf("fast-voice" to 2f, "abdul-basit-mujawwad" to 4f),
            catalog,
            quiz,
        )
        assertEquals(first, reordered)
        assertEquals(
            affinityFor(slowMujawwad, first),
            affinityFor(slowMujawwad, reordered),
            1e-6f,
        )
    }
}
