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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.ghais.data.repository.FollowStore
import com.ghais.domain.model.Reciter
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.util.realImageUrlOrNull

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
    val followerCounts by FollowStore.followerCounts.collectAsState()
    // Best-effort live counts for the visible slugs (unknown stays hidden).
    LaunchedEffect(reciters) {
        FollowStore.refreshCounts(reciters.map { it.slug })
    }
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
                color = GhaisNoir.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            // Monochrome ghost pill — replaces the old blue "See all" link.
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                    .noirClickable(onClick = onSeeAll)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "See all",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = reciters,
                key = { it.catalogKey() }
            ) { reciter ->
                ReciterReelCard(
                    reciter = reciter,
                    photoUrl = photoFor(reciter.slug),
                    onClick = { onReciter(reciter.slug) },
                    onPlayClick = { onPlayReciter(reciter) },
                    isFollowing = reciter.slug in followedSlugs,
                    onFollowClick = { FollowStore.toggle(reciter.slug) },
                    followerCount = followerCounts[reciter.slug]
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
    onFollowClick: () -> Unit = {},
    followerCount: Long? = null
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug
    val activelyPlaying = isCurrentReciter && isPlaying

    // Full-bleed portrait card: photo header, left-aligned identity block,
    // lavender FAB + follow badge. Active playback reads through a lavender
    // border and the pause glyph.
    val cardFill = if (activelyPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFill()
    val cardBorder = if (activelyPlaying) GhaisNoir.Lavender else GhaisNoir.BorderCard

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(272.dp)
            .clip(GhaisShapes.cardNoir)
            .background(cardFill)
            .border(1.dp, cardBorder, GhaisShapes.cardNoir)
            .topSpecular()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.matchParentSize()) {
            // Full-bleed portrait header with a bottom scrim the FAB melts into.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(158.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(GhaisNoir.wellFill())
            ) {
                val realPhoto = remember(photoUrl) { realImageUrlOrNull(photoUrl) }
                if (realPhoto != null) {
                    AsyncImage(
                        model = realPhoto,
                        contentDescription = reciter.nameEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = reciter.nameEn.take(1),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Bottom scrim — text-zone transition, never a floating edge.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Follow badge — lavender fill when following (selected-state
                // language), smoked glass otherwise.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = if (isFollowing) GhaisNoir.lavenderFill()
                            else Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.60f),
                                    Color.Black.copy(alpha = 0.60f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isFollowing) Color.Transparent else Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                        .clickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                        contentDescription = if (isFollowing) "Following" else "Follow",
                        tint = if (isFollowing) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Identity block: left-aligned name + arabic + single meta line,
            // with the lavender FAB riding the photo boundary.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciter.nameEn,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                    Text(
                        text = reciter.nameAr,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metaLineOf(reciter, followerCount),
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Signature FAB: always lavender, pause glyph while playing.
                Box(
                    modifier = Modifier
                        .offset(y = (-26).dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.lavenderFill())
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
                        tint = GhaisNoir.LavenderInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        // Diagonal glass sheen swept across the finished card.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
    }
}

/**
 * Single meta line: "Hafs • murattal • 12.4k". Follower count appended only
 * when known (null = hidden, never a placeholder).
 */
internal fun metaLineOf(reciter: Reciter, followerCount: Long?): String {
    val parts = mutableListOf(reciter.riwayah, reciter.style)
    if (followerCount != null) parts.add(FollowStore.formatFollowerCount(followerCount))
    return parts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" • ")
}
