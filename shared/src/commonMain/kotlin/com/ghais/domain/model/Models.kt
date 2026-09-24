package com.ghais.domain.model

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
enum class ReciterCatalog(val wire: String) {
    MP3QURAN("mp3quran"),
    EVERYAYAH("everyayah")
}

@Serializable
data class Reciter(
    val slug: String,
    val nameEn: String,
    val nameAr: String,
    val riwayah: String = "Hafs",
    val style: String = "murattal",
    val tempo: String = "medium",
    val imageUrl: String? = null,
    val audioFolder: String = "",
    val country: String = "",
    val serverUrl: String = "",
    val availableSurahList: String = "",
    val catalog: ReciterCatalog = ReciterCatalog.MP3QURAN,
    val description: String = "",
    val isTeacher: Boolean = false,
    val imageFileId: String? = null,
    val enabled: Boolean = true
) {
    /** Stable catalog-scoped identity: "<catalog.wire>:<slug>". */
    fun catalogKey(): String = "${catalog.wire}:$slug"

    /**
     * Per-ayah audio resolution for the EVERYAYAH catalog
     * (https://everyayah.com/data/<folder>/<SSSA AA>.mp3).
     * Branches on [catalog] explicitly, but the URL shape is identical for
     * both catalogs: per-ayah clips are only served via EveryAyah, so
     * MP3QURAN reciters reuse the same EveryAyah folder resolution.
     */
    fun getAyahAudioUrl(surahId: Int, ayahNo: Int): String {
        val s = surahId.toString().padStart(3, '0')
        val a = ayahNo.toString().padStart(3, '0')
        val folder = when (catalog) {
            ReciterCatalog.EVERYAYAH,
            ReciterCatalog.MP3QURAN -> audioFolder.ifEmpty { resolveEveryAyahFolder(slug) }
        }
        return "https://everyayah.com/data/$folder/$s$a.mp3"
    }

    fun isSurahAvailable(surahId: Int): Boolean {
        if (availableSurahList.isBlank()) return true
        val ids = parseAvailableSurahIds(availableSurahList)
        return ids.isEmpty() || ids.contains(surahId)
    }

    fun getAvailableSurahIds(): List<Int> {
        if (availableSurahList.isBlank()) return (1..114).toList()
        val ids = parseAvailableSurahIds(availableSurahList)
        return if (ids.isEmpty()) (1..114).toList() else ids
    }

    /**
     * Full-surah audio resolution for the MP3QURAN catalog.
     * Branches on [catalog] explicitly, but the URL is identical for both
     * catalogs: [serverUrl] wins when set, otherwise the legacy per-slug
     * mp3quran mapping applies (EveryAyah has no full-surah endpoint, so
     * EVERYAYAH reciters reuse the same MP3QURAN full-surah URLs).
     */
    fun getFullSurahUrl(surahId: Int): String {
        val pad = surahId.toString().padStart(3, '0')
        if (serverUrl.isNotBlank()) {
            val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            return "$base$pad.mp3"
        }
        // Catalog-aware branch kept behavior-identical: both catalogs share
        // the MP3QURAN full-surah mapping below.
        when (catalog) {
            ReciterCatalog.MP3QURAN,
            ReciterCatalog.EVERYAYAH -> Unit
        }
        return when (slug.lowercase()) {
            "mishary", "alafasy" -> "https://server8.mp3quran.net/afs/$pad.mp3"
            "al-sudais", "sudais" -> "https://server11.mp3quran.net/sds/$pad.mp3"
            "al-muaiqly", "muaiqly" -> "https://server12.mp3quran.net/maher/$pad.mp3"
            "al-dossari", "dossari" -> "https://server11.mp3quran.net/yasser/$pad.mp3"
            "islam-sobhi", "islam_sobhi" -> "https://server14.mp3quran.net/islam/Rewayat-Hafs-A-n-Assem/$pad.mp3"
            "omar-hisham", "omar_hisham" -> "https://archive.org/download/Omar-Hisham/$pad.mp3"
            "abdul-basit", "abdul_basit", "basit" -> "https://server7.mp3quran.net/basit/Almusshaf-Al-Mojawwad/$pad.mp3"
            "al-husary", "husary" -> "https://server13.mp3quran.net/husr/$pad.mp3"
            "saud-shuraim", "shuraim" -> "https://server7.mp3quran.net/shur/$pad.mp3"
            "saad-alghamdi", "alghamdi", "ghamdi" -> "https://server7.mp3quran.net/s_gmd/$pad.mp3"
            "al-minshawi", "minshawi" -> "https://server10.mp3quran.net/minsh/$pad.mp3"
            "ahmed-alajamy", "alajamy", "ajamy" -> "https://server10.mp3quran.net/ajm/$pad.mp3"
            else -> "https://server8.mp3quran.net/afs/$pad.mp3"
        }
    }
}

/**
 * Lenient parser for `Reciter.availableSurahList` / Appwrite `available_surahs`.
 *
 * Accepts comma/semicolon/whitespace-separated ids AND inclusive ranges
 * (`"1-114"`, the shape the admin sync script writes), tolerates stray
 * punctuation, and keeps only 1..114. Unparseable input yields an empty list
 * so callers keep the established "blank-or-garbage means all 114" contract.
 * Never throws.
 */
fun parseAvailableSurahIds(raw: String): List<Int> {
    if (raw.isBlank()) return emptyList()
    return try {
        val out = LinkedHashSet<Int>()
        for (part in raw.split(',', ';', ' ', '\n', '\t', '|')) {
            val t = part.trim().trim('.', '(', ')', '[', ']', '"', '\'')
            if (t.isEmpty()) continue
            val dash = t.indexOf('-')
            if (dash > 0) {
                val start = t.substring(0, dash).trim().toIntOrNull()
                val end = t.substring(dash + 1).trim().toIntOrNull()
                if (start != null && end != null && start in 1..114 && end in 1..114) {
                    val lo = minOf(start, end)
                    val hi = maxOf(start, end)
                    for (id in lo..hi) out.add(id)
                }
                continue
            }
            t.toIntOrNull()?.let { if (it in 1..114) out.add(it) }
        }
        out.toList()
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Resolves the EveryAyah audio folder for a given reciter slug.
 * Maps known reciter slugs to their corresponding EveryAyah folder.
 */
fun resolveEveryAyahFolder(slug: String, defaultFolder: String = "Alafasy_128kbps"): String {
    val normalized = slug.trim().lowercase().replace('_', '-')
    return when (normalized) {
        "alafasy", "mishary" -> "Alafasy_128kbps"
        "abdul-basit", "basit" -> "Abdul_Basit_Murattal_64kbps"
        "abdul-basit-mujawwad" -> "Abdul_Basit_Mujawwad_128kbps"
        "al-minshawi", "minshawi" -> "Minshawy_Murattal_128kbps"
        "minshawi-mujawwad" -> "Minshawy_Mujawwad_192kbps"
        "minshawi-teacher" -> "Minshawy_Teacher_128kbps"
        "al-husary", "husary" -> "Husary_128kbps"
        "husary-muallim" -> "Husary_Muallim_128kbps"
        "husary-mujawwad" -> "Husary_128kbps_Mujawwad"
        "ayman-sowaid", "ayman-suwayd" -> "Ayman_Sowaid_64kbps"
        "al-sudais", "sudais" -> "Abdurrahmaan_As-Sudais_192kbps"
        "al-muaiqly", "muaiqly" -> "MaherAlMuaiqly128kbps"
        "saad-alghamdi", "alghamdi", "ghamdi" -> "Ghamadi_40kbps"
        "saud-shuraim", "shuraim" -> "Saood_ash-Shuraym_128kbps"
        "al-dossari", "dossari" -> "Yasser_Ad-Dussary_128kbps"
        "hudhaify", "al-hudhaify", "huthaify", "ali-bin-abdulrahman-al-huthaify" -> "Hudhaify_128kbps"
        "abdullah-al-juhany", "al-juhany", "juhany" -> "Abdullaah_3awwaad_Al-Juhaynee_128kbps"
        "abu-bakr-al-shatri", "shatri" -> "Abu_Bakr_Ash-Shaatree_128kbps"
        "ahmed-alajamy", "alajamy", "ajamy" -> "Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net"
        "ali-jaber" -> "Ali_Jaber_64kbps"
        "fares-abbad", "abbad" -> "Fares_Abbad_64kbps"
        "hani-ar-rifai", "rifai" -> "Hani_Rifai_192kbps"
        "muhammad-ayyub", "ayyub" -> "Muhammad_Ayyoub_128kbps"
        "muhammad-jibreel", "jibreel" -> "Muhammad_Jibreel_128kbps"
        "nasser-alqatami", "nasser-al-qatami", "qatami" -> "Nasser_Alqatami_128kbps"
        "mohammad-al-tablawi", "tablawi" -> "Mohammad_al_Tablaway_128kbps"
        "salah-al-budair", "budair" -> "Salah_Al_Budair_128kbps"
        "ibrahim-al-akhdar", "akhdar" -> "Ibrahim_Akhdar_32kbps"
        "abdullah-basfar", "basfar" -> "Abdullah_Basfar_192kbps"
        "abdullah-matroud", "matroud" -> "Abdullah_Matroud_128kbps"
        "yasser-salama", "yasser-salamah", "salama", "salamah" -> "Yasser_Salamah_128kbps"
        "mahmoud-ali-al-banna", "banna" -> "mahmoud_ali_al_banna_32kbps"
        "mustafa-ismail" -> "Mustafa_Ismail_48kbps"
        "khalifa-al-tunaiji", "tunaiji" -> "khalefa_al_tunaiji_64kbps"
        else -> defaultFolder
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

/** Sentinel for "duration not yet known" — real duration comes from PlayerBridge once streaming starts. */
const val UNKNOWN_DURATION_MS: Long = 0L

@Serializable
data class TrackItem(
    val reciterSlug: String,
    val reciterName: String,
    val surahId: Int,
    val surahNameEn: String,
    val surahNameAr: String,
    val ayahNo: Int = 0,
    val audioUrl: String,
    val textUthmani: String = "",
    /**
     * Estimated/known duration in ms. 0 == unknown (streaming, resolved at
     * runtime via PlayerBridge.durationMs). Never treat 0 as "0-length" —
     * use [hasKnownDuration] / [resolvedDurationMs].
     */
    val durationMs: Long = UNKNOWN_DURATION_MS
)

val TrackItem.imageUrl: String?
    get() = null

/** True when the track carries a usable pre-known duration. */
val TrackItem.hasKnownDuration: Boolean
    get() = durationMs > 0L

/** True when the track represents a full surah rather than an individual ayah recitation. */
val TrackItem.isFullSurah: Boolean
    get() {
        if (ayahNo <= 0) return true
        val url = audioUrl.lowercase()
        if (url.contains("mp3quran.net") || url.contains("archive.org")) return true
        if (url.startsWith("file:") || url.startsWith("/") || url.startsWith("content:")) return true
        if (url.contains("everyayah.com")) return false
        if (textUthmani.isBlank() && (durationMs > 60_000L || durationMs == UNKNOWN_DURATION_MS)) return true
        return false
    }

/**
 * Effective duration: pre-known [TrackItem.durationMs] when > 0,
 * otherwise [fallbackMs] (0 by default = still unknown).
 */
fun TrackItem.resolvedDurationMs(fallbackMs: Long = UNKNOWN_DURATION_MS): Long =
    durationMs.takeIf { it > 0L } ?: fallbackMs

enum class RepeatMode {
    OFF,
    AYAH,
    SURAH,
    QUEUE
}

@Serializable
data class HifzRange(
    val surahId: Int,
    val startAyah: Int,
    val endAyah: Int,
    val targetLoops: Int = 1,
    val currentLoop: Int = 1
)

