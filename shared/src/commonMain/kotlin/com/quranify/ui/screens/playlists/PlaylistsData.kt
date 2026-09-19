package com.quranify.ui.screens.playlists

enum class PlaylistArt {
    FAVOURITES,
    FOCUS_WORK,
    BEAUTIFUL,
    SLEEP,
    DUAA_RUQIA,
    EMOTIONAL,
    STUDY,
    TAHAJJUD,
    SUNRISE
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
        id = "study-focus",
        title = "Deep Focus & Study",
        description = "Focus-oriented recitation with gentle tempo to keep your mind centered and calm during study sessions.",
        art = PlaylistArt.STUDY,
        surahIds = listOf(18, 20, 31, 55, 68, 94, 96)
    ),
    MoodPlaylist(
        id = "focus-work",
        title = "Deep Work & Flow",
        description = "Clarity and deep focus with rhythmic, steady cadence for long, uninterrupted sessions of productive work.",
        art = PlaylistArt.FOCUS_WORK,
        surahIds = listOf(36, 55, 56, 57, 67, 76)
    ),
    MoodPlaylist(
        id = "sleep-mode",
        title = "Restful Sleep & Sakinah",
        description = "Calming, slow-paced night recitations to quiet your heart, bring tranquility, and ease you into restful sleep.",
        art = PlaylistArt.SLEEP,
        surahIds = listOf(32, 67, 76, 89, 112, 113, 114)
    ),
    MoodPlaylist(
        id = "heart-soothing",
        title = "Heart Soothing & Peace",
        description = "Verses of divine solace, hope, and reassurance that soften the soul and dissolve worries.",
        art = PlaylistArt.BEAUTIFUL,
        surahIds = listOf(12, 13, 19, 93, 94)
    ),
    MoodPlaylist(
        id = "duaa-ruqia",
        title = "Ayat Ash-Shifa & Healing",
        description = "The healing and protective verses (Ayat ash-Shifa) for spiritual wellness, peace of mind, and divine protection.",
        art = PlaylistArt.DUAA_RUQIA,
        surahIds = listOf(1, 2, 10, 17, 26, 112, 113, 114)
    ),
    MoodPlaylist(
        id = "tahajjud",
        title = "Tahajjud & Night Qiyam",
        description = "Deep and soul-stirring recitations curated for the quiet moments of the night and tahajjud prayer.",
        art = PlaylistArt.TAHAJJUD,
        surahIds = listOf(17, 25, 39, 50, 73)
    ),
    MoodPlaylist(
        id = "sunrise-barakah",
        title = "Sunrise Barakah",
        description = "Luminous recitations to start your morning with barakah, mental clarity, and gratitude.",
        art = PlaylistArt.SUNRISE,
        surahIds = listOf(24, 36, 48, 62, 93)
    ),
    MoodPlaylist(
        id = "emotional",
        title = "Emotional Recitations",
        description = "Verses that soften the heart — profound emotional resonance that stirs the heart to contemplation.",
        art = PlaylistArt.EMOTIONAL,
        surahIds = listOf(12, 14, 19, 75, 81, 82)
    ),
    MoodPlaylist(
        id = "most-beautiful",
        title = "Most Beautiful Recitations",
        description = "The most moving voices and verses — timeless recitations loved by millions around the world.",
        art = PlaylistArt.BEAUTIFUL,
        surahIds = listOf(12, 19, 36, 55, 56, 75)
    ),
    MoodPlaylist(
        id = "favourites",
        title = "Favourites",
        description = "The surahs you return to again and again — your most loved recitations, all in one place.",
        art = PlaylistArt.FAVOURITES,
        surahIds = listOf(1, 36, 55, 67, 78, 112, 113, 114)
    )
)

fun moodPlaylistById(id: String): MoodPlaylist =
    MoodPlaylists.firstOrNull { it.id == id } ?: MoodPlaylists.first()
