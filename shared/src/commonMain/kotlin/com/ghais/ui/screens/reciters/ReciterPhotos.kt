package com.ghais.ui.screens.reciters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.ReciterCloudCache
import com.ghais.data.seed.GhaisAssets
import com.ghais.data.seed.QuranDataRepository as SeedRepository
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.util.realImageUrlOrNull

private fun reciterPhotoOrNull(photoUrl: String?): String? =
    realImageUrlOrNull(photoUrl)

private fun cloudPhotoForSlug(slug: String): String? {
    if (slug.isBlank()) return null
    val clean = normalizeReciterSlug(slug)
    return reciterPhotoOrNull(ReciterCloudCache.cloudImageForSlug(clean))
        ?: reciterPhotoOrNull(
            ReciterCloudCache.cloudReciters.value.firstOrNull {
                normalizeReciterSlug(it.slug) == clean
            }?.imageUrl
        )
}

fun photoForSlug(slug: String): String? {
    // Cloud first (admin-uploaded via Appwrite `reciters.image_url`), then local.
    cloudPhotoForSlug(slug)?.let { return it }
    val clean = normalizeReciterSlug(slug)
    val verified = ALL_VERIFIED_RECITERS.find {
        val vSlug = normalizeReciterSlug(it.slug)
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
    if (verified != null) return reciterPhotoOrNull(verified.photoUrl)

    val photos = GhaisAssets.VerifiedReciters
    return reciterPhotoOrNull(
        when {
            clean.contains("alafasy") || clean.contains("mishary") -> photos.firstOrNull { normalizeReciterSlug(it.slug) == "mishary" }?.photoUrl
            clean.contains("sudais") -> photos.firstOrNull { normalizeReciterSlug(it.slug) == "al-sudais" }?.photoUrl
            clean.contains("muaiqly") -> photos.firstOrNull { normalizeReciterSlug(it.slug) == "al-muaiqly" }?.photoUrl
            clean.contains("dossari") -> photos.firstOrNull { normalizeReciterSlug(it.slug) == "al-dossari" }?.photoUrl
            clean.contains("basit") || clean.contains("abdulbaset") -> photos.firstOrNull { normalizeReciterSlug(it.slug) == "abdul-basit" }?.photoUrl
            clean.contains("husary") -> ALL_VERIFIED_RECITERS.firstOrNull { normalizeReciterSlug(it.slug) == "husary" }?.photoUrl
            clean.contains("minshawi") -> ALL_VERIFIED_RECITERS.firstOrNull { normalizeReciterSlug(it.slug) == "minshawi" }?.photoUrl
            clean.contains("shuraim") || clean.contains("shuraym") -> ALL_VERIFIED_RECITERS.firstOrNull { normalizeReciterSlug(it.slug) == "shuraim" }?.photoUrl
            else -> null
        }
    )
}

private fun normalizeReciterSlug(slug: String): String =
    slug.trim().lowercase().replace('_', '-')

fun resolveReciterPhoto(slug: String?): String? {
    if (slug.isNullOrBlank()) return null
    val clean = normalizeReciterSlug(slug)

    cloudPhotoForSlug(clean)?.let { return it }

    val browseReciter = QuranDataRepository.getBrowseReciterBySlug(clean)
        ?: QuranDataRepository.getBrowseReciters().firstOrNull {
            normalizeReciterSlug(it.slug) == clean
        }
    val directReciter = QuranDataRepository.getReciterBySlug(clean)
    val aliasCloudPhoto = reciterPhotoOrNull(browseReciter?.imageUrl)
        ?: reciterPhotoOrNull(
            directReciter.takeIf { normalizeReciterSlug(it.slug) == clean }?.imageUrl
        )
    aliasCloudPhoto?.let { return it }

    val canonicalSlug = browseReciter?.slug?.let(::normalizeReciterSlug)
    photoForSlug(clean)?.let { return it }

    val detailedReciter = SeedRepository.getReciterBySlug(clean)
        ?: canonicalSlug?.let { SeedRepository.getReciterBySlug(it) }
    reciterPhotoOrNull(detailedReciter?.photoUrl)?.let { return it }

    return null
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

@Composable
fun rememberCloudPhoto(slug: String?): String? =
    rememberReciterPhoto(slug)

@Composable
fun rememberReciterPhoto(slug: String?): String? {
    val cloudCatalog by ReciterCloudCache.cloudReciters.collectAsState()
    val resolvedPhoto = remember(slug, cloudCatalog) { resolveReciterPhoto(slug) }
    var remoteUrl by remember(slug) { mutableStateOf<String?>(null) }
    var fetchAttempted by remember(slug) { mutableStateOf(false) }

    if (resolvedPhoto == null && !slug.isNullOrBlank() && !fetchAttempted) {
        LaunchedEffect(slug) {
            fetchAttempted = true
            val fetched = try {
                remotePhotoFetcher?.invoke(slug)
            } catch (_: Exception) {
                null
            }
            reciterPhotoOrNull(fetched)?.let { remoteUrl = it }
        }
    }

    return resolvedPhoto ?: remoteUrl
}

/**
 * Noir reciter artwork helpers — reference [com.ghais.ui.theme.GhaisNoir].
 *
 * All reciter photo/avatar rendering must go through [NoirReciterAvatar] (or
 * the section-level [NoirReciterArtwork] in NoirReciterSections.kt), which
 * delegates to the shared home [NoirArtworkWell]: natural-colour artwork by
 * default with an optional explicit grayscale treatment, monogram fallback in
 * a clay well, optional chromium ring. [photoForSlug] above is intentionally
 * untouched so existing callers (RecitersScreen, RegionRecitersScreen,
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
 * Reciter avatar in a clay well with monogram fallback. Portraits render in
 * natural colour by default, with an optional explicit grayscale treatment.
 *
 * Cloud upgrade: when [slug] is provided and [remotePhotoFetcher] is wired,
 * the local [photoUrl]/monogram renders instantly and the cloud portrait
 * swaps in only on null→url (never blank, never flashes, never blocks the
 * UI). [slug] defaults to null so existing callers are unaffected. The
 * monochrome treatment is opt-in per call site by setting [grayscale] to true.
 *
 * @param photoUrl nullable local portrait URL; blank/null renders the monogram well.
 * @param nameEn English name used for the monogram + content description.
 * @param ring when true uses the bright chromium top-specular border, else the ghost card border.
 * @param slug reciter slug used for the async cloud-photo lookup; null disables it.
 * @param grayscale when true applies the shared grayscale artwork treatment.
 * @param scrimAlpha opacity of the artwork scrim.
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
    grayscale: Boolean = false,
    scrimAlpha: Float = 0.35f,
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
        grayscale = grayscale,
        scrimAlpha = scrimAlpha,
    )
}

