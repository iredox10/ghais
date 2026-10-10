package com.ghais.player

import com.ghais.data.repository.QuranDataRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reciter radio station tests: queue shape, fallback, adjacency rule, and the
 * [QueueManager.stationLabel] contract. Pure common code — no platform player
 * involved ([QueueManager]/[QuranDataRepository] seeds only).
 */
class ReciterRadioTest {

    @Test
    fun stationIsNonEmptyForKnownSlug() {
        val queue = ReciterRadio.stationQueue("mishary", 25)
        assertEquals(25, queue.size)
        val seedSlug = QuranDataRepository.getReciterBySlug("mishary").slug
        assertTrue(queue.any { it.reciterSlug == seedSlug }, "station must lead with the seed qari")
    }

    @Test
    fun stationUsesDefaultCountOf25() {
        assertEquals(25, ReciterRadio.stationQueue("mishary").size)
    }

    @Test
    fun unknownSlugFallsBackToMishary() {
        val resolved = QuranDataRepository.getReciterBySlug("no-such-qari-xyz")
        assertEquals(QuranDataRepository.getFallbackReciter().slug, resolved.slug)
        val queue = ReciterRadio.stationQueue("no-such-qari-xyz", 10)
        assertEquals(10, queue.size)
    }

    @Test
    fun allItemsArePlayableFullSurahShape() {
        val queue = ReciterRadio.stationQueue("mishary", 25)
        assertTrue(queue.isNotEmpty())
        queue.forEach { track ->
            // Playable-shape per the profile's playableTracks builder convention.
            assertTrue(track.audioUrl.isNotBlank(), "audioUrl must be set (surah ${track.surahId})")
            assertEquals(0, track.ayahNo, "station emits full-surah items only")
            assertTrue(track.surahId in 1..114)
            // Only surahs the emitting reciter actually has available.
            val reciter = QuranDataRepository.getReciterBySlug(track.reciterSlug)
            assertTrue(
                reciter.isSurahAvailable(track.surahId),
                "${track.reciterSlug} must have surah ${track.surahId} available"
            )
        }
    }

    @Test
    fun noAdjacentSameSurahFromSameQari() {
        val queue = ReciterRadio.stationQueue("mishary", 25)
        for (i in 1 until queue.size) {
            assertFalse(
                queue[i].reciterSlug == queue[i - 1].reciterSlug &&
                    queue[i].surahId == queue[i - 1].surahId,
                "adjacent duplicate at index $i: ${queue[i].reciterSlug}/${queue[i].surahId}"
            )
        }
    }

    @Test
    fun stationLabelIsSetByPlayStationAndClearedByOtherPaths() {
        val manager = QueueManager()
        assertNull(manager.stationLabel)

        manager.playStation("Radio: Mishary", ReciterRadio.stationQueue("mishary", 5))
        assertEquals("Radio: Mishary", manager.stationLabel)
        assertEquals(5, manager.queue.size)

        manager.setQueue(emptyList())
        assertNull(manager.stationLabel)

        manager.playStation("Radio: Mishary", ReciterRadio.stationQueue("mishary", 5))
        assertEquals("Radio: Mishary", manager.stationLabel)
        manager.clear()
        assertNull(manager.stationLabel)
    }
}
