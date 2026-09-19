package com.quranify.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android actual: A/B two-ExoPlayer muted crossfade manager for the fullscreen
 * ambient background-video layer in Now Playing.
 *
 * - Two lazy, muted players (no audio focus, no MediaSession). The composable
 *   binds layer A <-> [playerA] and layer B <-> [playerB] permanently; logical
 *   front/back roles flip via [confirmSwap].
 * - Single-key rotation loops seamlessly (REPEAT_MODE_ALL). Multi-key rotation
 *   plays the front clip once (REPEAT_MODE_OFF); on STATE_ENDED the next key is
 *   started on the back player BEFORE the swap event fires, so the composable
 *   can fade the back layer in with no flicker, then call [confirmSwap] to stop
 *   the old front and promote back -> front.
 * - Transport mirrors [AudioEngine]: pause retains positions, resume never
 *   auto-loads, stop unloads everything.
 *
 * Asset keys map to `asset:///videos/<key>.mp4`
 * (androidApp/src/main/assets/videos/<key>.mp4).
 */
actual object AmbientVideoBridge {
    private const val TAG = "AmbientVideo"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mutex = Mutex()

    private var appContext: android.content.Context? = null

    private var exoA: androidx.media3.exoplayer.ExoPlayer? = null
    private var exoB: androidx.media3.exoplayer.ExoPlayer? = null

    private var rotation: List<String> = emptyList()
    private var nextIndex: Int = 0
    private var generation: Long = 0L
    private var isFrontA: Boolean = true

    private val _frontIsA = MutableStateFlow(true)
    internal val frontIsA: StateFlow<Boolean> = _frontIsA.asStateFlow()

    /** Key currently on the visible (front) player, null when idle. */
    private val _frontKey = MutableStateFlow<String?>(null)
    internal val frontKey: StateFlow<String?> = _frontKey.asStateFlow()

    /** Key loaded + playing on the hidden (back) player, awaiting fade-in. Null when no swap pending. */
    private val _backReadyKey = MutableStateFlow<String?>(null)
    internal val backReadyKey: StateFlow<String?> = _backReadyKey.asStateFlow()

    /** Monotonic counter bumped each time the back player becomes ready; the view fades then confirms. */
    private val _swapEvent = MutableStateFlow(0L)
    internal val swapEvent: StateFlow<Long> = _swapEvent.asStateFlow()

    private val listenerA = object : androidx.media3.common.Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                onPlayerEnded(firedIsA = true, gen = generation)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e(TAG, "playerA error: ${error.message}", error)
        }
    }

    private val listenerB = object : androidx.media3.common.Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                onPlayerEnded(firedIsA = false, gen = generation)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e(TAG, "playerB error: ${error.message}", error)
        }
    }

    /**
     * Must be called once from Android (e.g. MainActivity.onCreate). All player
     * acquisition paths stay lazily safe and log when init was skipped.
     */
    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    /** Fixed layer binding for the composable: layer A always shows this player. Null when init was skipped. */
    internal fun playerA(): androidx.media3.exoplayer.ExoPlayer? = ensureA()

    /** Fixed layer binding for the composable: layer B always shows this player. Null when init was skipped. */
    internal fun playerB(): androidx.media3.exoplayer.ExoPlayer? = ensureB()

    private fun front(): androidx.media3.exoplayer.ExoPlayer? = if (isFrontA) exoA else exoB

    private fun back(): androidx.media3.exoplayer.ExoPlayer? = if (isFrontA) exoB else exoA

    actual fun playVideos(assetKeys: List<String>) {
        scope.launch {
            mutex.withLock {
                if (assetKeys.isEmpty()) {
                    stopLocked()
                    return@withLock
                }
                if (assetKeys == rotation && rotation.isNotEmpty()) {
                    // Same key-set already active — just resume, never restart.
                    resumeLocked()
                    return@withLock
                }
                generation++
                stopPlayersLocked()
                rotation = assetKeys.toList()
                nextIndex = if (assetKeys.size > 1) 1 % assetKeys.size else 0
                isFrontA = true
                val frontPlayer = ensureA() ?: run {
                    android.util.Log.e(TAG, "playVideos($assetKeys): no app context (init missing?)")
                    rotation = emptyList()
                    return@withLock
                }
                loadLocked(frontPlayer, assetKeys[0], repeatAll = assetKeys.size == 1)
                _frontIsA.value = true
                _frontKey.value = assetKeys[0]
                _backReadyKey.value = null
                android.util.Log.d(TAG, "rotation started: $assetKeys")
            }
        }
    }

    actual fun pauseVideos() {
        try {
            exoA?.pause()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "pauseVideos A failed", e)
        }
        try {
            exoB?.pause()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "pauseVideos B failed", e)
        }
    }

    actual fun resumeVideos() {
        // Resume only when a rotation is active; never auto-loads here
        // (loading is driven by explicit selection via playVideos).
        if (rotation.isEmpty()) return
        try {
            front()?.play()
            // Keep a pending crossfade alive across pause/resume.
            if (_backReadyKey.value != null) back()?.play()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "resumeVideos failed", e)
        }
    }

    actual fun stopVideos() {
        scope.launch {
            mutex.withLock {
                stopLocked()
            }
        }
    }

    /**
     * Called by the composable after it finishes fading the back layer in
     * (~1.2s alpha animation): stops the old front and promotes back -> front.
     * No-op when nothing is pending (e.g. stopped mid-fade).
     */
    internal fun confirmSwap() {
        scope.launch {
            mutex.withLock {
                val readyKey = _backReadyKey.value
                val newFront = back()
                if (readyKey == null || newFront == null) return@withLock
                val oldFront = front()
                try {
                    oldFront?.stop()
                    oldFront?.clearMediaItems()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "confirmSwap: stop old front failed", e)
                }
                isFrontA = !isFrontA
                _frontIsA.value = isFrontA
                _frontKey.value = readyKey
                _backReadyKey.value = null
                android.util.Log.d(TAG, "swapped to $readyKey")
                nextIndex = if (rotation.isNotEmpty()) (nextIndex + 1) % rotation.size else 0
            }
        }
    }

    private fun onPlayerEnded(firedIsA: Boolean, gen: Long) {
        scope.launch {
            mutex.withLock {
                if (gen != generation) {
                    android.util.Log.d(TAG, "ignoring stale ENDED (gen $gen != $generation)")
                    return@withLock
                }
                advanceLocked(firedIsA)
            }
        }
    }

    private fun advanceLocked(firedIsA: Boolean) {
        try {
            if (rotation.isEmpty()) return
            if (rotation.size <= 1) return // single-key uses REPEAT_MODE_ALL; nothing to rotate
            if (firedIsA != isFrontA) {
                // Back layer ended before the fade completed (short clip) — loop it in place.
                try {
                    back()?.seekTo(0L)
                    back()?.play()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "back replay failed", e)
                }
                return
            }
            if (_backReadyKey.value != null) return // swap already pending; wait for confirmSwap()
            // Lazily build the back player here: it is intentionally NOT created
            // in playVideos (max 1 decoder until rotation actually needs the 2nd).
            val backPlayer = (if (isFrontA) ensureB() else ensureA()) ?: run {
                android.util.Log.e(TAG, "advance: back player null (init missing?)")
                return
            }
            val key = rotation[nextIndex % rotation.size]
            loadLocked(backPlayer, key, repeatAll = false)
            _backReadyKey.value = key
            _swapEvent.value = _swapEvent.value + 1
        } catch (e: Exception) {
            android.util.Log.e(TAG, "advance failed", e)
        }
    }

    private fun resumeLocked() {
        try {
            front()?.play()
            if (_backReadyKey.value != null) back()?.play()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "resumeLocked failed", e)
        }
    }

    private fun stopLocked() {
        generation++
        stopPlayersLocked()
        rotation = emptyList()
        nextIndex = 0
        isFrontA = true
        _frontIsA.value = true
        _frontKey.value = null
        _backReadyKey.value = null
        // swapEvent stays monotonic so the view's last-handled guard stays valid.
    }

    private fun stopPlayersLocked() {
        try {
            exoA?.stop()
            exoA?.clearMediaItems()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "stop A failed", e)
        }
        try {
            exoB?.stop()
            exoB?.clearMediaItems()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "stop B failed", e)
        }
    }

    private fun loadLocked(
        player: androidx.media3.exoplayer.ExoPlayer,
        key: String,
        repeatAll: Boolean,
    ) {
        try {
            val uri = android.net.Uri.parse("asset:///videos/$key.mp4")
            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            player.repeatMode = if (repeatAll) {
                androidx.media3.common.Player.REPEAT_MODE_ALL
            } else {
                androidx.media3.common.Player.REPEAT_MODE_OFF
            }
            player.prepare()
            player.play()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "load($key) failed", e)
        }
    }

    private fun ensureA(): androidx.media3.exoplayer.ExoPlayer? {
        exoA?.let { return it }
        return buildPlayer(listenerA)?.also { exoA = it }
    }

    private fun ensureB(): androidx.media3.exoplayer.ExoPlayer? {
        exoB?.let { return it }
        return buildPlayer(listenerB)?.also { exoB = it }
    }

    private fun buildPlayer(
        listener: androidx.media3.common.Player.Listener,
    ): androidx.media3.exoplayer.ExoPlayer? {
        val ctx = appContext ?: run {
            android.util.Log.e(
                TAG,
                "ExoPlayer build: appContext null — init() not called " +
                    "(call AmbientVideoBridge.init(context) from MainActivity)",
            )
            return null
        }
        return try {
            androidx.media3.exoplayer.ExoPlayer.Builder(ctx)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .build(),
                    false,
                )
                .setHandleAudioBecomingNoisy(false)
                .build()
                .apply {
                    volume = 0f
                    videoScalingMode =
                        androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    addListener(listener)
                }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "ExoPlayer build failed", e)
            null
        }
    }
}
