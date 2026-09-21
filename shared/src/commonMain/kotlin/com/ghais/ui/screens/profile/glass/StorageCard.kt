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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.DeleteOutline
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

private val GlassWhite = Color.White
private val GlassMuted = Color(0xFF9A9AA0)
private val GlassBlue = Color(0xFF4C8DFF)
private val GlassEmerald = Color(0xFF4EDEA3)
private val GlassRed = Color(0xFFFF5A6E)

@Composable
fun StorageGlassCard(
    storageLabel: String,
    cachedSummary: String,
    progressFraction: Float,
    cacheBadge: String,
    onClearClick: () -> Unit
) {
    val fraction = progressFraction.coerceIn(0.02f, 1f)
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassWhite.copy(alpha = 0.08f),
                        GlassWhite.copy(alpha = 0.03f)
                    )
                )
            )
            .border(1.dp, GlassWhite.copy(alpha = 0.10f), cardShape)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GlassBlue.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = GlassBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Storage Used",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassWhite
                    )
                    Text(
                        text = cachedSummary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassMuted
                    )
                }
                Text(
                    text = storageLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlassWhite
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GlassWhite.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(GlassEmerald, GlassBlue)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlassWhite.copy(alpha = 0.15f))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlassWhite.copy(alpha = 0.06f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassRed.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = GlassRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Clear Audio Cache",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassWhite
                    )
                    Text(
                        text = "Frees temporary streaming buffer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = GlassMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GlassWhite.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cacheBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlassWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
