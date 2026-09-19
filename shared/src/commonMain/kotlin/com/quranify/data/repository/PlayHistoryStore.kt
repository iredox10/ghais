package com.quranify.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HistoryEntry(
    val reciterSlug: String,
    val reciterName: String,
    val surahId: Int,
    val surahNameEn: String,
    val timestampMs: Long
)

/**
 * PlayHistoryStore: persisted store of recently played reciter/surah pairs.
 *
 * Mirrors [FavoritesStore]/[FollowStore] persistence approach (multiplatform
 * Settings + JSON-encoded list).
 */
object PlayHistoryStore {
    const val HISTORY_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
    private const val MAX_ENTRIES = 200
    private const val KEY_PLAY_HISTORY = "quranify_play_history"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

    /** All entries from the last 30 days, newest first (pruned on load + on record). */
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    init {
        load()
    }

    fun record(reciterSlug: String, reciterName: String, surahId: Int, surahNameEn: String) {
        if (reciterSlug.isBlank() || reciterName.isBlank()) return
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val cutoff = now - HISTORY_WINDOW_MS
        val entry = HistoryEntry(
            reciterSlug = reciterSlug,
            reciterName = reciterName,
            surahId = surahId,
            surahNameEn = surahNameEn,
            timestampMs = now
        )
        _entries.value = (listOf(entry) + _entries.value)
            .filter { it.timestampMs >= cutoff }
            .take(MAX_ENTRIES)
        save()
    }

    fun clear() {
        _entries.value = emptyList()
        save()
    }

    private fun load() {
        try {
            val raw = settings.getString(KEY_PLAY_HISTORY, "")
            if (raw.isBlank()) {
                _entries.value = emptyList()
                return
            }
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val cutoff = now - HISTORY_WINDOW_MS
            val decoded = json.decodeFromString<List<HistoryEntry>>(raw)
            val loaded = decoded
                .filter { it.timestampMs >= cutoff }
                .sortedByDescending { it.timestampMs }
                .take(MAX_ENTRIES)
            _entries.value = loaded
            if (loaded.size != decoded.size) {
                save()
            }
        } catch (_: Exception) {
            _entries.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(KEY_PLAY_HISTORY, json.encodeToString(_entries.value))
        } catch (_: Exception) {
        }
    }
}
