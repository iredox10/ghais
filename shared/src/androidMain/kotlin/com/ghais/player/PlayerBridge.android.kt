package com.ghais.player

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
    private var pendingTitle: String? = null
    private var pendingArtist: String? = null
    private var pendingArtworkUri: String? = null

    /**
     * Spotify-style resume-on-focus-gain: ExoPlayer auto-resumes transient
     * focus loss itself, but a PERMANENT loss (WhatsApp status takes
     * AUDIOFOCUS_GAIN) drops playWhenReady with no auto-resume. Remember the
     * interruption and resume when focus returns — but only if the user
     * still intends playback (AudioEngine.isPlaying) and didn't pause
     * mid-interruption.
     */
    @Volatile
    private var resumeOnFocusGain = false

    private val audioFocusListener = android.media.AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            android.media.AudioManager.AUDIOFOCUS_LOSS,
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (AudioEngine.isPlaying.value) resumeOnFocusGain = true
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    val p = exoPlayer
                    if (AudioEngine.isPlaying.value && p != null && !p.isPlaying) {
                        try { p.play() } catch (_: Exception) { }
                    }
                }
            }
        }
    }

    private fun ensureFocusListener(context: android.content.Context) {
        try {
            val am = context.applicationContext
                .getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val req = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusListener,
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.AUDIOFOCUS_GAIN,
                )
            }
        } catch (_: Exception) { }
    }

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

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
            // User/app pause (anything but a focus loss) cancels a pending
            // focus resume — e.g. pausing mid-interruption must stay paused.
            if (!playWhenReady &&
                reason != androidx.media3.common.Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
            ) {
                resumeOnFocusGain = false
            }
        }
    }

    private fun player(context: android.content.Context): androidx.media3.exoplayer.ExoPlayer {
        var p = exoPlayer
        if (p == null) {
            p = androidx.media3.exoplayer.ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .build(),
                    true,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
                .build()
            p.addListener(listener)
            exoPlayer = p
            ensureFocusListener(context)
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
     * boots the [com.ghais.android.QuranPlaybackService] foreground
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
        pendingTitle = title
        pendingArtist = artist
        try {
            val p = exoPlayer ?: return
            if (p.mediaItemCount == 0) return
            // Never replace the item mid-playback: it resets the timeline and
            // kills the current ayah (surah stops after ~1 ayah). The item
            // already carries correct metadata from play() time.
            if (p.isPlaying) return
            val index = p.currentMediaItemIndex.coerceIn(0, p.mediaItemCount - 1)
            val current = p.getMediaItemAt(index)
            val artworkUriStr = try { AmbientVideoArt.artworkUri.value?.toString() } catch (_: Exception) { null }
            val currentArtworkStr = try { current.mediaMetadata.artworkUri?.toString() } catch (_: Exception) { null }
            if (current.mediaMetadata.title?.toString() == title && currentArtworkStr == artworkUriStr) return
            // Replacing the item resets the timeline — preserve position so a
            // resume seek applied just before us isn't wiped out.
            val resumePos = p.currentPosition.coerceAtLeast(0L)
            val meta = current.mediaMetadata.buildUpon()
                .setTitle(title)
                .apply { artist?.let { setArtist(it) } }
                .apply {
                    if (!artworkUriStr.isNullOrBlank() && artworkUriStr != currentArtworkStr) {
                        try { setArtworkUri(android.net.Uri.parse(artworkUriStr)) } catch (_: Exception) { }
                    }
                }
                .build()
            p.replaceMediaItem(index, current.buildUpon().setMediaMetadata(meta).build())
            if (resumePos > 1_000L) {
                try { p.seekTo(resumePos) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    actual fun play(url: String, startPositionMs: Long) {
        playWithMetadata(url, pendingTitle, pendingArtist, startPositionMs)
    }

    actual fun prepare(url: String, startPositionMs: Long) {
        prepareWithMetadata(url, pendingTitle, pendingArtist, startPositionMs)
    }

    actual fun setPlaybackMetadata(title: String?, artist: String?) {
        if (!title.isNullOrBlank()) pendingTitle = title
        if (!artist.isNullOrBlank()) pendingArtist = artist
    }

    actual fun setPlaybackArtworkUri(uri: String?) {
        pendingArtworkUri = uri?.takeIf { it.isNotBlank() }
    }

    /** Android-only: start playback with lockscreen/notification metadata set atomically. */
    fun playWithMetadata(url: String, title: String?, artist: String?, startPositionMs: Long = 0L) {
        if (!title.isNullOrBlank()) pendingTitle = title
        if (!artist.isNullOrBlank()) pendingArtist = artist
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
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .apply {
                    val t = pendingTitle ?: title
                    val a = pendingArtist ?: artist
                    if (!t.isNullOrBlank()) setTitle(t) else setTitle("Ghais")
                    if (!a.isNullOrBlank()) setArtist(a)
                    val artwork = pendingArtworkUri
                    if (!artwork.isNullOrBlank()) {
                        try { setArtworkUri(android.net.Uri.parse(artwork)) } catch (_: Exception) { }
                    }
                }
                .build()
            val item = androidx.media3.common.MediaItem.Builder()
                .setUri(android.net.Uri.parse(url))
                .setMediaId(url)
                .setMediaMetadata(metadata)
                .build()
            p.setMediaItem(item, startPositionMs.coerceAtLeast(0L))
            p.prepare()
            p.play()
        } catch (e: Exception) {
            _isBuffering.value = false
            _errorMessage.value = e.message ?: "Unable to start playback"
        }
    }

    /**
     * Android-only: load + prepare + seek WITHOUT starting playback (stays
     * paused). Silent preload for continue-listening restore.
     *
     * Deliberately mirrors [playWithMetadata] MINUS `p.play()` and MINUS
     * `onPlayRequested`: no foreground-service boot while idle — ExoPlayer can
     * prepare/buffer in the app process with no audio output, and the FGS
     * starts later via [resume] when the user actually presses play (the
     * service then reuses this preloaded instance via `attachPlayer`, keeping
     * the seeked position). The explicit `p.pause()` pins playWhenReady=false
     * so a reused player (left with playWhenReady=true by a prior session)
     * can never autoplay on READY — the no-autoplay guarantee.
     */
    fun prepareWithMetadata(url: String, title: String?, artist: String?, startPositionMs: Long = 0L) {
        if (!title.isNullOrBlank()) pendingTitle = title
        if (!artist.isNullOrBlank()) pendingArtist = artist
        val p = exoPlayer ?: appContext?.let { player(it) } ?: run {
            _errorMessage.value = "Player not initialised — call PlayerBridge.init(context) from MainActivity"
            return
        }
        try {
            _errorMessage.value = null
            _isBuffering.value = true
            _positionMs.value = startPositionMs.coerceAtLeast(0L)
            _durationMs.value = 0L
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .apply {
                    val t = pendingTitle ?: title
                    val a = pendingArtist ?: artist
                    if (!t.isNullOrBlank()) setTitle(t) else setTitle("Ghais")
                    if (!a.isNullOrBlank()) setArtist(a)
                    val artwork = pendingArtworkUri
                    if (!artwork.isNullOrBlank()) {
                        try { setArtworkUri(android.net.Uri.parse(artwork)) } catch (_: Exception) { }
                    }
                }
                .build()
            val item = androidx.media3.common.MediaItem.Builder()
                .setUri(android.net.Uri.parse(url))
                .setMediaId(url)
                .setMediaMetadata(metadata)
                .build()
            p.setMediaItem(item, startPositionMs.coerceAtLeast(0L))
            p.prepare()
            p.pause()
        } catch (e: Exception) {
            _isBuffering.value = false
            _errorMessage.value = e.message ?: "Unable to load playback"
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
        resumeOnFocusGain = false
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

    /** Drop a released ExoPlayer so the next play() builds a fresh instance. */
    fun onPlayerReleased(released: androidx.media3.exoplayer.ExoPlayer?) {
        if (released != null && exoPlayer === released) {
            try { released.removeListener(listener) } catch (_: Exception) { }
            exoPlayer = null
            pollStarted = false
        }
    }
}
