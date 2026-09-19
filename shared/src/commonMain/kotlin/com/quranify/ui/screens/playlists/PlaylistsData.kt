package com.quranify.ui.screens.playlists

enum class PlaylistArt {
    FAVOURITES,
    FOCUS_WORK,
    BEAUTIFUL,
    SLEEP,
    DUAA_RUQIA,
    EMOTIONAL
}

data class MoodPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val art: PlaylistArt,
    val surahIds: List<Int>
)

val MoodPlaylists = listOf(
    MoodPlaylist(
        id = "favourites",
        title = "Favourites",
        description = "The surahs you return to again and again — your most loved recitations, all in one place.",
        art = PlaylistArt.FAVOURITES,
        surahIds = listOf(1, 36, 55, 67, 78, 112, 113, 114)
    ),
    MoodPlaylist(
        id = "focus-work",
        title = "Focus & Work",
        description = "Steady, calm recitation that keeps you in deep focus through long hours of work and study.",
        art = PlaylistArt.FOCUS_WORK,
        surahIds = listOf(18, 36, 55, 67, 76, 87, 93, 94)
    ),
    MoodPlaylist(
        id = "most-beautiful",
        title = "Most Beautiful Recitations",
        description = "The most moving voices and verses — timeless recitations loved by millions around the world.",
        art = PlaylistArt.BEAUTIFUL,
        surahIds = listOf(12, 19, 36, 55, 56, 75, 81, 82)
    ),
    MoodPlaylist(
        id = "sleep-mode",
        title = "Sleep Mode",
        description = "Gentle, slow-paced recitation that quiets the heart and carries you softly into sleep.",
        art = PlaylistArt.SLEEP,
        surahIds = listOf(1, 36, 67, 73, 78, 112, 113, 114)
    ),
    MoodPlaylist(
        id = "duaa-ruqia",
        title = "Duaa & Ruqia",
        description = "Verses of protection, healing and heartfelt duaa — comfort for the heart and home.",
        art = PlaylistArt.DUAA_RUQIA,
        surahIds = listOf(1, 2, 17, 36, 55, 112, 113, 114)
    ),
    MoodPlaylist(
        id = "emotional",
        title = "Emotional Recitations",
        description = "Verses that soften the heart — hope, mercy and consolation for heavy days.",
        art = PlaylistArt.EMOTIONAL,
        surahIds = listOf(12, 19, 39, 50, 55, 75, 93, 94)
    )
)

fun moodPlaylistById(id: String): MoodPlaylist =
    MoodPlaylists.firstOrNull { it.id == id } ?: MoodPlaylists.first()
