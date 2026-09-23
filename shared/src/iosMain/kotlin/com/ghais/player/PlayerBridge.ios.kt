@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ghais.player

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL

/**
 * iOS actual — real AVPlayer bridge.
 *
 * - play/pause/resume/stop/seek/speed/volume delegate to AVPlayer.
 * - Position + duration polling via addPeriodicTimeObserver (0.25s).
 * - Track-end via AVPlayerItemDidPlayToEndTimeNotification.
 * - Playback category on AVAudioSession for background audio.
 *
 * Background-audio entitlement (Xcode app target):
 * Target -> Signing & Capabilities -> Background Modes -> check
 * "Audio, AirPlay, and Picture in Picture". That adds `audio` to
 * UIBackgroundModes in Info.plist:
 * <key>UIBackgroundModes</key><array><string>audio</string></array>
 * (or INFOPLIST_KEY_UIBackgroundModes = audio in build settings.)
 * This file also sets AVAudioSession category Playback + setActive(true).
 */
actual object PlayerBridge {
    private var player: AVPlayer? = null
    private var timeObserverToken: Any? = null
    private var endObserverToken: Any? = null

    private var currentSpeed: Float = 1.0f
    private var currentVolume: Float = 1.0f

    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    actual val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    actual val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onTrackEnd: (() -> Unit)? = null

    private var pendingArtworkUri: String? = null

    private fun configureAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            memScopedForSession(session)
        } catch (_: Exception) {
        }
    }

    private fun ensurePlayer(): AVPlayer {
        var p = player
        if (p == null) {
            configureAudioSession()
            p = AVPlayer.playerWithPlayerItem(null)
            player = p
            val strong = p
            timeObserverToken = strong.addPeriodicTimeObserverForInterval(
                CMTimeMakeWithSeconds(0.25, 600),
                null
            ) {
                poll(strong)
            }
        }
        return p
    }

    private fun poll(p: AVPlayer) {
        try {
            val seconds = CMTimeGetSeconds(p.currentTime())
            if (seconds.isFinite() && seconds >= 0.0) {
                _positionMs.value = (seconds * 1000.0).toLong().coerceAtLeast(0L)
            }
            val item = p.currentItem
            if (item != null) {
                if (item.status == AVPlayerItemStatusReadyToPlay) {
                    if (_isBuffering.value) _isBuffering.value = false
                    val durSeconds = CMTimeGetSeconds(item.duration)
                    if (durSeconds.isFinite() && durSeconds > 0.0) {
                        _durationMs.value = (durSeconds * 1000.0).toLong().coerceAtLeast(0L)
                    }
                } else {
                    if (!_isBuffering.value) _isBuffering.value = true
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun observeTrackEnd(item: AVPlayerItem) {
        try {
            endObserverToken?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        } catch (_: Exception) {
        }
        endObserverToken = null
        try {
            endObserverToken = NSNotificationCenter.defaultCenter.addObserverForName(
                AVPlayerItemDidPlayToEndTimeNotification,
                item,
                null
            ) {
                try {
                    onTrackEnd?.invoke()
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    actual fun play(url: String, startPositionMs: Long) {
        try {
            _errorMessage.value = null
            _isBuffering.value = true
            _positionMs.value = 0L
            _durationMs.value = 0L
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                _isBuffering.value = false
                _errorMessage.value = "Invalid audio URL"
                return
            }
            configureAudioSession()
            val p = ensurePlayer()
            val item = AVPlayerItem.playerItemWithURL(nsUrl)
            observeTrackEnd(item)
            p.replaceCurrentItemWithPlayerItem(item)
            p.volume = currentVolume
            if (startPositionMs > 0L) {
                p.seekToTime(CMTimeMakeWithSeconds(startPositionMs / 1000.0, 600))
            }
            p.play()
            p.rate = currentSpeed
        } catch (e: Exception) {
            _isBuffering.value = false
            _errorMessage.value = e.message ?: "Unable to start playback"
        }
    }

    actual fun prepare(url: String, startPositionMs: Long) {
        try {
            _errorMessage.value = null
            _isBuffering.value = true
            _positionMs.value = startPositionMs.coerceAtLeast(0L)
            _durationMs.value = 0L
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                _isBuffering.value = false
                _errorMessage.value = "Invalid audio URL"
                return
            }
            configureAudioSession()
            val p = ensurePlayer()
            val item = AVPlayerItem.playerItemWithURL(nsUrl)
            observeTrackEnd(item)
            p.replaceCurrentItemWithPlayerItem(item)
            p.volume = currentVolume
            if (startPositionMs > 0L) {
                p.seekToTime(CMTimeMakeWithSeconds(startPositionMs / 1000.0, 600))
            }
            // NOTE: no p.play() — stays paused (silent preload, no autoplay).
        } catch (e: Exception) {
            _isBuffering.value = false
            _errorMessage.value = e.message ?: "Unable to load playback"
        }
    }

    actual fun pause() {
        try {
            player?.pause()
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun resume() {
        try {
            _errorMessage.value = null
            configureAudioSession()
            val p = ensurePlayer()
            p.play()
            p.rate = currentSpeed
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun stop() {
        try {
            player?.pause()
            player?.replaceCurrentItemWithPlayerItem(null)
        } catch (_: Exception) {
        }
        try {
            endObserverToken?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        } catch (_: Exception) {
        }
        endObserverToken = null
        _positionMs.value = 0L
        _isBuffering.value = false
    }

    actual fun seekTo(positionMs: Long) {
        try {
            val seconds = positionMs.coerceAtLeast(0L) / 1000.0
            player?.seekToTime(CMTimeMakeWithSeconds(seconds, 600))
            _positionMs.value = positionMs.coerceAtLeast(0L)
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 3.0f)
        try {
            player?.rate = currentSpeed
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            player?.volume = currentVolume
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }

    actual fun setOnTrackEndListener(listener: (() -> Unit)?) {
        onTrackEnd = listener
    }

    actual fun setPlaybackMetadata(title: String?, artist: String?) {
        // iOS NowPlayingInfo is out of scope for this fix; no-op.
    }

    actual fun setPlaybackArtworkUri(uri: String?) {
        pendingArtworkUri = uri
        // AVPlayerItem externalMetadata artwork is out of scope; no-op.
    }
}
