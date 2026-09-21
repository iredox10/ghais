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
    private const val KEY_FAVORITES = "quranify_favorite_tracks"

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
            val raw = settings.getString(key(KEY_FAVORITES), "")
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
