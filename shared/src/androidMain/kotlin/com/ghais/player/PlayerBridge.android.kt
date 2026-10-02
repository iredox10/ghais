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
     * Telegram-style single-owner audio focus (mirrors
     * MediaController.onAudioFocusChange): ExoPlayer's internal focus
     * handling is DISABLED in every builder (handleAudioFocus=false) so
     * exactly one listener owns pause/resume/duck. A WhatsApp status or
     * voice note takes transient focus -> we get LOSS_TRANSIENT -> pause +
     * remember, then auto-resume on GAIN. A permanent LOSS (another music
     * app) pauses and stays paused until the user presses play. Ducking
     * lowers to 0.2 and keeps playing, like Telegram's VOLUME_DUCK.
     *
     * Focus-driven pauses flow through AudioEngine (UI stays honest) but
     * are marked via [focusPausedByUs] so the service mirror and the
     * playWhenReady callback never mistake them for user pauses — that
     * double-pause is what used to kill every auto-resume.
     */
    @Volatile
    private var resumeOnFocusGain = false

    @Volatile
    private var focusPausedByUs = false

    @Volatile
    private var ducked = false

    @Volatile
    private var hasFocus = false

    private var baseVolume = 1f
    private var audioFocusRequest: android.media.AudioFocusRequest? = null

    /** True while the current pause was initiated by a focus loss. */
    fun isFocusPausedByUs(): Boolean = focusPausedByUs

    private fun audioManagerOf(): android.media.AudioManager? = try {
        appContext?.applicationContext
            ?.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
    } catch (_: Exception) {
        null
    }

    private val audioFocusListener = android.media.AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            android.media.AudioManager.AUDIOFOCUS_LOSS -> {
                ducked = false
                applyVolume()
                val wasPlaying = AudioEngine.isPlaying.value
                AudioEngine.pause()
                focusPausedByUs = wasPlaying
                resumeOnFocusGain = false
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                ducked = false
                applyVolume()
                val wasPlaying = AudioEngine.isPlaying.value
                AudioEngine.pause()
                focusPausedByUs = wasPlaying
                resumeOnFocusGain = wasPlaying
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (AudioEngine.isPlaying.value) {
                    ducked = true
                    applyVolume()
                }
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                ducked = false
                applyVolume()
                if (resumeOnFocusGain && focusPausedByUs) {
                    resumeOnFocusGain = false
                    focusPausedByUs = false
                    try {
                        AudioEngine.resume()
                    } catch (_: Exception) {
                    }
                } else {
                    focusPausedByUs = false
                }
            }
        }
    }

    private fun requestFocus(): Boolean {
        val am = audioManagerOf() ?: return false
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                var req = audioFocusRequest
                if (req == null) {
                    req = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setOnAudioFocusChangeListener(audioFocusListener)
                        .build()
                    audioFocusRequest = req
                }
                hasFocus = am.requestAudioFocus(req) ==
                    android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                hasFocus
            } else {
                @Suppress("DEPRECATION")
                hasFocus = am.requestAudioFocus(
                    audioFocusListener,
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.AUDIOFOCUS_GAIN,
                ) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                hasFocus
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun abandonFocus() {
        try {
            val am = audioManagerOf() ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {
        }
        hasFocus = false
    }

    private fun applyVolume() {
        try {
            val factor = if (ducked) 0.2f else 1f
            exoPlayer?.volume = (baseVolume * factor).coerceIn(0f, 1f)
        } catch (_: Exception) {
        }
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
            // Our own focus handler sets its flags AFTER calling
            // AudioEngine.pause(), so this never clobbers a fresh
            // focus-driven pause.
            if (!playWhenReady &&
                reason != androidx.media3.common.Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
            ) {
                resumeOnFocusGain = false
                focusPausedByUs = false
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
                    // Focus is owned SOLELY by audioFocusListener above
                    // (Telegram-style); ExoPlayer must not self-pause.
                    false,
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
                .build()
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
        try { requestFocus() } catch (_: Exception) { }
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
        // Explicit pause wins over any pending focus resume.
        resumeOnFocusGain = false
        focusPausedByUs = false
        try { exoPlayer?.pause() } catch (_: Exception) { }
    }

    actual fun resume() {
        try { onPlayRequested?.invoke() } catch (_: Exception) { }
        try { requestFocus() } catch (_: Exception) { }
        try { exoPlayer?.play() } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun stop() {
        resumeOnFocusGain = false
        focusPausedByUs = false
        ducked = false
        try { abandonFocus() } catch (_: Exception) { }
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
        baseVolume = volume.coerceIn(0f, 1f)
        applyVolume()
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
