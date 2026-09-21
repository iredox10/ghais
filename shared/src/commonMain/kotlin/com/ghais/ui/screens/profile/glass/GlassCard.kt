package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProtonGlassTop = Color.White.copy(alpha = 0.08f)
private val ProtonGlassBottom = Color.White.copy(alpha = 0.03f)
private val ProtonGlassBorder = Color.White.copy(alpha = 0.10f)
private val ProtonDivider = Color.White.copy(alpha = 0.06f)
private val ProtonMuted = Color(0xFF9A9AA0)
private val ProtonTrack = Color.White.copy(alpha = 0.12f)
private val ProtonFillSolid = Color.White
private val ProtonFillFaded = Color.White.copy(alpha = 0.60f)
private val ProtonHighlight = Color.White.copy(alpha = 0.25f)
private val ProtonGrid = Color.White.copy(alpha = 0.04f)
private val ProtonChromeTop = Color(0xFFF5F5F7)
private val ProtonChromeBottom = Color(0xFFC7C7CC)
private val ProtonChromeText = Color(0xFF0B0C0E)
private val ProtonAxis = Color.White.copy(alpha = 0.18f)
private val ProtonLineMain = Color.White.copy(alpha = 0.85f)
private val ProtonLineMid = Color.White.copy(alpha = 0.45f)
private val ProtonLineFaint = Color.White.copy(alpha = 0.25f)

@Composable
fun GlassCardContainer(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(ProtonGlassTop, ProtonGlassBottom)
                )
            )
            .border(1.dp, ProtonGlassBorder, shape),
        content = content
    )
}

@Composable
fun GlassDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ProtonDivider)
    )
}

@Composable
fun GlassSectionLabel(text: String) {
    Text(
        text = text,
        color = ProtonMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
fun HealthBar(progress: Float) {
    val clamped = progress.coerceIn(0.03f, 1f)
    val trackShape = RoundedCornerShape(50.dp)
    val fillShape = RoundedCornerShape(50.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(trackShape)
            .background(ProtonTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(10.dp)
                .clip(fillShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(ProtonFillSolid, ProtonFillFaded)
                    )
                )
                .align(Alignment.CenterStart)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ProtonHighlight)
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
fun FeatureCard(
    title: String,
    body: String,
    buttonText: String,
    onButton: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(ProtonGlassTop, ProtonGlassBottom)
                )
            )
            .border(1.dp, ProtonGlassBorder, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (i in 1..4) {
                val x = w * i / 5f
                drawLine(
                    color = ProtonGrid,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (j in 1..5) {
                val y = h * j / 6f
                drawLine(
                    color = ProtonGrid,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = body,
                        color = ProtonMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Canvas(modifier = Modifier.size(width = 88.dp, height = 56.dp)) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = ProtonAxis,
                        start = Offset(6f, 0f),
                        end = Offset(6f, h - 6f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = ProtonAxis,
                        start = Offset(6f, h - 6f),
                        end = Offset(w, h - 6f),
                        strokeWidth = 1.dp.toPx()
                    )
                    val main = listOf(
                        Offset(6f, h * 0.62f),
                        Offset(w * 0.28f, h * 0.55f),
                        Offset(w * 0.46f, h * 0.60f),
                        Offset(w * 0.64f, h * 0.30f),
                        Offset(w * 0.82f, h * 0.36f),
                        Offset(w - 2f, h * 0.14f)
                    )
                    val mid = listOf(
                        Offset(6f, h * 0.74f),
                        Offset(w * 0.28f, h * 0.70f),
                        Offset(w * 0.46f, h * 0.72f),
                        Offset(w * 0.64f, h * 0.52f),
                        Offset(w * 0.82f, h * 0.56f),
                        Offset(w - 2f, h * 0.40f)
                    )
                    val faint = listOf(
                        Offset(6f, h * 0.84f),
                        Offset(w * 0.28f, h * 0.82f),
                        Offset(w * 0.46f, h * 0.83f),
                        Offset(w * 0.64f, h * 0.70f),
                        Offset(w * 0.82f, h * 0.72f),
                        Offset(w - 2f, h * 0.62f)
                    )
                    fun drawPolyline(points: List<Offset>, color: Color) {
                        for (k in 0 until points.size - 1) {
                            drawLine(
                                color = color,
                                start = points[k],
                                end = points[k + 1],
                                strokeWidth = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    drawPolyline(faint, ProtonLineFaint)
                    drawPolyline(mid, ProtonLineMid)
                    drawPolyline(main, ProtonLineMain)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val pill = RoundedCornerShape(50.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pill)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ProtonChromeTop, ProtonChromeBottom)
                        )
                    )
                    .clickable(onClick = onButton)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    color = ProtonChromeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
