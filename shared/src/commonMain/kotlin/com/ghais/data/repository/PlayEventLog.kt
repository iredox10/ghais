package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * PlayEventLog: buffer of qualifying surah plays for the Appwrite
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
 *
 * The buffer is persisted to Settings on every mutation and restored on first
 * access, so events recorded offline survive a process kill and still upload
 * on the next launch's reconnect pass.
 */
object PlayEventLog {
    /** Max buffered events; oldest evicted first. */
    const val MAX_BUFFER = 200

    @Serializable
    data class PlayEvent(
        val reciterSlug: String,
        val surahId: Int,
        val ms: Long,
        /** Signing-in account at record time, null while signed out. */
        val ownerId: String? = null,
        val playedAtMs: Long = Clock.System.now().toEpochMilliseconds()
    )

    private const val STORE_KEY = "ghais_play_events_v1"
    private val json = Json { ignoreUnknownKeys = true }
    private val settings: Settings by lazy { Settings() }

    private val lock = Mutex()
    private var buffer: List<PlayEvent> = emptyList()
    private var restored = false

    /** Restores the persisted buffer once, on first use. Never throws. */
    private fun ensureRestored() {
        if (restored) return
        restored = true
        try {
            val raw = settings.getString(STORE_KEY, "")
            if (raw.isBlank()) return
            buffer = json.decodeFromString<List<PlayEvent>>(raw).takeLast(MAX_BUFFER)
        } catch (_: Exception) {
        }
    }

    private fun persistLocked() {
        try {
            settings.putString(STORE_KEY, json.encodeToString(buffer))
        } catch (_: Exception) {
        }
    }

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
                ensureRestored()
                buffer = (buffer + event).takeLast(MAX_BUFFER)
                persistLocked()
            } finally {
                lock.unlock()
            }
        } catch (_: Exception) {
        }
    }

    fun drain(): List<PlayEvent> = try {
        if (!lock.tryLock()) return emptyList()
        try {
            ensureRestored()
            val out = buffer
            buffer = emptyList()
            persistLocked()
            out
        } finally {
            lock.unlock()
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** Buffered event count (restores first). For tests/diagnostics. Never throws. */
    fun pendingCount(): Int = try {
        if (!lock.tryLock()) return -1
        try {
            ensureRestored()
            buffer.size
        } finally {
            lock.unlock()
        }
    } catch (_: Exception) {
        -1
    }
}
