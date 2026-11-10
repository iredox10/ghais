package com.ghais.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackMetadataStateTest {
    @Test fun changingToArtistFreeTrackClearsPreviousReciter() {
        val old = PlaybackMetadataState("Al-Fatihah", "Previous reciter")
        val next = old.replace("Dua", null)
        assertEquals("Dua", next.title)
        assertNull(next.artist)
    }

    @Test fun clearingMetadataDoesNotRetainPreviousTrack() {
        val next = PlaybackMetadataState("Old title", "Old artist").replace(" ", "")
        assertNull(next.title)
        assertNull(next.artist)
    }
}
