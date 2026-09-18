package com.quranify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val tracks: List<TrackItem> = emptyList(),
    val coverUrl: String? = null
) {
    val itemCount: Int get() = tracks.size
    val totalPlaytimeMs: Long get() = tracks.sumOf { it.durationMs }
}

@Serializable
data class KhatmaPlan(
    val id: String,
    val title: String,
    val targetDays: Int,
    val currentSurahId: Int = 1,
    val currentAyahNo: Int = 1,
    val streak: Int = 0,
    val startDateMs: Long,
    val lastProgressMs: Long = 0L,
    val totalAyahsRead: Int = 0
) {
    val progressPercentage: Float
        get() = (totalAyahsRead.toFloat() / 6236f).coerceIn(0f, 1f)

    val daysRemaining: Int
        get() = maxOf(0, targetDays - ((lastProgressMs - startDateMs) / (1000 * 60 * 60 * 24)).toInt())
}

@Serializable
data class PlayHistory(
    val track: TrackItem,
    val timestampMs: Long
)
