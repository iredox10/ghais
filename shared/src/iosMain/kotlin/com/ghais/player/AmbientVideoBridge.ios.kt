package com.ghais.player

/**
 * iOS renders no ambient video (glow fallback).
 */
actual object AmbientVideoBridge {
    actual fun playVideos(assetKeys: List<String>) {}
    actual fun pauseVideos() {}
    actual fun resumeVideos() {}
    actual fun stopVideos() {}
}
