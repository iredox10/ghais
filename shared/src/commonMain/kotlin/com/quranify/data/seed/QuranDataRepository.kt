package com.quranify.data.seed

import com.quranify.domain.model.TrackItem

/**
 * Detailed reciter information including biography, vocal style, country, riwayah,
 * fan following, photo asset, and primary recitation audio tracks.
 */
data class DetailedReciter(
    val slug: String,
    val nameEn: String,
    val nameAr: String,
    val bio: String,
    val country: String,
    val riwayah: String,
    val style: String,
    val tempo: String,
    val fans: String,
    val photoUrl: String,
    val recitations: List<RecitationTrack>
)

/**
 * Recitation track data representation with surah metadata and direct MP3 stream URL.
 */
data class RecitationTrack(
    val surahNumber: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val ayahCount: Int,
    val duration: String,
    val audioUrl: String
)

/**
 * Track inside a curated playlist with surah, reciter name, duration, and direct MP3 audio URL.
 */
data class CuratedTrack(
    val surahNumber: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val reciterName: String,
    val duration: String,
    val audioUrl: String
)

/**
 * Detailed curated playlist collection model.
 */
data class DetailedCuratedPlaylist(
    val id: String,
    val title: String,
    val tag: String,
    val subtitle: String,
    val description: String,
    val curator: String,
    val totalDuration: String,
    val coverUrl: String,
    val tracks: List<CuratedTrack>
)

/**
 * Convert [RecitationTrack] to domain [TrackItem] for playback engine and queue manager.
 */
fun RecitationTrack.toTrackItem(reciter: DetailedReciter): TrackItem {
    return TrackItem(
        reciterSlug = reciter.slug,
        reciterName = reciter.nameEn,
        surahId = surahNumber,
        surahNameEn = surahNameEn,
        surahNameAr = surahNameAr,
        ayahNo = 1,
        audioUrl = audioUrl,
        textUthmani = "",
        durationMs = parseDurationStringToMs(duration)
    )
}

/**
 * Convert [CuratedTrack] to domain [TrackItem] for playback engine and queue manager.
 */
fun CuratedTrack.toTrackItem(reciterSlug: String = ""): TrackItem {
    return TrackItem(
        reciterSlug = reciterSlug,
        reciterName = reciterName,
        surahId = surahNumber,
        surahNameEn = surahNameEn,
        surahNameAr = surahNameAr,
        ayahNo = 1,
        audioUrl = audioUrl,
        textUthmani = "",
        durationMs = parseDurationStringToMs(duration)
    )
}

private fun parseDurationStringToMs(durationStr: String): Long {
    val parts = durationStr.split(":").mapNotNull { it.trim().toLongOrNull() }
    return when (parts.size) {
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
        2 -> (parts[0] * 60 + parts[1]) * 1000L
        else -> 0L
    }
}

/**
 * Unified, rich seed data repository for reciters, audio tracks, and curated collections.
 */
object QuranDataRepository {

    // =========================================================================
    // 1. DETAILED RECITERS
    // =========================================================================

    val reciters: List<DetailedReciter> = listOf(
        // 1. Mishary Rashid Alafasy
        DetailedReciter(
            slug = "mishary",
            nameEn = "Mishary Rashid Alafasy",
            nameAr = "مشاري راشد العفاسي",
            bio = "Internationally renowned Kuwaiti Qari, Imam of the Grand Mosque of Kuwait, celebrated worldwide for his melodic, emotive, and crystal-clear Murattal recitations that have inspired millions across the globe.",
            country = "Kuwait",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Medium",
            fans = "4.8M fans",
            photoUrl = StitchAssets.VerifiedReciters[0].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:52", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "26:45", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "14:12", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "11:20", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/55.mp3"),
                RecitationTrack(56, "Al-Waqi'ah", "الواقعة", 96, "10:15", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/56.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:48", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/67.mp3"),
                RecitationTrack(112, "Al-Ikhlas", "الإخلاص", 4, "0:24", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/112.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:35", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/114.mp3")
            )
        ),

        // 2. Abdur-Rahman As-Sudais
        DetailedReciter(
            slug = "al-sudais",
            nameEn = "Abdur-Rahman As-Sudais",
            nameAr = "عبد الرحمن السديس",
            bio = "Chief Imam and Khateeb of the Grand Mosque (Masjid al-Haram) in Makkah, and President of the General Presidency for the Affairs of the Two Holy Mosques. Iconic for his heartfelt, impassioned, and soul-stirring Taraweeh recitations.",
            country = "Saudi Arabia",
            riwayah = "Hafs 'an 'Asim",
            style = "Taraweeh",
            tempo = "Fast",
            fans = "3.9M fans",
            photoUrl = StitchAssets.VerifiedReciters[1].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:48", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/1.mp3"),
                RecitationTrack(2, "Al-Baqarah", "البقرة", 286, "1:52:10", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/2.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "22:15", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "11:50", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "9:30", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "5:45", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/67.mp3"),
                RecitationTrack(78, "An-Naba", "النبأ", 40, "4:15", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/78.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:30", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/114.mp3")
            )
        ),

        // 3. Maher Al-Muaiqly
        DetailedReciter(
            slug = "al-muaiqly",
            nameEn = "Maher Al-Muaiqly",
            nameAr = "ماهر المعيقلي",
            bio = "Beloved Imam of Masjid al-Haram in Makkah, renowned for his serene, warm, and deeply emotional voice that brings tranquility and spiritual warmth to listeners during daily prayers and Tahajjud.",
            country = "Saudi Arabia",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Medium",
            fans = "3.2M fans",
            photoUrl = StitchAssets.VerifiedReciters[2].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:50", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/1.mp3"),
                RecitationTrack(12, "Yusuf", "يوسف", 111, "28:30", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/12.mp3"),
                RecitationTrack(19, "Maryam", "مريم", 98, "19:40", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/19.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "13:25", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "10:45", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:10", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/67.mp3"),
                RecitationTrack(93, "Ad-Duha", "الضحى", 11, "0:55", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/93.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:32", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/114.mp3")
            )
        ),

        // 4. Yasser Al-Dossari
        DetailedReciter(
            slug = "al-dossari",
            nameEn = "Yasser Al-Dossari",
            nameAr = "ياسر الدوسري",
            bio = "Imam of Masjid al-Haram in Makkah, renowned for his majestic, resounding vocal resonance, unmatched breath control, and powerful maqam modulations that stir the hearts in Qiyam prayers.",
            country = "Saudi Arabia",
            riwayah = "Hafs 'an 'Asim",
            style = "Taraweeh",
            tempo = "Medium",
            fans = "2.7M fans",
            photoUrl = StitchAssets.VerifiedReciters[3].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:55", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "24:10", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/18.mp3"),
                RecitationTrack(32, "As-Sajdah", "السجدة", 30, "7:15", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/32.mp3"),
                RecitationTrack(50, "Qaf", "ق", 45, "8:40", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/50.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "11:05", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:20", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/67.mp3"),
                RecitationTrack(89, "Al-Fajr", "الفجر", 30, "3:50", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/89.mp3")
            )
        ),

        // 5. Islam Sobhi
        DetailedReciter(
            slug = "islam-sobhi",
            nameEn = "Islam Sobhi",
            nameAr = "إسلام صبحي",
            bio = "Young Egyptian reciter whose uniquely soft, deep, and reflective recitations went viral globally, bringing immense calm and serenity to the youth and millions meditating upon the Quran.",
            country = "Egypt",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Slow",
            fans = "2.1M fans",
            photoUrl = StitchAssets.VerifiedReciters[4].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:05", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "29:10", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/18.mp3"),
                RecitationTrack(19, "Maryam", "مريم", 98, "24:30", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/19.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "16:45", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/36.mp3"),
                RecitationTrack(50, "Qaf", "ق", 45, "10:20", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/50.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:40", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/67.mp3"),
                RecitationTrack(79, "An-Nazi'at", "النازعات", 46, "5:30", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/79.mp3")
            )
        ),

        // 6. Omar Hisham Al Arabi
        DetailedReciter(
            slug = "omar-hisham",
            nameEn = "Omar Hisham Al Arabi",
            nameAr = "عمر هشام العربي",
            bio = "Modern pioneer of cinematic, crisp, and studio-grade Quranic audio productions with an intimate and soothing vocal style favored by millions for study, deep focus, and daily contemplation.",
            country = "Egypt",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Slow",
            fans = "1.5M fans",
            photoUrl = StitchAssets.VerifiedReciters[5].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:02", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "28:50", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "15:30", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "12:15", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:10", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/67.mp3"),
                RecitationTrack(71, "Nuh", "نوح", 28, "5:15", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/71.mp3"),
                RecitationTrack(87, "Al-A'la", "الأعلى", 19, "1:55", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/87.mp3")
            )
        ),

        // 7. Abdul Basit Abdul Samad
        DetailedReciter(
            slug = "abdul-basit",
            nameEn = "Abdul Basit Abdul Samad",
            nameAr = "عبد الباسط عبد الصمد",
            bio = "The Golden Throat of Egypt and the legendary undisputed master of Tajweed. His iconic Mujawwad recitations with extraordinary breath control and majestic melodic heights defined the golden era of Quran recitation.",
            country = "Egypt",
            riwayah = "Hafs 'an 'Asim",
            style = "Mujawwad",
            tempo = "Slow",
            fans = "5.1M fans",
            photoUrl = StitchAssets.VerifiedReciters[6].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:15", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/1.mp3"),
                RecitationTrack(12, "Yusuf", "يوسف", 111, "42:00", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/12.mp3"),
                RecitationTrack(19, "Maryam", "مريم", 98, "36:20", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/19.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "19:45", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "11:30", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/67.mp3"),
                RecitationTrack(89, "Al-Fajr", "الفجر", 30, "7:15", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/89.mp3"),
                RecitationTrack(93, "Ad-Duha", "الضحى", 11, "2:40", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/93.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "1:10", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/114.mp3")
            )
        ),

        // 8. Mahmoud Khalil Al-Husary
        DetailedReciter(
            slug = "al-husary",
            nameEn = "Mahmoud Khalil Al-Husary",
            nameAr = "محمود خليل الحصري",
            bio = "Shaykh al-Maqari of Egypt and globally revered as the ultimate benchmark of flawless Tajweed, classical Tartil cadence, and pedagogical precision. First Qari to record the complete Mushaf Mu'allam.",
            country = "Egypt",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Slow",
            fans = "3.5M fans",
            photoUrl = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=600&q=80",
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:00", "https://cdn.islamic.network/quran/audio/128/ar.husary/1.mp3"),
                RecitationTrack(2, "Al-Baqarah", "البقرة", 286, "2:10:00", "https://cdn.islamic.network/quran/audio/128/ar.husary/2.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "28:15", "https://cdn.islamic.network/quran/audio/128/ar.husary/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "15:40", "https://cdn.islamic.network/quran/audio/128/ar.husary/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "13:20", "https://cdn.islamic.network/quran/audio/128/ar.husary/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:25", "https://cdn.islamic.network/quran/audio/128/ar.husary/67.mp3"),
                RecitationTrack(112, "Al-Ikhlas", "الإخلاص", 4, "0:30", "https://cdn.islamic.network/quran/audio/128/ar.husary/112.mp3")
            )
        ),

        // 9. Saud Al-Shuraim
        DetailedReciter(
            slug = "saud-shuraim",
            nameEn = "Saud Al-Shuraim",
            nameAr = "سعود الشريم",
            bio = "Former longtime Dean of Islamic Judicial Studies at Umm Al-Qura University and illustrious Imam of Masjid al-Haram for over three decades, famed for his brisk, scholarly, and rhythmic cadence during Ramadan Taraweeh.",
            country = "Saudi Arabia",
            riwayah = "Hafs 'an 'Asim",
            style = "Taraweeh",
            tempo = "Fast",
            fans = "2.9M fans",
            photoUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=600&q=80",
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:45", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "21:30", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "11:15", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "9:10", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/55.mp3"),
                RecitationTrack(56, "Al-Waqi'ah", "الواقعة", 96, "8:45", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/56.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "5:30", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/67.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:30", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/114.mp3")
            )
        ),

        // 10. Saad Al-Ghamdi
        DetailedReciter(
            slug = "saad-alghamdi",
            nameEn = "Saad Al-Ghamdi",
            nameAr = "سعد الغامدي",
            bio = "Celebrated Saudi Imam and scholar whose smooth, resonant, and tranquil vocal timbre is beloved worldwide, especially cherished by students of the Quran for its comforting cadence.",
            country = "Saudi Arabia",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Medium",
            fans = "2.4M fans",
            photoUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=600&q=80",
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:52", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "24:50", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "13:40", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "10:55", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:15", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/67.mp3"),
                RecitationTrack(71, "Nuh", "نوح", 28, "4:50", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/71.mp3"),
                RecitationTrack(112, "Al-Ikhlas", "الإخلاص", 4, "0:25", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/112.mp3")
            )
        ),

        // 11. Mohamed Siddiq Al-Minshawi
        DetailedReciter(
            slug = "al-minshawi",
            nameEn = "Mohamed Siddiq Al-Minshawi",
            nameAr = "محمد صديق المنشاوي",
            bio = "Known as 'The Weeping Voice' (Al-Sawt Al-Baki) of Egypt, renowned for his incomparably sorrowful, heartfelt, and deeply sincere recitation that moved generations to tears.",
            country = "Egypt",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Slow",
            fans = "4.2M fans",
            photoUrl = "https://images.unsplash.com/photo-1591604466107-ec97de577aff?w=600&q=80",
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:02", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/1.mp3"),
                RecitationTrack(12, "Yusuf", "يوسف", 111, "34:10", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/12.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "27:40", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "15:20", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "12:50", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:15", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/67.mp3"),
                RecitationTrack(89, "Al-Fajr", "الفجر", 30, "4:30", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/89.mp3")
            )
        ),

        // 12. Ahmed ibn Ali Al-Ajamy
        DetailedReciter(
            slug = "ahmed-alajamy",
            nameEn = "Ahmed ibn Ali Al-Ajamy",
            nameAr = "أحمد بن علي العجمي",
            bio = "Saudi Qari famous for his deep, emotive, and distinctively rich vocal delivery that provides an intense spiritual and reflective experience across mosques and gatherings.",
            country = "Saudi Arabia",
            riwayah = "Hafs 'an 'Asim",
            style = "Murattal",
            tempo = "Medium",
            fans = "2.3M fans",
            photoUrl = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=600&q=80",
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:56", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/1.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "25:30", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/18.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "14:05", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/36.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "11:40", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/55.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:35", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/67.mp3"),
                RecitationTrack(113, "Al-Falaq", "الفلق", 5, "0:30", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/113.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:35", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/114.mp3")
            )
        )
    )

    // =========================================================================
    // 2. DETAILED CURATED PLAYLISTS (8 Collections)
    // =========================================================================

    val curatedPlaylists: List<DetailedCuratedPlaylist> = listOf(
        // 1. Deep Focus & Study
        DetailedCuratedPlaylist(
            id = "deep-focus-study",
            title = "Deep Focus & Study",
            tag = "Tartil",
            subtitle = "Calm, slow tempo recitation",
            description = "Carefully calibrated, slow-tempo Tartil recitations designed to cultivate a state of stillness, flow, and deep intellectual focus during reading, study, and contemplation.",
            curator = "Quranify Editorial",
            totalDuration = "52 mins",
            coverUrl = StitchAssets.CuratedForPeace[0].coverUrl,
            tracks = listOf(
                CuratedTrack(67, "Al-Mulk", "الملك", "Mahmoud Khalil Al-Husary", "7:25", "https://cdn.islamic.network/quran/audio/128/ar.husary/67.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mohamed Siddiq Al-Minshawi", "15:20", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/36.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Omar Hisham Al Arabi", "12:15", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/55.mp3"),
                CuratedTrack(18, "Al-Kahf", "الكهف", "Islam Sobhi", "29:10", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/18.mp3")
            )
        ),

        // 2. Heart Soothing
        DetailedCuratedPlaylist(
            id = "heart-soothing",
            title = "Heart Soothing",
            tag = "Emotional",
            subtitle = "Comforting verses of mercy",
            description = "Verses highlighting Allah's boundless mercy, forgiveness, and unconditional love, recited by masters of emotional expression to bring comfort to weary hearts.",
            curator = "Quranify Peace Team",
            totalDuration = "48 mins",
            coverUrl = StitchAssets.CuratedForPeace[1].coverUrl,
            tracks = listOf(
                CuratedTrack(12, "Yusuf", "يوسف", "Maher Al-Muaiqly", "28:30", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/12.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Mishary Rashid Alafasy", "11:20", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/55.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Abdul Basit Abdul Samad", "2:40", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/93.mp3"),
                CuratedTrack(19, "Maryam", "مريم", "Islam Sobhi", "24:30", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/19.mp3")
            )
        ),

        // 3. Morning Adhkar
        DetailedCuratedPlaylist(
            id = "morning-adhkar",
            title = "Morning Adhkar",
            tag = "Morning",
            subtitle = "Protection & Barakah",
            description = "Morning protection Surahs and invocations to begin your day under the shade of Divine guidance, tranquility, and abundant blessing.",
            curator = "Imams of Haramain",
            totalDuration = "35 mins",
            coverUrl = StitchAssets.CuratedForPeace[2].coverUrl,
            tracks = listOf(
                CuratedTrack(1, "Al-Fatihah", "الفاتحة", "Mishary Rashid Alafasy", "0:52", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mishary Rashid Alafasy", "14:12", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/36.mp3"),
                CuratedTrack(112, "Al-Ikhlas", "الإخلاص", "Mahmoud Khalil Al-Husary", "0:30", "https://cdn.islamic.network/quran/audio/128/ar.husary/112.mp3"),
                CuratedTrack(113, "Al-Falaq", "الفلق", "Ahmed ibn Ali Al-Ajamy", "0:30", "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/113.mp3"),
                CuratedTrack(114, "An-Nas", "الناس", "Saud Al-Shuraim", "0:30", "https://cdn.islamic.network/quran/audio/128/ar.saoodshuraym/114.mp3")
            )
        ),

        // 4. Bedtime Sakinah
        DetailedCuratedPlaylist(
            id = "bedtime-sakinah",
            title = "Bedtime Sakinah",
            tag = "Sleep",
            subtitle = "Gentle sleep timer mix",
            description = "Soft, tranquil recitations curated specifically to ease your mind into peaceful slumber under the protection of the Word of Allah.",
            curator = "Quranify Editorial",
            totalDuration = "42 mins",
            coverUrl = StitchAssets.CuratedForPeace[3].coverUrl,
            tracks = listOf(
                CuratedTrack(67, "Al-Mulk", "الملك", "Mishary Rashid Alafasy", "6:48", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/67.mp3"),
                CuratedTrack(32, "As-Sajdah", "السجدة", "Yasser Al-Dossari", "7:15", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/32.mp3"),
                CuratedTrack(56, "Al-Waqi'ah", "الواقعة", "Saad Al-Ghamdi", "8:45", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/56.mp3"),
                CuratedTrack(71, "Nuh", "نوح", "Omar Hisham Al Arabi", "5:15", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/71.mp3")
            )
        ),

        // 5. Tahajjud Peace
        DetailedCuratedPlaylist(
            id = "tahajjud-peace",
            title = "Tahajjud Peace",
            tag = "Qiyam",
            subtitle = "Heart softeners in the late night",
            description = "Deeply moving recitations from the depths of the night when prayers are answered and hearts connect intimately with their Creator.",
            curator = "Mishary Alafasy & Friends",
            totalDuration = "1 hr 15 mins",
            coverUrl = StitchAssets.JumpBackInItems[3].coverUrl,
            tracks = listOf(
                CuratedTrack(50, "Qaf", "ق", "Yasser Al-Dossari", "8:40", "https://cdn.islamic.network/quran/audio/128/ar.yasseraldossari/50.mp3"),
                CuratedTrack(19, "Maryam", "مريم", "Maher Al-Muaiqly", "19:40", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/19.mp3"),
                CuratedTrack(89, "Al-Fajr", "الفجر", "Abdul Basit Abdul Samad", "7:15", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/89.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Islam Sobhi", "16:45", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/36.mp3")
            )
        ),

        // 6. Jumu'ah Sunnah
        DetailedCuratedPlaylist(
            id = "jumuah-sunnah",
            title = "Jumu'ah Sunnah",
            tag = "Sunnah",
            subtitle = "Friday light & illumination",
            description = "Surah Al-Kahf and Friday blessings recited by renowned Imams to illuminate your week with spiritual light from one Jumu'ah to the next.",
            curator = "Masjid Al-Haram Circle",
            totalDuration = "1 hr 30 mins",
            coverUrl = StitchAssets.JumpBackInItems[1].coverUrl,
            tracks = listOf(
                CuratedTrack(18, "Al-Kahf", "الكهف", "Mishary Rashid Alafasy", "26:45", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/18.mp3"),
                CuratedTrack(18, "Al-Kahf", "الكهف", "Abdur-Rahman As-Sudais", "22:15", "https://cdn.islamic.network/quran/audio/128/ar.abdurrahmaansudais/18.mp3"),
                CuratedTrack(18, "Al-Kahf", "الكهف", "Saad Al-Ghamdi", "24:50", "https://cdn.islamic.network/quran/audio/128/ar.saadalghamdi/18.mp3"),
                CuratedTrack(62, "Al-Jumu'ah", "الجمعة", "Mahmoud Khalil Al-Husary", "3:10", "https://cdn.islamic.network/quran/audio/128/ar.husary/62.mp3")
            )
        ),

        // 7. Anxiety & Relief
        DetailedCuratedPlaylist(
            id = "anxiety-relief",
            title = "Anxiety & Relief",
            tag = "Healing",
            subtitle = "Solace for distressed minds",
            description = "A therapeutic selection of soothing verses reminding us that with every hardship comes ease, alleviating grief, anxiety, and distress.",
            curator = "Quranify Wellness",
            totalDuration = "46 mins",
            coverUrl = StitchAssets.JumpBackInItems[0].coverUrl,
            tracks = listOf(
                CuratedTrack(94, "Ash-Sharh", "الشرح", "Maher Al-Muaiqly", "0:45", "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/94.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Abdul Basit Abdul Samad", "2:40", "https://cdn.islamic.network/quran/audio/128/ar.abdulbasitmujawwad/93.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Islam Sobhi", "7:40", "https://cdn.islamic.network/quran/audio/128/ar.islamsobhi/67.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Mishary Rashid Alafasy", "11:20", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/55.mp3"),
                CuratedTrack(12, "Yusuf", "يوسف", "Mohamed Siddiq Al-Minshawi", "34:10", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/12.mp3")
            )
        ),

        // 8. Hifz & Memorization
        DetailedCuratedPlaylist(
            id = "hifz-memorization",
            title = "Hifz & Memorization",
            tag = "Hifz",
            subtitle = "Clear pronunciation benchmark",
            description = "Paced, articulated, and repetitive-friendly recitations by foundational teachers designed to assist Huffaz in mastering pronunciation and retention.",
            curator = "Al-Azhar Quranic Institute",
            totalDuration = "1 hr 10 mins",
            coverUrl = StitchAssets.JumpBackInItems[2].coverUrl,
            tracks = listOf(
                CuratedTrack(1, "Al-Fatihah", "الفاتحة", "Mahmoud Khalil Al-Husary", "1:00", "https://cdn.islamic.network/quran/audio/128/ar.husary/1.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mahmoud Khalil Al-Husary", "15:40", "https://cdn.islamic.network/quran/audio/128/ar.husary/36.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Mohamed Siddiq Al-Minshawi", "7:15", "https://cdn.islamic.network/quran/audio/128/ar.minshawi/67.mp3"),
                CuratedTrack(87, "Al-A'la", "الأعلى", "Omar Hisham Al Arabi", "1:55", "https://cdn.islamic.network/quran/audio/128/ar.omarhishamalarabi/87.mp3"),
                CuratedTrack(112, "Al-Ikhlas", "الإخلاص", "Mishary Rashid Alafasy", "0:24", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/112.mp3")
            )
        )
    )

    // =========================================================================
    // 3. REPOSITORY QUERY HELPERS
    // =========================================================================

    /**
     * Get list of verified reciters.
     */
    fun getVerifiedReciters(): List<DetailedReciter> = reciters

    /**
     * Find a [DetailedReciter] by slug (case-insensitive).
     */
    fun getReciterBySlug(slug: String): DetailedReciter? {
        val normalized = slug.trim().lowercase()
        return reciters.firstOrNull { 
            it.slug.lowercase() == normalized ||
            it.slug.replace("-", "").lowercase() == normalized.replace("-", "")
        }
    }

    /**
     * Search reciters by text query and category/style filter.
     */
    fun searchReciters(query: String = "", filter: String = "All"): List<DetailedReciter> {
        val q = query.trim().lowercase()
        val f = filter.trim().lowercase()

        return reciters.filter { reciter ->
            val matchesFilter = when {
                f == "all" || f.isEmpty() -> true
                f.contains("murattal") -> reciter.style.equals("Murattal", ignoreCase = true)
                f.contains("mujawwad") -> reciter.style.equals("Mujawwad", ignoreCase = true)
                f.contains("taraweeh") -> reciter.style.equals("Taraweeh", ignoreCase = true)
                f.contains("slow") -> reciter.tempo.equals("Slow", ignoreCase = true)
                f.contains("fast") -> reciter.tempo.equals("Fast", ignoreCase = true)
                else -> reciter.style.contains(f, ignoreCase = true) || reciter.tempo.contains(f, ignoreCase = true)
            }

            val matchesQuery = q.isEmpty() ||
                reciter.nameEn.lowercase().contains(q) ||
                reciter.nameAr.lowercase().contains(q) ||
                reciter.country.lowercase().contains(q) ||
                reciter.slug.lowercase().contains(q)

            matchesFilter && matchesQuery
        }
    }

    /**
     * Find a [DetailedCuratedPlaylist] by playlist ID (case-insensitive).
     */
    fun getPlaylistById(id: String): DetailedCuratedPlaylist? {
        val normalized = id.trim().lowercase()
        return curatedPlaylists.firstOrNull { 
            it.id.lowercase() == normalized ||
            it.title.lowercase() == normalized ||
            it.id.replace("-", "").lowercase() == normalized.replace("-", "")
        }
    }

    /**
     * Alias for [getPlaylistById] to retrieve a curated playlist.
     */
    fun getCuratedPlaylist(playlistId: String): DetailedCuratedPlaylist? = getPlaylistById(playlistId)

    /**
     * Search curated playlists by title, tag, subtitle, or description.
     */
    fun searchPlaylists(query: String): List<DetailedCuratedPlaylist> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return curatedPlaylists

        return curatedPlaylists.filter {
            it.title.lowercase().contains(q) ||
            it.tag.lowercase().contains(q) ||
            it.subtitle.lowercase().contains(q) ||
            it.description.lowercase().contains(q)
        }
    }
}
