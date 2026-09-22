package com.ghais.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.ui.navigation.LocalRootNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.RepeatMode
import com.ghais.player.AmbientMixer
import com.ghais.player.AudioEngine
import com.ghais.player.displayName
import com.ghais.player.videoKeys
import com.ghais.ui.components.noir.ChromeFab
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.player.components.NowPlayingLyricsCard
import com.ghais.ui.screens.player.components.NowPlayingVolumePanel
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Now Playing — strict Noir Glass monochrome.
 *
 * Presentation only. Screen signature, drag-to-dismiss, AudioEngine / queue /
 * repeat / sleep / favorite wiring, sheet wiring and navigation are untouched.
 * Zero hue: canvas #050506 + glow + grain, grayscale artwork with scrim and
 * chromium ring, chrome/ghost controls, NoirSegmentedProgress meter language,
 * text ladder 100 / 62 / 38 / 24%.
 */
class NowPlayingScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current
        val coroutineScope = rememberCoroutineScope()
        val density = LocalDensity.current
        val dismissThresholdPx = with(density) { 120.dp.toPx() }

        var dragOffsetY by remember { mutableStateOf(0f) }
        var isDismissed by remember { mutableStateOf(false) }
        var settleJob by remember { mutableStateOf<Job?>(null) }
        var lastDragTime by remember { mutableStateOf(0L) }
        var dragVelocityY by remember { mutableStateOf(0f) }

        val dismissPlayer: () -> Unit = remember(rootNavigator, navigator) {
            {
                if (!isDismissed) {
                    isDismissed = true
                    (rootNavigator ?: navigator)?.pop()
                }
            }
        }
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()
        val progress by AudioEngine.progress.collectAsState()
        val currentPositionMs by AudioEngine.currentPositionMs.collectAsState()
        val durationMs by AudioEngine.durationMs.collectAsState()
        val queue by AudioEngine.queue.collectAsState()
        val currentIndex by AudioEngine.currentIndex.collectAsState()
        val playbackState by AudioEngine.playbackState.collectAsState()
        val sleepTimerState by com.ghais.player.SleepTimer.state.collectAsState()
        val repeatMode = playbackState.settings.repeatMode

        var showSleepTimer by remember { mutableStateOf(false) }
        var showQueue by remember { mutableStateOf(false) }
        var showAmbient by remember { mutableStateOf(false) }
        val mixer = remember { AmbientMixer }
        val ambientChannels by mixer.channels.collectAsState()
        val selectedAmbientType = ambientChannels.firstOrNull { it.isEnabled }?.type
        val hasAmbientVideo = selectedAmbientType?.videoKeys()?.isNotEmpty() == true

        val track = currentTrack
        val title = track?.surahNameEn ?: "Ar-Rahman"
        val surahNameAr = track?.surahNameAr ?: "الرحمن"
        val reciterName = track?.reciterName ?: "Mishary Rashid Alafasy"

        val canSkipNext = remember(queue, currentIndex, repeatMode, track) {
            if (track == null || queue.isEmpty()) false
            else if (repeatMode != com.ghais.domain.model.RepeatMode.OFF) true
            else currentIndex < queue.size - 1
        }

        val canSkipPrevious = remember(queue, currentIndex, repeatMode, track, currentPositionMs) {
            if (track == null) false
            else if (currentPositionMs > 3_000L) true
            else if (queue.isEmpty()) false
            else if (repeatMode != com.ghais.domain.model.RepeatMode.OFF) true
            else currentIndex > 0
        }

        val totalMs = if (durationMs > 0L) durationMs
            else (track?.durationMs?.takeIf { it > 0L } ?: 0L)
        // Time labels derive from the scrub-aware active position below.

        NoirScreenRoot(
            modifier = Modifier
                .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            settleJob?.cancel()
                            dragVelocityY = 0f
                            lastDragTime = 0L
                        },
                        onDragEnd = {
                            val shouldDismiss = dragOffsetY >= dismissThresholdPx ||
                                (dragVelocityY > 700f && dragOffsetY > with(density) { 20.dp.toPx() })
                            if (shouldDismiss) {
                                dismissPlayer()
                            } else {
                                settleJob = coroutineScope.launch {
                                    animate(
                                        initialValue = dragOffsetY,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) { value, _ ->
                                        dragOffsetY = value
                                    }
                                }
                            }
                            dragVelocityY = 0f
                            lastDragTime = 0L
                        },
                        onDragCancel = {
                            settleJob = coroutineScope.launch {
                                animate(
                                    initialValue = dragOffsetY,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { value, _ ->
                                    dragOffsetY = value
                                }
                            }
                            dragVelocityY = 0f
                            lastDragTime = 0L
                        },
                        onVerticalDrag = { change, dragAmount ->
                            val now = change.uptimeMillis
                            if (lastDragTime > 0L) {
                                val dt = (now - lastDragTime).coerceAtLeast(1L)
                                val instantVelocity = (dragAmount / dt.toFloat()) * 1000f
                                dragVelocityY = 0.7f * dragVelocityY + 0.3f * instantVelocity
                            }
                            lastDragTime = now

                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                            if (dragOffsetY > dismissThresholdPx) {
                                dismissPlayer()
                            }
                        }
                    )
                }
        ) {
            // Ambient video is the hero — only a light veil + bottom grade
            // for legibility, so the video stays the focus of the player.
            AmbientVideoView(selectedType = selectedAmbientType, modifier = Modifier.fillMaxSize())
            if (hasAmbientVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GhaisNoir.NoirBlack.copy(alpha = 0.22f))
                )
            }
            // Bottom legibility grade — monochrome melt into the canvas.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GhaisNoir.NoirBlack.copy(alpha = 0.80f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Top pill bar handle (tap-to-dismiss option with generous touch target)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { (rootNavigator ?: navigator)?.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GhaisNoir.TextDisabled)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Video-first: clear the middle so the ambient video breathes.
                Spacer(modifier = Modifier.weight(1f))

                // Minimal identity floating over the video.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            color = GhaisNoir.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (surahNameAr.isNotBlank()) {
                            Text(
                                text = surahNameAr,
                                color = GhaisNoir.TextSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reciterName,
                        color = GhaisNoir.TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Minimal transport: prev / play / next only.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NoirTransportWell(
                        icon = Icons.Filled.FastRewind,
                        contentDescription = "Previous",
                        enabled = canSkipPrevious,
                        onClick = { AudioEngine.skipPrevious() },
                        iconSize = 28.dp
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    ChromeFab(
                        icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        onClick = { AudioEngine.togglePlayPause() },
                        size = 76.dp,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    NoirTransportWell(
                        icon = Icons.Filled.FastForward,
                        contentDescription = "Next",
                        enabled = canSkipNext,
                        onClick = { AudioEngine.skipNext() },
                        iconSize = 28.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress scrubber (tap & horizontal scrub with live preview) —
                // NoirSegmentedProgress language: engraved track + chrome fill.
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubFraction by remember { mutableStateOf(0f) }

                val activeProgress = if (isScrubbing) scrubFraction else progress.coerceIn(0f, 1f)
                val activePositionMs = if (isScrubbing && totalMs > 0L) {
                    (scrubFraction * totalMs).toLong()
                } else {
                    currentPositionMs
                }
                val activeElapsedText = formatMs(activePositionMs)
                val activeRemainingText = if (totalMs > 0L) {
                    "-${formatMs((totalMs - activePositionMs).coerceAtLeast(0L))}"
                } else {
                    "--:--"
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val width = maxWidth
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(totalMs) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val widthPx = size.width.toFloat()
                                    if (widthPx <= 0f) return@awaitEachGesture

                                    down.consume()
                                    scrubFraction = (down.position.x / widthPx).coerceIn(0f, 1f)
                                    isScrubbing = true

                                    val pointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (change.changedToUp()) {
                                            change.consume()
                                            if (totalMs > 0L) {
                                                AudioEngine.seekTo((scrubFraction * totalMs).toLong())
                                            }
                                            break
                                        }
                                        if (change.isConsumed) {
                                            break
                                        }
                                        change.consume()
                                        scrubFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                                    }
                                    isScrubbing = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Engraved track (mirrors NoirSegmentedProgress).
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.insetFill())
                                .border(1.dp, GhaisNoir.InsetBorder, CircleShape)
                        ) {
                            // Chrome fill.
                            if (activeProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(activeProgress)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.chromeFill())
                                )
                            }
                        }

                        // Chrome thumb (visible during scrubbing/drag).
                        if (isScrubbing) {
                            val thumbOffset = ((width - 14.dp) * activeProgress).coerceAtLeast(0.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = thumbOffset)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.chromeFill())
                                        .border(1.dp, GhaisNoir.NoirBlack, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = activeElapsedText,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = activeRemainingText,
                        color = GhaisNoir.TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Overflow: background video, sleep timer, queue — tiny ghost wells.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        14.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NoirUtilityWell(
                        icon = Icons.Filled.Movie,
                        contentDescription = "Background",
                        active = selectedAmbientType != null,
                        tint = GhaisNoir.TextTertiary,
                        onClick = { showAmbient = true }
                    )
                    NoirUtilityWell(
                        icon = Icons.Filled.Bedtime,
                        contentDescription = "Sleep timer",
                        active = sleepTimerState.isActive,
                        tint = GhaisNoir.TextTertiary,
                        onClick = { showSleepTimer = true }
                    )
                    NoirUtilityWell(
                        icon = Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        active = false,
                        tint = GhaisNoir.TextTertiary,
                        onClick = { showQueue = true }
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

@Composable
private fun NoirTransportWell(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (enabled) GhaisNoir.Fill2 else GhaisNoir.Fill1)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) GhaisNoir.TextPrimary else GhaisNoir.TextDisabled,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun NoirUtilityWell(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) GhaisNoir.Fill4 else GhaisNoir.Fill1)
                .border(
                    1.dp,
                    if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms.coerceAtLeast(0L) / 1000).toInt()
    return "${(s / 60).toString().padStart(2, '0')}:${(s % 60).toString().padStart(2, '0')}"
}


