package com.ghais.ui.screens.reciters

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.seed.GhaisAssets
import com.ghais.ui.screens.home.NoirArtworkWell

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

    val photos = GhaisAssets.VerifiedReciters
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

/**
 * Noir reciter artwork helpers — reference [com.ghais.ui.theme.GhaisNoir].
 *
 * All reciter photo/avatar rendering must go through [NoirReciterAvatar] (or
 * the section-level [NoirReciterArtwork] in NoirReciterSections.kt), which
 * delegates to the shared home [NoirArtworkWell]: grayscale
 * [ColorFilter.colorMatrix] (saturation 0), monogram fallback in a clay well,
 * optional chromium ring. [photoForSlug] above is intentionally untouched so
 * existing callers (RecitersScreen, RegionRecitersScreen,
 * FollowedRecitersScreen) keep compiling with no edits.
 */

/** Shared true-grayscale filter (saturation 0). Single instance, never re-allocated. */
internal val NoirReciterGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/** Monogram for the clay-well fallback — first Latin initial, Arabic fallback otherwise. */
fun monogramForReciter(nameEn: String): String =
    nameEn.firstOrNull { it.isLetter() }?.uppercase() ?: "ق"

/**
 * Grayscale reciter avatar in a clay well with monogram fallback.
 *
 * @param photoUrl nullable portrait URL; blank/null renders the monogram well.
 * @param nameEn English name used for the monogram + content description.
 * @param ring when true uses the bright chromium top-specular border, else the ghost card border.
 */
@Composable
fun NoirReciterAvatar(
    photoUrl: String?,
    nameEn: String,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    monogramSize: TextUnit = 28.sp,
    ring: Boolean = false,
) {
    NoirArtworkWell(
        coverUrl = photoUrl.orEmpty(),
        monogram = monogramForReciter(nameEn),
        shape = shape,
        modifier = modifier,
        size = size,
        monogramSize = monogramSize,
        ring = ring,
    )
}

