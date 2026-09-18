package com.quranify.ui.screens.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.ui.theme.QuranifyColors

@Composable
fun LibraryQuickGridSection(
    onLikedVersesClick: () -> Unit = {},
    onDownloadedClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Liked Verses
        Box(
            modifier = Modifier
                .weight(1f)
                .height(118.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(QuranifyColors.SurfaceContainer)
                .clickable { onLikedVersesClick() }
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(QuranifyColors.Secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = QuranifyColors.Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = QuranifyColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Liked Verses",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )
                    Text(
                        text = "142 verses saved",
                        fontSize = 11.sp,
                        color = QuranifyColors.Secondary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }

        // Card 2: Downloaded Surahs
        Box(
            modifier = Modifier
                .weight(1f)
                .height(118.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(QuranifyColors.SurfaceContainer)
                .clickable { onDownloadedClick() }
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(QuranifyColors.Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Downloaded",
                            tint = QuranifyColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = QuranifyColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Downloaded",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )
                    Text(
                        text = "18 Surahs • 2.4 GB",
                        fontSize = 11.sp,
                        color = QuranifyColors.Primary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

data class LibraryPlaylistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val trackCount: Int
)

@Composable
fun LibraryPlaylistsSection(
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val samplePlaylists = listOf(
        LibraryPlaylistItem("1", "Morning Protection Adhkar", "Alafasy & Al-Sudais", 7),
        LibraryPlaylistItem("2", "Heart Soothing Recitations", "Calm tempo reciters", 12),
        LibraryPlaylistItem("3", "Sleep & Rest Qiyam", "Deep night tartil", 9)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Your Playlists",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = QuranifyColors.TextPrimary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        samplePlaylists.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPlaylistClick(item.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(QuranifyColors.SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = "Playlist",
                            tint = QuranifyColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuranifyColors.TextPrimary
                        )
                        Text(
                            text = "${item.subtitle} • ${item.trackCount} tracks",
                            fontSize = 12.sp,
                            color = QuranifyColors.TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(QuranifyColors.SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = QuranifyColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
