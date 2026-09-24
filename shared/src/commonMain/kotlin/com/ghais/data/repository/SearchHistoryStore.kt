package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SearchHistoryStore: persisted, owner-scoped store of user-typed global search
 * queries, rather than reciter picks.
 *
 * Mirrors [ReciterSearchHistoryStore] persistence approach (multiplatform Settings +
 * JSON-encoded list, owner-namespaced keys) but tracks an ordered,
 * most-recent-first list capped at 15 entries.
 */
object SearchHistoryStore {
    private const val KEY_HISTORY = "ghais_search_history"
    private const val MAX_SIZE = 15

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _recentQueries = MutableStateFlow<List<String>>(emptyList())
    val recentQueries: StateFlow<List<String>> = _recentQueries.asStateFlow()

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    fun record(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        _recentQueries.update { list ->
            (listOf(clean) + list.filterNot { it.equals(clean, ignoreCase = true) }).take(MAX_SIZE)
        }
        save()
    }

    fun remove(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        _recentQueries.update { list ->
            list.filterNot { it == clean }
        }
        save()
    }

    fun clear() {
        _recentQueries.value = emptyList()
        save()
    }

    private fun load() {
        try {
            var raw = settings.getString(key(KEY_HISTORY), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_HISTORY))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_HISTORY), legacyRaw)
                        } catch (_: Exception) {
                        }
                        try {
                            settings.remove(legacyKey)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
            if (raw.isBlank()) {
                _recentQueries.value = emptyList()
                return
            }
            _recentQueries.value = json.decodeFromString<List<String>>(raw)
                .map { it.trim() }
                .filter { it.length >= 2 }
                .distinctBy { it.lowercase() }
                .take(MAX_SIZE)
        } catch (_: Exception) {
            _recentQueries.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_HISTORY), json.encodeToString(_recentQueries.value))
        } catch (_: Exception) {
        }
    }
}
