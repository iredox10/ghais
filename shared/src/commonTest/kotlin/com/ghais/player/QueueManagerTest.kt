package com.ghais.player

import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.TrackItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * QueueManager pure-logic tests: setQueue/playNext advance, end-of-queue per
 * repeat mode, move rejection, removeAt index adjustment, shuffle current-track
 * preservation, and the stationLabel contract. No platform player involved.
 *
 * Where semantics could be read two ways, the test asserts the READ behavior
 * and says so in its name — production was not touched.
 */
class QueueManagerTest {

    private fun track(surah: Int, ayah: Int = 0, slug: String = "mishary"): TrackItem =
        TrackItem(
            reciterSlug = slug,
            reciterName = "Mishary",
            surahId = surah,
            surahNameEn = "Surah $surah",
            surahNameAr = "سورة $surah",
            ayahNo = ayah,
            audioUrl = "https://example.com/$slug/$surah/$ayah.mp3"
        )

    private fun threeSurahs(): List<TrackItem> =
        listOf(track(1, 1), track(1, 2), track(2, 1))

    // --- setQueue ---

    @Test
    fun setQueueStartsAtGivenIndex() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items, startIndex = 2)
        assertEquals(2, manager.currentIndex)
        assertEquals(items[2], manager.currentTrack)
    }

    @Test
    fun setQueueOutOfRangeStartIndexFallsBackToZero_ReadBehavior() {
        // READ: `if (items.isNotEmpty() && startIndex in indices) startIndex
        // else if (items.isNotEmpty()) 0 else -1` — OOB start lands on 0, not -1.
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 99)
        assertEquals(0, manager.currentIndex)
        assertEquals(track(1, 1), manager.currentTrack)
    }

    @Test
    fun setQueueEmptyYieldsNoCurrent() {
        val manager = QueueManager()
        manager.setQueue(emptyList())
        assertEquals(-1, manager.currentIndex)
        assertNull(manager.currentTrack)
    }

    @Test
    fun setQueueClearsStationLabel() {
        val manager = QueueManager()
        manager.playStation("Radio: Mishary", threeSurahs())
        assertEquals("Radio: Mishary", manager.stationLabel)
        manager.setQueue(threeSurahs())
        assertNull(manager.stationLabel)
    }

    // --- playNext ---

    @Test
    fun playNextAdvancesInOffMode() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items)
        val next = manager.playNext()
        assertEquals(items[1], next)
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun playNextAtEndInOffModeReturnsNullAndHoldsIndex_ReadBehavior() {
        // READ: OFF branch only advances when `currentIndex + 1 < size`;
        // otherwise falls through to `return null` with the index untouched.
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 2)
        assertNull(manager.playNext())
        assertEquals(2, manager.currentIndex)
        assertEquals(track(2, 1), manager.currentTrack)
    }

    @Test
    fun playNextWrapsToFirstInQueueMode() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items, startIndex = 2)
        manager.setRepeatMode(RepeatMode.QUEUE)
        assertEquals(items[0], manager.playNext())
        assertEquals(0, manager.currentIndex)
    }

    @Test
    fun playNextInAyahModeHoldsSameTrack_ReadBehavior() {
        // READ: AYAH branch returns currentTrack without moving the index.
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items, startIndex = 1)
        manager.setRepeatMode(RepeatMode.AYAH)
        assertEquals(items[1], manager.playNext())
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun playNextInSurahModeAdvancesWithinSurahThenWrapsToSurahStart_ReadBehavior() {
        // READ: SURAH advances while the next track shares surahId; on a surah
        // boundary (or end of queue) it jumps back to the first track of the
        // current surah instead of crossing into the next surah.
        val manager = QueueManager()
        manager.setQueue(threeSurahs())
        manager.setRepeatMode(RepeatMode.SURAH)

        assertEquals(track(1, 2), manager.playNext())
        assertEquals(1, manager.currentIndex)

        // Next item belongs to surah 2 -> wrap to first track of surah 1.
        assertEquals(track(1, 1), manager.playNext())
        assertEquals(0, manager.currentIndex)
    }

    @Test
    fun playNextInSurahModeAtQueueEndWrapsToSurahStart_ReadBehavior() {
        // READ: last track of surah 2 with no next item -> first track of
        // surah 2 (here: itself, index unchanged).
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 2)
        manager.setRepeatMode(RepeatMode.SURAH)
        assertEquals(track(2, 1), manager.playNext())
        assertEquals(2, manager.currentIndex)
    }

    @Test
    fun playNextForceAdvanceSkipsInAyahMode_ReadBehavior() {
        // READ: forceAdvance bypasses the AYAH hold and steps forward.
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items)
        manager.setRepeatMode(RepeatMode.AYAH)
        assertEquals(items[1], manager.playNext(forceAdvance = true))
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun playNextOnEmptyQueueReturnsNull() {
        val manager = QueueManager()
        manager.setQueue(emptyList())
        assertNull(manager.playNext())
    }

    // --- playPrevious / skip gates ---

    @Test
    fun playPreviousStepsBackInOffModeAndStopsAtStart() {
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 1)
        assertEquals(track(1, 1), manager.playPrevious())
        assertEquals(0, manager.currentIndex)
        assertNull(manager.playPrevious())
        assertEquals(0, manager.currentIndex)
    }

    @Test
    fun playPreviousWrapsInQueueMode() {
        val manager = QueueManager()
        manager.setQueue(threeSurahs())
        manager.setRepeatMode(RepeatMode.QUEUE)
        assertEquals(track(2, 1), manager.playPrevious())
        assertEquals(2, manager.currentIndex)
    }

    @Test
    fun canSkipNextReflectsRepeatMode() {
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 2)
        assertFalse(manager.canSkipNext())
        manager.setRepeatMode(RepeatMode.QUEUE)
        assertTrue(manager.canSkipNext())
        manager.setRepeatMode(RepeatMode.AYAH)
        assertTrue(manager.canSkipNext())
        manager.setRepeatMode(RepeatMode.SURAH)
        assertTrue(manager.canSkipNext())
        manager.setQueue(emptyList())
        assertFalse(manager.canSkipNext())
    }

    @Test
    fun canSkipPreviousReflectsRepeatMode() {
        val manager = QueueManager()
        manager.setQueue(threeSurahs())
        assertFalse(manager.canSkipPrevious())
        manager.setRepeatMode(RepeatMode.QUEUE)
        assertTrue(manager.canSkipPrevious())
        manager.setQueue(emptyList())
        assertFalse(manager.canSkipPrevious())
    }

    @Test
    fun skipToIndexMovesAndRejectsOutOfRange() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items)
        assertEquals(items[2], manager.skipToIndex(2))
        assertEquals(2, manager.currentIndex)
        assertNull(manager.skipToIndex(9))
        assertEquals(2, manager.currentIndex)
    }

    // --- move ---

    @Test
    fun moveRejectsOutOfRangeAndIdentityInsteadOfClamping_ReadBehavior() {
        // READ: `move` returns false for OOB indices or fromIndex == toIndex;
        // there is no clamping — the queue and index stay untouched.
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items, startIndex = 1)
        assertFalse(manager.move(0, 9))
        assertFalse(manager.move(-1, 0))
        assertFalse(manager.move(1, 1))
        assertEquals(items, manager.queue)
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun moveTracksCurrentWhenCurrentItemMoves() {
        val manager = QueueManager()
        val items = listOf(track(1), track(2), track(3), track(4), track(5))
        manager.setQueue(items, startIndex = 2)
        assertTrue(manager.move(2, 4))
        assertEquals(4, manager.currentIndex)
        assertEquals(track(3), manager.currentTrack)
    }

    @Test
    fun moveShiftsIndexWhenCrossingCurrent() {
        val manager = QueueManager()
        val items = listOf(track(1), track(2), track(3), track(4), track(5))
        manager.setQueue(items, startIndex = 2)

        // from < current, to >= current -> index shifts down, same track.
        assertTrue(manager.move(0, 4))
        assertEquals(1, manager.currentIndex)
        assertEquals(track(3), manager.currentTrack)

        // Reset, then from > current, to <= current -> index shifts up.
        manager.setQueue(items, startIndex = 2)
        assertTrue(manager.move(4, 0))
        assertEquals(3, manager.currentIndex)
        assertEquals(track(3), manager.currentTrack)
    }

    // --- removeAt ---

    @Test
    fun removeBeforeCurrentShiftsIndexDownKeepingTrack() {
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 2)
        manager.removeAt(0)
        assertEquals(1, manager.currentIndex)
        assertEquals(track(2, 1), manager.currentTrack)
        assertEquals(2, manager.queue.size)
    }

    @Test
    fun removeCurrentAdvancesToNextTrackAtSameIndex() {
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 1)
        val current = manager.removeAt(1)
        assertEquals(track(2, 1), current)
        assertEquals(1, manager.currentIndex)
        assertEquals(2, manager.queue.size)
    }

    @Test
    fun removeLastCurrentInOffModeLeavesNoCurrent_ReadBehavior() {
        // READ: removing the current item at the tail with repeat OFF sets
        // currentIndex to -1 and returns null (currentTrack).
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 2)
        assertNull(manager.removeAt(2))
        assertEquals(-1, manager.currentIndex)
        assertNull(manager.currentTrack)
        assertEquals(2, manager.queue.size)
    }

    @Test
    fun removeLastCurrentInQueueModeWrapsToFirst_ReadBehavior() {
        // READ: same tail removal with repeat QUEUE wraps currentIndex to 0.
        val manager = QueueManager()
        manager.setQueue(threeSurahs(), startIndex = 2)
        manager.setRepeatMode(RepeatMode.QUEUE)
        assertEquals(track(1, 1), manager.removeAt(2))
        assertEquals(0, manager.currentIndex)
    }

    @Test
    fun removeOnlyTrackEmptiesQueue() {
        val manager = QueueManager()
        manager.setQueue(listOf(track(7)))
        assertNull(manager.removeAt(0))
        assertEquals(-1, manager.currentIndex)
        assertTrue(manager.queue.isEmpty())
    }

    @Test
    fun removeOutOfBoundsReturnsNullAndKeepsQueue() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items, startIndex = 1)
        assertNull(manager.removeAt(9))
        assertNull(manager.removeAt(-1))
        assertEquals(items, manager.queue)
        assertEquals(1, manager.currentIndex)
    }

    // --- shuffle ---

    @Test
    fun toggleShuffleOnPreservesCurrentTrack() {
        val manager = QueueManager()
        val items = listOf(track(1), track(2), track(3), track(4), track(5))
        manager.setQueue(items, startIndex = 2)
        val current = manager.currentTrack
        manager.toggleShuffle()
        assertTrue(manager.isShuffle)
        assertEquals(current, manager.currentTrack)
        assertEquals(0, manager.currentIndex)
        assertEquals(items.toSet(), manager.queue.toSet())
        assertEquals(items.size, manager.queue.size)
    }

    @Test
    fun toggleShuffleOffRestoresOriginalOrderAndCurrentTrack() {
        val manager = QueueManager()
        val items = listOf(track(1), track(2), track(3), track(4), track(5))
        manager.setQueue(items, startIndex = 2)
        val current = manager.currentTrack
        manager.toggleShuffle()
        manager.toggleShuffle()
        assertFalse(manager.isShuffle)
        assertEquals(items, manager.queue)
        assertEquals(current, manager.currentTrack)
        assertEquals(2, manager.currentIndex)
    }

    // --- station label ---

    @Test
    fun playStationSetsLabelAndQueue() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.playStation("Radio: Mishary", items)
        assertEquals("Radio: Mishary", manager.stationLabel)
        assertEquals(items, manager.queue)
        assertEquals(0, manager.currentIndex)
    }

    @Test
    fun clearClearsLabelQueueAndIndex() {
        val manager = QueueManager()
        manager.playStation("Radio: Mishary", threeSurahs())
        manager.clear()
        assertNull(manager.stationLabel)
        assertTrue(manager.queue.isEmpty())
        assertEquals(-1, manager.currentIndex)
        assertNull(manager.currentTrack)
    }

    @Test
    fun setStationLabelStampsWithoutTouchingQueueOrIndex() {
        val manager = QueueManager()
        val items = threeSurahs()
        manager.setQueue(items, startIndex = 1)
        manager.setStationLabel("Radio: Mishary")
        assertEquals("Radio: Mishary", manager.stationLabel)
        assertEquals(items, manager.queue)
        assertEquals(1, manager.currentIndex)
        assertEquals(items[1], manager.currentTrack)
    }
}
