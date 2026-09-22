package com.ghais.ui.screens.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ghais.domain.model.RepeatMode
import com.ghais.ui.screens.player.PlayerControls as PlayerControlsImpl

/**
 * Re-export of Noir Glass PlayerControls in components package for consistency.
 * Signatures mirror the canonical overloads; repeat params appended with
 * defaults so existing callers compile untouched.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    speed: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isSleepTimerActive: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSleepTimerClick: () -> Unit = {},
    repeatMode: RepeatMode = RepeatMode.OFF,
    onRepeatClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlayerControlsImpl(
        isPlaying = isPlaying,
        speed = speed,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        isSleepTimerActive = isSleepTimerActive,
        repeatMode = repeatMode,
        onRepeatClick = onRepeatClick,
        onTogglePlayPause = onTogglePlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onSpeedChange = onSpeedChange,
        onSleepTimerClick = onSleepTimerClick,
        modifier = modifier
    )
}

@Composable
fun PlayerControls(
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerControlsImpl(
        onSleepTimerClick = onSleepTimerClick,
        modifier = modifier
    )
}
