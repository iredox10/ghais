package com.quranify.ui.screens.home.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quranify.data.seed.CuratedPlaylist
import com.quranify.data.seed.StitchAssets
import com.quranify.ui.theme.QuranifyColors

@Composable
fun CuratedForPeaceSection(
    modifier: Modifier = Modifier,
    playlists: List<CuratedPlaylist> = StitchAssets.CuratedForPeace,
    onPlayClick: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Curated for Peace",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Selected recitations for your daily routine",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary
                )
            }
            
            // Equalizer Bars Indicator
            Row(
                modifier = Modifier.height(20.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(modifier = Modifier.width(3.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(QuranifyColors.Primary))
                Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(QuranifyColors.Primary))
                Box(modifier = Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(QuranifyColors.Primary))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onPlayClick = { onPlayClick(playlist.title) }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: CuratedPlaylist,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(144.dp)) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(QuranifyColors.SurfaceContainer)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom gradient shade overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
            
            // Bottom Action Bar inside card (Tag + Play button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tag Pill
                val tagColor = when (playlist.tag.lowercase()) {
                    "tartil" -> QuranifyColors.Primary
                    "emotional" -> QuranifyColors.Secondary
                    "morning" -> QuranifyColors.Primary
                    "sleep" -> Color(0xFF2DD4BF)
                    else -> QuranifyColors.Primary
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = playlist.tag,
                        color = tagColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Emerald Play Circle Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF10B981),
                                    Color(0xFF059669)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = playlist.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = playlist.subtitle,
            color = QuranifyColors.TextSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
