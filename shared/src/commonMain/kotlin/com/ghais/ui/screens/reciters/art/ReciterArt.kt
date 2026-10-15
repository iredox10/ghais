package com.ghais.ui.screens.reciters.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import kotlin.random.Random

/**
 * Agent A6 — deterministic generative reciter art.
 *
 * Fallback artwork for reciters with no photo ([photoForSlug] / cloud both
 * null): a seeded vertical-bars "waveform" on a Noir gradient, drawn with
 * [Canvas]. Monochrome only — depth comes from bar alpha, never hue.
 *
 * [artSeed] is a manual FNV-style fold (not `String.hashCode`) so the seed —
 * and therefore the artwork — is identical on every platform. `Random(seed)`
 * guarantees the same sequence per seed across platforms, so the same slug
 * always paints the same bars.
 */

/** Deterministic seed for [slug]: lowercase, `_` -> `-`, blank -> 0. */
fun artSeed(slug: String): Int {
    val clean = slug.trim().lowercase().replace('_', '-')
    if (clean.isEmpty()) return 0
    var h = 7
    for (c in clean) h = h * 31 + c.code
    return h
}

/**
 * Seeded waveform artwork used when no photo exists.
 *
 * @param slug reciter slug driving the pattern (same slug, same art).
 * @param monogram optional center initial painted over the bars; null hides it.
 */
@Composable
fun ReciterArtwork(
    slug: String,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    shape: Shape = CircleShape,
    monogram: String? = null,
    monogramSize: TextUnit = 26.sp,
) {
    val seed = remember(slug) { artSeed(slug) }
    // 28 mirrored bar heights in 0.18..1.0, fixed at composition for the seed.
    val bars = remember(seed) {
        val random = Random(seed)
        List(28) { 0.18f + random.nextFloat() * 0.82f }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF17171A), GhaisNoir.NoirBlack)
                ),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // NOTE: `size` here is the Dp artwork param (it shadows
            // DrawScope.size); convert once to px for bar math.
            val sidePx = size.toPx()
            val n = bars.size
            val gap = sidePx / (n * 1.6f)
            val barW = gap * 0.72f
            val totalW = n * gap
            var x = (sidePx - totalW) / 2f + (gap - barW) / 2f
            for (fraction in bars) {
                val barH = sidePx * 0.72f * fraction
                val top = (sidePx - barH) / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f + 0.38f * fraction),
                    topLeft = Offset(x, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
                x += gap
            }
        }
        if (!monogram.isNullOrEmpty()) {
            Text(
                text = monogram,
                color = GhaisNoir.TextPrimary,
                fontSize = monogramSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
