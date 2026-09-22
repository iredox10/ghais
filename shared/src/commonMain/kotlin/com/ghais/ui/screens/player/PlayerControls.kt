package com.ghais.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.RepeatMode
import com.ghais.player.AudioEngine
import com.ghais.player.SleepTimer
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

val SpeedsList = listOf(1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 0.75f)

fun formatSpeedLabel(speed: Float): String {
    val label = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${label}×"
}

// Content tracks are full-surah audio files, so AYAH repeat would loop a whole
// surah file under a confusing label — intentionally skipped in the UI cycle.
fun nextRepeatMode(current: RepeatMode): RepeatMode = when (current) {
    RepeatMode.OFF -> RepeatMode.SURAH
    RepeatMode.SURAH -> RepeatMode.QUEUE
    else -> RepeatMode.OFF
}

/**
 * Noir Glass player controls:
 * - Play/pause = chrome disc (chromeFill + OnChrome), same recipe as ChromeFab.
 * - Skip prev/next = clay wells (wellFill + ghost hairline), monochrome glyphs.
 * - Speed = ghost pill (Fill2 wash + hairline), secondary text.
 * - Repeat / sleep = clay wells; active state echoes NoirSegmentedProgress
 *   language (stronger wash + specular border + chrome dot), zero hue.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    speed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isSleepTimerActive: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onRepeatClick: () -> Unit = { AudioEngine.setRepeatMode(nextRepeatMode(repeatMode)) },
    onTogglePlayPause: () -> Unit = { AudioEngine.togglePlayPause() },
    onPrevious: () -> Unit = { AudioEngine.skipPrevious() },
    onNext: () -> Unit = { AudioEngine.skipNext() },
    onSpeedChange: (Float) -> Unit = { currentSpeed ->
        val idx = SpeedsList.indexOf(currentSpeed).takeIf { it >= 0 } ?: 0
        AudioEngine.setPlaybackSpeed(SpeedsList[(idx + 1) % SpeedsList.size])
    },
    onSleepTimerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        // Speed selector (generous touch target >= 48dp) — ghost pill.
        SpeedWell(speed = speed, onSpeedChange = onSpeedChange)

        // Previous button — clay well.
        SkipWell(
            icon = Icons.Filled.FastRewind,
            contentDescription = "Previous",
            enabled = canSkipPrevious,
            onClick = onPrevious
        )

        // Play / Pause — chrome disc.
        ChromePlayDisc(
            isPlaying = isPlaying,
            onTogglePlayPause = onTogglePlayPause
        )

        // Next button — clay well.
        SkipWell(
            icon = Icons.Filled.FastForward,
            contentDescription = "Next",
            enabled = canSkipNext,
            onClick = onNext
        )

        // Repeat mode (cycles OFF → SURAH → QUEUE) — toggle well.
        ToggleWell(
            icon = if (repeatMode == RepeatMode.SURAH) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = when (repeatMode) {
                RepeatMode.SURAH -> "Repeat surah"
                RepeatMode.QUEUE -> "Repeat queue"
                else -> "Repeat off"
            },
            active = repeatMode != RepeatMode.OFF,
            onClick = onRepeatClick
        )

        // Sleep timer — toggle well.
        ToggleWell(
            icon = Icons.Filled.Bedtime,
            contentDescription = "Sleep timer",
            active = isSleepTimerActive,
            onClick = onSleepTimerClick
        )
    }
}

/** Ghost pill for the speed cycler — wash fill + hairline, secondary label. */
@Composable
private fun SpeedWell(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 48.dp)
            .clip(shape)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, shape)
            .noirClickable { onSpeedChange(speed) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatSpeedLabel(speed),
            color = GhaisNoir.TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Clay icon-well for skip actions — embossed dome + ghost rim, no ripple. */
@Composable
private fun SkipWell(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(GhaisShapes.well)
            .background(GhaisNoir.wellFill())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
            .noirClickable { if (enabled) onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) GhaisNoir.TextPrimary else GhaisNoir.TextDisabled,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** Chrome disc for play/pause — ChromeFab recipe at hero scale, dark glyph. */
@Composable
private fun ChromePlayDisc(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "playPress"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(72.dp)
            .clip(GhaisShapes.well)
            .background(GhaisNoir.chromeFill())
            .border(1.dp, Color.White.copy(alpha = 0.4f), GhaisShapes.well)
            .clickable(interactionSource = interaction, indication = null, onClick = onTogglePlayPause),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = GhaisNoir.OnChrome,
            modifier = Modifier.size(34.dp)
        )
    }
}

/**
 * Toggle well for repeat / sleep — clay at rest, chrome-dot + specular
 * hairline when active (NoirSegmentedProgress language: chrome on engraved).
 */
@Composable
private fun ToggleWell(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(GhaisShapes.well)
            .then(
                if (active) Modifier.background(GhaisNoir.Fill4)
                else Modifier.background(GhaisNoir.wellFill())
            )
            .border(
                1.dp,
                if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.well
            )
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(22.dp)
        )
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.chromeFill())
            )
        }
    }
}

/**
 * Convenient parameterless overload that automatically subscribes to AudioEngine and SleepTimer.
 */
@Composable
fun PlayerControls(
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val speed by AudioEngine.playbackSpeed.collectAsState()
    val queue by AudioEngine.queue.collectAsState()
    val currentIndex by AudioEngine.currentIndex.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val currentPositionMs by AudioEngine.currentPositionMs.collectAsState()
    val playbackState by AudioEngine.playbackState.collectAsState()
    val sleepTimerState by SleepTimer.state.collectAsState()

    val repeatMode = playbackState.settings.repeatMode

    val canSkipNext = remember(queue, currentIndex, repeatMode, currentTrack) {
        if (currentTrack == null || queue.isEmpty()) false
        else if (repeatMode != RepeatMode.OFF) true
        else currentIndex < queue.size - 1
    }

    val canSkipPrevious = remember(queue, currentIndex, repeatMode, currentTrack, currentPositionMs) {
        if (currentTrack == null) false
        else if (currentPositionMs > 3_000L) true
        else if (queue.isEmpty()) false
        else if (repeatMode != RepeatMode.OFF) true
        else currentIndex > 0
    }

    PlayerControls(
        isPlaying = isPlaying,
        speed = speed,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        isSleepTimerActive = sleepTimerState.isActive,
        repeatMode = repeatMode,
        onRepeatClick = { AudioEngine.setRepeatMode(nextRepeatMode(repeatMode)) },
        onTogglePlayPause = { AudioEngine.togglePlayPause() },
        onPrevious = { AudioEngine.skipPrevious() },
        onNext = { AudioEngine.skipNext() },
        onSpeedChange = { currentSpeed ->
            val idx = SpeedsList.indexOf(currentSpeed).takeIf { it >= 0 } ?: 0
            AudioEngine.setPlaybackSpeed(SpeedsList[(idx + 1) % SpeedsList.size])
        },
        onSleepTimerClick = onSleepTimerClick,
        modifier = modifier
    )
}
