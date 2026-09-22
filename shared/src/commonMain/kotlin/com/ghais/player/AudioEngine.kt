package com.ghais.player

import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.isFullSurah
import com.ghais.domain.model.resolvedDurationMs
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

    private val _isAyahMode = MutableStateFlow(false)
    val isAyahMode: StateFlow<Boolean> = _isAyahMode.asStateFlow()

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

    private val _queue = MutableStateFlow<List<TrackItem>>(emptyList())
    val queue: StateFlow<List<TrackItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    init {
        PlayerBridge.setOnTrackEndListener { onBridgeTrackEnd() }
        scope.launch {
            PlayerBridge.positionMs.collect { pos ->
                val pending = pendingSeekMs
                if (pending > 0L && PlayerBridge.durationMs.value > 0L) {
                    if (pos >= pending - 3_000L) {
                        // Landed (or overtook) the resume point — done.
                        pendingSeekMs = 0L
                        pendingSeekTries = 0
                    } else if (pendingSeekTries > 0) {
                        pendingSeekTries--
                        if (pendingSeekTries == 0) pendingSeekMs = 0L
                        seekToInternal(pending)
                    } else {
                        pendingSeekMs = 0L
                    }
                }
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
                    // Auto-skip a broken ayah (404/timeout) so the surah plays fully.
                    val failed = _currentTrack.value
                    if (failed != null && consecutiveErrors < maxAutoSkipErrors) {
                        consecutiveErrors++
                        val nextTrack = queueManager.playNext(forceAdvance = true)
                        if (nextTrack != null) {
                            startPlayback(nextTrack)
                        } else {
                            SleepTimer.onQueueEnded()
                            stopPlayback()
                        }
                    } else if (failed != null) {
                        stopPlayback()
                    }
                } else if (error == null && _playbackState.value.status == PlaybackStatus.ERROR) {
                    consecutiveErrors = 0
                    _playbackState.update { state ->
                        state.copy(status = if (_isPlaying.value) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED)
                    }
                } else if (error == null) {
                    consecutiveErrors = 0
                }
            }
        }
        SleepTimer.onStopPlayer = { pause() }
    }

    private var progressJob: kotlinx.coroutines.Job? = null

    /** One-shot resume offset applied once the bridge reports a known duration. */
    private var pendingSeekMs: Long = 0L
    /** Retries left for landing [pendingSeekMs] (early seeks can be dropped while buffering). */
    private var pendingSeekTries: Int = 0

    // Single-flight / de-dupe for STATE_ENDED re-emission + manual/auto race.
    private var lastEndUrl: String? = null
    private var lastEndAtMs: Long = 0L
    // Bounded auto-skip so one 404 ayah doesn't halt the surah,
    // but a fully-broken reciter folder doesn't loop forever.
    private var consecutiveErrors: Int = 0
    private val maxAutoSkipErrors: Int = 10

    fun playTrack(track: TrackItem, startPositionMs: Long = 0L) {
        _isAyahMode.value = track.ayahNo > 0 && !track.isFullSurah
        queueManager.clear()
        queueManager.addToQueue(track)
        _queue.value = queueManager.queue
        pendingSeekMs = startPositionMs.coerceAtLeast(0L)
        pendingSeekTries = if (pendingSeekMs > 0L) 40 else 0
        startPlayback(track)
    }

    fun playQueue(tracks: List<TrackItem>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (tracks.isEmpty()) {
            clear()
            return
        }
        val firstTrack = tracks.getOrNull(startIndex) ?: tracks.firstOrNull()
        if (firstTrack != null) {
            _isAyahMode.value = firstTrack.ayahNo > 0 && !firstTrack.isFullSurah
        }
        queueManager.setQueue(tracks, startIndex)
        _queue.value = queueManager.queue
        val track = queueManager.currentTrack
        if (track != null) {
            pendingSeekMs = startPositionMs.coerceAtLeast(0L)
            pendingSeekTries = if (pendingSeekMs > 0L) 40 else 0
            startPlayback(track)
        } else {
            stopPlayback()
        }
    }

    fun setAyahMode(enabled: Boolean) {
        if (enabled) {
            val current = _currentTrack.value
            if (current?.let { it.ayahNo <= 0 || it.isFullSurah } == true) {
                val surahId = current.surahId
                val reciterSlug = current.reciterSlug
                val reciter = QuranDataRepository.getReciterBySlug(reciterSlug)
                val (ayahTracks, totalAyahs) = buildAyahTracksForSurah(
                    surahId = surahId,
                    reciter = reciter,
                    fallbackSurahNameEn = current.surahNameEn,
                    fallbackSurahNameAr = current.surahNameAr,
                    fallbackReciterName = current.reciterName
                )
                val progressFraction = progress.value
                val targetAyah = ((progressFraction * totalAyahs).toInt() + 1).coerceIn(1, totalAyahs)
                val startIndex = targetAyah - 1
                playQueue(ayahTracks, startIndex)
                _isAyahMode.value = true
            } else if ((_currentTrack.value?.ayahNo ?: 0) > 0) {
                _isAyahMode.value = true
            } else {
                _isAyahMode.value = true
            }
        } else {
            _isAyahMode.value = false
        }
    }

    fun toggleAyahMode() {
        setAyahMode(!_isAyahMode.value)
    }

    fun playSurahInAyahMode(surahId: Int, reciterSlug: String? = null, startAyahNo: Int = 1) {
        val current = _currentTrack.value
        val targetSlug = reciterSlug ?: current?.reciterSlug
        val reciter = QuranDataRepository.getReciterBySlug(targetSlug)
        val (ayahTracks, totalAyahs) = buildAyahTracksForSurah(
            surahId = surahId,
            reciter = reciter,
            fallbackReciterName = current?.reciterName ?: ""
        )
        val startIndex = (startAyahNo - 1).coerceIn(0, (totalAyahs - 1).coerceAtLeast(0))
        playQueue(ayahTracks, startIndex)
        _isAyahMode.value = true
    }

    private fun buildAyahTracksForSurah(
        surahId: Int,
        reciter: Reciter,
        fallbackSurahNameEn: String = "",
        fallbackSurahNameAr: String = "",
        fallbackReciterName: String = ""
    ): Pair<List<TrackItem>, Int> {
        val surah = QuranDataRepository.getSurahById(surahId)
        val totalAyahs = surah?.ayahsCount ?: QuranData.SURAHS.firstOrNull { it.id == surahId }?.ayahsCount ?: 7
        val surahNameEn = surah?.nameEn ?: fallbackSurahNameEn
        val surahNameAr = surah?.nameAr ?: fallbackSurahNameAr
        val reciterName = if (reciter.nameEn.isNotBlank()) reciter.nameEn else fallbackReciterName

        val ayahTracks = (1..totalAyahs).map { ayahNo ->
            val verse = QuranAyahRepository.getAyahImmediate(surahId, ayahNo)
            TrackItem(
                reciterSlug = reciter.slug,
                reciterName = reciterName,
                surahId = surahId,
                surahNameEn = surahNameEn,
                surahNameAr = surahNameAr,
                ayahNo = ayahNo,
                audioUrl = reciter.getAyahAudioUrl(surahId, ayahNo),
                textUthmani = verse.textUthmani
            )
        }
        return Pair(ayahTracks, totalAyahs)
    }

    fun addToQueue(track: TrackItem) {
        queueManager.addToQueue(track)
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        if (_currentTrack.value == null && _playbackState.value.status == PlaybackStatus.IDLE) {
            queueManager.currentTrack?.let { startPlayback(it) }
        }
    }

    fun addToQueue(tracks: List<TrackItem>) {
        if (tracks.isEmpty()) return
        val wasEmpty = queueManager.queue.isEmpty()
        queueManager.addAllToQueue(tracks)
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        if (wasEmpty && _currentTrack.value == null && _playbackState.value.status == PlaybackStatus.IDLE) {
            queueManager.currentTrack?.let { startPlayback(it) }
        }
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
        // After a transient error ExoPlayer sits in IDLE — bare play() is a no-op.
        if (_playbackState.value.status == PlaybackStatus.ERROR) {
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
        pendingSeekMs = 0L
        pendingSeekTries = 0
        seekToInternal(positionMs)
    }

    private fun seekToInternal(positionMs: Long) {
        val duration = effectiveDuration()
        val clamped = positionMs.coerceIn(0L, if (duration > 0L) duration else Long.MAX_VALUE)
        PlayerBridge.seekTo(clamped)
        _currentPositionMs.value = clamped
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
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        _playbackState.update { state ->
            state.copy(settings = state.settings.copy(shuffle = queueManager.isShuffle))
        }
    }

    fun next() {
        val nextTrack = queueManager.playNext(forceAdvance = true)
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
        val prevTrack = queueManager.playPrevious(forceAdvance = true)
        if (prevTrack != null) {
            startPlayback(prevTrack)
        } else {
            seekTo(0L)
        }
    }

    fun previousAyah() = previous()

    fun skipPrevious() = previous()

    fun canSkipNext(): Boolean {
        if (_currentTrack.value == null || _queue.value.isEmpty()) return false
        return queueManager.canSkipNext()
    }

    fun canSkipPrevious(): Boolean {
        if (_currentTrack.value == null) return false
        if (_currentPositionMs.value > 3_000L) return true
        if (_queue.value.isEmpty()) return false
        return queueManager.canSkipPrevious()
    }

    fun skipToIndex(index: Int) {
        val track = queueManager.skipToIndex(index)
        if (track != null) {
            startPlayback(track)
        }
    }

    fun removeFromQueue(index: Int) {
        if (index !in queueManager.queue.indices) return
        val wasCurrent = (index == queueManager.currentIndex)
        val nextTrack = queueManager.removeAt(index)
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex

        if (queueManager.queue.isEmpty()) {
            clear()
        } else if (wasCurrent) {
            if (nextTrack != null) {
                startPlayback(nextTrack)
            } else {
                stopPlayback()
            }
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        if (queueManager.move(fromIndex, toIndex)) {
            _queue.value = queueManager.queue
            _currentIndex.value = queueManager.currentIndex
        }
    }

    fun clear() {
        stopPlayback()
        queueManager.clear()
        _queue.value = emptyList()
        _currentIndex.value = -1
        _isAyahMode.value = false
    }

    fun stop() {
        stopPlayback()
    }

    private fun startPlayback(track: TrackItem) {
        progressJob?.cancel()
        _currentTrack.value = track
        _currentIndex.value = queueManager.currentIndex
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
                    isPlaying = true,
                    queueIndex = queueManager.currentIndex
                ),
                settings = state.settings.copy(
                    repeatMode = queueManager.repeatMode,
                    shuffle = queueManager.isShuffle
                )
            )
        }
        PlayerBridge.setSpeed(_playbackSpeed.value)
        PlayerBridge.setVolume(_volume.value)
        PlayerBridge.setPlaybackMetadata(trackDisplayTitle(track), track.reciterName)
        // Prefer offline file when downloaded for full surahs; a download completing mid-play
        // does not interrupt the current stream — offline applies from next startPlayback.
        val playbackUri = if (track.isFullSurah || track.ayahNo <= 0) {
            QuranDownloads.localUri(track.reciterSlug, track.surahId) ?: track.audioUrl
        } else {
            track.audioUrl
        }
        // Atomic resume: start position rides along with prepare, so early
        // seeks can't be dropped while buffering. The retry net below stays
        // as a backstop for mid-play item replacements.
        val resumeAt = pendingSeekMs.also { pendingSeekMs = 0L; pendingSeekTries = 0 }
        PlayerBridge.play(playbackUri, resumeAt)
    }

    private fun stopPlayback() {
        PlayerBridge.stop()
        progressJob?.cancel()
        pendingSeekMs = 0L
        pendingSeekTries = 0
        _currentTrack.value = null
        _currentIndex.value = -1
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _progress.value = 0f
        _durationMs.value = 0L
        _playbackState.update { state ->
            state.copy(
                status = PlaybackStatus.IDLE,
                currentTrackInfo = state.currentTrackInfo.copy(
                    track = null,
                    progressMs = 0L,
                    durationMs = 0L,
                    isPlaying = false,
                    queueIndex = -1
                )
            )
        }
    }

    private fun onBridgeTrackEnd() {
        val endedUrl = _currentTrack.value?.audioUrl
        val now = currentTimeMs()
        if (endedUrl != null && endedUrl == lastEndUrl && (now - lastEndAtMs) < 1500L) return
        lastEndUrl = endedUrl
        lastEndAtMs = now
        consecutiveErrors = 0
        val current = _currentTrack.value
        val isFullSurah = current?.isFullSurah == true || (current != null && current.ayahNo <= 0)
        SleepTimer.onAyahEnded()
        if (isFullSurah) {
            SleepTimer.onSurahEnded()
        }
        if (!_isPlaying.value) return

        val nextTrack = queueManager.playNext(forceAdvance = false)
        if (nextTrack != null) {
            if (!isFullSurah && current != null && nextTrack.surahId != current.surahId) {
                SleepTimer.onSurahEnded()
            }
            if (!_isPlaying.value) return
            startPlayback(nextTrack)
        } else {
            SleepTimer.onSurahEnded()
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

    fun trackDisplayTitle(track: TrackItem): String {
        val name = track.surahNameEn.trim()
        if (name.isBlank()) return "Ghais"
        return if (track.ayahNo <= 0 || track.isFullSurah) {
            name
        } else {
            "$name - Ayah ${track.ayahNo}"
        }
    }

    private fun currentTimeMs(): Long =
        try { platformTimeMs() } catch (_: Exception) { lastEndAtMs + 2000L }

    private fun platformTimeMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}
