package com.quranify.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.quranify.data.seed.StitchAssets
import com.quranify.player.AmbientMixer
import com.quranify.player.AudioEngine
import com.quranify.player.displayName
import com.quranify.player.videoKeys
import com.quranify.ui.screens.player.components.NowPlayingLyricsCard
import com.quranify.ui.screens.player.components.NowPlayingVolumePanel

private val MutedGrey = Color(0xFF9A9AA0)
private val Speeds = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 0.75f)

class NowPlayingScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val progress by AudioEngine.progress.collectAsState()
        val currentPositionMs by AudioEngine.currentPositionMs.collectAsState()
        val durationMs by AudioEngine.durationMs.collectAsState()
        val speed by AudioEngine.playbackSpeed.collectAsState()
        val volume by AudioEngine.volume.collectAsState()
        val queue by AudioEngine.queue.collectAsState()
        val currentIndex by AudioEngine.currentIndex.collectAsState()
        val playbackState by AudioEngine.playbackState.collectAsState()
        val sleepTimerState by com.quranify.player.SleepTimer.state.collectAsState()
        val repeatMode = playbackState.settings.repeatMode

        var showSleepTimer by remember { mutableStateOf(false) }
        var showQueue by remember { mutableStateOf(false) }
        var showAmbient by remember { mutableStateOf(false) }
        var showLyrics by remember { mutableStateOf(false) }
        var showVolume by remember { mutableStateOf(false) }
        val mixer = remember { AmbientMixer }
        val ambientChannels by mixer.channels.collectAsState()
        val ambientVolume by mixer.masterAmbientVolume.collectAsState()
        val selectedAmbientType = ambientChannels.firstOrNull { it.isEnabled }?.type
        val hasAmbientVideo = selectedAmbientType?.videoKeys()?.isNotEmpty() == true

        val track = currentTrack
        val title = track?.surahNameEn ?: "Ar-Rahman"
        val surahNameAr = track?.surahNameAr ?: "الرحمن"
        val reciterName = track?.reciterName ?: "Mishary Rashid Alafasy"
        var isFav by remember(track?.audioUrl) { mutableStateOf(false) }

        val canSkipNext = remember(queue, currentIndex, repeatMode, track) {
            if (track == null || queue.isEmpty()) false
            else if (repeatMode != com.quranify.domain.model.RepeatMode.OFF) true
            else currentIndex < queue.size - 1
        }

        val canSkipPrevious = remember(queue, currentIndex, repeatMode, track, currentPositionMs) {
            if (track == null) false
            else if (currentPositionMs > 3_000L) true
            else if (queue.isEmpty()) false
            else if (repeatMode != com.quranify.domain.model.RepeatMode.OFF) true
            else currentIndex > 0
        }

        val queuePositionText = remember(queue.size, currentIndex, track) {
            if (track == null) {
                "Surah 55 of 114"
            } else if (queue.size > 1 && currentIndex in queue.indices) {
                "Track ${currentIndex + 1} of ${queue.size} • Surah ${track.surahId} of 114"
            } else {
                "Surah ${track.surahId} of 114"
            }
        }

        val totalMs = if (durationMs > 0L) durationMs
            else (track?.durationMs?.takeIf { it > 0L } ?: 0L)
        val elapsedText = formatMs(currentPositionMs)
        val remainingText = if (totalMs > 0L) "-${formatMs((totalMs - currentPositionMs).coerceAtLeast(0L))}" else "--:--"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05050C))
        ) {
            AmbientVideoView(selectedType = selectedAmbientType, modifier = Modifier.fillMaxSize())
            if (hasAmbientVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }
            // Deep blue glow rising from the bottom (reference look)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E4FD8).copy(alpha = 0.38f),
                                Color(0xFF1E4FD8).copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(200f, 900f),
                            radius = 700f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Swipe-down to minimize & click to dismiss handle
                var dragOffsetY by remember { mutableStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetY > 60f) {
                                        navigator?.pop()
                                    }
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    dragOffsetY = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 0) {
                                        dragOffsetY += dragAmount
                                        if (dragOffsetY > 90f) {
                                            navigator?.pop()
                                            dragOffsetY = 0f
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.35f))
                            .clickable { navigator?.pop() }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Background sound pill
                val ambientButtonText = selectedAmbientType?.displayName() ?: "Background sound"
                val isAmbientActive = selectedAmbientType != null
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isAmbientActive) Color(0xFFD4A853).copy(alpha = 0.14f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isAmbientActive) Color(0xFFD4A853).copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(50)
                        )
                        .clickable { showAmbient = true }
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = ambientButtonText,
                        tint = if (isAmbientActive) Color(0xFFD4A853) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ambientButtonText,
                        color = if (isAmbientActive) Color(0xFFD4A853) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Open middle (lyrics appear here when toggled)
                if (showLyrics && track != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NowPlayingLyricsCard(
                        arabicVerse = track.textUthmani.ifBlank { "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ" },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Track row: photo + title/reciter + star
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = track?.let { photoForSlug(it.reciterSlug) }
                            ?: StitchAssets.LogoUrl,
                        contentDescription = reciterName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = queuePositionText,
                            color = Color(0xFFD4A853),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (surahNameAr.isNotBlank()) {
                                Text(
                                    text = surahNameAr,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = reciterName,
                            color = MutedGrey,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable { isFav = !isFav },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color.White else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Controls: speed | prev | play | next | sleep
                PlayerControls(
                    isPlaying = isPlaying,
                    speed = speed,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    isSleepTimerActive = sleepTimerState.isActive,
                    onTogglePlayPause = { AudioEngine.togglePlayPause() },
                    onPrevious = { AudioEngine.skipPrevious() },
                    onNext = { AudioEngine.skipNext() },
                    onSpeedChange = { currentSpd ->
                        val idx = Speeds.indexOf(currentSpd).takeIf { it >= 0 } ?: 0
                        AudioEngine.setPlaybackSpeed(Speeds[(idx + 1) % Speeds.size])
                    },
                    onSleepTimerClick = { showSleepTimer = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Progress bar (tap to seek)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val width = maxWidth
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                            .pointerInput(totalMs) {
                                detectTapGestures { offset ->
                                    if (totalMs > 0L) {
                                        val fraction = (offset.x / width.toPx()).coerceIn(0f, 1f)
                                        AudioEngine.seekTo((fraction * totalMs).toLong())
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = elapsedText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = remainingText, color = MutedGrey, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Bottom utility row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    IconButton(
                        onClick = { showVolume = !showVolume },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (volume > 0f) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = "Volume",
                            tint = if (volume > 0f) MutedGrey else MutedGrey.copy(alpha = 0.5f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { showLyrics = !showLyrics },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ق",
                            color = if (showLyrics) Color.White else MutedGrey.copy(alpha = 0.55f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = { showAmbient = true }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Cast,
                            contentDescription = "Soundscapes",
                            tint = MutedGrey,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { showQueue = true }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (queue.isNotEmpty()) Color(0xFFD4A853) else MutedGrey,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showVolume,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NowPlayingVolumePanel(
                        quranVolume = volume,
                        onQuranVolumeChange = { mixer.setQuranVolume(it) },
                        ambientVolume = ambientVolume,
                        onAmbientVolumeChange = { mixer.setMasterAmbientVolume(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            if (showSleepTimer) SleepTimerSheet(onDismiss = { showSleepTimer = false })
            if (showQueue) QueueSheet(onDismiss = { showQueue = false })
            if (showAmbient) AmbientMixerSheet(mixer = mixer, onDismissRequest = { showAmbient = false })
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms.coerceAtLeast(0L) / 1000).toInt()
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}

private fun formatSpeed(speed: Float): String {
    val label = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${label}×"
}

private fun photoForSlug(slug: String): String {
    val photos = StitchAssets.VerifiedReciters
    val match = when (slug) {
        "alafasy" -> photos.firstOrNull { it.slug == "mishary" }
        "sudais" -> photos.firstOrNull { it.slug == "al-sudais" }
        "muaiqly" -> photos.firstOrNull { it.slug == "al-muaiqly" }
        "dossari" -> photos.firstOrNull { it.slug == "al-dossari" }
        "abdulbaset_murattal", "abdulbaset_mujawwad" ->
            photos.firstOrNull { it.slug == "abdul-basit" }
        else -> photos.firstOrNull { it.slug == slug }
    }
    return match?.photoUrl ?: StitchAssets.LogoUrl
}
