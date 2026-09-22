package com.ghais.data.seed

import com.ghais.domain.model.Reciter

/**
 * Verified EveryAyah Reciters Catalog.
 * 
 * Every reciter in this catalog has authentic, verified Ayah-by-Ayah audio recordings
 * hosted on https://everyayah.com/data/<audioFolder>/<surahId><ayahNo>.mp3.
 * These reciters are specially suited for Hifz (memorization) and Tajweed study.
 */
data class EveryAyahReciter(
    val slug: String,
    val nameEn: String,
    val nameAr: String,
    val style: String = "Murattal",
    val riwayah: String = "Hafs 'an 'Asim",
    val tempo: String = "Medium",
    val audioFolder: String,
    val serverUrl: String = "",
    val country: String = "",
    val description: String = "",
    val isTeacher: Boolean = false
) {
    fun toReciter(): Reciter = Reciter(
        slug = slug,
        nameEn = nameEn,
        nameAr = nameAr,
        riwayah = riwayah,
        style = style,
        tempo = tempo,
        imageUrl = null,
        audioFolder = audioFolder,
        country = country,
        serverUrl = serverUrl
    )
}

object EveryAyahReciters {

    val ALL: List<EveryAyahReciter> = listOf(
        // --- Teacher / Mu'allim Edition (Repetition & Instruction for Hifz) ---
        EveryAyahReciter(
            slug = "husary-muallim",
            nameEn = "Mahmoud Khalil Al-Husary (Teacher)",
            nameAr = "محمود خليل الحصري (المعلم)",
            style = "Teacher (Mu'allim)",
            tempo = "Slow & Measured",
            audioFolder = "Husary_Muallim_128kbps",
            serverUrl = "https://server13.mp3quran.net/husr/",
            country = "Egypt",
            description = "The gold standard for Quran memorization. Pauses after each Ayah for student recitation with immaculate Tajweed.",
            isTeacher = true
        ),
        EveryAyahReciter(
            slug = "minshawi-teacher",
            nameEn = "Mohamed Siddiq Al-Minshawi (Teacher)",
            nameAr = "محمد صديق المنشاوي (المعلم)",
            style = "Teacher (Mu'allim)",
            tempo = "Slow & Emotive",
            audioFolder = "Minshawy_Teacher_128kbps",
            serverUrl = "https://server10.mp3quran.net/minsh/",
            country = "Egypt",
            description = "Celebrated Egyptian teaching recitation featuring young students echoing verses in traditional classroom harmony.",
            isTeacher = true
        ),
        EveryAyahReciter(
            slug = "ayman-sowaid",
            nameEn = "Dr. Ayman Rusydi Suwayd",
            nameAr = "أيمن رشدي سويد",
            style = "Tajweed Master",
            tempo = "Precise & Slow",
            audioFolder = "Ayman_Sowaid_64kbps",
            serverUrl = "https://server10.mp3quran.net/ajm/",
            country = "Syria",
            description = "World renowned scholar and supreme authority in the science of Tajweed and Makharij al-Huruf.",
            isTeacher = true
        ),

        // --- Classical Murattal Masters ---
        EveryAyahReciter(
            slug = "mishary",
            nameEn = "Mishary Rashid Alafasy",
            nameAr = "مشاري راشد العفاسي",
            style = "Murattal",
            tempo = "Medium",
            audioFolder = "Alafasy_128kbps",
            serverUrl = "https://server8.mp3quran.net/afs/",
            country = "Kuwait",
            description = "Crystal clear melodious recitation celebrated across the world for smooth vocal cadence."
        ),
        EveryAyahReciter(
            slug = "al-husary",
            nameEn = "Mahmoud Khalil Al-Husary",
            nameAr = "محمود خليل الحصري",
            style = "Murattal",
            tempo = "Measured",
            audioFolder = "Husary_128kbps",
            serverUrl = "https://server13.mp3quran.net/husr/",
            country = "Egypt",
            description = "Impeccable articulation and pure classical Egyptian Murattal tradition."
        ),
        EveryAyahReciter(
            slug = "al-minshawi",
            nameEn = "Mohamed Siddiq Al-Minshawi",
            nameAr = "محمد صديق المنشاوي",
            style = "Murattal",
            tempo = "Emotive",
            audioFolder = "Minshawy_Murattal_128kbps",
            serverUrl = "https://server10.mp3quran.net/minsh/",
            country = "Egypt",
            description = "The 'Weeping Voice' known for profound spiritual devotion and serene humility."
        ),
        EveryAyahReciter(
            slug = "abdul-basit",
            nameEn = "Abdul Basit Abdul Samad",
            nameAr = "عبد الباسط عبد الصمد",
            style = "Murattal",
            tempo = "Medium",
            audioFolder = "Abdul_Basit_Murattal_64kbps",
            serverUrl = "https://server7.mp3quran.net/basit/",
            country = "Egypt",
            description = "The Golden Throat whose breath control and tonal precision remain unmatched."
        ),

        // --- Mujawwad Classical Artistry ---
        EveryAyahReciter(
            slug = "abdul-basit-mujawwad",
            nameEn = "Abdul Basit (Mujawwad)",
            nameAr = "عبد الباسط عبد الصمد (مُجوّد)",
            style = "Mujawwad",
            tempo = "Slow & Resonant",
            audioFolder = "Abdul_Basit_Mujawwad_128kbps",
            serverUrl = "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/",
            country = "Egypt",
            description = "Monumental Mujawwad performance highlighting deep resonance and expansive maqamat."
        ),
        EveryAyahReciter(
            slug = "minshawi-mujawwad",
            nameEn = "Al-Minshawi (Mujawwad)",
            nameAr = "محمد صديق المنشاوي (مُجوّد)",
            style = "Mujawwad",
            tempo = "Slow & Emotional",
            audioFolder = "Minshawy_Mujawwad_192kbps",
            serverUrl = "https://server10.mp3quran.net/minsh/Almusshaf-Al-Mojawwad/",
            country = "Egypt",
            description = "Historic Mujawwad recitation captured with stirring emotive depth."
        ),
        EveryAyahReciter(
            slug = "husary-mujawwad",
            nameEn = "Al-Husary (Mujawwad)",
            nameAr = "محمود خليل الحصري (مُجوّد)",
            style = "Mujawwad",
            tempo = "Measured & Reverent",
            audioFolder = "Husary_128kbps_Mujawwad",
            serverUrl = "https://server13.mp3quran.net/husr/",
            country = "Egypt",
            description = "Sublime Mujawwad reading blending rigorous phonetic accuracy with solemn beauty."
        ),

        // --- Haramain & Saudi Imams ---
        EveryAyahReciter(
            slug = "al-sudais",
            nameEn = "Abdurrahman As-Sudais",
            nameAr = "عبد الرحمن السديس",
            style = "Haramain",
            tempo = "Brisk & Resonant",
            audioFolder = "Abdurrahmaan_As-Sudais_192kbps",
            serverUrl = "https://server11.mp3quran.net/sds/",
            country = "Saudi Arabia",
            description = "Chief Imam of the Grand Mosque in Makkah, famed for passionate tearful recitations."
        ),
        EveryAyahReciter(
            slug = "saud-shuraim",
            nameEn = "Saud Ash-Shuraim",
            nameAr = "سعود الشريم",
            style = "Haramain",
            tempo = "Fast & Powerful",
            audioFolder = "Saood_ash-Shuraym_128kbps",
            serverUrl = "https://server7.mp3quran.net/shur/",
            country = "Saudi Arabia",
            description = "Former Imam of Masjid al-Haram, known for vibrant rhythmic flow and clarity."
        ),
        EveryAyahReciter(
            slug = "al-muaiqly",
            nameEn = "Maher Al-Muaiqly",
            nameAr = "ماهر المعيقلي",
            style = "Haramain",
            tempo = "Medium",
            audioFolder = "MaherAlMuaiqly128kbps",
            serverUrl = "https://server12.mp3quran.net/maher/",
            country = "Saudi Arabia",
            description = "Imam of Masjid al-Haram with a deeply soothing, resonant, and universally loved voice."
        ),
        EveryAyahReciter(
            slug = "al-dossari",
            nameEn = "Yasser Al-Dossari",
            nameAr = "ياسر الدوسري",
            style = "Haramain",
            tempo = "Emotive",
            audioFolder = "Yasser_Ad-Dussary_128kbps",
            serverUrl = "https://server11.mp3quran.net/yasser/",
            country = "Saudi Arabia",
            description = "Imam of Masjid al-Haram characterized by soulful pitch shifts and emotional power."
        ),
        EveryAyahReciter(
            slug = "hudhaify",
            nameEn = "Ali Al-Hudhaify",
            nameAr = "علي بن عبد الرحمن الحذيفي",
            style = "Madinah Murattal",
            tempo = "Deliberate & Slow",
            audioFolder = "Hudhaify_128kbps",
            serverUrl = "https://server9.mp3quran.net/hthfi/",
            country = "Saudi Arabia",
            description = "Chief Imam of the Prophet's Mosque in Madinah, celebrated for slow, crystal Tajweed."
        ),
        EveryAyahReciter(
            slug = "abdullah-al-juhany",
            nameEn = "Abdullah Awad Al-Juhany",
            nameAr = "عبد الله عواد الجهني",
            style = "Haramain",
            tempo = "Steady & Melodic",
            audioFolder = "Abdullaah_3awwaad_Al-Juhaynee_128kbps",
            serverUrl = "https://server13.mp3quran.net/jhn/",
            country = "Saudi Arabia",
            description = "Imam of Masjid al-Haram known for his rich baritone cadence."
        ),
        EveryAyahReciter(
            slug = "salah-al-budair",
            nameEn = "Salah Al-Budair",
            nameAr = "صلاح البدير",
            style = "Madinah Murattal",
            tempo = "Medium",
            audioFolder = "Salah_Al_Budair_128kbps",
            serverUrl = "https://server6.mp3quran.net/bdr/",
            country = "Saudi Arabia",
            description = "Imam and Khatib of the Prophet's Mosque in Madinah."
        ),
        EveryAyahReciter(
            slug = "ali-jaber",
            nameEn = "Ali Jaber",
            nameAr = "علي جابر",
            style = "Haramain Legend",
            tempo = "Flowing",
            audioFolder = "Ali_Jaber_64kbps",
            serverUrl = "https://server11.mp3quran.net/a_jbr/",
            country = "Saudi Arabia",
            description = "Legendary former Imam of Masjid al-Haram whose iconic recitations shaped a generation."
        ),

        // --- Renowned International Qaris ---
        EveryAyahReciter(
            slug = "saad-alghamdi",
            nameEn = "Saad Al-Ghamdi",
            nameAr = "سعد الغامدي",
            style = "Murattal",
            tempo = "Medium",
            audioFolder = "Ghamadi_40kbps",
            serverUrl = "https://server7.mp3quran.net/s_gmd/",
            country = "Saudi Arabia",
            description = "A warm, clear voice beloved for decades across Muslim households."
        ),
        EveryAyahReciter(
            slug = "abu-bakr-al-shatri",
            nameEn = "Abu Bakr Ash-Shatri",
            nameAr = "أبو بكر الشاطري",
            style = "Murattal",
            tempo = "Calm & Reflective",
            audioFolder = "Abu_Bakr_Ash-Shaatree_128kbps",
            serverUrl = "https://server11.mp3quran.net/shatri/",
            country = "Saudi Arabia",
            description = "Mellow, tranquil recitation ideal for nighttime contemplation and memorization."
        ),
        EveryAyahReciter(
            slug = "ahmed-alajamy",
            nameEn = "Ahmed Al-Ajamy",
            nameAr = "أحمد بن علي العجمي",
            style = "Murattal",
            tempo = "Resonant",
            audioFolder = "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net",
            serverUrl = "https://server10.mp3quran.net/ajm/",
            country = "Saudi Arabia",
            description = "Deeply moving tone, widely recognized for passionate Surah recitations."
        ),
        EveryAyahReciter(
            slug = "fares-abbad",
            nameEn = "Fares Abbad",
            nameAr = "فارس عباد",
            style = "Murattal",
            tempo = "Melodic",
            audioFolder = "Fares_Abbad_64kbps",
            serverUrl = "https://server8.mp3quran.net/frs_a/",
            country = "Yemen",
            description = "Distinctive melodious recitation widely celebrated across the Arab world."
        ),
        EveryAyahReciter(
            slug = "hani-ar-rifai",
            nameEn = "Hani Ar-Rifai",
            nameAr = "هاني الرفاعي",
            style = "Tadabbur",
            tempo = "Slow & Tearful",
            audioFolder = "Hani_Rifai_192kbps",
            serverUrl = "https://server8.mp3quran.net/rifai/",
            country = "Saudi Arabia",
            description = "Deeply poignant recitation conveying humility, repentance, and awe."
        ),
        EveryAyahReciter(
            slug = "muhammad-ayyub",
            nameEn = "Muhammad Ayyub",
            nameAr = "محمد أيوب",
            style = "Hijazi Classic",
            tempo = "Melodic & Serene",
            audioFolder = "Muhammad_Ayyoub_128kbps",
            serverUrl = "https://server8.mp3quran.net/ayyub/",
            country = "Saudi Arabia",
            description = "Former Imam of the Prophet's Mosque, renowned for the authentic Hijazi maqam."
        ),
        EveryAyahReciter(
            slug = "muhammad-jibreel",
            nameEn = "Muhammad Jibreel",
            nameAr = "محمد جبريل",
            style = "Murattal",
            tempo = "Fast & Expressive",
            audioFolder = "Muhammad_Jibreel_128kbps",
            serverUrl = "https://server8.mp3quran.net/jbrl/",
            country = "Egypt",
            description = "Famed Imam of Amr ibn al-Aas Mosque in Cairo, known for powerful Du'a recitations."
        ),
        EveryAyahReciter(
            slug = "nasser-alqatami",
            nameEn = "Nasser Al-Qatami",
            nameAr = "ناصر القطامي",
            style = "Modern Murattal",
            tempo = "Medium",
            audioFolder = "Nasser_Alqatami_128kbps",
            serverUrl = "https://server6.mp3quran.net/qtm/",
            country = "Saudi Arabia",
            description = "Inspiring Riyadh Qari with an expressive modern recitation style."
        ),
        EveryAyahReciter(
            slug = "mohammad-al-tablawi",
            nameEn = "Mohammad Al-Tablawi",
            nameAr = "محمد محمود الطبلاوي",
            style = "Egyptian Classic",
            tempo = "Rich & Vocal",
            audioFolder = "Mohammad_al_Tablaway_128kbps",
            serverUrl = "https://server12.mp3quran.net/tblwi/",
            country = "Egypt",
            description = "Former Sheikh of Egyptian Reciters, recognized for unique vocal timbre and breath."
        ),
        EveryAyahReciter(
            slug = "ibrahim-al-akhdar",
            nameEn = "Ibrahim Al-Akhdar",
            nameAr = "إبراهيم الأخضر",
            style = "Madinah Classic",
            tempo = "Steady & Clean",
            audioFolder = "Ibrahim_Akhdar_32kbps",
            serverUrl = "https://server6.mp3quran.net/akdr/",
            country = "Saudi Arabia",
            description = "Sheikh of Madinah Qaris, famous for pristine diction and teaching clarity."
        ),
        EveryAyahReciter(
            slug = "abdullah-basfar",
            nameEn = "Abdullah Basfar",
            nameAr = "عبد الله بصفر",
            style = "Educational",
            tempo = "Measured",
            audioFolder = "Abdullah_Basfar_192kbps",
            serverUrl = "https://server6.mp3quran.net/bsfr/",
            country = "Saudi Arabia",
            description = "Renowned Quran scholar focused on global Quran memorization initiatives."
        ),
        EveryAyahReciter(
            slug = "yasser-salama",
            nameEn = "Yasser Salama",
            nameAr = "ياسر سلامة",
            style = "Murattal",
            tempo = "Medium",
            audioFolder = "Yaser_Salamah_128kbps",
            serverUrl = "https://server11.mp3quran.net/salamah/",
            country = "Egypt",
            description = "Gentle, soothing recitation with pristine phonetic transitions."
        ),
        EveryAyahReciter(
            slug = "mahmoud-ali-al-banna",
            nameEn = "Mahmoud Ali Al-Banna",
            nameAr = "محمود علي البنا",
            style = "Murattal Classic",
            tempo = "Steady",
            audioFolder = "mahmoud_ali_al_banna_32kbps",
            serverUrl = "https://server8.mp3quran.net/bna/",
            country = "Egypt",
            description = "One of the four founding masters of recorded Murattal on Cairo Radio."
        ),
        EveryAyahReciter(
            slug = "mustafa-ismail",
            nameEn = "Mustafa Ismail",
            nameAr = "مصطفى إسماعيل",
            style = "Maqamat Master",
            tempo = "Measured",
            audioFolder = "Mustafa_Ismail_48kbps",
            serverUrl = "https://server8.mp3quran.net/mustafa/",
            country = "Egypt",
            description = "The architect of Quranic maqamat, admired by musicians and reciters worldwide."
        ),
        EveryAyahReciter(
            slug = "khalifa-al-tunaiji",
            nameEn = "Khalifa Al-Tunaiji",
            nameAr = "خليفة الطنيجي",
            style = "Teacher / Educational",
            tempo = "Slow & Measured",
            audioFolder = "khalefa_al_tunaiji_64kbps",
            serverUrl = "https://server12.mp3quran.net/tnjy/",
            country = "UAE",
            description = "Emirati Qari renowned for high pedagogical clarity and deliberate pacing for beginners.",
            isTeacher = true
        )
    )

    fun findBySlug(slug: String): EveryAyahReciter? {
        val clean = slug.trim().lowercase().replace("_", "-")
        return ALL.find { reciter ->
            val rSlug = reciter.slug.lowercase().replace("_", "-")
            rSlug == clean ||
            (clean == "alafasy" && rSlug == "mishary") ||
            (clean == "mishary" && rSlug == "mishary") ||
            (clean == "sudais" && rSlug == "al-sudais") ||
            (clean == "muaiqly" && rSlug == "al-muaiqly") ||
            (clean == "dossari" && rSlug == "al-dossari") ||
            (clean == "shuraim" && rSlug == "saud-shuraim") ||
            (clean == "ghamdi" && rSlug == "saad-alghamdi") ||
            (clean == "ajamy" && rSlug == "ahmed-alajamy") ||
            (clean == "abbad" && rSlug == "fares-abbad") ||
            (clean == "husary" && rSlug == "al-husary") ||
            (clean == "minshawi" && rSlug == "al-minshawi") ||
            (clean == "basit" && rSlug == "abdul-basit") ||
            reciter.nameEn.lowercase().contains(clean)
        }
    }
}
