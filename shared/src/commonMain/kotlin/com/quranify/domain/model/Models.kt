package com.quranify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Surah(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val transliteration: String,
    val ayahsCount: Int,
    val revelationType: String,
    val meaning: String = ""
)

@Serializable
data class Ayah(
    val surahId: Int,
    val ayahNo: Int,
    val textUthmani: String,
    val juz: Int = 1,
    val page: Int = 1
)

@Serializable
data class Reciter(
    val slug: String,
    val nameEn: String,
    val nameAr: String,
    val riwayah: String = "Hafs",
    val style: String = "murattal",
    val tempo: String = "medium",
    val imageUrl: String? = null,
    val audioFolder: String
) {
    fun getAyahAudioUrl(surahId: Int, ayahNo: Int): String {
        val s = surahId.toString().padStart(3, '0')
        val a = ayahNo.toString().padStart(3, '0')
        return "https://everyayah.com/data/$audioFolder/$s$a.mp3"
    }
}

enum class RoutineModeType(val title: String, val subtitle: String) {
    STUDY("Quran for Study", "Steady recitation + ambient sound to boost concentration"),
    WORK("Quran for Work", "Deep focus & mindful productivity"),
    SLEEP("Quran for Sleep", "Calming recitations & relaxing soundscapes")
}

enum class AmbientType(val displayName: String) {
    RAIN("Rain"),
    OCEAN("Ocean Waves"),
    BIRDS("Birdsong"),
    WIND("Gentle Wind"),
    STREAM("Forest Stream"),
    NIGHT("Night Crickets")
}

@Serializable
data class TrackItem(
    val reciterSlug: String,
    val reciterName: String,
    val surahId: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val ayahNo: Int,
    val audioUrl: String,
    val textUthmani: String = "",
    val durationMs: Long = 0L
)

enum class RepeatMode {
    OFF,
    AYAH,
    SURAH,
    QUEUE
}
