package com.quranify.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.quranify.data.seed.StitchAssets
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
        var showReciters by remember { mutableStateOf(false) }
        var isShuffle by remember { mutableStateOf(false) }
        var isRepeat by remember { mutableStateOf(true) }

        val trackTitle = currentTrack?.surahNameEn ?: "Surah Ar-Rahman"
        val reciterName = currentTrack?.reciterName ?: "Sheikh Mishary Rashid Alafasy"
        val subtitle = currentTrack?.surahId?.let { "Surah $it • Juz 27" } ?: "The Most Merciful • 55:13"

        // Format times
        val elapsedSec = (currentPositionMs / 1000).toInt()
        val durationMs = currentTrack?.durationMs ?: 480_000L
        val totalSec = (durationMs / 1000).toInt()
        val remainingSec = (totalSec - elapsedSec).coerceAtLeast(0)
        val elapsedText = "${(elapsedSec / 60).toString().padStart(2, '0')}:${(elapsedSec % 60).toString().padStart(2, '0')}"
        val remainingText = "-${(remainingSec / 60).toString().padStart(2, '0')}:${(remainingSec % 60).toString().padStart(2, '0')}"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
        ) {
            // 1. Multi-layered Apple-style ambient dynamic liquid glow backdrop
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                QuranifyColors.Primary.copy(alpha = 0.18f),
                                QuranifyColors.PrimaryContainer.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                QuranifyColors.Secondary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Main Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar Context Panel
                NowPlayingTopBar(
                    onMinimizeClick = { navigator?.pop() },
                    playlistTitle = "Heart Soothing Recitations",
                    onMoreOptionsClick = { showQueue = true },
                    onPlaylistDropdownClick = { /* Playlist Dropdown */ }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Rotating Vinyl Disc with Sacred Geometry Mandala & Floating Tajweed Ribbon
                NowPlayingVinylDisc(
                    imageUrl = StitchAssets.NowPlayingVinylArtUrl,
                    riwayahText = "Hafs 'an 'Asim",
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Track Metadata, Reciter & Badges
                NowPlayingMetadata(
                    title = trackTitle,
                    subtitle = subtitle,
                    reciterName = reciterName,
                    juzText = "Juz 27",
                    ayahCountText = "Ayah 13 of 78",
                    onTafsirClick = { /* Open Tafsir */ }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Synchronized Ayah Lyrics Card (Liquid Glass with grand Arabic typography & preview)
                NowPlayingLyricsCard()

                Spacer(modifier = Modifier.height(18.dp))

                // 5. High Fidelity Glowing Emerald Progress Scrubber
                NowPlayingScrubber(
                    progress = progress,
                    elapsedText = elapsedText,
                    totalText = remainingText,
                    onSeek = { newProgress ->
                        val duration = currentTrack?.durationMs ?: 480_000L
                        AudioEngine.seekTo((newProgress * duration).toLong())
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Primary Emerald Playback Controls
                NowPlayingControlsBar(
                    isPlaying = isPlaying,
                    isShuffle = isShuffle,
                    isRepeat = isRepeat,
                    onPlayPause = { AudioEngine.togglePlayPause() },
                    onPrevious = { AudioEngine.skipPrevious() },
                    onNext = { AudioEngine.skipNext() },
                    onShuffleToggle = { isShuffle = !isShuffle },
                    onRepeatToggle = { isRepeat = !isRepeat }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 7. Bottom Audio Utility Dock (Floating frosted glass capsule toolbar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xCC181D1A))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reciters Switcher
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showReciters = true }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Reciters",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reciters",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Speed Control
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val nextSpeed = when (speed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 0.75f
                                    else -> 1.0f
                                }
                                AudioEngine.setPlaybackSpeed(nextSpeed)
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SlowMotionVideo,
                            contentDescription = "Playback Speed",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${speed}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QuranifyColors.Primary
                        )
                    }

                    // Sleep Bedtime Timer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showSleepTimer = true }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "30m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = QuranifyColors.Secondary
                        )
                    }

                    // Share Ayah
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { /* Share verse action */ }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Ayah",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Share Ayah",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
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
            if (showReciters) {
                ReciterPickerSheet(
                    onDismiss = { showReciters = false },
                    onSelectReciter = { _ ->
                        /* Reciter selected */
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReciterPickerSheet(
    onDismiss: () -> Unit,
    onSelectReciter: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = QuranifyColors.SurfaceLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Select Reciter",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(14.dp))
            StitchAssets.VerifiedReciters.forEach { reciter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectReciter(reciter.name)
                            onDismiss()
                        }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = reciter.photoUrl,
                        contentDescription = reciter.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reciter.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = reciter.fans,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = QuranifyColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
