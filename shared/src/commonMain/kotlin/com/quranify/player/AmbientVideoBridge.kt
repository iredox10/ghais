package com.quranify.player

/**
 * Muted looping background-video player (fullscreen layer in Now Playing).
 * Single-select: at most one [playVideos] list active; empty list = stop.
 * All calls idempotent.
 *
 * Asset keys map to `androidApp/src/main/assets/videos/<key>.mp4` on Android.
 * iOS is a no-op fallback.
 */
expect object AmbientVideoBridge {
    /** Start (or switch to) looping these asset keys in rotation with crossfade. */
    fun playVideos(assetKeys: List<String>)
    fun pauseVideos()
    fun resumeVideos()
    fun stopVideos()
}
