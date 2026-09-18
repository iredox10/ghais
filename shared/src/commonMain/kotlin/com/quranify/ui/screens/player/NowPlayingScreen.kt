package com.quranify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.quranify.player.AudioEngine
import com.quranify.ui.screens.player.components.NowPlayingControlsBar
import com.quranify.ui.screens.player.components.NowPlayingLyricsCard
import com.quranify.ui.screens.player.components.NowPlayingMetadata
import com.quranify.ui.screens.player.components.NowPlayingScrubber
import com.quranify.ui.screens.player.components.NowPlayingTopBar
import com.quranify.ui.screens.player.components.NowPlayingVinylDisc
import com.quranify.ui.theme.QuranifyColors

class NowPlayingScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val progress by AudioEngine.progress.collectAsState()
        val currentPositionMs by AudioEngine.currentPositionMs.collectAsState()
        val speed by AudioEngine.playbackSpeed.collectAsState()

        var showSleepTimer by remember { mutableStateOf(false) }
        var showQueue by remember { mutableStateOf(false) }

        val trackTitle = currentTrack?.surahNameEn ?: "Surah Ar-Rahman"
        val reciterName = currentTrack?.reciterName ?: "Sheikh Mishary Rashid Alafasy"
        val subtitle = currentTrack?.surahId?.let { "Surah $it • Juz 27" } ?: "The Most Merciful • 55:13"

        // Format times
        val elapsedSec = (currentPositionMs / 1000).toInt()
        val totalSec = ((currentTrack?.durationMs ?: 480_000L) / 1000).toInt()
        val elapsedText = "${(elapsedSec / 60).toString().padStart(2, '0')}:${(elapsedSec % 60).toString().padStart(2, '0')}"
        val totalText = "${(totalSec / 60).toString().padStart(2, '0')}:${(totalSec % 60).toString().padStart(2, '0')}"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                NowPlayingTopBar(
                    onMinimizeClick = { navigator?.pop() },
                    playlistTitle = "Heart Soothing Recitations",
                    onMoreOptionsClick = { /* Options */ }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vinyl Disc Artwork
                NowPlayingVinylDisc(
                    imageUrl = "",
                    riwayahText = "Hafs 'an 'Asim",
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Track Metadata & Badges
                NowPlayingMetadata(
                    title = trackTitle,
                    subtitle = subtitle,
                    reciterName = reciterName,
                    juzText = "Juz 27",
                    ayahCountText = "Ayah 13 of 78"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Synchronized Lyrics Card
                NowPlayingLyricsCard()

                Spacer(modifier = Modifier.height(18.dp))

                // Scrubber Slider
                NowPlayingScrubber(
                    progress = progress,
                    elapsedText = elapsedText,
                    totalText = totalText,
                    onSeek = { newProgress ->
                        val duration = currentTrack?.durationMs ?: 480_000L
                        AudioEngine.seekTo((newProgress * duration).toLong())
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Playback Controls
                NowPlayingControlsBar(
                    isPlaying = isPlaying,
                    onPlayPause = { AudioEngine.togglePlayPause() },
                    onPrevious = { AudioEngine.skipPrevious() },
                    onNext = { AudioEngine.skipNext() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Quick Actions Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sleep Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSleepTimer = true }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = QuranifyColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Timer",
                            fontSize = 11.sp,
                            color = QuranifyColors.TextSecondary
                        )
                    }

                    // Speed selector pill
                    Box(
                        modifier = Modifier
                            .background(QuranifyColors.SurfaceHigh, RoundedCornerShape(14.dp))
                            .clickable {
                                val nextSpeed = when (speed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 0.75f
                                    else -> 1.0f
                                }
                                AudioEngine.setPlaybackSpeed(nextSpeed)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            fontSize = 12.sp,
                            color = QuranifyColors.Primary
                        )
                    }

                    // Bookmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { /* Save Bookmark */ }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = QuranifyColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Queue sheet
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQueue = true }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = QuranifyColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Sheets
            if (showSleepTimer) {
                SleepTimerSheet(onDismiss = { showSleepTimer = false })
            }
            if (showQueue) {
                QueueSheet(onDismiss = { showQueue = false })
            }
        }
    }
}
