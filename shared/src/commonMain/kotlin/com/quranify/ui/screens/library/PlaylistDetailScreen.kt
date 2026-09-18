package com.quranify.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.domain.model.TrackItem
import com.quranify.ui.theme.QuranifyColors

data class PlaylistDetailScreen(val playlistId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Mock data
        val tracks = listOf(
            TrackItem("alafasy", "Mishary Alafasy", 1, "Al-Fatihah", "الفاتحة", 1, "url", "Bismillah", 3000),
            TrackItem("alafasy", "Mishary Alafasy", 1, "Al-Fatihah", "الفاتحة", 2, "url", "Alhamdulillah", 4000)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
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
                    text = "Morning Adhkar",
                    color = QuranifyColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Playlist Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(QuranifyColors.Card),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = QuranifyColors.Primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "2 Tracks • 15 mins",
                    color = QuranifyColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = QuranifyColors.Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { }, modifier = Modifier.background(QuranifyColors.Card, RoundedCornerShape(50))) {
                        Icon(imageVector = Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = QuranifyColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { }, modifier = Modifier.background(QuranifyColors.Card, RoundedCornerShape(50))) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share", tint = QuranifyColors.TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(tracks) { track ->
                    PlaylistTrackRow(track)
                }
            }
        }
    }
}

@Composable
fun PlaylistTrackRow(track: TrackItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${track.surahNameEn} • Ayah ${track.ayahNo}",
                color = QuranifyColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = track.reciterName,
                color = QuranifyColors.TextSecondary,
                fontSize = 14.sp
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove",
                tint = QuranifyColors.TextSecondary
            )
        }
    }
}
