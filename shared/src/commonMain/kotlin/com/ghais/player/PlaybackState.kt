package com.ghais.player

import com.ghais.domain.model.RepeatMode
import com.ghais.domain.model.TrackItem

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

data class CurrentTrackInfo(
    val track: TrackItem?,
    val progressMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val queueIndex: Int = -1
)

data class PlaybackSettings(
    val speed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false
)

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrackInfo: CurrentTrackInfo = CurrentTrackInfo(null, 0L, 0L, false),
    val settings: PlaybackSettings = PlaybackSettings()
)
