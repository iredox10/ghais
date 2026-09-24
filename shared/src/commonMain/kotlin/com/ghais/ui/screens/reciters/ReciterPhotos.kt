package com.ghais.ui.screens.reciters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    // Cloud first (admin-uploaded via Appwrite `reciters.image_url`), then local.
    com.ghais.data.repository.ReciterCloudCache.cloudImageForSlug(slug)?.let { return it }
    val clean = slug.lowercase().replace('_', '-')
    val verified = ALL_VERIFIED_RECITERS.find {
        val vSlug = it.slug.lowercase().replace('_', '-')
        vSlug == clean ||
        (clean.contains("alafasy") || clean.contains("mishary")) && (vSlug == "mishary" || vSlug == "alafasy") ||
        clean.contains("sudais") && vSlug.contains("sudais") ||
        clean.contains("muaiqly") && vSlug.contains("muaiqly") ||
        clean.contains("dossari") && vSlug.contains("dossari") ||
        (clean.contains("abdul-basit") || clean.contains("abdulbaset") || clean.contains("basit")) && (vSlug.contains("abdul-basit") || vSlug.contains("basit")) ||
        (clean.contains("shuraym") || clean.contains("shuraim")) && (vSlug.contains("shuraim") || vSlug.contains("shuraym")) ||
        clean.contains("minshawi") && vSlug.contains("minshawi") ||
        clean.contains("husary") && vSlug.contains("husary")
    }
    if (verified != null) return verified.photoUrl

    val photos = GhaisAssets.VerifiedReciters
    return when {
        clean.contains("alafasy") || clean.contains("mishary") -> photos.firstOrNull { it.slug == "mishary" }?.photoUrl
        clean.contains("sudais") -> photos.firstOrNull { it.slug == "al-sudais" }?.photoUrl
        clean.contains("muaiqly") -> photos.firstOrNull { it.slug == "al-muaiqly" }?.photoUrl
        clean.contains("dossari") -> photos.firstOrNull { it.slug == "al-dossari" }?.photoUrl
        clean.contains("basit") || clean.contains("abdulbaset") -> photos.firstOrNull { it.slug == "abdul-basit" }?.photoUrl
        clean.contains("husary") -> ALL_VERIFIED_RECITERS.firstOrNull { it.slug == "husary" }?.photoUrl
        clean.contains("minshawi") -> ALL_VERIFIED_RECITERS.firstOrNull { it.slug == "minshawi" }?.photoUrl
        clean.contains("shuraim") || clean.contains("shuraym") -> ALL_VERIFIED_RECITERS.firstOrNull { it.slug == "shuraim" }?.photoUrl
        else -> null
    }
}

/**
 * Injectable single-slug cloud photo reader (public `reciters` collection,
 * `image_url` per reciter — null on 404/any failure). Wired from platform
 * code that owns the cloud client (android `SyncEngine.init` →
 * `reciterImage`); null on platforms without one, where avatars simply stay
 * on the local [photoForSlug] fallback.
 *
 * Mirrors `FollowStore.countFetcher`.
 */
var remotePhotoFetcher: (suspend (String) -> String?)? = null

/**
 * Lazily resolve a reciter's cloud portrait ([remotePhotoFetcher]) for
 * [slug], remembering the result per slug.
 *
 * The catalog pull ([ReciterCloudCache]) only refreshes on sync, so a photo
 * uploaded in the admin after the last sync would otherwise stay invisible
 * until the next one. Every reciter row that renders a photo should prefer
 * this over a bare [photoForSlug] call, so admin edits appear on next open.
 * Returns null while loading / on any failure, letting callers fall back to
 * their local photo.
 */
@Composable
fun rememberCloudPhoto(slug: String?): String? {
    var remoteUrl by remember(slug) { mutableStateOf<String?>(null) }
    if (!slug.isNullOrBlank() && remoteUrl == null) {
        LaunchedEffect(slug) {
            val fetched = try {
                remotePhotoFetcher?.invoke(slug)
            } catch (_: Exception) {
                null
            }
            if (!fetched.isNullOrBlank()) remoteUrl = fetched
        }
    }
    return remoteUrl
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
 * Cloud upgrade: when [slug] is provided and [remotePhotoFetcher] is wired,
 * the local [photoUrl]/monogram renders instantly and the cloud portrait
 * swaps in only on null→url (never blank, never flashes, never blocks the
 * UI). [slug] defaults to null so existing callers are unaffected.
 *
 * @param photoUrl nullable local portrait URL; blank/null renders the monogram well.
 * @param nameEn English name used for the monogram + content description.
 * @param ring when true uses the bright chromium top-specular border, else the ghost card border.
 * @param slug reciter slug used for the async cloud-photo lookup; null disables it.
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
    slug: String? = null,
) {
    val remoteUrl = rememberCloudPhoto(slug)
    NoirArtworkWell(
        coverUrl = remoteUrl ?: photoUrl.orEmpty(),
        monogram = monogramForReciter(nameEn),
        shape = shape,
        modifier = modifier,
        size = size,
        monogramSize = monogramSize,
        ring = ring,
    )
}

