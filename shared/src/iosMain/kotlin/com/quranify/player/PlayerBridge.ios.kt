package com.quranify.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS actual stub — compiles on iosArm64/iosSimulatorArm64.
 * TODO: wire AVPlayer (AVPlayerItem + periodic time observer) here.
 */
actual object PlayerBridge {
    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    actual val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    actual val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onTrackEnd: (() -> Unit)? = null

    actual fun play(url: String) {
        // TODO(iOS): create AVPlayerItem(URL(string: url)), replaceCurrentItem, play().
        _errorMessage.value = null
        _isBuffering.value = false
    }

    actual fun pause() {
        // TODO(iOS): player.pause()
    }

    actual fun resume() {
        // TODO(iOS): player.play()
    }

    actual fun stop() {
        // TODO(iOS): player.pause(); replaceCurrentItem(null)
        _positionMs.value = 0L
    }

    actual fun seekTo(positionMs: Long) {
        // TODO(iOS): player.seek(to: CMTime(...))
        _positionMs.value = positionMs.coerceAtLeast(0L)
    }

    actual fun setSpeed(speed: Float) {
        // TODO(iOS): player.rate = speed
    }

    actual fun setVolume(volume: Float) {
        // TODO(iOS): player.volume = volume
    }

    actual fun setOnTrackEndListener(listener: (() -> Unit)?) {
        onTrackEnd = listener
    }
}
