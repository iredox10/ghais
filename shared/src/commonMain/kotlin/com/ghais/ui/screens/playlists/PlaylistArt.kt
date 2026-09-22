package com.ghais.ui.screens.playlists

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Artistic monochrome covers for each playlist card — strict Noir Glass.
 *
 * Zero hue: every background is a grayscale ramp (R == G == B) and every
 * motif is drawn in alpha-white line work, so covers read as engraved tiles
 * rather than colored stickers. Motifs stay distinct per playlist (rings /
 * petals / moon / rays / burst) through geometry and alpha, never color.
 * Pure Compose Canvas — no image assets needed.
 */
@Composable
fun PlaylistCover(art: PlaylistArt, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(coverBackground(art))) {
        when (art) {
            PlaylistArt.FAVOURITES -> {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                )
            }
            PlaylistArt.FOCUS_WORK -> ConcentricRingsArt()
            PlaylistArt.BEAUTIFUL -> FlowerArt()
            PlaylistArt.SLEEP -> SleepArt()
            PlaylistArt.DUAA_RUQIA -> RaysArt()
            PlaylistArt.EMOTIONAL -> BurstArt()
            PlaylistArt.STUDY -> ConcentricRingsArt()
            PlaylistArt.TAHAJJUD -> SleepArt()
            PlaylistArt.SUNRISE -> RaysArt()
        }
    }
}

/**
 * Grayscale cover ramps only — each playlist keeps its own elevation so cards
 * stay distinguishable, with depth from lightness rather than hue.
 */
private fun coverBackground(art: PlaylistArt): Brush = when (art) {
    PlaylistArt.FAVOURITES -> Brush.verticalGradient(listOf(Color(0xFF232326), Color(0xFF101012)))
    PlaylistArt.FOCUS_WORK -> Brush.verticalGradient(listOf(Color(0xFF26262B), Color(0xFF0B0B0D)))
    PlaylistArt.BEAUTIFUL -> Brush.verticalGradient(listOf(Color(0xFF2A2A2E), Color(0xFF0E0E10)))
    PlaylistArt.SLEEP -> Brush.verticalGradient(listOf(Color(0xFF1C1C20), Color(0xFF08080A)))
    PlaylistArt.DUAA_RUQIA -> Brush.verticalGradient(listOf(Color(0xFF222226), Color(0xFF0C0C0E)))
    PlaylistArt.EMOTIONAL -> Brush.verticalGradient(listOf(Color(0xFF2E2E34), Color(0xFF0A0A0C)))
    PlaylistArt.STUDY -> Brush.verticalGradient(listOf(Color(0xFF202027), Color(0xFF0B0B0D)))
    PlaylistArt.TAHAJJUD -> Brush.verticalGradient(listOf(Color(0xFF18181C), Color(0xFF070708)))
    PlaylistArt.SUNRISE -> Brush.verticalGradient(listOf(Color(0xFF242428), Color(0xFF0D0D0F)))
}

@Composable
private fun ConcentricRingsArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.34f
        val maxR = size.width * 0.52f
        for (i in 4 downTo 1) {
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = maxR * i / 4f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
        // soft top glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, 0f),
                radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(cx, 0f)
        )
    }
}

@Composable
private fun FlowerArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val petalR = size.width * 0.20f
        val orbitR = size.width * 0.20f
        repeat(8) { k ->
            val a = k * (PI / 4.0)
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = petalR,
                center = Offset(
                    cx + (orbitR * cos(a)).toFloat(),
                    cy + (orbitR * sin(a)).toFloat()
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = 0.30f),
            radius = petalR * 0.55f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
        )
    }
}

@Composable
private fun SleepArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // moon glow, lower-left — monochrome white wash, never blue.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * 0.30f, size.height * 0.62f),
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.30f, size.height * 0.62f)
        )
        // thin orbital arc
        drawArc(
            color = Color.White.copy(alpha = 0.35f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(-size.width * 0.25f, size.height * 0.10f),
            size = androidx.compose.ui.geometry.Size(size.width * 1.5f, size.height * 0.9f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
    // floating Z's, rising to the top-right
    Box(modifier = Modifier.fillMaxSize()) {
        SleepZ("Z", 30, 0.60f, Alignment.TopEnd, end = 16, top = 14)
        SleepZ("Z", 22, 0.40f, Alignment.TopEnd, end = 44, top = 42)
        SleepZ("Z", 15, 0.28f, Alignment.TopEnd, end = 68, top = 64)
    }
}

@Composable
private fun BoxScope.SleepZ(
    text: String,
    sizeSp: Int,
    alpha: Float,
    align: Alignment,
    end: Int,
    top: Int
) {
    androidx.compose.material3.Text(
        text = text,
        color = Color.White.copy(alpha = alpha),
        fontSize = sizeSp.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier
            .align(align)
            .padding(end = end.dp, top = top.dp)
    )
}

@Composable
private fun RaysArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val origin = Offset(size.width * 0.15f, size.height * 1.05f)
        for (i in 0..10) {
            val a = -90.0 + i * (80.0 / 10.0)
            val rad = a * PI / 180.0
            drawLine(
                color = Color.White.copy(alpha = 0.30f),
                start = origin,
                end = Offset(
                    origin.x + (size.width * 1.4f * cos(rad)).toFloat(),
                    origin.y + (size.width * 1.4f * sin(rad)).toFloat()
                ),
                strokeWidth = 2f
            )
        }
        // soft light from top-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = size.width
            ),
            radius = size.width,
            center = Offset(0f, 0f)
        )
    }
}

@Composable
private fun BurstArt() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.36f)
        // radiating thin strokes
        repeat(24) { k ->
            val a = k * (PI * 2.0 / 24.0)
            val r1 = size.width * 0.16f
            val r2 = size.width * (0.30f + (k % 3) * 0.05f)
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(center.x + (r1 * cos(a)).toFloat(), center.y + (r1 * sin(a)).toFloat()),
                end = Offset(center.x + (r2 * cos(a)).toFloat(), center.y + (r2 * sin(a)).toFloat()),
                strokeWidth = 2f
            )
        }
        // petals ring
        repeat(10) { k ->
            val a = k * (PI * 2.0 / 10.0)
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = size.width * 0.13f,
                center = Offset(
                    center.x + (size.width * 0.19f * cos(a)).toFloat(),
                    center.y + (size.width * 0.19f * sin(a)).toFloat()
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
    }
}
