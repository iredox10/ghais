package com.quranify.ui.screens.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.ui.theme.QuranifyColors

object KhatmaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QuranifyColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Khatma Plan",
                    color = QuranifyColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Progress Dashboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Progress
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = 0.35f
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = QuranifyColors.Card,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = QuranifyColors.Primary,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = QuranifyColors.TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed",
                            color = QuranifyColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox("Streak", "12 Days", Icons.Filled.LocalFireDepartment, QuranifyColors.Primary)
                    StatBox("Remaining", "18 Days", null, QuranifyColors.TextPrimary)
                    StatBox("Est. Finish", "Oct 15", null, QuranifyColors.TextPrimary)
                }
            }

            // Today's Target Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(QuranifyColors.Card)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Today's Target",
                    color = QuranifyColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Surah Al-Baqarah",
                    color = QuranifyColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ayahs 1-141",
                    color = QuranifyColors.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = QuranifyColors.Primary)
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Today's Portion", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, iconTint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(24.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = QuranifyColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = QuranifyColors.TextSecondary,
            fontSize = 12.sp
        )
    }
}
