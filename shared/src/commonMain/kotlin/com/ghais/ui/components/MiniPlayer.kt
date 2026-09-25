package com.ghais.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.player.AudioEngine
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisColors
import com.ghais.ui.theme.GhaisShapes

/**
 * Modern Black & White + Trending Purple Glassmorphic Mini Player
 * Hovers directly above the bottom navigation dock with:
 * - Frosted translucent obsidian backdrop with specular purple top border
 * - Top-edge animated Trending Purple to Neon Lilac progress track
 * - Dynamic pulsing Trending Purple equalizer indicator
 * - Live Surah name in pure white, reciter and Surah info in muted light grey
 * - Electric Purple gradient circular play/pause button and crisp white skip next button
 * - Swipe-down to dismiss gesture and tap to expand to NowPlayingScreen
 */
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onOpenNowPlaying: (() -> Unit)? = null
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val progress by AudioEngine.progress.collectAsState()
    val queue by AudioEngine.queue.collectAsState()
    val currentIndex by AudioEngine.currentIndex.collectAsState()
    val playbackState by AudioEngine.playbackState.collectAsState()

    val track = currentTrack ?: return
    val rootNav = com.ghais.ui.navigation.LocalRootNavigator.current
    val navigator = rootNav ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    // Single-flight open: tap + swipe-up race each other (clickable vs drag
    // threshold), and a stacked duplicate shares Voyager's class-name key and
    // renders blank when revealed. Never stack two players.
    fun openPlayerOnce() {
        if (navigator?.lastItem is NowPlayingScreen) return
        if (onOpenNowPlaying != null) {
            onOpenNowPlaying()
        } else {
            navigator?.push(NowPlayingScreen())
        }
    }

    val canSkipNext = remember(queue, currentIndex, playbackState.settings.repeatMode, track) {
        if (queue.isEmpty()) false
        else if (playbackState.settings.repeatMode != com.ghais.domain.model.RepeatMode.OFF) true
        else currentIndex < queue.size - 1
    }

    val isAyah = track.ayahNo > 0
    val displayTitle = if (isAyah) "${track.surahNameEn} • Ayah ${track.ayahNo}" else track.surahNameEn

    // Reciter only: the surah/track counters ("Surah 2 of 114 • Track 2 of
    // 114") were noise on a 90dp bar — the title and the progress hairline
    // already say what is playing and how far in.
    val subtitleText = track.reciterName

    // Glassmorphic black background
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1A1C),
            Color(0xFF101012)
        )
    )

    // Subtle white glass border
    val glassBorder = BorderStroke(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.12f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = GhaisShapes.playerBar,
            color = Color.Transparent,
            border = glassBorder,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glassGradient)
                    .clip(GhaisShapes.playerBar)
                    .clickable { openPlayerOnce() }
                    .pointerInput(Unit) {
                        var cumulativeDragY = 0f
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (cumulativeDragY > 100f) {
                                    AudioEngine.clear()
                                } else if (cumulativeDragY < -50f) {
                                    openPlayerOnce()
                                }
                                cumulativeDragY = 0f
                            },
                            onDragCancel = {
                                cumulativeDragY = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                cumulativeDragY += dragAmount
                                if (cumulativeDragY > 120f) {
                                    AudioEngine.clear()
                                    cumulativeDragY = 0f
                                } else if (cumulativeDragY < -70f) {
                                    openPlayerOnce()
                                    cumulativeDragY = 0f
                                }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top-edge progress track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFA855F7),
                                            Color(0xFFC084FC)
                                        )
                                    )
                                )
                        )
                    }

                    // Content Row: Photo thumbnail | Title + Reciter | Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: reciter photo thumbnail
                        coil3.compose.AsyncImage(
                            model = miniPlayerPhotoFor(track.reciterSlug),
                            contentDescription = track.reciterName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF242426))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Center: Surah name + reciter
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = displayTitle,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = subtitleText,
                                color = Color(0xFF9A9AA0),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Right: prev + play/pause + next
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Previous is always live: inside the first 3s of a
                            // track it restarts the track, past that it steps
                            // back — both are valid user intents, so unlike
                            // "next" it never needs disabling.
                            IconButton(
                                onClick = { AudioEngine.skipPrevious() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { AudioEngine.togglePlayPause() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            IconButton(
                                onClick = { AudioEngine.skipNext() },
                                enabled = canSkipNext,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Skip Next",
                                    tint = if (canSkipNext) Color.White else Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps a playback reciter slug to the closest display photo.
 * Falls back to the app logo when no photo exists.
 */
private fun miniPlayerPhotoFor(reciterSlug: String): String {
    val photos = com.ghais.data.seed.GhaisAssets.VerifiedReciters
    val match = when (reciterSlug) {
        "alafasy" -> photos.firstOrNull { it.slug == "mishary" }
        "sudais" -> photos.firstOrNull { it.slug == "al-sudais" }
        "muaiqly" -> photos.firstOrNull { it.slug == "al-muaiqly" }
        "dossari" -> photos.firstOrNull { it.slug == "al-dossari" }
        "abdulbaset_murattal", "abdulbaset_mujawwad" ->
            photos.firstOrNull { it.slug == "abdul-basit" }
        else -> photos.firstOrNull { it.slug == reciterSlug }
    }
    return match?.photoUrl ?: com.ghais.data.seed.GhaisAssets.LogoUrl
}

/**
 * Animated live equalizer bars for the mini player thumbnail.
 * Dynamically pulses 4 Trending Purple vertical bars when audio is active,
 * or gracefully rests when playback is paused.
 */
@Composable
private fun MiniPlayerEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Equalizer",
            tint = Color(0xFFA855F7).copy(alpha = 0.75f),
            modifier = modifier.size(20.dp)
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "MiniEqualizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 17f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(490, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar4"
    )

    Row(
        modifier = modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h1.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h4.dp)
                .background(Color(0xFFA855F7), RoundedCornerShape(1.dp))
        )
    }
}

