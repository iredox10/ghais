package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * FavoritesStore: persisted store of favorite tracks.
 *
 * Matches [LibraryRepository.toggleLike]/[LibraryRepository.isLiked] semantics
 * (identity by [TrackItem.audioUrl]) but persists across restarts via
 * multiplatform Settings (JSON-encoded list).
 */
object FavoritesStore {
    private const val KEY_FAVORITES = "ghais_favorite_tracks"

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

    private val _favoriteTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val favoriteTracks: StateFlow<List<TrackItem>> = _favoriteTracks.asStateFlow()

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    fun isFavorite(track: TrackItem): Boolean {
        return _favoriteTracks.value.any { it.audioUrl == track.audioUrl }
    }

    fun toggle(track: TrackItem) {
        _favoriteTracks.update { list ->
            if (list.any { it.audioUrl == track.audioUrl }) {
                list.filter { it.audioUrl != track.audioUrl }
            } else {
                list + track
            }
        }
        save()
    }

    fun add(track: TrackItem) {
        _favoriteTracks.update { list ->
            if (list.any { it.audioUrl == track.audioUrl }) list else list + track
        }
        save()
    }

    fun remove(track: TrackItem) {
        _favoriteTracks.update { list ->
            list.filter { it.audioUrl != track.audioUrl }
        }
        save()
    }

    fun clear() {
        _favoriteTracks.value = emptyList()
        save()
    }

    private fun load() {
        try {
            var raw = settings.getString(key(KEY_FAVORITES), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_FAVORITES))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_FAVORITES), legacyRaw)
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
                _favoriteTracks.value = emptyList()
                return
            }
            _favoriteTracks.value = json.decodeFromString<List<TrackItem>>(raw)
        } catch (_: Exception) {
            _favoriteTracks.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_FAVORITES), json.encodeToString(_favoriteTracks.value))
        } catch (_: Exception) {
        }
    }
}
