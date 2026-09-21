package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HealthWhite = Color.White
private val HealthMuted = Color(0xFF9AA0A6)
private val HealthRed = Color(0xFFFF6B7A)

@Composable
fun StorageHealthCard(usedLabel: String, summary: String, progress: Float, onClear: () -> Unit) {
    val fraction = progress.coerceIn(0.03f, 1f)
    val cardShape = RoundedCornerShape(24.dp)
    val barShape = RoundedCornerShape(50)
    val pillShape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        HealthWhite.copy(alpha = 0.08f),
                        HealthWhite.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, HealthWhite.copy(alpha = 0.10f), cardShape)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HealthWhite.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = null,
                        tint = HealthWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Offline downloads",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthWhite
                    )
                    Text(
                        text = summary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = HealthMuted
                    )
                }
                Text(
                    text = usedLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HealthWhite
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(barShape)
                    .background(HealthWhite.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(barShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    HealthWhite.copy(alpha = 0.95f),
                                    HealthWhite.copy(alpha = 0.55f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HealthWhite.copy(alpha = 0.35f))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(pillShape)
                    .background(HealthRed.copy(alpha = 0.12f))
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = HealthRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear cache",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthRed
                    )
                }
            }
        }
    }
}
