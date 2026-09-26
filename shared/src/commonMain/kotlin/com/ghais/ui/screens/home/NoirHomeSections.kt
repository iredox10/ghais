package com.ghais.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.util.realImageUrlOrNull

/**
 * Phase 4 — Noir Home shared chrome.
 *
 * Same depth recipe as the Profile screen: alpha-white fills + 1px ghost border
 * + bright TOP-ONLY specular. No hue anywhere — state reads through fill
 * elevation (Fill2 -> Fill4), chromium fill, weight and opacity.
 */

/**
 * Optional true-grayscale filter for the explicit monochrome image path.
 * Instantiated once (ColorMatrix is immutable in use) so recompositions never
 * re-allocate it.
 */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Recessed artwork plate. Falls back to a monogram when there is no artwork,
 * mirroring the Profile identity pill.
 *
 * [grayscale] applies the optional monochrome image treatment when true;
 * images render in their natural colour by default. [scrimAlpha] controls the
 * dark overlay applied only when artwork is present.
 */
@Composable
fun NoirArtworkWell(
    coverUrl: String,
    monogram: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    monogramSize: TextUnit = 22.sp,
    ring: Boolean = false,
    grayscale: Boolean = false,
    scrimAlpha: Float = 0.35f,
    errorFallbackUrl: String? = null
) {
    var imageFailed by remember(coverUrl) { mutableStateOf(false) }
    val requested = remember(coverUrl) { realImageUrlOrNull(coverUrl) }
    val effectiveUrl = if (imageFailed) realImageUrlOrNull(errorFallbackUrl) else requested
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(GhaisNoir.wellFill())
            .border(1.dp, if (ring) GhaisNoir.SpecularTop else GhaisNoir.BorderCard, shape),
        contentAlignment = Alignment.Center
    ) {
        if (effectiveUrl != null) {
            AsyncImage(
                model = effectiveUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (grayscale) NoirGrayscale else null,
                onState = { state ->
                    if (state is coil3.compose.AsyncImagePainter.State.Error) imageFailed = true
                },
                modifier = Modifier.fillMaxSize()
            )
            // Darkening scrim: keeps the plate recessed instead of glowing.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        } else {
            Text(
                text = monogram,
                color = GhaisNoir.TextPrimary,
                fontSize = monogramSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Thin ghost divider for stacked rows inside a Noir card. */
@Composable
fun NoirHomeDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GhaisNoir.BorderGhost)
    )
}

/** Non-interactive stat chip — informational, so it carries no press affordance. */
@Composable
fun NoirStatChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Engraved concentric rings — the monochrome stand-in for the old blue/violet
 * playlist gradients. Depth comes from ring alpha, not hue.
 */
@Composable
fun NoirEngravedRings(
    modifier: Modifier = Modifier,
    ringSize: Dp = 96.dp
) {
    Canvas(modifier = modifier.size(ringSize)) {
        val half = this.size.minDimension / 2f
        val step = half / 4f
        repeat(4) { i ->
            drawCircle(
                color = Color.White.copy(alpha = 0.08f + i * 0.06f),
                radius = half - i * step,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
