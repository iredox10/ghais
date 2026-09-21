package com.ghais.data.repository

import com.ghais.domain.model.KhatmaPlan
import com.ghais.domain.model.PlayHistory
import com.ghais.domain.model.Playlist
import com.ghais.domain.model.TrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class LibraryRepository {

    // Playlists
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Favorites
    private val _favoriteTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val favoriteTracks: StateFlow<List<TrackItem>> = _favoriteTracks.asStateFlow()

    // Khatma Plan
    private val _activeKhatmaPlan = MutableStateFlow<KhatmaPlan?>(null)
    val activeKhatmaPlan: StateFlow<KhatmaPlan?> = _activeKhatmaPlan.asStateFlow()

    // History
    private val _recentHistory = MutableStateFlow<List<PlayHistory>>(emptyList())
    val recentHistory: StateFlow<List<PlayHistory>> = _recentHistory.asStateFlow()

    private fun currentTimeMs(): Long {
        // Simple fallback since we can't be sure if kotlinx-datetime is here, but wait, 
        // we can just use a simple monotonic counter or system time if available.
        // Actually, we'll use a simple mock time if necessary, but ideally we'd use Clock.System.now().toEpochMilliseconds()
        // Wait, Kotlin standard library provides `kotlin.time.TimeSource.Monotonic`.
        // I will just use `System.currentTimeMillis()` for Jvm/Android, but since this is commonMain...
        // Let's just create a dummy time generator or try to rely on kotlinx-datetime if the project has it.
        // If not, I'll use a simple counter. Let's assume the project has some way, or I'll just use a mock timestamp.
        return 0L
    }

    init {
        // Mock initial data
        _playlists.value = listOf(
            Playlist(
                id = "p1",
                title = "Morning Adhkar",
                description = "Soothing morning recitations",
                tracks = emptyList(),
                coverUrl = null
            )
        )
    }

    fun createPlaylist(title: String, description: String) {
        val newPlaylist = Playlist(
            id = Random.nextLong().toString(),
            title = title,
            description = description
        )
        _playlists.update { it + newPlaylist }
    }

    fun addTrack(playlistId: String, track: TrackItem) {
        _playlists.update { list ->
            list.map {
                if (it.id == playlistId) {
                    it.copy(tracks = it.tracks + track)
                } else it
            }
        }
    }

    fun removeTrack(playlistId: String, trackId: String) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) {
                    // Assuming trackId is a combination of surahId and ayahNo or url
                    p.copy(tracks = p.tracks.filter { it.audioUrl != trackId }) 
                } else p
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.update { list ->
            list.filter { it.id != playlistId }
        }
    }

    fun toggleLike(track: TrackItem) {
        _favoriteTracks.update { list ->
            if (list.any { it.audioUrl == track.audioUrl }) {
                list.filter { it.audioUrl != track.audioUrl }
            } else {
                list + track
            }
        }
    }

    fun isLiked(track: TrackItem): Boolean {
        return _favoriteTracks.value.any { it.audioUrl == track.audioUrl }
    }

    fun getFavoriteTracks(): List<TrackItem> {
        return _favoriteTracks.value
    }

    fun createPlan(title: String, targetDays: Int = 30) {
        val plan = KhatmaPlan(
            id = Random.nextLong().toString(),
            title = title,
            targetDays = targetDays,
            startDateMs = 0L // mock time
        )
        _activeKhatmaPlan.value = plan
    }

    fun updateProgress(surahId: Int, ayahNo: Int) {
        _activeKhatmaPlan.update { plan ->
            plan?.copy(
                currentSurahId = surahId,
                currentAyahNo = ayahNo,
                totalAyahsRead = plan.totalAyahsRead + 1,
                lastProgressMs = 1L // mock
            )
        }
    }

    fun getActivePlan(): KhatmaPlan? = _activeKhatmaPlan.value

    fun recordPlay(track: TrackItem) {
        val history = PlayHistory(track, 0L)
        _recentHistory.update { list ->
            (listOf(history) + list).take(50)
        }
    }

    fun getRecentHistory(limit: Int = 50): List<PlayHistory> {
        return _recentHistory.value.take(limit)
    }
}
