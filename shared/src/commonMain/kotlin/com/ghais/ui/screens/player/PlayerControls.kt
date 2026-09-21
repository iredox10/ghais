package com.ghais.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.RepeatMode
import com.ghais.player.AudioEngine
import com.ghais.player.SleepTimer
import com.ghais.ui.theme.GhaisColors

private val MutedGrey = Color(0xFF9A9AA0)
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
 * Modern Ghais Player Controls:
 * - Playback speed selector (cycles through SpeedsList)
 * - Previous skip/rewind button with enabled state logic (rewinds to 0s if pos > 3s, skips prev if in queue)
 * - Prominent Play/Pause toggle button
 * - Next skip button with enabled state logic (enabled if next track exists or repeat mode active)
 * - Repeat mode button (cycles OFF → SURAH → QUEUE)
 * - Sleep timer trigger (glowing primary tint when active timer is running)
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
        // Speed selector (generous touch target >= 48dp)
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSpeedChange(speed) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatSpeedLabel(speed),
                color = MutedGrey,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Previous button
        IconButton(
            onClick = onPrevious,
            enabled = canSkipPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FastRewind,
                contentDescription = "Previous",
                tint = if (canSkipPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            )
        }

        // Play / Pause button
        IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(62.dp)
            )
        }

        // Next button
        IconButton(
            onClick = onNext,
            enabled = canSkipNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = "Next",
                tint = if (canSkipNext) Color.White else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            )
        }

        // Repeat mode button (cycles OFF → SURAH → QUEUE)
        IconButton(
            onClick = onRepeatClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.SURAH) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = when (repeatMode) {
                    RepeatMode.SURAH -> "Repeat surah"
                    RepeatMode.QUEUE -> "Repeat queue"
                    else -> "Repeat off"
                },
                tint = if (repeatMode == RepeatMode.OFF) MutedGrey else GhaisColors.Primary,
                modifier = Modifier.size(27.dp)
            )
        }

        // Sleep timer button
        IconButton(
            onClick = onSleepTimerClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = "Sleep timer",
                tint = if (isSleepTimerActive) GhaisColors.Primary else MutedGrey,
                modifier = Modifier.size(27.dp)
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
