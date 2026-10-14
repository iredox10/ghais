package com.ghais.ui.screens.reciters.today

import com.ghais.domain.model.Reciter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Agent A6 — pins the [voiceOfDay] contract the Reciters tab relies on:
 *
 * 1. Determinism: same catalog + same epoch day always yields the same
 *    reciter (including negative epoch days — `floorMod`, never a crash).
 * 2. Rotation: a full weight cycle surfaces more than one voice, and the
 *    obscure voice gets its slot (index 5 of a 5+1 cycle below).
 * 3. Weighting: over one full cycle the well-known voice appears
 *    [WELL_KNOWN_WEIGHT]× per [OTHER_WEIGHT]× for the obscure voice.
 * 4. Edges: empty list -> null, single reciter -> itself.
 */
class VoiceOfDayTest {

    private fun qari(slug: String, nameEn: String): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameEn,
    )

    private val mishary = qari("mishary", "Mishary Rashid Alafasy")
    private val obscure = qari("some-obscure-qari", "Some Obscure Qari")
    private val pair = listOf(obscure, mishary) // intentionally unsorted

    @Test
    fun testDeterministicForSameDay() {
        assertEquals(voiceOfDay(pair, 0), voiceOfDay(pair, 0))
        assertEquals(voiceOfDay(pair, 42), voiceOfDay(pair, 42))
        assertEquals(voiceOfDay(pair, -7), voiceOfDay(pair, -7))
    }

    @Test
    fun testRotatesAcrossCycle() {
        // Sorted order is mishary (weight 5) then obscure (weight 1):
        // slots 0..4 -> mishary, slot 5 -> obscure.
        val week = (0L..5L).map { assertNotNull(voiceOfDay(pair, it)).slug }
        assertTrue(week.toSet().size > 1, "no rotation over a full cycle: $week")
        assertEquals("mishary", week[0])
        assertEquals("some-obscure-qari", week[5])
    }

    @Test
    fun testWellKnownWeightedOverObscure() {
        val counts = (0L..5L)
            .map { assertNotNull(voiceOfDay(pair, it)).slug }
            .groupingBy { it }.eachCount()
        assertEquals(WELL_KNOWN_WEIGHT, counts["mishary"])
        assertEquals(OTHER_WEIGHT, counts["some-obscure-qari"])
    }

    @Test
    fun testEmptyAndSingleEdges() {
        assertNull(voiceOfDay(emptyList(), 0))
        assertEquals(obscure, voiceOfDay(listOf(obscure), 99))
    }
}
