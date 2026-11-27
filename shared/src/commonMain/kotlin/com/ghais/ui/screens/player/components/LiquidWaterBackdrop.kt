package com.ghais.ui.screens.player.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FluidSwell(
    val baseYFraction: Float,
    val amplitudeFraction: Float,
    val cycles: Float,
    val speedMultiplier: Float,
    val phaseOffset: Float,
    val topColor: Color,
    val midColor: Color,
)

/**
 * Minimal, ultra-blurry, full-bleed liquid water gradient backdrop for the
 * Now Playing screen when no ambient video is active. Uses large, softly
 * diffused oceanic/indigo/lavender light pools and stroke-free translucent
 * water swells spanning the entire viewport height.
 */
@Composable
fun LiquidWaterBackdrop(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "liquidWater")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waterPhase",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "causticDrift",
    )
    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.78f,
        animationSpec = tween(durationMillis = 1000),
        label = "waterEnergy",
    )

    val swells = remember {
        listOf(
            FluidSwell(
                baseYFraction = 0.28f,
                amplitudeFraction = 0.065f,
                cycles = 1.15f,
                speedMultiplier = 1f,
                phaseOffset = 0.0f,
                topColor = Color(0xFF38BDF8).copy(alpha = 0.22f),
                midColor = Color(0xFF4F46E5).copy(alpha = 0.12f),
            ),
            FluidSwell(
                baseYFraction = 0.54f,
                amplitudeFraction = 0.070f,
                cycles = 1.35f,
                speedMultiplier = -1f,
                phaseOffset = 2.1f,
                topColor = Color(0xFF818CF8).copy(alpha = 0.20f),
                midColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
            ),
            FluidSwell(
                baseYFraction = 0.78f,
                amplitudeFraction = 0.055f,
                cycles = 1.10f,
                speedMultiplier = 1f,
                phaseOffset = 4.0f,
                topColor = Color(0xFF2DD4BF).copy(alpha = 0.18f),
                midColor = Color(0xFF6366F1).copy(alpha = 0.08f),
            ),
        )
    }

    val swellPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(26.dp)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Full-screen deep abyssal oceanic gradient (no flat black bottom)
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF070B18),
                0.38f to Color(0xFF0A1428),
                0.72f to Color(0xFF091326),
                1.0f to Color(0xFF060A16),
            )
        )

        // 2. Large, dreamy, blurred underwater light pools spanning top to bottom
        val pools = listOf(
            Triple(
                Offset(w * (0.24f + 0.22f * cos(drift)), h * (0.18f + 0.10f * sin(drift))),
                w * 0.95f,
                Color(0xFF06B6D4).copy(alpha = 0.34f * energy),
            ),
            Triple(
                Offset(w * (0.78f + 0.18f * sin(drift)), h * (0.30f + 0.12f * cos(drift))),
                w * 0.92f,
                Color(0xFF6366F1).copy(alpha = 0.36f * energy),
            ),
            Triple(
                Offset(w * (0.46f + 0.24f * sin(drift + 1.9f)), h * (0.55f + 0.11f * cos(drift + 1.1f))),
                w * 1.00f,
                Color(0xFFC9BFF2).copy(alpha = 0.22f * energy),
            ),
            Triple(
                Offset(w * (0.22f + 0.20f * cos(drift + 3.2f)), h * (0.76f + 0.10f * sin(drift + 2.4f))),
                w * 0.95f,
                Color(0xFF0D9488).copy(alpha = 0.32f * energy),
            ),
            Triple(
                Offset(w * (0.76f + 0.18f * cos(drift + 4.5f)), h * (0.88f + 0.08f * sin(drift + 1.7f))),
                w * 0.90f,
                Color(0xFF4F46E5).copy(alpha = 0.30f * energy),
            ),
        )
        pools.forEach { (center, radius, color) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = color.alpha * 0.35f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        // 3. Three ultra-soft, stroke-free translucent fluid water swells
        val steps = 20
        val stepX = w / steps

        swells.forEach { swell ->
            swellPath.reset()
            val baseY = h * swell.baseYFraction
            val amp = h * swell.amplitudeFraction * energy
            val omega = (2.0 * PI * swell.cycles) / w
            val shift = phase * swell.speedMultiplier + swell.phaseOffset

            fun swellY(x: Float): Float {
                val primary = sin(x * omega + shift).toFloat()
                val secondary = 0.35f * cos(x * omega * 1.4f - shift * 0.7f).toFloat()
                return baseY + amp * (primary + secondary)
            }

            val startY = swellY(0f)
            swellPath.moveTo(0f, startY)

            var prevX = 0f
            var prevY = startY
            for (i in 1..steps) {
                val x = i * stepX
                val y = swellY(x)
                val midX = (prevX + x) * 0.5f
                val midY = (prevY + y) * 0.5f
                swellPath.quadraticTo(prevX, prevY, midX, midY)
                prevX = x
                prevY = y
            }
            swellPath.lineTo(w, swellY(w))
            swellPath.lineTo(w, h)
            swellPath.lineTo(0f, h)
            swellPath.close()

            drawPath(
                path = swellPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        swell.topColor.copy(alpha = swell.topColor.alpha * energy),
                        swell.midColor.copy(alpha = swell.midColor.alpha * energy),
                        Color.Transparent,
                    ),
                    startY = baseY - amp * 1.4f,
                    endY = (baseY + h * 0.42f).coerceAtMost(h),
                ),
            )
        }
    }
}
