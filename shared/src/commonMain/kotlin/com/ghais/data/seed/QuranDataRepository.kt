package com.ghais.data.seed

import com.ghais.domain.model.TrackItem

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
    val reciterModel = com.ghais.data.repository.QuranDataRepository.getReciterBySlug(reciter.slug)
    // When the reciter has no audio for this surah, keep the curated seed URL
    // (known-good) instead of a recomputed URL that would 404. Indices untouched.
    val resolvedUrl = if (reciterModel.isSurahAvailable(surahNumber)) {
        reciterModel.getFullSurahUrl(surahNumber).ifEmpty { audioUrl }
    } else {
        audioUrl
    }
    return TrackItem(
        reciterSlug = reciter.slug,
        reciterName = reciter.nameEn,
        surahId = surahNumber,
        surahNameEn = surahNameEn,
        surahNameAr = surahNameAr,
        ayahNo = 0,
        audioUrl = resolvedUrl,
        textUthmani = "",
        durationMs = parseDurationStringToMs(duration)
    )
}

/**
 * Convert [CuratedTrack] to domain [TrackItem] for playback engine and queue manager.
 */
fun CuratedTrack.toTrackItem(reciterSlug: String = ""): TrackItem {
    val reciter = com.ghais.data.repository.QuranDataRepository.getReciterBySlug(
        if (reciterSlug.isNotBlank()) reciterSlug else reciterName
    )
    // When the reciter has no audio for this surah, keep the curated seed URL
    // (known-good) instead of a recomputed URL that would 404. Indices untouched.
    val resolvedUrl = if (reciter.isSurahAvailable(surahNumber)) {
        reciter.getFullSurahUrl(surahNumber).ifEmpty { audioUrl }
    } else {
        audioUrl
    }
    return TrackItem(
        reciterSlug = reciter.slug,
        reciterName = reciterName,
        surahId = surahNumber,
        surahNameEn = surahNameEn,
        surahNameAr = surahNameAr,
        ayahNo = 0,
        audioUrl = resolvedUrl,
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
            photoUrl = GhaisAssets.VerifiedReciters[0].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:52", "https://server8.mp3quran.net/afs/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "26:45", "https://server8.mp3quran.net/afs/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "14:12", "https://server8.mp3quran.net/afs/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "11:20", "https://server8.mp3quran.net/afs/055.mp3"),
                RecitationTrack(56, "Al-Waqi'ah", "الواقعة", 96, "10:15", "https://server8.mp3quran.net/afs/056.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:48", "https://server8.mp3quran.net/afs/067.mp3"),
                RecitationTrack(112, "Al-Ikhlas", "الإخلاص", 4, "0:24", "https://server8.mp3quran.net/afs/112.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:35", "https://server8.mp3quran.net/afs/114.mp3")
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
            photoUrl = GhaisAssets.VerifiedReciters[1].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:48", "https://server11.mp3quran.net/sds/001.mp3"),
                RecitationTrack(2, "Al-Baqarah", "البقرة", 286, "1:52:10", "https://server11.mp3quran.net/sds/002.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "22:15", "https://server11.mp3quran.net/sds/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "11:50", "https://server11.mp3quran.net/sds/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "9:30", "https://server11.mp3quran.net/sds/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "5:45", "https://server11.mp3quran.net/sds/067.mp3"),
                RecitationTrack(78, "An-Naba", "النبأ", 40, "4:15", "https://server11.mp3quran.net/sds/078.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:30", "https://server11.mp3quran.net/sds/114.mp3")
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
            photoUrl = GhaisAssets.VerifiedReciters[2].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:50", "https://server12.mp3quran.net/maher/001.mp3"),
                RecitationTrack(12, "Yusuf", "يوسف", 111, "28:30", "https://server12.mp3quran.net/maher/012.mp3"),
                RecitationTrack(19, "Maryam", "مريم", 98, "19:40", "https://server12.mp3quran.net/maher/019.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "13:25", "https://server12.mp3quran.net/maher/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "10:45", "https://server12.mp3quran.net/maher/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:10", "https://server12.mp3quran.net/maher/067.mp3"),
                RecitationTrack(93, "Ad-Duha", "الضحى", 11, "0:55", "https://server12.mp3quran.net/maher/093.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:32", "https://server12.mp3quran.net/maher/114.mp3")
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
            photoUrl = GhaisAssets.VerifiedReciters[3].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:55", "https://server11.mp3quran.net/yasser/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "24:10", "https://server11.mp3quran.net/yasser/018.mp3"),
                RecitationTrack(32, "As-Sajdah", "السجدة", 30, "7:15", "https://server11.mp3quran.net/yasser/032.mp3"),
                RecitationTrack(50, "Qaf", "ق", 45, "8:40", "https://server11.mp3quran.net/yasser/050.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "11:05", "https://server11.mp3quran.net/yasser/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:20", "https://server11.mp3quran.net/yasser/067.mp3"),
                RecitationTrack(89, "Al-Fajr", "الفجر", 30, "3:50", "https://server11.mp3quran.net/yasser/089.mp3")
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
            photoUrl = GhaisAssets.VerifiedReciters[4].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:05", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "29:10", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/018.mp3"),
                RecitationTrack(19, "Maryam", "مريم", 98, "24:30", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/019.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "16:45", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/036.mp3"),
                RecitationTrack(50, "Qaf", "ق", 45, "10:20", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/050.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:40", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/067.mp3"),
                RecitationTrack(79, "An-Nazi'at", "النازعات", 46, "5:30", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/079.mp3")
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
            photoUrl = GhaisAssets.VerifiedReciters[5].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:02", "https://archive.org/download/Omar-Hisham/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "28:50", "https://archive.org/download/Omar-Hisham/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "15:30", "https://archive.org/download/Omar-Hisham/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "12:15", "https://archive.org/download/Omar-Hisham/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:10", "https://archive.org/download/Omar-Hisham/067.mp3"),
                RecitationTrack(71, "Nuh", "نوح", 28, "5:15", "https://archive.org/download/Omar-Hisham/071.mp3"),
                RecitationTrack(87, "Al-A'la", "الأعلى", 19, "1:55", "https://archive.org/download/Omar-Hisham/087.mp3")
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
            photoUrl = GhaisAssets.VerifiedReciters[6].photoUrl,
            recitations = listOf(
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:15", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/001.mp3"),
                RecitationTrack(12, "Yusuf", "يوسف", 111, "42:00", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/012.mp3"),
                RecitationTrack(19, "Maryam", "مريم", 98, "36:20", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/019.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "19:45", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "11:30", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/067.mp3"),
                RecitationTrack(89, "Al-Fajr", "الفجر", 30, "7:15", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/089.mp3"),
                RecitationTrack(93, "Ad-Duha", "الضحى", 11, "2:40", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/093.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "1:10", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/114.mp3")
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
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:00", "https://server13.mp3quran.net/husr/001.mp3"),
                RecitationTrack(2, "Al-Baqarah", "البقرة", 286, "2:10:00", "https://server13.mp3quran.net/husr/002.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "28:15", "https://server13.mp3quran.net/husr/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "15:40", "https://server13.mp3quran.net/husr/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "13:20", "https://server13.mp3quran.net/husr/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:25", "https://server13.mp3quran.net/husr/067.mp3"),
                RecitationTrack(112, "Al-Ikhlas", "الإخلاص", 4, "0:30", "https://server13.mp3quran.net/husr/112.mp3")
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
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:45", "https://server7.mp3quran.net/shur/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "21:30", "https://server7.mp3quran.net/shur/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "11:15", "https://server7.mp3quran.net/shur/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "9:10", "https://server7.mp3quran.net/shur/055.mp3"),
                RecitationTrack(56, "Al-Waqi'ah", "الواقعة", 96, "8:45", "https://server7.mp3quran.net/shur/056.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "5:30", "https://server7.mp3quran.net/shur/067.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:30", "https://server7.mp3quran.net/shur/114.mp3")
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
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:52", "https://server7.mp3quran.net/s_gmd/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "24:50", "https://server7.mp3quran.net/s_gmd/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "13:40", "https://server7.mp3quran.net/s_gmd/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "10:55", "https://server7.mp3quran.net/s_gmd/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:15", "https://server7.mp3quran.net/s_gmd/067.mp3"),
                RecitationTrack(71, "Nuh", "نوح", 28, "4:50", "https://server7.mp3quran.net/s_gmd/071.mp3"),
                RecitationTrack(112, "Al-Ikhlas", "الإخلاص", 4, "0:25", "https://server7.mp3quran.net/s_gmd/112.mp3")
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
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "1:02", "https://server10.mp3quran.net/minsh/001.mp3"),
                RecitationTrack(12, "Yusuf", "يوسف", 111, "34:10", "https://server10.mp3quran.net/minsh/012.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "27:40", "https://server10.mp3quran.net/minsh/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "15:20", "https://server10.mp3quran.net/minsh/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "12:50", "https://server10.mp3quran.net/minsh/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "7:15", "https://server10.mp3quran.net/minsh/067.mp3"),
                RecitationTrack(89, "Al-Fajr", "الفجر", 30, "4:30", "https://server10.mp3quran.net/minsh/089.mp3")
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
                RecitationTrack(1, "Al-Fatihah", "الفاتحة", 7, "0:56", "https://server10.mp3quran.net/ajm/001.mp3"),
                RecitationTrack(18, "Al-Kahf", "الكهف", 110, "25:30", "https://server10.mp3quran.net/ajm/018.mp3"),
                RecitationTrack(36, "Ya-Sin", "يس", 83, "14:05", "https://server10.mp3quran.net/ajm/036.mp3"),
                RecitationTrack(55, "Ar-Rahman", "الرحمن", 78, "11:40", "https://server10.mp3quran.net/ajm/055.mp3"),
                RecitationTrack(67, "Al-Mulk", "الملك", 30, "6:35", "https://server10.mp3quran.net/ajm/067.mp3"),
                RecitationTrack(113, "Al-Falaq", "الفلق", 5, "0:30", "https://server10.mp3quran.net/ajm/113.mp3"),
                RecitationTrack(114, "An-Nas", "الناس", 6, "0:35", "https://server10.mp3quran.net/ajm/114.mp3")
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
            curator = "Ghais Editorial",
            totalDuration = "52 mins",
            coverUrl = GhaisAssets.CuratedForPeace[0].coverUrl,
            tracks = listOf(
                CuratedTrack(67, "Al-Mulk", "الملك", "Mahmoud Khalil Al-Husary", "7:25", "https://server13.mp3quran.net/husr/067.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mohamed Siddiq Al-Minshawi", "15:20", "https://server10.mp3quran.net/minsh/036.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Omar Hisham Al Arabi", "12:15", "https://archive.org/download/Omar-Hisham/055.mp3"),
                CuratedTrack(18, "Al-Kahf", "الكهف", "Islam Sobhi", "29:10", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/018.mp3")
            )
        ),

        // 2. Heart Soothing
        DetailedCuratedPlaylist(
            id = "heart-soothing",
            title = "Heart Soothing",
            tag = "Emotional",
            subtitle = "Comforting verses of mercy",
            description = "Verses highlighting Allah's boundless mercy, forgiveness, and unconditional love, recited by masters of emotional expression to bring comfort to weary hearts.",
            curator = "Ghais Peace Team",
            totalDuration = "48 mins",
            coverUrl = GhaisAssets.CuratedForPeace[1].coverUrl,
            tracks = listOf(
                CuratedTrack(12, "Yusuf", "يوسف", "Maher Al-Muaiqly", "28:30", "https://server12.mp3quran.net/maher/012.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Mishary Rashid Alafasy", "11:20", "https://server8.mp3quran.net/afs/055.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Abdul Basit Abdul Samad", "2:40", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/093.mp3"),
                CuratedTrack(19, "Maryam", "مريم", "Islam Sobhi", "24:30", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/019.mp3")
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
            coverUrl = GhaisAssets.CuratedForPeace[2].coverUrl,
            tracks = listOf(
                CuratedTrack(1, "Al-Fatihah", "الفاتحة", "Mishary Rashid Alafasy", "0:52", "https://server8.mp3quran.net/afs/001.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mishary Rashid Alafasy", "14:12", "https://server8.mp3quran.net/afs/036.mp3"),
                CuratedTrack(112, "Al-Ikhlas", "الإخلاص", "Mahmoud Khalil Al-Husary", "0:30", "https://server13.mp3quran.net/husr/112.mp3"),
                CuratedTrack(113, "Al-Falaq", "الفلق", "Ahmed ibn Ali Al-Ajamy", "0:30", "https://server10.mp3quran.net/ajm/113.mp3"),
                CuratedTrack(114, "An-Nas", "الناس", "Saud Al-Shuraim", "0:30", "https://server7.mp3quran.net/shur/114.mp3")
            )
        ),

        // 4. Bedtime Sakinah
        DetailedCuratedPlaylist(
            id = "bedtime-sakinah",
            title = "Bedtime Sakinah",
            tag = "Sleep",
            subtitle = "Gentle sleep timer mix",
            description = "Soft, tranquil recitations curated specifically to ease your mind into peaceful slumber under the protection of the Word of Allah.",
            curator = "Ghais Editorial",
            totalDuration = "42 mins",
            coverUrl = GhaisAssets.CuratedForPeace[3].coverUrl,
            tracks = listOf(
                CuratedTrack(67, "Al-Mulk", "الملك", "Mishary Rashid Alafasy", "6:48", "https://server8.mp3quran.net/afs/067.mp3"),
                CuratedTrack(32, "As-Sajdah", "السجدة", "Yasser Al-Dossari", "7:15", "https://server11.mp3quran.net/yasser/032.mp3"),
                CuratedTrack(56, "Al-Waqi'ah", "الواقعة", "Saad Al-Ghamdi", "8:45", "https://server7.mp3quran.net/s_gmd/056.mp3"),
                CuratedTrack(71, "Nuh", "نوح", "Omar Hisham Al Arabi", "5:15", "https://archive.org/download/Omar-Hisham/071.mp3")
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
            coverUrl = GhaisAssets.JumpBackInItems[3].coverUrl,
            tracks = listOf(
                CuratedTrack(50, "Qaf", "ق", "Yasser Al-Dossari", "8:40", "https://server11.mp3quran.net/yasser/050.mp3"),
                CuratedTrack(19, "Maryam", "مريم", "Maher Al-Muaiqly", "19:40", "https://server12.mp3quran.net/maher/019.mp3"),
                CuratedTrack(89, "Al-Fajr", "الفجر", "Abdul Basit Abdul Samad", "7:15", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/089.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Islam Sobhi", "16:45", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/036.mp3")
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
            coverUrl = GhaisAssets.JumpBackInItems[1].coverUrl,
            tracks = listOf(
                CuratedTrack(18, "Al-Kahf", "الكهف", "Mishary Rashid Alafasy", "26:45", "https://server8.mp3quran.net/afs/018.mp3"),
                CuratedTrack(18, "Al-Kahf", "الكهف", "Abdur-Rahman As-Sudais", "22:15", "https://server11.mp3quran.net/sds/018.mp3"),
                CuratedTrack(18, "Al-Kahf", "الكهف", "Saad Al-Ghamdi", "24:50", "https://server7.mp3quran.net/s_gmd/018.mp3"),
                CuratedTrack(62, "Al-Jumu'ah", "الجمعة", "Mahmoud Khalil Al-Husary", "3:10", "https://server13.mp3quran.net/husr/062.mp3")
            )
        ),

        // 7. Anxiety & Relief
        DetailedCuratedPlaylist(
            id = "anxiety-relief",
            title = "Anxiety & Relief",
            tag = "Healing",
            subtitle = "Solace for distressed minds",
            description = "A therapeutic selection of soothing verses reminding us that with every hardship comes ease, alleviating grief, anxiety, and distress.",
            curator = "Ghais Wellness",
            totalDuration = "46 mins",
            coverUrl = GhaisAssets.JumpBackInItems[0].coverUrl,
            tracks = listOf(
                CuratedTrack(94, "Ash-Sharh", "الشرح", "Maher Al-Muaiqly", "0:45", "https://server12.mp3quran.net/maher/094.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Abdul Basit Abdul Samad", "2:40", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/093.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Islam Sobhi", "7:40", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/067.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Mishary Rashid Alafasy", "11:20", "https://server8.mp3quran.net/afs/055.mp3"),
                CuratedTrack(12, "Yusuf", "يوسف", "Mohamed Siddiq Al-Minshawi", "34:10", "https://server10.mp3quran.net/minsh/012.mp3")
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
            coverUrl = GhaisAssets.JumpBackInItems[2].coverUrl,
            tracks = listOf(
                CuratedTrack(1, "Al-Fatihah", "الفاتحة", "Mahmoud Khalil Al-Husary", "1:00", "https://server13.mp3quran.net/husr/001.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mahmoud Khalil Al-Husary", "15:40", "https://server13.mp3quran.net/husr/036.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Mohamed Siddiq Al-Minshawi", "7:15", "https://server10.mp3quran.net/minsh/067.mp3"),
                CuratedTrack(87, "Al-A'la", "الأعلى", "Omar Hisham Al Arabi", "1:55", "https://archive.org/download/Omar-Hisham/087.mp3"),
                CuratedTrack(112, "Al-Ikhlas", "الإخلاص", "Mishary Rashid Alafasy", "0:24", "https://server8.mp3quran.net/afs/112.mp3")
            )
        ),

        // 9. Ayat Ash-Shifa & Healing
        DetailedCuratedPlaylist(
            id = "ayat-ash-shifa",
            title = "Ayat Ash-Shifa & Healing",
            tag = "Healing",
            subtitle = "Sacred verses of spiritual restoration",
            description = "Selected healing Surahs and ruqyah recitations bringing comfort, tranquility, and divine shifa to the soul and physical body.",
            curator = "Ghais Wellness",
            totalDuration = "36 mins",
            coverUrl = GhaisAssets.LibraryTahajjudCover,
            tracks = listOf(
                CuratedTrack(1, "Al-Fatihah", "الفاتحة", "Mishary Rashid Alafasy", "0:52", "https://server8.mp3quran.net/afs/001.mp3"),
                CuratedTrack(94, "Ash-Sharh", "الشرح", "Maher Al-Muaiqly", "0:45", "https://server12.mp3quran.net/maher/094.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Abdul Basit Abdul Samad", "2:40", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/093.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Mishary Rashid Alafasy", "11:20", "https://server8.mp3quran.net/afs/055.mp3"),
                CuratedTrack(112, "Al-Ikhlas", "الإخلاص", "Mahmoud Khalil Al-Husary", "0:30", "https://server13.mp3quran.net/husr/112.mp3"),
                CuratedTrack(113, "Al-Falaq", "الفلق", "Ahmed ibn Ali Al-Ajamy", "0:30", "https://server10.mp3quran.net/ajm/113.mp3"),
                CuratedTrack(114, "An-Nas", "الناس", "Saud Al-Shuraim", "0:30", "https://server7.mp3quran.net/shur/114.mp3")
            )
        ),

        // 10. Mindful Memorization (Hifz)
        DetailedCuratedPlaylist(
            id = "mindful-hifz",
            title = "Mindful Memorization (Hifz)",
            tag = "Focus",
            subtitle = "Rhythmic murattal loops for deep retention",
            description = "Clear, articulate, and rhythmic Tartil recitations designed to anchor verses in memory through repetitive focused listening.",
            curator = "Al-Azhar Quranic Institute",
            totalDuration = "1 hr 10 mins",
            coverUrl = GhaisAssets.JumpBackInItems[2].coverUrl,
            tracks = listOf(
                CuratedTrack(1, "Al-Fatihah", "الفاتحة", "Mahmoud Khalil Al-Husary", "1:00", "https://server13.mp3quran.net/husr/001.mp3"),
                CuratedTrack(87, "Al-A'la", "الأعلى", "Omar Hisham Al Arabi", "1:55", "https://archive.org/download/Omar-Hisham/087.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Mahmoud Khalil Al-Husary", "15:40", "https://server13.mp3quran.net/husr/036.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Mohamed Siddiq Al-Minshawi", "7:15", "https://server10.mp3quran.net/minsh/067.mp3"),
                CuratedTrack(112, "Al-Ikhlas", "الإخلاص", "Mishary Rashid Alafasy", "0:24", "https://server8.mp3quran.net/afs/112.mp3")
            )
        ),

        // 11. Tahajjud & Night Qiyam
        DetailedCuratedPlaylist(
            id = "tahajjud-night-qiyam",
            title = "Tahajjud & Night Qiyam",
            tag = "Night",
            subtitle = "Deep emotional recitations in stillness",
            description = "Deeply moving recitations from late-night Qiyam prayers by Imams of Makkah and Medina, fostering intimate devotion.",
            curator = "Mishary Alafasy & Friends",
            totalDuration = "50 mins",
            coverUrl = GhaisAssets.JumpBackInItems[3].coverUrl,
            tracks = listOf(
                CuratedTrack(50, "Qaf", "ق", "Yasser Al-Dossari", "8:40", "https://server11.mp3quran.net/yasser/050.mp3"),
                CuratedTrack(19, "Maryam", "مريم", "Maher Al-Muaiqly", "19:40", "https://server12.mp3quran.net/maher/019.mp3"),
                CuratedTrack(89, "Al-Fajr", "الفجر", "Abdul Basit Abdul Samad", "7:15", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/089.mp3"),
                CuratedTrack(36, "Ya-Sin", "يس", "Islam Sobhi", "16:45", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/036.mp3")
            )
        ),

        // 12. Anxiety Relief & Inshirah
        DetailedCuratedPlaylist(
            id = "anxiety-relief-inshirah",
            title = "Anxiety Relief & Inshirah",
            tag = "Healing",
            subtitle = "Ash-Sharh & Ad-Duha for weary souls",
            description = "A therapeutic selection of soothing verses reminding us that with every hardship comes ease, alleviating grief, anxiety, and distress.",
            curator = "Ghais Wellness",
            totalDuration = "32 mins",
            coverUrl = GhaisAssets.JumpBackInItems[0].coverUrl,
            tracks = listOf(
                CuratedTrack(94, "Ash-Sharh", "الشرح", "Maher Al-Muaiqly", "0:45", "https://server12.mp3quran.net/maher/094.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Abdul Basit Abdul Samad", "2:40", "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/093.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Islam Sobhi", "7:40", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/067.mp3"),
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Mishary Rashid Alafasy", "11:20", "https://server8.mp3quran.net/afs/055.mp3")
            )
        ),

        // 13. Sunrise Barakah & Gratitude
        DetailedCuratedPlaylist(
            id = "sunrise-barakah",
            title = "Sunrise Barakah & Gratitude",
            tag = "Morning",
            subtitle = "Surah Ar-Rahman & Al-Waqi'ah recitations",
            description = "Uplifting dawn recitations celebrating Allah's creation, boundless blessings, and opening the gates of sustenance and barakah.",
            curator = "Ghais Editorial",
            totalDuration = "40 mins",
            coverUrl = GhaisAssets.LibraryMorningCover,
            tracks = listOf(
                CuratedTrack(55, "Ar-Rahman", "الرحمن", "Yasser Al-Dossari", "11:05", "https://server11.mp3quran.net/yasser/055.mp3"),
                CuratedTrack(56, "Al-Waqi'ah", "الواقعة", "Saad Al-Ghamdi", "8:45", "https://server7.mp3quran.net/s_gmd/056.mp3"),
                CuratedTrack(93, "Ad-Duha", "الضحى", "Maher Al-Muaiqly", "0:55", "https://server12.mp3quran.net/maher/093.mp3"),
                CuratedTrack(67, "Al-Mulk", "الملك", "Islam Sobhi", "7:40", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/067.mp3")
            )
        ),

        // 14. Garden of Tranquility
        DetailedCuratedPlaylist(
            id = "garden-of-tranquility",
            title = "Garden of Tranquility",
            tag = "Peace",
            subtitle = "Soft acoustic ambiance with Surat Maryam",
            description = "Gentle, heart-settling recitations echoing the tranquility of Paradise, perfect for peaceful evening contemplation and relaxation.",
            curator = "Ghais Peace Team",
            totalDuration = "46 mins",
            coverUrl = GhaisAssets.CuratedForPeace[1].coverUrl,
            tracks = listOf(
                CuratedTrack(19, "Maryam", "مريم", "Maher Al-Muaiqly", "19:40", "https://server12.mp3quran.net/maher/019.mp3"),
                CuratedTrack(12, "Yusuf", "يوسف", "Mohamed Siddiq Al-Minshawi", "28:30", "https://server10.mp3quran.net/minsh/012.mp3"),
                CuratedTrack(50, "Qaf", "ق", "Islam Sobhi", "10:20", "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/050.mp3"),
                CuratedTrack(89, "Al-Fajr", "الفجر", "Yasser Al-Dossari", "3:50", "https://server11.mp3quran.net/yasser/089.mp3")
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
     * Supports exact ID, title matching, normalized hyphen/space variations, and fuzzy matching.
     */
    fun getPlaylistById(id: String): DetailedCuratedPlaylist? {
        val normalized = id.trim().lowercase().replace("_", "-")
        val cleanNoHyphen = normalized.replace("-", "").replace(" ", "")

        return curatedPlaylists.firstOrNull { cp ->
            val cpId = cp.id.lowercase().replace("_", "-")
            val cpTitle = cp.title.lowercase()
            val cpClean = cpId.replace("-", "").replace(" ", "")

            cpId == normalized ||
            cpTitle == normalized ||
            cpClean == cleanNoHyphen ||
            normalized.contains(cpId) ||
            cpId.contains(normalized) ||
            normalized.contains(cpClean) ||
            cpTitle.contains(normalized.replace("-", " "))
        }
    }

    /**
     * Alias for [getPlaylistById] to retrieve a curated playlist.
     */
    fun getCuratedPlaylist(playlistId: String): DetailedCuratedPlaylist? = getPlaylistById(playlistId)

    /**
     * Get a curated playlist by ID or fuzzy matching, with fallback to first playlist.
     */
    fun getCuratedPlaylistOrDefault(playlistId: String): DetailedCuratedPlaylist {
        return getCuratedPlaylist(playlistId) ?: curatedPlaylists.first()
    }

    /**
     * Get full list of domain [TrackItem]s for a curated playlist by ID.
     */
    fun getCuratedTracksForPlaylist(playlistId: String): List<TrackItem> {
        val playlist = getCuratedPlaylistOrDefault(playlistId)
        return playlist.tracks.map { track ->
            val reciter = com.ghais.data.repository.QuranDataRepository.getReciterBySlug(track.reciterName)
            // Unavailable surah -> keep the curated seed URL (known-good), never a 404.
            val resolvedUrl = if (reciter.isSurahAvailable(track.surahNumber)) {
                reciter.getFullSurahUrl(track.surahNumber)
            } else {
                track.audioUrl
            }
            TrackItem(
                reciterSlug = reciter.slug,
                reciterName = track.reciterName,
                surahId = track.surahNumber,
                surahNameEn = track.surahNameEn,
                surahNameAr = track.surahNameAr,
                ayahNo = 0,
                audioUrl = resolvedUrl,
                textUthmani = "",
                durationMs = parseDurationStringToMs(track.duration)
            )
        }
    }

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

/**
 * Genuine full-surah MP3 audio stream for this reciter from mp3quran.net / archive.org.
 */
fun DetailedReciter.fullSurahUrl(surahId: Int): String {
    val pad = surahId.toString().padStart(3, '0')
    return when (slug.lowercase()) {
        "mishary" -> "https://server8.mp3quran.net/afs/$pad.mp3"
        "al-sudais" -> "https://server11.mp3quran.net/sds/$pad.mp3"
        "al-muaiqly" -> "https://server12.mp3quran.net/maher/$pad.mp3"
        "al-dossari" -> "https://server11.mp3quran.net/yasser/$pad.mp3"
        "islam-sobhi" -> "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/$pad.mp3"
        "omar-hisham" -> "https://archive.org/download/Omar-Hisham/$pad.mp3"
        "abdul-basit" -> "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/$pad.mp3"
        "al-husary" -> "https://server13.mp3quran.net/husr/$pad.mp3"
        "saud-shuraim" -> "https://server7.mp3quran.net/shur/$pad.mp3"
        "saad-alghamdi" -> "https://server7.mp3quran.net/s_gmd/$pad.mp3"
        "al-minshawi" -> "https://server10.mp3quran.net/minsh/$pad.mp3"
        "ahmed-alajamy" -> "https://server10.mp3quran.net/ajm/$pad.mp3"
        else -> "https://server8.mp3quran.net/afs/$pad.mp3"
    }
}
