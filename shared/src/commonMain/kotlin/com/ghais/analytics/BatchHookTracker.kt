package com.ghais.analytics

import com.russhwolf.settings.Settings
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Persistent offline-safe queue for [HookAnalytics] events.
 *
 * Mirrors [com.ghais.data.repository.PlayEventLog]: non-blocking `tryLock()`,
 * capped at [MAX_BUFFER] (drop-oldest), persisted to [Settings] so events
 * recorded offline survive process restarts and flush in [SyncEngine].
 */
object BatchHookTracker : HookTracker {
    const val MAX_BUFFER = 200
    private const val STORE_KEY = "ghais_app_events_queue_v1"

    @Serializable
    data class QueuedEvent(
        val event: String,
        val props: Map<String, String> = emptyMap(),
        val tsMs: Long = Clock.System.now().toEpochMilliseconds(),
        val eventId: String = newEventId(),
    )

    private fun newEventId(): String =
        Random.nextBytes(10).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val settings: Settings by lazy { Settings() }
    private val lock = Mutex()
    private var buffer: List<QueuedEvent> = emptyList()
    private var restored = false

    private fun ensureRestored() {
        if (restored) return
        restored = true
        try {
            val raw = settings.getString(STORE_KEY, "")
            if (raw.isBlank()) return
            buffer = json.decodeFromString<List<QueuedEvent>>(raw).takeLast(MAX_BUFFER)
        } catch (_: Exception) {
        }
    }

    private fun persistLocked() {
        try {
            settings.putString(STORE_KEY, json.encodeToString(buffer))
        } catch (_: Exception) {
        }
    }

    override fun track(event: String, props: Map<String, String>) {
        try {
            val cleanEvent = event.trim().take(80)
            if (cleanEvent.isEmpty()) return
            val cleanProps = props.entries
                .take(12)
                .associate { (k, v) -> k.trim().take(40) to v.trim().take(120) }
            if (!lock.tryLock()) return
            try {
                ensureRestored()
                val next = buffer + QueuedEvent(event = cleanEvent, props = cleanProps)
                buffer = if (next.size > MAX_BUFFER) next.takeLast(MAX_BUFFER) else next
                persistLocked()
            } finally {
                lock.unlock()
            }
        } catch (_: Exception) {
        }
    }

    fun pending(limit: Int = 50): List<QueuedEvent> {
        if (!lock.tryLock()) return emptyList()
        return try {
            ensureRestored()
            buffer.take(limit)
        } finally {
            lock.unlock()
        }
    }

    fun acknowledge(eventId: String) {
        if (!lock.tryLock()) return
        try {
            ensureRestored()
            val next = buffer.filterNot { it.eventId == eventId }
            if (next.size != buffer.size) {
                buffer = next
                persistLocked()
            }
        } finally {
            lock.unlock()
        }
    }
}
