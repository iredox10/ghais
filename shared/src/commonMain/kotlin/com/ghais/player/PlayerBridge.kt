package com.ghais.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Real audio playback bridge — expect/actual.
 *
 * commonMain declares the contract; androidMain drives Media3 ExoPlayer,
 * iosMain is a compiling AVPlayer stub (TODO: full AVPlayer wiring).
 *
 * Streaming sources: everyayah.com per-ayah mp3 + mp3quran per-surah streams.
 */
expect object PlayerBridge {
    /** Current playback position in ms (polled from the real player). */
    val positionMs: StateFlow<Long>

    /** Current track duration in ms, 0 while unknown/loading. */
    val durationMs: StateFlow<Long>

    /** True while the real player is buffering/loading. */
    val isBuffering: StateFlow<Boolean>

    /** Last player error message, null when healthy. */
    val errorMessage: StateFlow<String?>

    fun play(url: String)
    /** Hint for lockscreen/notification title; consumed atomically by the next play() (no race). */
    fun setPlaybackMetadata(title: String?, artist: String?)
    /**
     * Hint for lockscreen/notification artwork; consumed atomically by the next play().
     * Null clears the pending artwork.
     */
    fun setPlaybackArtworkUri(uri: String?)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setOnTrackEndListener(listener: (() -> Unit)?)
}
