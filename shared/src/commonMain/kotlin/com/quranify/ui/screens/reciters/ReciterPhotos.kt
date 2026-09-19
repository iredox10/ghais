package com.quranify.ui.screens.reciters

import com.quranify.data.seed.StitchAssets

fun photoForSlug(slug: String): String? {
    val photos = StitchAssets.VerifiedReciters
    return when (slug) {
        "alafasy" -> photos.firstOrNull { it.slug == "mishary" }?.photoUrl
        "sudais" -> photos.firstOrNull { it.slug == "al-sudais" }?.photoUrl
        "muaiqly" -> photos.firstOrNull { it.slug == "al-muaiqly" }?.photoUrl
        "dossari" -> photos.firstOrNull { it.slug == "al-dossari" }?.photoUrl
        "abdulbaset_murattal", "abdulbaset_mujawwad" ->
            photos.firstOrNull { it.slug == "abdul-basit" }?.photoUrl
        else -> null
    }
}
