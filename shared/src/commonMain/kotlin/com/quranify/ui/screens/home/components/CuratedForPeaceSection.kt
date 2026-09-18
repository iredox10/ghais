package com.quranify.ui.screens.home.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.quranify.ui.theme.QuranifyColors


data class PeacePlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val imageUrl: String
)

@Composable
fun CuratedForPeaceSection(
    modifier: Modifier = Modifier,
    onPlayClick: (String) -> Unit = {}
) {
    val playlists = listOf(
        PeacePlaylist(
            id = "deep_focus",
            title = "Deep Focus & Study",
            subtitle = "Calm, slow tempo...",
            tag = "Tartil",
            imageUrl = "https://picsum.photos/seed/focus/320/320"
        ),
        PeacePlaylist(
            id = "heart_soothing",
            title = "Heart Soothing",
            subtitle = "Comforting verses of...",
            tag = "Emotional",
            imageUrl = "https://picsum.photos/seed/soothing/320/320"
        ),
        PeacePlaylist(
            id = "morning_protection",
            title = "Morning Protection",
            subtitle = "Sunnah Adhkar & Surahs",
            tag = "Morning",
            imageUrl = "https://picsum.photos/seed/morning/320/320"
        )
    )

    Column(modifier = modifier) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Curated for Peace",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selected recitations for your daily routine",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary
                )
            }
            
            // Equalizer Icon
            Row(
                modifier = Modifier.height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(modifier = Modifier.width(4.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(QuranifyColors.Primary))
                Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(QuranifyColors.Primary))
                Box(modifier = Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(QuranifyColors.Primary))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onPlayClick = { onPlayClick(playlist.id) }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: PeacePlaylist,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(QuranifyColors.Card)
        ) {
            AsyncImage(
                model = playlist.imageUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Tag Pill
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .align(Alignment.TopStart)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = playlist.tag,
                    color = QuranifyColors.TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Glowing Emerald Play Circle Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.Primary)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                // Play Arrow (Using a simple triangle made with shapes or a generic unicode character since Material Icons aren't specified)
                Text(
                    text = "▶",
                    color = Color.Black,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = playlist.title,
            color = QuranifyColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = playlist.subtitle,
            color = QuranifyColors.TextSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
