package com.ghais.ui.screens.surah

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Ayah
import com.ghais.ui.theme.QuranifyColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AyahRow(
    ayah: Ayah,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    /** True when this ayah is the current track (highlight stays even while paused). Defaults to [isPlaying] for backward compat. */
    isActive: Boolean = isPlaying
) {
    val backgroundColor = if (isActive) QuranifyColors.Primary.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Ayah Star Badge
        AyahBadge(number = ayah.ayahNo)

        Spacer(modifier = Modifier.width(16.dp))

        // Center: Arabic Text
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = ayah.textUthmani,
                color = QuranifyColors.TextPrimary,
                fontSize = 22.sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Ayah",
                            tint = QuranifyColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite Ayah",
                            tint = if (isFavorite) QuranifyColors.Error else QuranifyColors.TextSecondary
                        )
                    }
                }

                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Ayah" else "Play Ayah",
                        tint = QuranifyColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AyahBadge(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        val primaryColor = QuranifyColors.Primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val c = center
            val path = Path()

            val points = 8
            for (i in 0 until points * 2) {
                val currentRadius = if (i % 2 == 0) radius else radius * 0.8f
                val angle = (i * (PI / points) - PI / 2).toDouble()

                val x = (c.x + currentRadius * cos(angle)).toFloat()
                val y = (c.y + currentRadius * sin(angle)).toFloat()

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Text(
            text = number.toString(),
            color = QuranifyColors.Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
