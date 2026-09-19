package com.quranify.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quranify.data.repository.UserUsageRepository
import com.quranify.data.seed.JumpBackInItem
import com.quranify.player.AudioEngine
import com.quranify.ui.theme.QuranifyColors

private val DarkCard = Color(0xFF141418)
private val MutedGrey = Color(0xFF9A9AA0)

@Composable
fun HomeContinueListeningRow(onPlay: (JumpBackInItem) -> Unit) {
    val historyItems by UserUsageRepository.history.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isEnginePlaying by AudioEngine.isPlaying.collectAsState()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = historyItems,
            key = { "${it.surahId}-${it.reciterSlug}" }
        ) { item ->
            val isCurrent = currentTrack != null &&
                currentTrack?.surahId == item.surahId &&
                currentTrack?.reciterSlug == item.reciterSlug
            val activelyPlaying = isCurrent && isEnginePlaying

            val borderColor by animateColorAsState(
                targetValue = if (activelyPlaying) QuranifyColors.Primary.copy(alpha = 0.50f)
                else Color.White.copy(alpha = 0.09f),
                animationSpec = tween(300)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(310.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DarkCard)
                    .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                    .clickable { onPlay(item) }
                    .padding(12.dp)
            ) {
                // Cover Artwork
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2325))
                ) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(76.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.subtitle,
                        color = if (activelyPlaying) QuranifyColors.Primary else MutedGrey,
                        fontSize = 12.sp,
                        fontWeight = if (activelyPlaying) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    if (activelyPlaying) QuranifyColors.Primary else Color(0xFF4C8DFF),
                                    RoundedCornerShape(50)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Action Frosted Play / Pause Pill
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (activelyPlaying) QuranifyColors.Primary.copy(alpha = 0.20f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (activelyPlaying) QuranifyColors.Primary.copy(alpha = 0.40f)
                            else Color.White.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (activelyPlaying) "Pause" else "Resume",
                        tint = if (activelyPlaying) QuranifyColors.Primary else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
