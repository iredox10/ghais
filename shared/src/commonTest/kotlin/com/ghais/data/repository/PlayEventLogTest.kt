package com.ghais.data.repository

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the play-event buffer: record validation, drain semantics, and the
 * 200-event cap.
 *
 * `PlayEventLog` is pure Kotlin with no `Settings` and no engine dependency
 * (`PlayEventLog.kt`), so everything here runs deterministically on any
 * platform: `drain` in setup/teardown is the whole isolation story (there is
 * no other reset API, and none is needed — `drain` empties the buffer).
 *
 * Deliberately NOT covered: the `Mutex.tryLock` contention paths (record
 * dropping under extreme contention, drain returning empty while busy) —
 * they cannot be forced deterministically without a lock-injection hook.
 */
class PlayEventLogTest {

    @BeforeTest
    fun setUp() {
        PlayEventLog.drain()
    }

    @AfterTest
    fun tearDown() {
        PlayEventLog.drain()
    }

    @Test
    fun testRecordThenDrainRoundtripsTheEvent() {
        PlayEventLog.record(reciterSlug = "alafasy", surahId = 1, ms = 1000L, ownerId = "user-1")

        val events = PlayEventLog.drain()

        assertEquals(1, events.size)
        val event = events.single()
        assertEquals("alafasy", event.reciterSlug)
        assertEquals(1, event.surahId)
        assertEquals(1000L, event.ms)
        assertEquals("user-1", event.ownerId)
        assertTrue(event.playedAtMs > 0L)
    }

    @Test
    fun testRecordTrimsTheSlug() {
        PlayEventLog.record(reciterSlug = "  alafasy  ", surahId = 1, ms = 10L)

        assertEquals("alafasy", PlayEventLog.drain().single().reciterSlug)
    }

    @Test
    fun testBlankSlugIsDropped() {
        PlayEventLog.record(reciterSlug = "", surahId = 1, ms = 10L)
        PlayEventLog.record(reciterSlug = "   ", surahId = 1, ms = 10L)

        assertTrue(PlayEventLog.drain().isEmpty())
    }

    @Test
    fun testSurahIdOutsideOneToOneHundredFourteenIsDropped() {
        PlayEventLog.record("alafasy", surahId = 0, ms = 10L)
        PlayEventLog.record("alafasy", surahId = -1, ms = 10L)
        PlayEventLog.record("alafasy", surahId = 115, ms = 10L)

        assertTrue(PlayEventLog.drain().isEmpty())
    }

    @Test
    fun testSurahIdBoundariesAreKept() {
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L)
        PlayEventLog.record("alafasy", surahId = 114, ms = 10L)

        assertEquals(listOf(1, 114), PlayEventLog.drain().map { it.surahId })
    }

    @Test
    fun testNegativeMsIsDroppedButZeroIsKept() {
        PlayEventLog.record("alafasy", surahId = 1, ms = -1L)
        PlayEventLog.record("alafasy", surahId = 1, ms = 0L)

        // `ms` is a position stamp, not a duration: 0 is a real value.
        assertEquals(listOf(0L), PlayEventLog.drain().map { it.ms })
    }

    @Test
    fun testBlankOwnerIdBecomesNull() {
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L, ownerId = "")
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L, ownerId = "   ")
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L)

        val events = PlayEventLog.drain()
        assertEquals(3, events.size)
        assertTrue(events.all { it.ownerId == null })
    }

    @Test
    fun testNullOwnerIdIsPreserved() {
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L, ownerId = "user-1")

        assertEquals("user-1", PlayEventLog.drain().single().ownerId)
    }

    @Test
    fun testDrainEmptiesTheBuffer() {
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L)

        assertEquals(1, PlayEventLog.drain().size)
        assertTrue(
            PlayEventLog.drain().isEmpty(),
            "A second drain must be empty — drains never duplicate",
        )
    }

    @Test
    fun testDrainOnAnEmptyBufferReturnsEmpty() {
        assertTrue(PlayEventLog.drain().isEmpty())
    }

    @Test
    fun testDrainPreservesRecordOrder() {
        PlayEventLog.record("alafasy", surahId = 1, ms = 10L)
        PlayEventLog.record("husary", surahId = 2, ms = 20L)
        PlayEventLog.record("minshawi", surahId = 3, ms = 30L)

        assertEquals(
            listOf("alafasy", "husary", "minshawi"),
            PlayEventLog.drain().map { it.reciterSlug },
        )
    }

    @Test
    fun testBufferCapsAtTwoHundredDroppingTheOldest() {
        for (i in 0 until 250) {
            PlayEventLog.record("alafasy", surahId = (i % 114) + 1, ms = i.toLong())
        }

        val events = PlayEventLog.drain()
        assertEquals(PlayEventLog.MAX_BUFFER, events.size)
        assertEquals(
            50L,
            events.first().ms,
            "The 50 oldest events must have been evicted oldest-first",
        )
        assertEquals(249L, events.last().ms)
        assertNull(events.firstOrNull { it.ms < 50L })
    }
}
