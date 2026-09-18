package com.quranify.player

import com.quranify.domain.model.RepeatMode
import com.quranify.domain.model.TrackItem
import com.quranify.domain.model.resolvedDurationMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Real audio engine — delegates actual streaming to [PlayerBridge]
 * (Media3 ExoPlayer on Android, AVPlayer stub/TODO on iOS).
 * Duration 0 == unknown until the stream reports it (see Models.kt).
 */

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

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    val errorMessage: StateFlow<String?> = PlayerBridge.errorMessage

    val queue: List<TrackItem> get() = queueManager.queue

    init {
        PlayerBridge.setOnTrackEndListener { onBridgeTrackEnd() }
        scope.launch {
            PlayerBridge.positionMs.collect { pos ->
                _currentPositionMs.value = pos
                val duration = effectiveDuration()
                _progress.value = if (duration > 0L) {
                    (pos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else 0f
                _playbackState.update { state ->
                    state.copy(currentTrackInfo = state.currentTrackInfo.copy(progressMs = pos))
                }
            }
        }
        scope.launch {
            PlayerBridge.durationMs.collect { bridgeDuration ->
                val track = _currentTrack.value ?: return@collect
                val merged = mergeDuration(track.durationMs, bridgeDuration)
                if (merged != _durationMs.value) {
                    _durationMs.value = merged
                    _playbackState.update { state ->
                        state.copy(currentTrackInfo = state.currentTrackInfo.copy(durationMs = merged))
                    }
                }
            }
        }
        scope.launch {
            PlayerBridge.isBuffering.collect { buffering ->
                val state = _playbackState.value
                if (state.currentTrackInfo.track == null) return@collect
                if (state.status == PlaybackStatus.ERROR) return@collect
                val target = when {
                    buffering -> PlaybackStatus.BUFFERING
                    _isPlaying.value -> PlaybackStatus.PLAYING
                    else -> PlaybackStatus.PAUSED
                }
                if (target != state.status) _playbackState.update { it.copy(status = target) }
            }
        }
        scope.launch {
            PlayerBridge.errorMessage.collect { error ->
                if (error != null && _currentTrack.value != null) {
                    _isPlaying.value = false
                    _playbackState.update { state ->
                        state.copy(
                            status = PlaybackStatus.ERROR,
                            currentTrackInfo = state.currentTrackInfo.copy(isPlaying = false)
                        )
                    }
                } else if (error == null && _playbackState.value.status == PlaybackStatus.ERROR) {
                    _playbackState.update { state ->
                        state.copy(status = if (_isPlaying.value) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED)
                    }
                }
            }
        }
        SleepTimer.onStopPlayer = { pause() }
    }

    private var progressJob: kotlinx.coroutines.Job? = null

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
        val status = _playbackState.value.status
        if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.BUFFERING) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        if (!_isPlaying.value && _playbackState.value.status != PlaybackStatus.BUFFERING) return
        PlayerBridge.pause()
        progressJob?.cancel()
        _isPlaying.value = false
        _playbackState.update { state ->
            state.copy(
                status = if (state.status == PlaybackStatus.ERROR) PlaybackStatus.ERROR else PlaybackStatus.PAUSED,
                currentTrackInfo = state.currentTrackInfo.copy(isPlaying = false)
            )
        }
    }

    fun resume() {
        val track = queueManager.currentTrack ?: _currentTrack.value ?: return
        if (_currentTrack.value?.audioUrl != track.audioUrl || _playbackState.value.status == PlaybackStatus.IDLE) {
            startPlayback(track)
            return
        }
        if (_playbackState.value.status == PlaybackStatus.PLAYING) return
        PlayerBridge.resume()
        _isPlaying.value = true
        _playbackState.update { state ->
            state.copy(
                status = if (PlayerBridge.isBuffering.value) PlaybackStatus.BUFFERING else PlaybackStatus.PLAYING,
                currentTrackInfo = state.currentTrackInfo.copy(isPlaying = true)
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        PlayerBridge.seekTo(clamped)
        _currentPositionMs.value = clamped
        val duration = effectiveDuration()
        _progress.value = if (duration > 0L) (clamped.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
        _playbackState.update { state ->
            state.copy(currentTrackInfo = state.currentTrackInfo.copy(progressMs = clamped))
        }
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _playbackSpeed.value = clamped
        PlayerBridge.setSpeed(clamped)
        _playbackState.update { state ->
            state.copy(settings = state.settings.copy(speed = clamped))
        }
    }

    fun setPlaybackSpeed(speed: Float) = setSpeed(speed)

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        PlayerBridge.setVolume(clamped)
    }

    fun setRepeatMode(mode: RepeatMode) {
        queueManager.setRepeatMode(mode)
        _playbackState.update { state ->
            state.copy(settings = state.settings.copy(repeatMode = mode))
        }
    }

    fun toggleShuffle() {
        queueManager.toggleShuffle()
        _playbackState.update { state ->
            state.copy(settings = state.settings.copy(shuffle = queueManager.isShuffle))
        }
    }

    fun next() {
        val nextTrack = queueManager.playNext()
        if (nextTrack != null) {
            startPlayback(nextTrack)
        } else {
            SleepTimer.onQueueEnded()
            stopPlayback()
        }
    }

    fun nextAyah() = next()

    fun skipNext() = next()

    fun previous() {
        if (_currentPositionMs.value > 3_000L && _currentTrack.value != null) {
            seekTo(0L)
            return
        }
        val prevTrack = queueManager.playPrevious()
        if (prevTrack != null) {
            startPlayback(prevTrack)
        } else {
            stopPlayback()
        }
    }

    fun previousAyah() = previous()

    fun skipPrevious() = previous()

    fun clear() {
        stopPlayback()
        queueManager.clear()
    }

    fun stop() {
        stopPlayback()
    }

    private fun startPlayback(track: TrackItem) {
        progressJob?.cancel()
        _currentTrack.value = track
        _isPlaying.value = true
        _currentPositionMs.value = 0L
        _progress.value = 0f
        val initialDuration = track.resolvedDurationMs()
        _durationMs.value = initialDuration
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.BUFFERING,
                currentTrackInfo = CurrentTrackInfo(
                    track = track,
                    progressMs = 0L,
                    durationMs = initialDuration,
                    isPlaying = true
                ),
                settings = state.settings.copy(
                    repeatMode = queueManager.repeatMode,
                    shuffle = queueManager.isShuffle
                )
            )
        }
        PlayerBridge.setSpeed(_playbackSpeed.value)
        PlayerBridge.setVolume(_volume.value)
        PlayerBridge.play(track.audioUrl)
    }

    private fun stopPlayback() {
        PlayerBridge.stop()
        progressJob?.cancel()
        _currentTrack.value = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _progress.value = 0f
        _durationMs.value = 0L
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.IDLE,
                currentTrackInfo = state.currentTrackInfo.copy(track = null, progressMs = 0L, durationMs = 0L, isPlaying = false)
            )
        }
    }

    private fun onBridgeTrackEnd() {
        SleepTimer.onAyahEnded()
        val current = _currentTrack.value
        val nextTrack = queueManager.playNext()
        if (nextTrack != null) {
            if (current != null && nextTrack.surahId != current.surahId) SleepTimer.onSurahEnded()
            startPlayback(nextTrack)
        } else {
            SleepTimer.onQueueEnded()
            stopPlayback()
        }
    }

    private fun effectiveDuration(): Long {
        val track = _currentTrack.value ?: return _durationMs.value
        val merged = mergeDuration(track.durationMs, PlayerBridge.durationMs.value)
        return merged.takeIf { it > 0L } ?: _durationMs.value
    }

    private fun mergeDuration(knownMs: Long, bridgeMs: Long): Long = when {
        bridgeMs > 0L -> bridgeMs
        knownMs > 0L -> knownMs
        else -> 0L
    }
}
