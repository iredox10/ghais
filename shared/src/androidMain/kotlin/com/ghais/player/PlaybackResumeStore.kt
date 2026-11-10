package com.ghais.player

import android.content.Context
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.isFullSurah
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
        AudioEngine.currentTrack.value?.let { track ->
            // Service destruction may release its player while the process and
            // engine queue survive. Rebuild only the missing native media item.
            val player = PlayerBridge.playerOrNull()
            if (player != null && player.mediaItemCount == 0) {
                val uri = if (track.ayahNo > 0 && !track.isFullSurah) {
                    QuranDownloads.localAyahUri(track.reciterSlug, track.surahId, track.ayahNo)
                        ?: QuranDownloads.localUri(track.reciterSlug, track.surahId)
                        ?: track.audioUrl
                } else QuranDownloads.localUri(track.reciterSlug, track.surahId) ?: track.audioUrl
                PlayerBridge.setSpeed(AudioEngine.playbackSpeed.value)
                PlayerBridge.setPlaybackArtworkUri(runCatching {
                    com.ghais.ui.screens.reciters.resolveReciterPhoto(track.reciterSlug)
                }.getOrNull())
                PlayerBridge.prepareWithMetadata(uri, AudioEngine.trackDisplayTitle(track), track.reciterName,
                    AudioEngine.currentPositionMs.value)
            }
            return
        }
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
