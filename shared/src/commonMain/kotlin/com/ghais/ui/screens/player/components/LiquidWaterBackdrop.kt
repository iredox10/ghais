package com.ghais.ui.screens.player.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ghais.ui.theme.GhaisNoir
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FluidWave(
    val baseYFraction: Float,
    val amplitudeFraction: Float,
    val cycles: Float,
    val speedMultiplier: Float,
    val phaseOffset: Float,
    val bodyTopColor: Color,
    val bodyMidColor: Color,
    val crestColor: Color,
    val crestWidthDp: Float,
)

/**
 * Minimal, full-bleed liquid water gradient backdrop for the Now Playing screen
 * when no ambient video is active.
 *
 * Combines two complementary layers in the native GhaisNoir palette:
 * 1. An ultra-blurry (36dp) Noir/Plum/Lavender ambient aura and drifting light pools.
 * 2. A softly diffused (14dp) flowing water-wave layer with luminous Lavender/Chrome
 *    crests so the wave motion stays clearly visible, calm, and non-distracting.
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
            animation = tween(durationMillis = 13500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waterPhase",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "causticDrift",
    )
    val breath by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientBreath",
    )
    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.70f,
        animationSpec = tween(durationMillis = 1000),
        label = "waterEnergy",
    )

    val waves = remember {
        listOf(
            FluidWave(
                baseYFraction = 0.28f,
                amplitudeFraction = 0.068f,
                cycles = 1.15f,
                speedMultiplier = 1f,
                phaseOffset = 0.0f,
                bodyTopColor = GhaisNoir.PlumTop.copy(alpha = 0.20f),
                bodyMidColor = GhaisNoir.LavenderDeep.copy(alpha = 0.11f),
                crestColor = GhaisNoir.Lavender.copy(alpha = 0.12f),
                crestWidthDp = 22f,
            ),
            FluidWave(
                baseYFraction = 0.48f,
                amplitudeFraction = 0.082f,
                cycles = 1.35f,
                speedMultiplier = -1f,
                phaseOffset = 2.0f,
                bodyTopColor = GhaisNoir.LavenderDeep.copy(alpha = 0.24f),
                bodyMidColor = GhaisNoir.PlumBottom.copy(alpha = 0.14f),
                crestColor = GhaisNoir.ChromeTop.copy(alpha = 0.09f),
                crestWidthDp = 26f,
            ),
            FluidWave(
                baseYFraction = 0.67f,
                amplitudeFraction = 0.072f,
                cycles = 1.10f,
                speedMultiplier = 1f,
                phaseOffset = 3.8f,
                bodyTopColor = GhaisNoir.PlumTop.copy(alpha = 0.22f),
                bodyMidColor = GhaisNoir.PlumBottom.copy(alpha = 0.15f),
                crestColor = GhaisNoir.Lavender.copy(alpha = 0.13f),
                crestWidthDp = 24f,
            ),
            FluidWave(
                baseYFraction = 0.84f,
                amplitudeFraction = 0.055f,
                cycles = 1.25f,
                speedMultiplier = -1f,
                phaseOffset = 5.1f,
                bodyTopColor = GhaisNoir.LavenderDeep.copy(alpha = 0.20f),
                bodyMidColor = GhaisNoir.CardTintBottom.copy(alpha = 0.16f),
                crestColor = GhaisNoir.Lavender.copy(alpha = 0.09f),
                crestWidthDp = 20f,
            ),
        )
    }

    val fillPath = remember { Path() }
    val crestPath = remember { Path() }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Ultra-blurry native Noir/Plum ambient glow & drifting pools
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(36.dp)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to GhaisNoir.CanvasTop,
                    0.26f to GhaisNoir.CardTintTop,
                    0.58f to Color(0xFF151024),
                    0.86f to GhaisNoir.CardTintBottom,
                    1.0f to GhaisNoir.NoirBlack,
                )
            )

            val auraCenter = Offset(
                x = w * (0.50f + 0.08f * sin(drift * 0.5f)),
                y = h * (0.36f + 0.05f * cos(drift * 0.5f)),
            )
            val auraRadius = w * 0.90f * breath
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to GhaisNoir.Lavender.copy(alpha = 0.16f * energy),
                    0.35f to GhaisNoir.PlumTop.copy(alpha = 0.22f * energy),
                    0.70f to GhaisNoir.LavenderDeep.copy(alpha = 0.10f * energy),
                    1.0f to Color.Transparent,
                    center = auraCenter,
                    radius = auraRadius,
                ),
                radius = auraRadius,
                center = auraCenter,
            )

            val pools = listOf(
                Triple(
                    Offset(w * (0.22f + 0.20f * cos(drift)), h * (0.20f + 0.09f * sin(drift))),
                    w * 0.92f,
                    GhaisNoir.PlumTop.copy(alpha = 0.24f * energy),
                ),
                Triple(
                    Offset(w * (0.80f + 0.16f * sin(drift)), h * (0.36f + 0.10f * cos(drift))),
                    w * 0.88f,
                    GhaisNoir.LavenderDeep.copy(alpha = 0.26f * energy),
                ),
                Triple(
                    Offset(w * (0.48f + 0.22f * sin(drift + 1.9f)), h * (0.64f + 0.10f * cos(drift + 1.1f))),
                    w * 0.88f,
                    GhaisNoir.PlumBottom.copy(alpha = 0.30f * energy),
                ),
            )
            pools.forEach { (center, radius, color) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to color,
                        0.45f to color.copy(alpha = color.alpha * 0.42f),
                        1.0f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }

        // Layer 2: Ultra-blurry flowing liquid waves with soft Lavender/Chrome crests
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val steps = 28
            val stepX = w / steps

            waves.forEach { wave ->
                fillPath.reset()
                crestPath.reset()

                val baseY = h * wave.baseYFraction
                val amp = h * wave.amplitudeFraction * energy
                val omega = (2.0 * PI * wave.cycles) / w
                val shift = phase * wave.speedMultiplier + wave.phaseOffset

                fun waveY(x: Float): Float {
                    val primary = sin(x * omega + shift).toFloat()
                    val secondary = 0.35f * cos(x * omega * 1.6f - shift * 0.8f).toFloat()
                    return baseY + amp * (primary + secondary)
                }

                val startY = waveY(0f)
                fillPath.moveTo(0f, startY)
                crestPath.moveTo(0f, startY)

                var prevX = 0f
                var prevY = startY
                for (i in 1..steps) {
                    val x = i * stepX
                    val y = waveY(x)
                    val midX = (prevX + x) * 0.5f
                    val midY = (prevY + y) * 0.5f
                    fillPath.quadraticTo(prevX, prevY, midX, midY)
                    crestPath.quadraticTo(prevX, prevY, midX, midY)
                    prevX = x
                    prevY = y
                }
                val endY = waveY(w)
                fillPath.lineTo(w, endY)
                crestPath.lineTo(w, endY)

                fillPath.lineTo(w, h)
                fillPath.lineTo(0f, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            wave.bodyTopColor.copy(alpha = wave.bodyTopColor.alpha * energy),
                            wave.bodyMidColor.copy(alpha = wave.bodyMidColor.alpha * energy),
                            Color.Transparent,
                        ),
                        startY = baseY - amp * 1.3f,
                        endY = (baseY + h * 0.34f).coerceAtMost(h),
                    ),
                )

                drawPath(
                    path = crestPath,
                    color = wave.crestColor.copy(alpha = wave.crestColor.alpha * energy),
                    style = Stroke(
                        width = wave.crestWidthDp.dp.toPx(),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
    }
}
