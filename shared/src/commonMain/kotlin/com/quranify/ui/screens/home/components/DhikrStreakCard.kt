package com.quranify.ui.screens.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.ui.theme.QuranifyColors

@Composable
fun DhikrStreakCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(QuranifyColors.SurfaceContainer)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Amber flame badge with gradient & border from Stitch Apple Glassmorphism
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFBBF24).copy(alpha = 0.20f),
                            QuranifyColors.Primary.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Flame Icon",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle text column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "7 Day Dhikr Streak",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                // "ACTIVE" pill badge matching Stitch
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(QuranifyColors.Primary.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = QuranifyColors.Primary.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ACTIVE",
                        color = QuranifyColors.Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Text(
                text = "35 mins recited today • Goal: 45m",
                fontSize = 12.sp,
                color = QuranifyColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right side: Custom Circular Progress Ring
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                // Inactive track
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Active sweep (78%)
                drawArc(
                    color = QuranifyColors.Primary,
                    startAngle = -90f,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = "78%",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
        }
    }
}
