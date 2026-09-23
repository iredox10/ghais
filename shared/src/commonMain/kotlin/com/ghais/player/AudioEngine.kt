package com.ghais.player

import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.HifzRange
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.UNKNOWN_DURATION_MS
import com.ghais.domain.model.isFullSurah
import com.ghais.domain.model.resolvedDurationMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val _engineError = MutableStateFlow<String?>(null)
    /**
     * Engine-surfaced error: friendly message when a full-surah queue exhausts
     * from source errors, bridge message otherwise. Surfacing this (and keeping
     * the current track) instead of stopPlayback() preserves the session and
     * its notification.
     */
    val errorMessage: StateFlow<String?> =
        combine(PlayerBridge.errorMessage, _engineError) { bridge, engine ->
            engine ?: bridge
        }.stateIn(scope, SharingStarted.Eagerly, PlayerBridge.errorMessage.value)

    private val _queue = MutableStateFlow<List<TrackItem>>(emptyList())
    val queue: StateFlow<List<TrackItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // --- Hifz & Memorization StateFlows ---
    private val _ayahRepetitionTarget = MutableStateFlow(1)
    val ayahRepetitionTarget: StateFlow<Int> = _ayahRepetitionTarget.asStateFlow()

    private val _currentAyahRepetition = MutableStateFlow(1)
    val currentAyahRepetition: StateFlow<Int> = _currentAyahRepetition.asStateFlow()

    private val _recitationGapSeconds = MutableStateFlow(0)
    val recitationGapSeconds: StateFlow<Int> = _recitationGapSeconds.asStateFlow()

    private val _isRecitationGapActive = MutableStateFlow(false)
    val isRecitationGapActive: StateFlow<Boolean> = _isRecitationGapActive.asStateFlow()

    private val _recitationGapCountdown = MutableStateFlow(0)
    val recitationGapCountdown: StateFlow<Int> = _recitationGapCountdown.asStateFlow()

    private val _hifzRange = MutableStateFlow<HifzRange?>(null)
    val hifzRange: StateFlow<HifzRange?> = _hifzRange.asStateFlow()

    private var gapJob: Job? = null
    private var pendingGapAction: (() -> Unit)? = null

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
                        if (_isAyahMode.value) {
                            nextAyah()
                        } else {
                            val nextTrack = queueManager.playNext(forceAdvance = true)
                            if (nextTrack != null) {
                                startPlayback(nextTrack)
                            } else {
                                // Queue exhausted from source errors: keep the session
                                // (track + notification) alive with a friendly error
                                // instead of stopPlayback(), which strands to IDLE
                                // with no track and kills the notification.
                                SleepTimer.onQueueEnded()
                                PlayerBridge.pause()
                                _engineError.value = "Couldn't play this recitation"
                            }
                        }
                    } else if (failed != null) {
                        if (_isAyahMode.value) {
                            stopPlayback()
                        } else {
                            // Too many consecutive source errors in full-surah mode:
                            // same graceful landing — keep session, friendly error.
                            PlayerBridge.pause()
                            _engineError.value = "Couldn't play this recitation"
                        }
                    }
                } else if (error == null && _playbackState.value.status == PlaybackStatus.ERROR) {
                    consecutiveErrors = 0
                    _engineError.value = null
                    _playbackState.update { state ->
                        state.copy(status = if (_isPlaying.value) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED)
                    }
                } else if (error == null) {
                    consecutiveErrors = 0
                    _engineError.value = null
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
        val isAyah = track.ayahNo > 0 && !track.isFullSurah
        _isAyahMode.value = isAyah
        val surahTrack = track.toSurahTrack()
        queueManager.clear()
        queueManager.addToQueue(surahTrack)
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        pendingSeekMs = startPositionMs.coerceAtLeast(0L)
        pendingSeekTries = if (pendingSeekMs > 0L) 40 else 0
        if (isAyah) {
            playAyah(track.surahId, track.ayahNo, track.reciterSlug, startPositionMs)
        } else {
            startPlayback(surahTrack)
        }
    }

    fun playQueue(tracks: List<TrackItem>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (tracks.isEmpty()) {
            clear()
            return
        }
        val targetTrack = tracks.getOrNull(startIndex) ?: tracks.first()
        val isAyahList = targetTrack.ayahNo > 0 && !targetTrack.isFullSurah

        if (isAyahList) {
            _isAyahMode.value = true
            val existingIndex = queueManager.queue.indexOfFirst { it.surahId == targetTrack.surahId }
            if (existingIndex != -1) {
                queueManager.skipToIndex(existingIndex)
            } else {
                val surahTracks = tracks.map { it.toSurahTrack() }.distinctBy { it.surahId }
                val surahStartIndex = surahTracks.indexOfFirst { it.surahId == targetTrack.surahId }.coerceAtLeast(0)
                queueManager.setQueue(surahTracks, surahStartIndex)
            }
            _queue.value = queueManager.queue
            _currentIndex.value = queueManager.currentIndex
            playAyah(targetTrack.surahId, targetTrack.ayahNo, targetTrack.reciterSlug, startPositionMs)
        } else {
            _isAyahMode.value = false
            queueManager.setQueue(tracks, startIndex)
            _queue.value = queueManager.queue
            _currentIndex.value = queueManager.currentIndex
            val surah = queueManager.currentTrack
            if (surah != null) {
                pendingSeekMs = startPositionMs.coerceAtLeast(0L)
                pendingSeekTries = if (pendingSeekMs > 0L) 40 else 0
                startPlayback(surah)
            } else {
                stopPlayback()
            }
        }
    }

    fun setAyahMode(enabled: Boolean) {
        if (enabled == _isAyahMode.value) return
        if (enabled) {
            _isAyahMode.value = true
            val current = _currentTrack.value
            if (current?.let { it.ayahNo <= 0 || it.isFullSurah } == true) {
                val surahId = current.surahId
                val reciterSlug = current.reciterSlug
                val totalAyahs = getAyahCountForSurah(surahId)
                val progressFraction = _progress.value
                val targetAyah = ((progressFraction * totalAyahs).toInt() + 1).coerceIn(1, totalAyahs)
                playAyah(surahId, targetAyah, reciterSlug)
            }
        } else {
            _isAyahMode.value = false
            val current = _currentTrack.value
            if (current != null && current.ayahNo > 0) {
                val surahTrack = queueManager.currentTrack ?: current.toSurahTrack()
                val totalAyahs = getAyahCountForSurah(current.surahId)
                val progressFraction = if (totalAyahs > 1) (current.ayahNo - 1).toFloat() / totalAyahs.toFloat() else 0f
                val knownDuration = surahTrack.resolvedDurationMs()
                val startMs = if (knownDuration > 0L) (progressFraction * knownDuration).toLong() else 0L
                pendingSeekMs = startMs
                pendingSeekTries = if (pendingSeekMs > 0L) 40 else 0
                startPlayback(surahTrack)
            }
        }
    }

    fun toggleAyahMode() {
        setAyahMode(!_isAyahMode.value)
    }

    fun playSurahInAyahMode(surahId: Int, reciterSlug: String? = null, startAyahNo: Int = 1) {
        val targetSlug = reciterSlug ?: _currentTrack.value?.reciterSlug ?: queueManager.currentTrack?.reciterSlug ?: "mishary"
        val reciter = QuranDataRepository.getReciterBySlug(targetSlug)
        val totalAyahs = getAyahCountForSurah(surahId)
        val validAyahNo = startAyahNo.coerceIn(1, totalAyahs.coerceAtLeast(1))
        _isAyahMode.value = true

        val existingIndex = queueManager.queue.indexOfFirst { it.surahId == surahId }
        if (existingIndex != -1) {
            if (existingIndex != queueManager.currentIndex) {
                queueManager.skipToIndex(existingIndex)
            }
        } else {
            val surahs = QuranDataRepository.getSurahsForReciter(reciter)
                .filter { reciter.isSurahAvailable(it.id) }
            // Requested surah may be unavailable: fall back to the nearest
            // available one so the queue never holds a known-404.
            val effectiveSurahId = if (reciter.isSurahAvailable(surahId)) surahId
            else nearestAvailableSurahId(reciter, surahId)
            val surahTracks = if (surahs.isNotEmpty()) {
                surahs.map { s -> buildSurahTrack(s.id, reciter) }
            } else {
                listOf(buildSurahTrack(effectiveSurahId, reciter))
            }
            val startIdx = surahTracks.indexOfFirst { it.surahId == effectiveSurahId }
                .takeIf { it >= 0 } ?: 0
            queueManager.setQueue(surahTracks, startIdx)
        }
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex

        playAyah(surahId, validAyahNo, reciter.slug)
    }

    // --- Hifz Internal Gap & Action Management ---
    private fun cancelGap() {
        gapJob?.cancel()
        gapJob = null
        pendingGapAction = null
        _isRecitationGapActive.value = false
        _recitationGapCountdown.value = 0
    }

    private fun executeWithRecitationGap(lastAyahDurationMs: Long, action: () -> Unit) {
        cancelGap()

        val gapSetting = _recitationGapSeconds.value
        val gapDuration = when (gapSetting) {
            0 -> 0
            -1 -> {
                if (lastAyahDurationMs > 0L) {
                    ((lastAyahDurationMs + 999L) / 1000L).toInt().coerceAtLeast(1)
                } else 3
            }
            else -> gapSetting.coerceAtLeast(0)
        }

        if (gapDuration <= 0 || !_isPlaying.value) {
            action()
            return
        }

        pendingGapAction = action
        _isRecitationGapActive.value = true
        _recitationGapCountdown.value = gapDuration

        gapJob = scope.launch {
            var remaining = gapDuration
            while (remaining > 0) {
                _recitationGapCountdown.value = remaining
                if (!_isPlaying.value) {
                    delay(200L)
                    continue
                }
                delay(1000L)
                remaining--
            }
            _recitationGapCountdown.value = 0
            _isRecitationGapActive.value = false

            if (_isPlaying.value) {
                val pending = pendingGapAction
                pendingGapAction = null
                pending?.invoke()
            }
        }
    }

    // --- Public Hifz Control APIs ---
    fun setAyahRepetitionTarget(target: Int) {
        _ayahRepetitionTarget.value = when {
            target == -1 -> -1
            target < 1 -> 1
            else -> target
        }
        _currentAyahRepetition.value = 1
    }

    fun setRecitationGapSeconds(seconds: Int) {
        _recitationGapSeconds.value = when {
            seconds == -1 -> -1
            seconds < 0 -> 0
            else -> seconds
        }
    }

    fun setHifzRange(startAyah: Int, endAyah: Int, targetLoops: Int = 1) {
        val currentSurahId = _currentTrack.value?.surahId ?: queueManager.currentTrack?.surahId ?: 1
        setHifzRange(currentSurahId, startAyah, endAyah, targetLoops)
    }

    fun setHifzRange(surahId: Int, startAyah: Int, endAyah: Int, targetLoops: Int = 1) {
        val start = minOf(startAyah, endAyah).coerceAtLeast(1)
        val end = maxOf(startAyah, endAyah).coerceAtLeast(1)
        _hifzRange.value = HifzRange(
            surahId = surahId,
            startAyah = start,
            endAyah = end,
            targetLoops = targetLoops,
            currentLoop = 1
        )
        _currentAyahRepetition.value = 1
        cancelGap()
    }

    fun clearHifzRange() {
        _hifzRange.value = null
    }

    fun resetAyahRepetition() {
        _currentAyahRepetition.value = 1
    }

    fun skipRecitationGap() {
        if (_isRecitationGapActive.value) {
            val pending = pendingGapAction
            cancelGap()
            pending?.invoke()
        }
    }

    fun playAyah(
        surahId: Int,
        ayahNo: Int,
        reciterSlug: String? = null,
        startPositionMs: Long = 0L
    ) {
        cancelGap()
        _currentAyahRepetition.value = 1
        playAyahInternal(surahId, ayahNo, reciterSlug, startPositionMs)
    }

    private fun playAyahInternal(
        surahId: Int,
        ayahNo: Int,
        reciterSlug: String? = null,
        startPositionMs: Long = 0L
    ) {
        val targetSlug = reciterSlug ?: _currentTrack.value?.reciterSlug ?: queueManager.currentTrack?.reciterSlug ?: "mishary"
        val reciter = QuranDataRepository.getReciterBySlug(targetSlug)
        val totalAyahs = getAyahCountForSurah(surahId)
        val validAyahNo = ayahNo.coerceIn(1, totalAyahs.coerceAtLeast(1))
        _isAyahMode.value = true

        val existingIndex = queueManager.queue.indexOfFirst { it.surahId == surahId }
        if (existingIndex != -1) {
            if (existingIndex != queueManager.currentIndex) {
                queueManager.skipToIndex(existingIndex)
            }
        } else {
            val surahs = QuranDataRepository.getSurahsForReciter(reciter)
                .filter { reciter.isSurahAvailable(it.id) }
            // Requested surah may be unavailable: fall back to the nearest
            // available one so the queue never holds a known-404.
            val effectiveSurahId = if (reciter.isSurahAvailable(surahId)) surahId
            else nearestAvailableSurahId(reciter, surahId)
            val surahTracks = if (surahs.isNotEmpty()) {
                surahs.map { s -> buildSurahTrack(s.id, reciter) }
            } else {
                listOf(buildSurahTrack(effectiveSurahId, reciter))
            }
            val startIdx = surahTracks.indexOfFirst { it.surahId == effectiveSurahId }
                .takeIf { it >= 0 } ?: 0
            queueManager.setQueue(surahTracks, startIdx)
        }
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex

        val ayahTrack = buildAyahTrack(surahId, validAyahNo, reciter.slug, reciter.nameEn)
        pendingSeekMs = startPositionMs.coerceAtLeast(0L)
        pendingSeekTries = if (pendingSeekMs > 0L) 40 else 0
        startPlayback(ayahTrack)
    }

    private fun TrackItem.toSurahTrack(): TrackItem {
        if (this.ayahNo <= 0 || this.isFullSurah) return this
        val reciter = QuranDataRepository.getReciterBySlug(this.reciterSlug)
        return TrackItem(
            reciterSlug = this.reciterSlug,
            reciterName = this.reciterName,
            surahId = this.surahId,
            surahNameEn = this.surahNameEn,
            surahNameAr = this.surahNameAr,
            ayahNo = 0,
            audioUrl = reciter.getFullSurahUrl(this.surahId),
            textUthmani = "",
            durationMs = UNKNOWN_DURATION_MS
        )
    }

    private fun nearestAvailableSurahId(reciter: Reciter, requestedId: Int): Int {
        val ids = reciter.getAvailableSurahIds()
        if (ids.isEmpty()) return requestedId.coerceIn(1, 114)
        return ids.minByOrNull { kotlin.math.abs(it - requestedId) } ?: requestedId
    }

    private fun buildSurahTrack(surahId: Int, reciter: Reciter): TrackItem {
        val surah = QuranDataRepository.getSurahById(surahId)
        val surahNameEn = surah?.nameEn ?: QuranData.SURAHS.firstOrNull { it.id == surahId }?.nameEn ?: "Surah $surahId"
        val surahNameAr = surah?.nameAr ?: QuranData.SURAHS.firstOrNull { it.id == surahId }?.nameAr ?: ""
        val reciterName = if (reciter.nameEn.isNotBlank()) reciter.nameEn else reciter.slug
        return TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciterName,
            surahId = surahId,
            surahNameEn = surahNameEn,
            surahNameAr = surahNameAr,
            ayahNo = 0,
            audioUrl = reciter.getFullSurahUrl(surahId),
            textUthmani = "",
            durationMs = UNKNOWN_DURATION_MS
        )
    }

    private fun buildAyahTrack(
        surahId: Int,
        ayahNo: Int,
        reciterSlug: String?,
        fallbackReciterName: String = ""
    ): TrackItem {
        val reciter = QuranDataRepository.getReciterBySlug(reciterSlug)
        val surah = QuranDataRepository.getSurahById(surahId)
        val surahNameEn = surah?.nameEn ?: QuranData.SURAHS.firstOrNull { it.id == surahId }?.nameEn ?: "Surah $surahId"
        val surahNameAr = surah?.nameAr ?: QuranData.SURAHS.firstOrNull { it.id == surahId }?.nameAr ?: ""
        val reciterName = if (reciter.nameEn.isNotBlank()) reciter.nameEn else fallbackReciterName
        val verse = QuranAyahRepository.getAyahImmediate(surahId, ayahNo)
        return TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciterName,
            surahId = surahId,
            surahNameEn = surahNameEn,
            surahNameAr = surahNameAr,
            ayahNo = ayahNo,
            audioUrl = reciter.getAyahAudioUrl(surahId, ayahNo),
            textUthmani = verse.textUthmani,
            durationMs = UNKNOWN_DURATION_MS
        )
    }

    private fun getAyahCountForSurah(surahId: Int): Int {
        val surah = QuranDataRepository.getSurahById(surahId)
        return surah?.ayahsCount ?: QuranData.SURAHS.firstOrNull { it.id == surahId }?.ayahsCount ?: 7
    }

    fun addToQueue(track: TrackItem) {
        val surahTrack = track.toSurahTrack()
        queueManager.addToQueue(surahTrack)
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        if (_currentTrack.value == null && _playbackState.value.status == PlaybackStatus.IDLE) {
            if (_isAyahMode.value) {
                playAyah(surahTrack.surahId, 1, surahTrack.reciterSlug)
            } else {
                startPlayback(surahTrack)
            }
        }
    }

    fun addToQueue(tracks: List<TrackItem>) {
        if (tracks.isEmpty()) return
        val wasEmpty = queueManager.queue.isEmpty()
        val surahTracks = tracks.map { it.toSurahTrack() }.distinctBy { it.surahId }
        queueManager.addAllToQueue(surahTracks)
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        if (wasEmpty && _currentTrack.value == null && _playbackState.value.status == PlaybackStatus.IDLE) {
            val firstSurah = queueManager.currentTrack ?: surahTracks.first()
            if (_isAyahMode.value) {
                playAyah(firstSurah.surahId, 1, firstSurah.reciterSlug)
            } else {
                startPlayback(firstSurah)
            }
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
        val track = _currentTrack.value ?: queueManager.currentTrack ?: return
        if (_playbackState.value.status == PlaybackStatus.IDLE || _playbackState.value.status == PlaybackStatus.ERROR) {
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

    fun next() = if (_isAyahMode.value) nextAyah() else skipNext()

    fun nextAyah() {
        cancelGap()
        _currentAyahRepetition.value = 1
        val current = _currentTrack.value
        if (current == null) {
            val surah = queueManager.currentTrack ?: return
            playAyah(surah.surahId, 1, surah.reciterSlug)
            return
        }
        val surahId = current.surahId
        val currentAyah = if (current.ayahNo > 0) current.ayahNo else 0
        val totalAyahs = getAyahCountForSurah(surahId)
        val reciterSlug = current.reciterSlug

        val range = _hifzRange.value
        if (range != null && range.surahId == surahId) {
            if (currentAyah < range.endAyah) {
                playAyahInternal(surahId, currentAyah + 1, reciterSlug)
            } else {
                playAyahInternal(surahId, range.startAyah, reciterSlug)
            }
            return
        }

        if (currentAyah < totalAyahs) {
            playAyahInternal(surahId, currentAyah + 1, reciterSlug)
        } else {
            // Cross boundary into next Surah
            val nextSurah = queueManager.playNext(forceAdvance = true)
            if (nextSurah != null) {
                _currentIndex.value = queueManager.currentIndex
                playAyahInternal(nextSurah.surahId, 1, nextSurah.reciterSlug)
            } else {
                SleepTimer.onQueueEnded()
                stopPlayback()
            }
        }
    }

    fun skipNext() {
        cancelGap()
        _currentAyahRepetition.value = 1
        val nextSurah = queueManager.playNext(forceAdvance = true)
        if (nextSurah != null) {
            _currentIndex.value = queueManager.currentIndex
            if (_isAyahMode.value) {
                playAyah(nextSurah.surahId, 1, nextSurah.reciterSlug)
            } else {
                startPlayback(nextSurah)
            }
        } else {
            SleepTimer.onQueueEnded()
            stopPlayback()
        }
    }

    fun previous() = if (_isAyahMode.value) previousAyah() else skipPrevious()

    fun previousAyah() {
        cancelGap()
        _currentAyahRepetition.value = 1
        val current = _currentTrack.value ?: return
        if (_currentPositionMs.value > 3_000L) {
            seekTo(0L)
            return
        }
        val surahId = current.surahId
        val currentAyah = current.ayahNo
        val reciterSlug = current.reciterSlug

        val range = _hifzRange.value
        if (range != null && range.surahId == surahId) {
            if (currentAyah > range.startAyah) {
                playAyahInternal(surahId, currentAyah - 1, reciterSlug)
            } else {
                seekTo(0L)
            }
            return
        }

        if (currentAyah > 1) {
            playAyahInternal(surahId, currentAyah - 1, reciterSlug)
        } else {
            // Cross boundary backwards: go to previous Surah, last Ayah
            val prevSurah = queueManager.playPrevious(forceAdvance = true)
            if (prevSurah != null) {
                _currentIndex.value = queueManager.currentIndex
                val prevTotalAyahs = getAyahCountForSurah(prevSurah.surahId)
                playAyahInternal(prevSurah.surahId, prevTotalAyahs, prevSurah.reciterSlug)
            } else {
                seekTo(0L)
            }
        }
    }

    fun skipPrevious() {
        val current = _currentTrack.value
        if (_isAyahMode.value) {
            if (current != null && (current.ayahNo > 1 || _currentPositionMs.value > 3_000L)) {
                playAyah(current.surahId, 1, current.reciterSlug)
                return
            }
        } else {
            if (_currentPositionMs.value > 3_000L && current != null) {
                seekTo(0L)
                return
            }
        }

        val prevSurah = queueManager.playPrevious(forceAdvance = true)
        if (prevSurah != null) {
            _currentIndex.value = queueManager.currentIndex
            if (_isAyahMode.value) {
                playAyah(prevSurah.surahId, 1, prevSurah.reciterSlug)
            } else {
                startPlayback(prevSurah)
            }
        } else {
            if (_isAyahMode.value && current != null) {
                playAyah(current.surahId, 1, current.reciterSlug)
            } else {
                seekTo(0L)
            }
        }
    }

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
        val surahTrack = queueManager.skipToIndex(index)
        if (surahTrack != null) {
            _currentIndex.value = queueManager.currentIndex
            if (_isAyahMode.value) {
                playAyah(surahTrack.surahId, 1, _currentTrack.value?.reciterSlug ?: surahTrack.reciterSlug)
            } else {
                startPlayback(surahTrack)
            }
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
                if (_isAyahMode.value) {
                    playAyah(nextTrack.surahId, 1, nextTrack.reciterSlug)
                } else {
                    startPlayback(nextTrack)
                }
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
        clearHifzRange()
        _ayahRepetitionTarget.value = 1
        _currentAyahRepetition.value = 1
        _recitationGapSeconds.value = 0
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
        cancelGap()
        _currentAyahRepetition.value = 1
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
        SleepTimer.onAyahEnded()
        if (!_isPlaying.value) return

        if (_isAyahMode.value) {
            handleAyahTrackEnd()
        } else {
            handleSurahTrackEnd()
        }
    }

    private fun handleAyahTrackEnd() {
        val current = _currentTrack.value ?: return
        val surahId = current.surahId
        val currentAyah = current.ayahNo
        val totalAyahs = getAyahCountForSurah(surahId)
        val reciterSlug = current.reciterSlug
        val lastDurationMs = effectiveDuration()

        // 1. Ayah Repetition Logic (N-times)
        val repTarget = _ayahRepetitionTarget.value
        val shouldRepeatAyah = (repTarget == -1) || (_currentAyahRepetition.value < repTarget)

        if (shouldRepeatAyah) {
            _currentAyahRepetition.value += 1
            executeWithRecitationGap(lastDurationMs) {
                playAyahInternal(surahId, currentAyah, reciterSlug)
            }
            return
        }

        // Ayah repetition target reached -> reset counter to 1 and proceed
        _currentAyahRepetition.value = 1

        // 2. Bounded Range Loop Mode
        val range = _hifzRange.value
        if (range != null && range.surahId == surahId) {
            if (currentAyah < range.endAyah) {
                val nextAyah = (currentAyah + 1).coerceAtLeast(range.startAyah)
                executeWithRecitationGap(lastDurationMs) {
                    playAyahInternal(surahId, nextAyah, reciterSlug)
                }
            } else {
                // At or beyond range endAyah
                val canLoopRange = (range.targetLoops == -1) || (range.currentLoop < range.targetLoops)
                if (canLoopRange) {
                    _hifzRange.value = range.copy(currentLoop = range.currentLoop + 1)
                    executeWithRecitationGap(lastDurationMs) {
                        playAyahInternal(surahId, range.startAyah, reciterSlug)
                    }
                } else {
                    executeWithRecitationGap(lastDurationMs) {
                        SleepTimer.onQueueEnded()
                        stopPlayback()
                    }
                }
            }
            return
        }

        // 3. Standard Queue / RepeatMode Logic (No HifzRange active)
        when (queueManager.repeatMode) {
            RepeatMode.AYAH -> {
                executeWithRecitationGap(lastDurationMs) {
                    playAyahInternal(surahId, currentAyah, reciterSlug)
                }
            }
            RepeatMode.SURAH -> {
                if (currentAyah < totalAyahs) {
                    executeWithRecitationGap(lastDurationMs) {
                        playAyahInternal(surahId, currentAyah + 1, reciterSlug)
                    }
                } else {
                    SleepTimer.onSurahEnded()
                    if (!_isPlaying.value) return
                    executeWithRecitationGap(lastDurationMs) {
                        playAyahInternal(surahId, 1, reciterSlug)
                    }
                }
            }
            RepeatMode.QUEUE, RepeatMode.OFF -> {
                if (currentAyah < totalAyahs) {
                    executeWithRecitationGap(lastDurationMs) {
                        playAyahInternal(surahId, currentAyah + 1, reciterSlug)
                    }
                } else {
                    SleepTimer.onSurahEnded()
                    if (!_isPlaying.value) return
                    val nextSurah = queueManager.playNext(forceAdvance = false)
                    if (nextSurah != null) {
                        _currentIndex.value = queueManager.currentIndex
                        executeWithRecitationGap(lastDurationMs) {
                            playAyahInternal(nextSurah.surahId, 1, nextSurah.reciterSlug)
                        }
                    } else {
                        SleepTimer.onQueueEnded()
                        stopPlayback()
                    }
                }
            }
        }
    }

    private fun handleSurahTrackEnd() {
        SleepTimer.onSurahEnded()
        if (!_isPlaying.value) return
        val nextTrack = queueManager.playNext(forceAdvance = false)
        if (nextTrack != null) {
            _currentIndex.value = queueManager.currentIndex
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
