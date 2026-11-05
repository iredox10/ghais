package com.ghais.player

import android.content.Context
import com.ghais.domain.model.TrackItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Local resume point for a service restarted without an Activity. Never autoplays. */
object PlaybackResumeStore {
    private val json = Json { ignoreUnknownKeys = true }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("playback_resume", Context.MODE_PRIVATE)
        val track = AudioEngine.currentTrack.value
        if (track == null) {
            prefs.edit().clear().apply()
        } else {
            prefs.edit()
                .putString("track", json.encodeToString(track))
                .putLong("position", AudioEngine.currentPositionMs.value)
                .apply()
        }
    }

    fun restore(context: Context) {
        if (AudioEngine.currentTrack.value != null) return
        val prefs = context.getSharedPreferences("playback_resume", Context.MODE_PRIVATE)
        val encoded = prefs.getString("track", null) ?: return
        try {
            AudioEngine.prepareTrack(json.decodeFromString<TrackItem>(encoded), prefs.getLong("position", 0L))
        } catch (_: Exception) {
            // A removed local file or an older incompatible snapshot is disposable.
            prefs.edit().clear().apply()
        }
    }
}
