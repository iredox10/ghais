package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.theme.GhaisNoir

// Phase 3 — legacy glass names re-skinned onto Noir tokens (Signatures kept).

@Composable
fun GlassCardContainer(content: @Composable ColumnScope.() -> Unit) {
    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        content = { Column(modifier = Modifier.fillMaxWidth(), content = content) }
    )
}

@Composable
fun GlassDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GhaisNoir.BorderGhost)
    )
}

@Composable
fun GlassSectionLabel(text: String) {
    Text(
        text = text,
        color = GhaisNoir.TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
fun HealthBar(progress: Float) {
    NoirSegmentedProgress(progress = progress, trackHeight = 10.dp)
}

@Composable
fun FeatureCard(
    title: String,
    body: String,
    buttonText: String,
    onButton: () -> Unit
) {
    NoirCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = body,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                NoirTrendSparkline()
            }
            Spacer(modifier = Modifier.height(16.dp))
            ChromePillButton(
                text = buttonText,
                onClick = onButton,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Mini trend sparkline — white triple-line chart (Dark Web Monitoring motif). */
@Composable
private fun NoirTrendSparkline() {
    Canvas(modifier = Modifier.size(width = 88.dp, height = 56.dp)) {
        val w = size.width
        val h = size.height
        val axis = GhaisNoir.BorderCard
        drawLine(axis, Offset(6f, 0f), Offset(6f, h - 6f), strokeWidth = 1.dp.toPx())
        drawLine(axis, Offset(6f, h - 6f), Offset(w, h - 6f), strokeWidth = 1.dp.toPx())
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
        fun drawPolyline(points: List<Offset>, alpha: Float) {
            for (k in 0 until points.size - 1) {
                drawLine(
                    color = GhaisNoir.TextPrimary.copy(alpha = alpha),
                    start = points[k],
                    end = points[k + 1],
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        drawPolyline(faint, 0.25f)
        drawPolyline(mid, 0.45f)
        drawPolyline(main, 0.85f)
    }
}
