package com.quranify.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranify.player.SleepTimer
import com.quranify.player.StopCondition
import com.quranify.ui.theme.QuranifyColors
import com.quranify.ui.theme.QuranifyTypography

// Obsidian-Emerald Glassmorphic Palette
private val SheetContainer = Color(0xFF181E20)       // Obsidian container
private val SheetBorder = Color(0x204EDEA3)          // Subtle emerald border
private val LiquidEmerald = Color(0xFF4EDEA3)        // Liquid Emerald accent
private val CardBackground = Color(0xFF1F2527)       // Slightly elevated dark obsidian card
private val CardBorder = Color(0x184EDEA3)           // Card border
private val TextPrimary = Color(0xFFFFFFFF)          // High-emphasis white
private val TextSecondary = Color(0xFF8E989C)        // Muted subtitle
private val TextMuted = Color(0xFF5E686C)            // Inactive
private val ErrorColor = Color(0xFFFFB4AB)           // Subtle error / cancel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    onDismiss: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by SleepTimer.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald.copy(alpha = 0.45f))
            )
        },
        modifier = Modifier.border(
            width = 1.dp,
            color = SheetBorder,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Sleep Timer",
                style = QuranifyTypography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (state.isActive) "Playback will automatically stop" else "Set audio to automatically stop when you rest",
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isActive) {
                ActiveTimerView(
                    remainingSeconds = state.remainingSeconds,
                    condition = state.stopCondition,
                    onCancel = { SleepTimer.cancelTimer() }
                )
            } else {
                TimerSetupView()
            }
        }
    }
}

@Composable
private fun ActiveTimerView(
    remainingSeconds: Long,
    condition: StopCondition,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (condition == StopCondition.MINUTES) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(130.dp),
                    color = LiquidEmerald,
                    strokeWidth = 5.dp,
                    trackColor = CardBackground
                )

                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                val seconds = remainingSeconds % 60

                val timeString = if (hours > 0) {
                    "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                } else {
                    "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        style = QuranifyTypography.titleMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidEmerald.copy(alpha = 0.15f))
                            .border(1.dp, LiquidEmerald.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Remaining",
                            style = QuranifyTypography.bodyMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LiquidEmerald
                            )
                        )
                    }
                }
            }
        } else {
            val (statusText, conditionIcon) = when (condition) {
                StopCondition.END_OF_AYAH -> "Stopping after current Ayah" to Icons.Default.GraphicEq
                StopCondition.END_OF_SURAH -> "Stopping after current Surah" to Icons.Default.Timer
                StopCondition.END_OF_QUEUE -> "Stopping after Queue ends" to Icons.AutoMirrored.Filled.QueueMusic
                else -> "Active" to Icons.Default.Timer
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LiquidEmerald.copy(alpha = 0.12f))
                    .border(1.dp, LiquidEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LiquidEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = conditionIcon,
                            contentDescription = null,
                            tint = LiquidEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "ACTIVE BOUNDARY",
                            style = QuranifyTypography.bodyMedium.copy(
                                fontSize = 11.sp,
                                color = LiquidEmerald,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusText,
                            style = QuranifyTypography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Cancel button: obsidian glass with subtle error rim
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                containerColor = CardBackground,
                contentColor = ErrorColor
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .border(1.dp, ErrorColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = ErrorColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Turn Off Timer",
                style = QuranifyTypography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = ErrorColor
                )
            )
        }
    }
}

@Composable
private fun TimerSetupView() {
    var fadeOutEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf<Int?>(null) }
    var selectedBoundary by remember { mutableStateOf<StopCondition?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Duration Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DURATION",
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
            )
        }

        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                15 to "15m",
                30 to "30m",
                45 to "45m",
                60 to "1h"
            ).forEach { (minutes, label) ->
                val isSelected = selectedPreset == minutes
                PresetChip(
                    text = label,
                    isSelected = isSelected,
                    onClick = {
                        selectedPreset = minutes
                        selectedBoundary = null
                        SleepTimer.startTimer(minutes, fadeOut = fadeOutEnabled)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Boundaries Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SMART BOUNDARIES",
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BoundaryOption(
                title = "End of current Ayah",
                subtitle = "Stop after completing the current verse",
                isSelected = selectedBoundary == StopCondition.END_OF_AYAH,
                onClick = {
                    selectedBoundary = StopCondition.END_OF_AYAH
                    selectedPreset = null
                    SleepTimer.startAtAyahBoundary(fadeOut = fadeOutEnabled)
                }
            )
            BoundaryOption(
                title = "End of Surah",
                subtitle = "Stop after completing the entire chapter",
                isSelected = selectedBoundary == StopCondition.END_OF_SURAH,
                onClick = {
                    selectedBoundary = StopCondition.END_OF_SURAH
                    selectedPreset = null
                    SleepTimer.startAtSurahBoundary(fadeOut = fadeOutEnabled)
                }
            )
            BoundaryOption(
                title = "End of Queue",
                subtitle = "Stop when all upcoming recitations finish",
                isSelected = selectedBoundary == StopCondition.END_OF_QUEUE,
                onClick = {
                    selectedBoundary = StopCondition.END_OF_QUEUE
                    selectedPreset = null
                    SleepTimer.startAtQueueBoundary(fadeOut = fadeOutEnabled)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Gentle Fade-out toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .clickable { fadeOutEnabled = !fadeOutEnabled }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gentle Fade-out",
                    style = QuranifyTypography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Slowly lower volume over the last 30 seconds",
                    style = QuranifyTypography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )
            }

            Switch(
                checked = fadeOutEnabled,
                onCheckedChange = { fadeOutEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF111415),
                    checkedTrackColor = LiquidEmerald,
                    checkedBorderColor = LiquidEmerald,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = Color(0xFF181E20),
                    uncheckedBorderColor = Color(0x304EDEA3)
                )
            )
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald.copy(alpha = 0.18f) else CardBackground
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald else CardBorder
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald else TextPrimary
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = QuranifyTypography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun BoundaryOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rowBackground by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald.copy(alpha = 0.12f) else CardBackground
    )
    val rowBorder by animateColorAsState(
        targetValue = if (isSelected) LiquidEmerald.copy(alpha = 0.6f) else CardBorder
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = rowBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = QuranifyTypography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Radio button with Emerald glow
        GlowingRadioButton(selected = isSelected)
    }
}

@Composable
private fun GlowingRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val ringColor by animateColorAsState(
        targetValue = if (selected) LiquidEmerald else Color(0xFF3C4A42)
    )

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = ringColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(LiquidEmerald)
            )
        }
    }
}
