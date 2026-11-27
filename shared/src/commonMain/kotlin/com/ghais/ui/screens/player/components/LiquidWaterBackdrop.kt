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

private data class WaterWaveLayer(
    val baseYFraction: Float,
    val amplitudeFraction: Float,
    val cycles: Float,
    val speedMultiplier: Float,
    val phaseOffset: Float,
    val topColor: Color,
    val bottomColor: Color,
    val crestColor: Color,
    val crestAlpha: Float,
)

private data class CausticMote(
    val xFraction: Float,
    val yOffset: Float,
    val radiusDp: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val alpha: Float,
    val tint: Color,
)

/**
 * Flowing liquid-water gradient backdrop shown in the Now Playing screen when
 * no ambient video/effect is active. Combines drifting deep oceanic/lavender
 * caustic light pools, layered translucent cubic-bezier water ribbons with
 * shimmering crests, and softly rising underwater light motes.
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
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waterPhase",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 23000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "causticDrift",
    )
    val energy by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.72f,
        animationSpec = tween(durationMillis = 900),
        label = "waterEnergy",
    )

    val waves = remember {
        listOf(
            WaterWaveLayer(
                baseYFraction = 0.24f,
                amplitudeFraction = 0.045f,
                cycles = 1.4f,
                speedMultiplier = 1f,
                phaseOffset = 0.0f,
                topColor = Color(0xFF4F46E5).copy(alpha = 0.34f),
                bottomColor = Color(0xFF06B6D4).copy(alpha = 0.04f),
                crestColor = Color(0xFFC9BFF2),
                crestAlpha = 0.38f,
            ),
            WaterWaveLayer(
                baseYFraction = 0.33f,
                amplitudeFraction = 0.052f,
                cycles = 1.8f,
                speedMultiplier = -1f,
                phaseOffset = 1.4f,
                topColor = Color(0xFF0EA5E9).copy(alpha = 0.30f),
                bottomColor = Color(0xFF312E81).copy(alpha = 0.05f),
                crestColor = Color(0xFF7DD3FC),
                crestAlpha = 0.45f,
            ),
            WaterWaveLayer(
                baseYFraction = 0.42f,
                amplitudeFraction = 0.048f,
                cycles = 2.1f,
                speedMultiplier = 1f,
                phaseOffset = 2.9f,
                topColor = Color(0xFF8B5CF6).copy(alpha = 0.28f),
                bottomColor = Color(0xFF0D9488).copy(alpha = 0.03f),
                crestColor = Color(0xFFE0E7FF),
                crestAlpha = 0.42f,
            ),
            WaterWaveLayer(
                baseYFraction = 0.51f,
                amplitudeFraction = 0.042f,
                cycles = 1.6f,
                speedMultiplier = -1f,
                phaseOffset = 4.2f,
                topColor = Color(0xFF06B6D4).copy(alpha = 0.26f),
                bottomColor = Color.Transparent,
                crestColor = Color(0xFFA5F3FC),
                crestAlpha = 0.50f,
            ),
            WaterWaveLayer(
                baseYFraction = 0.60f,
                amplitudeFraction = 0.034f,
                cycles = 2.4f,
                speedMultiplier = 1f,
                phaseOffset = 5.1f,
                topColor = Color(0xFFC9BFF2).copy(alpha = 0.20f),
                bottomColor = Color.Transparent,
                crestColor = Color(0xFFFFFFFF),
                crestAlpha = 0.34f,
            ),
        )
    }

    val motes = remember {
        listOf(
            CausticMote(0.16f, 0.12f, 22f, 1f, 0.03f, 0.22f, Color(0xFF38BDF8)),
            CausticMote(0.38f, 0.45f, 16f, 1f, 0.04f, 0.18f, Color(0xFFC9BFF2)),
            CausticMote(0.62f, 0.28f, 26f, 1f, 0.025f, 0.20f, Color(0xFF2DD4BF)),
            CausticMote(0.82f, 0.64f, 18f, 1f, 0.035f, 0.16f, Color(0xFF818CF8)),
            CausticMote(0.27f, 0.78f, 14f, 1f, 0.03f, 0.15f, Color(0xFFE0F2FE)),
            CausticMote(0.74f, 0.15f, 20f, 1f, 0.04f, 0.19f, Color(0xFF7DD3FC)),
        )
    }

    val wavePath = remember { Path() }
    val crestPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Deep nocturnal ocean base gradient
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF090D1A),
                0.45f to Color(0xFF0A1224),
                0.78f to Color(0xFF060810),
                1.0f to GhaisNoir.NoirBlack,
            )
        )

        // 2. Drifting underwater caustic light pools
        val poolCenters = listOf(
            Triple(
                Offset(w * (0.28f + 0.18f * cos(drift)), h * (0.20f + 0.09f * sin(drift * 1.0f))),
                w * 0.72f,
                Color(0xFF06B6D4).copy(alpha = 0.24f * energy),
            ),
            Triple(
                Offset(w * (0.74f + 0.16f * sin(drift)), h * (0.26f + 0.11f * cos(drift))),
                w * 0.68f,
                Color(0xFF6366F1).copy(alpha = 0.28f * energy),
            ),
            Triple(
                Offset(w * (0.50f + 0.20f * sin(drift + 2.1f)), h * (0.38f + 0.08f * cos(drift + 1.2f))),
                w * 0.65f,
                Color(0xFFC9BFF2).copy(alpha = 0.18f * energy),
            ),
            Triple(
                Offset(w * (0.22f + 0.14f * cos(drift + 3.4f)), h * (0.46f + 0.07f * sin(drift + 2.5f))),
                w * 0.55f,
                Color(0xFF0D9488).copy(alpha = 0.22f * energy),
            ),
        )
        poolCenters.forEach { (center, radius, color) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        // 3. Layered translucent flowing water ribbons + luminous crests
        val steps = 28
        val stepX = w / steps
        val crestStrokePx = 1.6.dp.toPx()

        waves.forEach { layer ->
            wavePath.reset()
            crestPath.reset()

            val baseY = h * layer.baseYFraction
            val amp = h * layer.amplitudeFraction * energy
            val omega = (2.0 * PI * layer.cycles) / w
            val shift = phase * layer.speedMultiplier + layer.phaseOffset

            fun waveY(x: Float): Float {
                val primary = sin(x * omega + shift).toFloat()
                val harmonic = 0.42f * cos(x * omega * 1.7f - shift * 0.8f).toFloat()
                return baseY + amp * (primary + harmonic)
            }

            val startY = waveY(0f)
            wavePath.moveTo(0f, startY)
            crestPath.moveTo(0f, startY)

            var prevX = 0f
            var prevY = startY
            for (i in 1..steps) {
                val x = i * stepX
                val y = waveY(x)
                val midX = (prevX + x) * 0.5f
                val midY = (prevY + y) * 0.5f
                wavePath.quadraticTo(prevX, prevY, midX, midY)
                crestPath.quadraticTo(prevX, prevY, midX, midY)
                prevX = x
                prevY = y
            }
            val endY = waveY(w)
            wavePath.lineTo(w, endY)
            crestPath.lineTo(w, endY)

            wavePath.lineTo(w, h)
            wavePath.lineTo(0f, h)
            wavePath.close()

            drawPath(
                path = wavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        layer.topColor.copy(alpha = layer.topColor.alpha * energy),
                        layer.bottomColor,
                        Color.Transparent,
                    ),
                    startY = baseY - amp * 1.5f,
                    endY = (baseY + h * 0.34f).coerceAtMost(h),
                ),
            )

            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.18f to layer.crestColor.copy(alpha = layer.crestAlpha * energy),
                    0.50f to Color.White.copy(alpha = layer.crestAlpha * 1.15f * energy),
                    0.82f to layer.crestColor.copy(alpha = layer.crestAlpha * energy),
                    1.0f to Color.Transparent,
                ),
                style = Stroke(width = crestStrokePx, cap = StrokeCap.Round),
            )
        }

        // 4. Softly rising caustic light droplets
        val twoPi = (2.0 * PI).toFloat()
        motes.forEach { mote ->
            val progress = ((drift / twoPi) * mote.speed + mote.yOffset) % 1f
            val y = h * (0.64f - progress * 0.56f)
            val x = w * (mote.xFraction + mote.swayAmplitude * sin(phase + mote.yOffset * twoPi))
            val radius = mote.radiusDp.dp.toPx()
            val fade = sin(progress * PI).toFloat().coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        mote.tint.copy(alpha = mote.alpha * fade * energy),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}
