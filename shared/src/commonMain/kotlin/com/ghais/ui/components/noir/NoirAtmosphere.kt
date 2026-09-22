package com.ghais.ui.components.noir

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas as DrawCanvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ghais.ui.theme.GhaisNoir
import kotlin.random.Random

/**
 * Phase 2 — Noir atmosphere: film grain + ambient top glow.
 *
 * Grain is drawn as a deterministic pseudo-random dot field on a Canvas
 * (remembered by size bucket) — no asset needed, works on all targets.
 * Keep the field sparse: ~1 dot per 3px at 3% alpha kills gradient banding
 * without a visible noise texture.
 */

private const val GRAIN_STEP_PX = 3
private const val GRAIN_DOT_PX = 1f

/**
 * Full-screen film-grain overlay. Place LAST in the screen root Box.
 *
 * Performance: the dot field (~300k dots at 3px step) is rendered ONCE into
 * an offscreen bitmap per size bucket; every frame (including the 4Hz
 * playback-position recompositions) is a single drawImage. Never draw the
 * dots inline — that drops frames on every progress tick.
 */
@Composable
fun NoirGrainOverlay(modifier: Modifier = Modifier) {
    // Deterministic seed so rebuilds don't shimmer.
    val seedPoints = remember { generateGrainField() }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = constraints.maxWidth.coerceAtLeast(1)
        val h = constraints.maxHeight.coerceAtLeast(1)
        val tile = remember(w, h) {
            renderGrainTile(w, h, seedPoints, density.density)
        }
        Image(
            bitmap = tile,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun renderGrainTile(
    w: Int,
    h: Int,
    seedPoints: IntArray,
    density: Float
): ImageBitmap {
    val bmp = ImageBitmap(w, h)
    val canvas = DrawCanvas(bmp)
    val step = GRAIN_STEP_PX * density
    val half = (w / step).toInt().coerceAtLeast(1)
    val rows = (h / step).toInt().coerceAtLeast(1)
    val total = half * rows
    val alpha = GhaisNoir.GrainAlpha
    val radius = GRAIN_DOT_PX * density
    val lightPaint = Paint().apply { color = Color.White.copy(alpha = alpha) }
    val darkPaint = Paint().apply { color = Color.Black.copy(alpha = alpha) }
    var i = 0
    while (i < total) {
        val s = seedPoints[i % seedPoints.size]
        val x = (i % half) * step + ((s ushr 16) % 100) / 100f * step
        val y = (i / half) * step + ((s ushr 8) % 100) / 100f * step
        canvas.drawCircle(Offset(x, y), radius, if ((s and 1) == 0) lightPaint else darkPaint)
        i++
    }
    return bmp
}

private fun generateGrainField(): IntArray {
    val rng = Random(0x6E6F6972) // "noir" — deterministic, shimmer-free
    return IntArray(4096) { rng.nextInt() }
}

/** Ambient top glow — diffused radial light bleeding from top-center. */
fun Modifier.noirAmbientGlow(): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                GhaisNoir.AmbientGlow,
                Color.Transparent
            ),
            center = Offset(size.width / 2f, -size.height * 0.25f),
            radius = size.height * 0.75f
        )
    )
}

/** Screen root: noir canvas (glow zone -> black) + slot for content. */
@Composable
fun NoirScreenRoot(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    grain: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(GhaisNoir.canvasFill()) }
            .then(if (glow) Modifier.noirAmbientGlow() else Modifier)
    ) {
        content()
        if (grain) NoirGrainOverlay()
    }
}
