package com.quranify.ui.screens.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Circular amber flame badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(QuranifyColors.Secondary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Flame Icon",
                tint = QuranifyColors.Secondary,
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = QuranifyColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                // "Active" pill badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(QuranifyColors.Primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Active",
                        color = QuranifyColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                    color = QuranifyColors.SurfaceHighest,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Active sweep
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
                fontSize = 9.sp,
                color = QuranifyColors.TextPrimary
            )
        }
    }
}
