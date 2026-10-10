package com.ghais.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock

/**
 * PlayEventLog: in-memory buffer of qualifying surah plays for the Appwrite
 * `play_events` collection.
 *
 * [UserUsageRepository.maybeCountSurahPlay] records here at the exact site
 * where `surahPlays` increments on the completion-or-30s rule, so every
 * buffered event is a play the local chart already counted — the buffer never
 * invents one. [com.ghais.data.sync.SyncEngine.uploadPlayEvents] drains the
 * buffer on the sync path (create-only, never update/delete).
 *
 * Pure Kotlin, no Android APIs: a [Mutex] guards an immutable list so the
 * non-suspend [record]/[drain] pair stays thread-safe on every platform.
 * [record] uses [Mutex.tryLock] and drops the event under extreme contention
 * rather than ever blocking playback; [drain] leaves the buffer intact when
 * the lock is busy so the next heartbeat retries — drains never lose data.
 * Capped at [MAX_BUFFER] (drop-oldest); upload failures re-record the unsent
 * remainder, which is also cap-bounded, so the buffer can never grow without
 * bound. Never throws.
 */
object PlayEventLog {
    /** Max buffered events; oldest evicted first. */
    const val MAX_BUFFER = 200

    data class PlayEvent(
        val reciterSlug: String,
        val surahId: Int,
        val ms: Long,
        /** Signing-in account at record time, null while signed out. */
        val ownerId: String? = null,
        val playedAtMs: Long = Clock.System.now().toEpochMilliseconds()
    )

    private val lock = Mutex()
    private var buffer: List<PlayEvent> = emptyList()

    fun record(reciterSlug: String, surahId: Int, ms: Long, ownerId: String? = null) {
        try {
            val slug = reciterSlug.trim()
            if (slug.isEmpty() || surahId !in 1..114 || ms < 0L) return
            val event = PlayEvent(
                reciterSlug = slug,
                surahId = surahId,
                ms = ms,
                ownerId = ownerId?.takeIf { it.isNotBlank() }
            )
            if (!lock.tryLock()) return
            try {
                buffer = (buffer + event).takeLast(MAX_BUFFER)
            } finally {
                lock.unlock()
            }
        } catch (_: Exception) {
        }
    }

    fun drain(): List<PlayEvent> = try {
        if (!lock.tryLock()) return emptyList()
        try {
            val out = buffer
            buffer = emptyList()
            out
        } finally {
            lock.unlock()
        }
    } catch (_: Exception) {
        emptyList()
    }
}
