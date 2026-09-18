package com.quranify.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android actual: Media3 ExoPlayer singleton.
 */
actual object PlayerBridge {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    private var appContext: android.content.Context? = null

    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    actual val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    actual val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onTrackEnd: (() -> Unit)? = null
    private var pollStarted = false

    private val listener = object : androidx.media3.common.Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            _isBuffering.value = playbackState == androidx.media3.common.Player.STATE_BUFFERING
            if (playbackState == androidx.media3.common.Player.STATE_READY) {
                val d = player.duration
                _durationMs.value = if (d != androidx.media3.common.C.TIME_UNSET && d > 0) d else 0L
                _errorMessage.value = null
            }
            if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                onTrackEnd?.invoke()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _errorMessage.value = error.message ?: "Playback error (${error.errorCode})"
            _isBuffering.value = false
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) _errorMessage.value = null
        }
    }

    private fun player(context: android.content.Context): androidx.media3.exoplayer.ExoPlayer {
        var p = exoPlayer
        if (p == null) {
            p = androidx.media3.exoplayer.ExoPlayer.Builder(context.applicationContext).build()
            p.addListener(listener)
            exoPlayer = p
            startPolling()
        }
        return p
    }

    private fun startPolling() {
        if (pollStarted) return
        pollStarted = true
        scope.launch {
            while (true) {
                val p = exoPlayer
                if (p != null) {
                    val pos = try { p.currentPosition } catch (_: Exception) { _positionMs.value }
                    val dur = try { p.duration } catch (_: Exception) { androidx.media3.common.C.TIME_UNSET }
                    _positionMs.value = pos.coerceAtLeast(0L)
                    if (dur != androidx.media3.common.C.TIME_UNSET && dur > 0) {
                        _durationMs.value = dur
                    }
                }
                delay(250)
            }
        }
    }

    /**
     * Must be called once from Android (e.g. MainActivity.onCreate) so the
     * singleton can build ExoPlayer with an application context.
     */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        player(context)
    }

    /**
     * Hook set by androidApp (MainActivity) so that any [play]/[resume]
     * boots the [com.quranify.android.QuranPlaybackService] foreground
     * service. Lives here (androidMain) instead of common code so the
     * shared module never references the app module (no circular dep).
     * AudioEngine.playTrack -> PlayerBridge.play -> this callback ->
     * ContextCompat.startForegroundService(...).
     */
    var onPlayRequested: (() -> Unit)? = null

    /** Canonical player owned (after service start) by QuranPlaybackService. */
    fun playerOrNull(): androidx.media3.exoplayer.ExoPlayer? = exoPlayer

    fun ensurePlayer(context: android.content.Context): androidx.media3.exoplayer.ExoPlayer =
        player(context)

    /**
     * Adopt an externally built ExoPlayer (the foreground service's) as the
     * canonical player. If a previous internal instance exists and differs,
     * it is released to avoid two players holding audio focus.
     */
    fun attachPlayer(external: androidx.media3.exoplayer.ExoPlayer) {
        val current = exoPlayer
        if (current === external) return
        if (current != null) {
            try { current.removeListener(listener) } catch (_: Exception) { }
            try { current.stop() } catch (_: Exception) { }
            // Only release the old instance when it is NOT currently playing
            // through the new path — safe because the service reuses the
            // existing instance whenever one is already present.
            try { current.release() } catch (_: Exception) { }
        }
        external.addListener(listener)
        exoPlayer = external
        startPolling()
        // Sync cached flows with the adopted player state.
        try {
            val d = external.duration
            _durationMs.value =
                if (d != androidx.media3.common.C.TIME_UNSET && d > 0) d else _durationMs.value
            _positionMs.value = external.currentPosition.coerceAtLeast(0L)
        } catch (_: Exception) { }
    }

    /**
     * Refresh the current MediaItem metadata (notification title/artist)
     * without interrupting playback. Android-only helper — not part of the
     * common expect contract, called by QuranPlaybackService when
     * AudioEngine.currentTrack changes.
     */
    fun updateMetadata(title: String, artist: String? = null) {
        try {
            val p = exoPlayer ?: return
            if (p.mediaItemCount == 0) return
            val index = p.currentMediaItemIndex.coerceIn(0, p.mediaItemCount - 1)
            val current = p.getMediaItemAt(index)
            val meta = current.mediaMetadata.buildUpon()
                .setTitle(title)
                .apply { artist?.let { setArtist(it) } }
                .build()
            p.replaceMediaItem(index, current.buildUpon().setMediaMetadata(meta).build())
        } catch (_: Exception) { }
    }

    actual fun play(url: String) {
        try { onPlayRequested?.invoke() } catch (_: Exception) { }
        val p = exoPlayer ?: appContext?.let { player(it) } ?: run {
            _errorMessage.value = "Player not initialised — call PlayerBridge.init(context) from MainActivity"
            return
        }
        try {
            _errorMessage.value = null
            _isBuffering.value = true
            _positionMs.value = 0L
            _durationMs.value = 0L
            val item = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(url))
            p.setMediaItem(item)
            p.prepare()
            p.play()
        } catch (e: Exception) {
            _isBuffering.value = false
            _errorMessage.value = e.message ?: "Unable to start playback"
        }
    }

    actual fun pause() {
        try { exoPlayer?.pause() } catch (_: Exception) { }
    }

    actual fun resume() {
        try { onPlayRequested?.invoke() } catch (_: Exception) { }
        try { exoPlayer?.play() } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun stop() {
        try {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        } catch (_: Exception) { }
        _positionMs.value = 0L
        _isBuffering.value = false
    }

    actual fun seekTo(positionMs: Long) {
        try { exoPlayer?.seekTo(positionMs.coerceAtLeast(0L)) } catch (_: Exception) { }
    }

    actual fun setSpeed(speed: Float) {
        try { exoPlayer?.setPlaybackSpeed(speed.coerceIn(0.25f, 3.0f)) } catch (_: Exception) { }
    }

    actual fun setVolume(volume: Float) {
        try { exoPlayer?.volume = volume.coerceIn(0f, 1f) } catch (_: Exception) { }
    }

    actual fun setOnTrackEndListener(listener: (() -> Unit)?) {
        onTrackEnd = listener
    }
}
