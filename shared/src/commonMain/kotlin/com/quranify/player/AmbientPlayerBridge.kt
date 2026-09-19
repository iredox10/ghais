package com.quranify.player

/**
 * Dedicated looping player for background ambience (rain, waves, …).
 *
 * Runs on a SECOND native player instance so it mixes under the Quran
 * recitation instead of replacing it. Files live in
 * `composeResources/files/ambient/<assetKey>.mp3` (shared, both platforms).
 *
 * All calls are idempotent: [playAmbient] with the already-loaded key just
 * resumes; pause/resume never restart the loop from the top.
 */
expect object AmbientPlayerBridge {
    /** Load (if needed) + start looping [assetKey] (e.g. "rain"). */
    fun playAmbient(assetKey: String)
    fun pauseAmbient()
    fun resumeAmbient()
    fun stopAmbient()
    fun setAmbientVolume(volume: Float)
}
