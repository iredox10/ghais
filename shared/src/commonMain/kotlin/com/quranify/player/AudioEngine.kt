package com.quranify.player

import com.quranify.domain.model.TrackItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AudioEngine {
    private val queueManager: QueueManager = QueueManager()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackItem?>(null)
    val currentTrack: StateFlow<TrackItem?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var progressJob: Job? = null

    fun playTrack(track: TrackItem) {
        queueManager.clear()
        queueManager.addToQueue(track)
        startPlayback(track)
    }

    fun playQueue(tracks: List<TrackItem>, startIndex: Int = 0) {
        queueManager.setQueue(tracks, startIndex)
        queueManager.currentTrack?.let { startPlayback(it) }
    }

    fun togglePlayPause() {
        val currentState = _playbackState.value
        if (currentState.status == PlaybackStatus.PLAYING) {
            pause()
        } else if (currentState.status == PlaybackStatus.PAUSED || currentState.status == PlaybackStatus.IDLE) {
            resume()
        }
    }

    fun pause() {
        if (_playbackState.value.status != PlaybackStatus.PLAYING) return
        progressJob?.cancel()
        _isPlaying.value = false
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.PAUSED,
                currentTrackInfo = state.currentTrackInfo.copy(isPlaying = false)
            )
        }
    }

    fun resume() {
        if (_playbackState.value.status == PlaybackStatus.PLAYING) return

        val track = queueManager.currentTrack ?: return
        _isPlaying.value = true
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.PLAYING,
                currentTrackInfo = state.currentTrackInfo.copy(isPlaying = true)
            )
        }
        startProgressSimulation(track)
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        val duration = _playbackState.value.currentTrackInfo.durationMs.takeIf { it > 0 } ?: 60000L
        _progress.value = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        _playbackState.update { state ->
            state.copy(currentTrackInfo = state.currentTrackInfo.copy(progressMs = positionMs))
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        _playbackState.update { state ->
            state.copy(settings = state.settings.copy(speed = speed))
        }
    }

    fun next() {
        val nextTrack = queueManager.playNext()
        if (nextTrack != null) {
            startPlayback(nextTrack)
        } else {
            stopPlayback()
        }
    }

    fun nextAyah() = next()

    fun previous() {
        val prevTrack = queueManager.playPrevious()
        if (prevTrack != null) {
            startPlayback(prevTrack)
        } else {
            stopPlayback()
        }
    }

    fun previousAyah() = previous()

    fun clear() {
        stopPlayback()
        queueManager.clear()
    }

    private fun startPlayback(track: TrackItem) {
        progressJob?.cancel()
        _currentTrack.value = track
        _isPlaying.value = true
        _currentPositionMs.value = 0L
        _progress.value = 0f
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.PLAYING,
                currentTrackInfo = CurrentTrackInfo(
                    track = track,
                    progressMs = 0L,
                    durationMs = track.durationMs.takeIf { it > 0 } ?: 60000L,
                    isPlaying = true
                )
            )
        }
        startProgressSimulation(track)
    }

    private fun stopPlayback() {
        progressJob?.cancel()
        _currentTrack.value = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _progress.value = 0f
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.IDLE,
                currentTrackInfo = state.currentTrackInfo.copy(track = null, progressMs = 0L, isPlaying = false)
            )
        }
    }

    private fun startProgressSimulation(track: TrackItem) {
        progressJob?.cancel()
        progressJob = scope.launch {
            val duration = track.durationMs.takeIf { it > 0 } ?: 60000L
            while (_currentPositionMs.value < duration) {
                delay((1000L / _playbackSpeed.value).toLong())
                val newPosition = _currentPositionMs.value + 1000L
                _currentPositionMs.value = newPosition
                _progress.value = (newPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                _playbackState.update { state ->
                    state.copy(currentTrackInfo = state.currentTrackInfo.copy(progressMs = newPosition))
                }
            }
            next()
        }
    }
}
