package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.domain.model.Reciter
import com.ghais.player.AudioEngine

private val DarkCard = Color(0xFF1C1C1E)
private val MutedGrey = Color(0xFF9A9AA0)

private val CardShape = RoundedCornerShape(28.dp)
private val AvatarShape = RoundedCornerShape(24.dp)

@Composable
fun NationReelBlock(
    nation: String,
    reciters: List<Reciter>,
    photoFor: (String) -> String?,
    onSeeAll: () -> Unit,
    onReciter: (String) -> Unit,
    onPlayReciter: (Reciter) -> Unit = {}
) {
    val followedSlugs by FollowStore.followedSlugs.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nation,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "See all",
                color = MutedGrey,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onSeeAll)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = reciters,
                key = { it.slug }
            ) { reciter ->
                ReciterReelCard(
                    reciter = reciter,
                    photoUrl = photoFor(reciter.slug),
                    onClick = { onReciter(reciter.slug) },
                    onPlayClick = { onPlayReciter(reciter) },
                    isFollowing = reciter.slug in followedSlugs,
                    onFollowClick = { FollowStore.toggle(reciter.slug) }
                )
            }
        }
    }
}

@Composable
fun ReciterReelCard(
    reciter: Reciter,
    photoUrl: String?,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {}
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug
    val activelyPlaying = isCurrentReciter && isPlaying

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(254.dp)
            .clip(CardShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (activelyPlaying) Color.White.copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.08f),
                CardShape
            )
            .clickable(onClick = onClick)
    ) {
        // Gradient scrim on card
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.32f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = reciter.nameEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AvatarShape)
                            .background(DarkCard)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AvatarShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reciter.nameEn.take(1),
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Play/Pause Action Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (activelyPlaying) Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFF5F5F7)
                                )
                            )
                            else Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (activelyPlaying) Color(0xFFF5F5F7)
                            else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                        .clickable {
                            if (activelyPlaying) {
                                AudioEngine.pause()
                            } else if (isCurrentReciter) {
                                AudioEngine.resume()
                            } else {
                                onPlayClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (activelyPlaying) "Pause" else "Play",
                        tint = if (activelyPlaying) Color(0xFF0B0C0E) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Follow badge — top-end overlapped on the avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                        .clickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                        contentDescription = if (isFollowing) "Following" else "Follow",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = reciter.nameEn,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 44.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${reciter.riwayah} • ${reciter.style}",
                color = MutedGrey,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
