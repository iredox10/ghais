package com.quranify.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.quranify.player.AudioEngine
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.theme.QuranifyColors
import com.quranify.ui.theme.QuranifyShapes

/**
 * Modern Black & White + Trending Purple Glassmorphic Mini Player
 * Hovers directly above the bottom navigation dock with:
 * - Frosted translucent obsidian backdrop with specular purple top border
 * - Top-edge animated Trending Purple to Neon Lilac progress track
 * - Dynamic pulsing Trending Purple equalizer indicator
 * - Live Surah name in pure white, reciter and Ayah info in muted light grey
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

    val track = currentTrack ?: return
    val rootNav = com.quranify.ui.navigation.LocalRootNavigator.current
    val navigator = rootNav ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    // Smoothly animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "MiniPlayerProgress"
    )

    // Glassmorphic Obsidian Background Gradient
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xEE16161A), // Frosted obsidian top
            Color(0xF60F0F12)  // Deep obsidian base
        )
    )

    // Glowing specular purple top rim with subtle glass border
    val glassBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF8B5CF6).copy(alpha = 0.35f),
                Color(0xFFFFFFFF).copy(alpha = 0.10f)
            )
        )
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
            shape = QuranifyShapes.playerBar,
            color = Color.Transparent,
            border = glassBorder,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glassGradient)
                    .clip(QuranifyShapes.playerBar)
                    .clickable {
                        if (onOpenNowPlaying != null) {
                            onOpenNowPlaying()
                        } else {
                            navigator?.push(NowPlayingScreen())
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount > 20) {
                                AudioEngine.clear()
                            }
                        }
                    }
            ) {
                // Subtle radiant purple underglow at top border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Playback progress track: Trending Purple to Neon Lilac gradient running across top border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF8B5CF6),
                                            Color(0xFFA855F7)
                                        )
                                    )
                                )
                        )
                    }

                    // Content Row: Equalizer Artwork | Surah Info | Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Obsidian tile with animated trending purple equalizer visualizer
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF202024),
                                            Color(0xFF121214)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                            Color(0xFFFFFFFF).copy(alpha = 0.08f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            MiniPlayerEqualizer(isPlaying = isPlaying)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Center: Current Surah Name, Arabic typography, and Reciter info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${track.surahId}. ${track.surahNameEn}",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (track.surahNameAr.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = track.surahNameAr,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.reciterName,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (track.ayahNo > 0) {
                                    Text(
                                        text = " • Ayah ${track.ayahNo}",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Controls (Play/Pause Electric Purple Gradient Circular Button & Crisp White Skip Button)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Electric Purple Circular Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFFA855F7)
                                            )
                                        )
                                    )
                                    .clickable { AudioEngine.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Skip Button (Crisp White)
                            IconButton(
                                onClick = { AudioEngine.skipNext() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Skip Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
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

