package com.ghais.ui.screens.reciters

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.screens.home.NoirArtworkWell

/**
 * Noir reciter section chrome — reference [com.ghais.ui.theme.GhaisNoir].
 *
 * Thin reciter-specific wrappers over the shared primitives
 * ([NoirSectionHeader], [NoirArtworkWell]) so reciter screens share one label
 * rhythm and one grayscale artwork recipe with Home/Profile. No new
 * dependencies; no caller edits required (purely additive, backward-compatible).
 */

/**
 * Nation reel header: bright Noir label + ghost "See all" action.
 * Replaces the legacy 22sp ExtraBold + LinkBlue link pattern inline in
 * NationReelBlock without touching its callers.
 *
 * @param nation section label (e.g. "Saudi Arabia").
 * @param count optional trailing count rendered as "nation • N"; null keeps the plain label.
 */
@Composable
fun NoirReciterSectionHeader(
    nation: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    NoirSectionHeader(
        label = if (count != null) "$nation • $count" else nation,
        modifier = modifier,
        actionLabel = if (onSeeAll != null) "See all" else null,
        onAction = onSeeAll,
    )
}

/**
 * Grayscale reciter artwork delegating to the shared home [NoirArtworkWell]:
 * saturation-0 photo in a clay well with monogram fallback and optional
 * chromium ring. Prefer this over raw AsyncImage in reciter rows/cards.
 */
@Composable
fun NoirReciterArtwork(
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
