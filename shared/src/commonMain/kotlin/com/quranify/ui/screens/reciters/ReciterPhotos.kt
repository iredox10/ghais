package com.quranify.ui.screens.reciters

import com.quranify.data.seed.StitchAssets

fun photoForSlug(slug: String): String? {
    val clean = slug.lowercase()
    val verified = ALL_VERIFIED_RECITERS.find {
        it.slug.lowercase() == clean ||
        (clean == "alafasy" && it.slug == "mishary") ||
        (clean == "sudais" && it.slug == "al-sudais") ||
        (clean == "muaiqly" && it.slug == "al-muaiqly") ||
        (clean == "dossari" && it.slug == "al-dossari") ||
        (clean.startsWith("abdulbaset") && it.slug == "abdul-basit") ||
        (clean == "shuraym" && it.slug == "shuraim") ||
        (clean.contains("minshawi") && it.slug.contains("minshawi"))
    }
    if (verified != null) return verified.photoUrl

    val photos = StitchAssets.VerifiedReciters
    return when (clean) {
        "alafasy", "mishary" -> photos.firstOrNull { it.slug == "mishary" }?.photoUrl
        "sudais", "al-sudais" -> photos.firstOrNull { it.slug == "al-sudais" }?.photoUrl
        "muaiqly", "al-muaiqly" -> photos.firstOrNull { it.slug == "al-muaiqly" }?.photoUrl
        "dossari", "al-dossari" -> photos.firstOrNull { it.slug == "al-dossari" }?.photoUrl
        "abdulbaset_murattal", "abdulbaset_mujawwad", "abdul-basit" ->
            photos.firstOrNull { it.slug == "abdul-basit" }?.photoUrl
        else -> null
    }
}

