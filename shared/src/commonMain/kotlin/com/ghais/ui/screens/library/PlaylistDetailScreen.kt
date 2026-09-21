package com.ghais.ui.screens.library

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
import androidx.compose.runtime.remember
import com.ghais.data.seed.toTrackItem
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
import com.ghais.data.seed.QuranDataRepository
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisColors

data class PlaylistDetailScreen(val playlistId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        val playlist = remember(playlistId) {
            QuranDataRepository.getCuratedPlaylistOrDefault(playlistId)
        }
        val tracks = remember(playlist) {
            playlist.tracks.map { it.toTrackItem() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GhaisColors.Background)
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
                        tint = GhaisColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playlist.title,
                    color = GhaisColors.TextPrimary,
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
                        .background(GhaisColors.Card),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = GhaisColors.Primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${tracks.size} Tracks • ${playlist.totalDuration}",
                    color = GhaisColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                AudioEngine.playQueue(tracks, startIndex = 0)
                                rootNavigator.push(NowPlayingScreen())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GhaisColors.Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                AudioEngine.playQueue(tracks.shuffled(), startIndex = 0)
                                rootNavigator.push(NowPlayingScreen())
                            }
                        },
                        modifier = Modifier.background(GhaisColors.Card, RoundedCornerShape(50))
                    ) {
                        Icon(imageVector = Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = GhaisColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { }, modifier = Modifier.background(GhaisColors.Card, RoundedCornerShape(50))) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share", tint = GhaisColors.TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(items = tracks) { track ->
                    PlaylistTrackRow(
                        track = track,
                        onTrackClick = {
                            val index = tracks.indexOf(track).coerceAtLeast(0)
                            AudioEngine.playQueue(tracks, startIndex = index)
                            rootNavigator.push(NowPlayingScreen())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistTrackRow(
    track: TrackItem,
    onTrackClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${track.surahNameEn} (${track.surahNameAr})",
                color = GhaisColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = track.reciterName,
                color = GhaisColors.TextSecondary,
                fontSize = 14.sp
            )
        }
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove",
                tint = GhaisColors.TextSecondary
            )
        }
    }
}
