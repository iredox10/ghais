package com.ghais.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.ghais.player.SleepTimer
import com.ghais.player.StopCondition
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

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
        containerColor = GhaisNoir.CanvasTop,
        scrimColor = GhaisNoir.Scrim,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header — sort-sheet recipe: IconWell + title, ghost close disc.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconWell(
                        icon = Icons.Default.Timer,
                        size = 40.dp,
                        iconSize = 20.dp,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = "Sleep Timer",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                        Text(
                            text = if (state.isActive) "Playback will automatically stop"
                            else "Audio stops automatically when you rest",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .noirClickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (state.isActive) {
                ActiveTimerView(
                    remainingSeconds = state.remainingSeconds,
                    condition = state.stopCondition,
                    onCancel = { SleepTimer.cancelTimer() }
                )
            } else {
                TimerSetupView()
            }

            Spacer(modifier = Modifier.height(8.dp))
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (condition == StopCondition.MINUTES) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(130.dp),
                    color = Color.White,
                    strokeWidth = 5.dp,
                    trackColor = GhaisNoir.Fill2
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
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhaisNoir.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Remaining",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextSecondary
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

            // Active boundary card — selected elevation, state reads through fill, never hue.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.row)
                    .background(GhaisNoir.cardFillActive())
                    .border(1.dp, GhaisNoir.SpecularTop, GhaisShapes.row)
                    .topSpecular(inset = 22.dp)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconWell(
                    icon = conditionIcon,
                    size = 44.dp,
                    iconSize = 22.dp,
                    contentDescription = null
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "ACTIVE BOUNDARY",
                        fontSize = 11.sp,
                        color = GhaisNoir.TextTertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary
                    )
                }
            }
        }

        // Turn-off — ghost pill, monochrome (no error hue).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                .noirClickable(onClick = onCancel)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Turn Off Timer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimerSetupView() {
    var fadeOutEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf<Int?>(null) }
    var selectedBoundary by remember { mutableStateOf<StopCondition?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NoirSectionHeader(label = "Duration")

        // Preset chips — pill variant of the row recipe.
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

        NoirSectionHeader(
            label = "Smart boundaries",
            modifier = Modifier.padding(top = 6.dp)
        )

        BoundaryOption(
            title = "End of current Ayah",
            subtitle = "Stop after completing the current verse",
            icon = Icons.Default.GraphicEq,
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
            icon = Icons.Default.Timer,
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
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            isSelected = selectedBoundary == StopCondition.END_OF_QUEUE,
            onClick = {
                selectedBoundary = StopCondition.END_OF_QUEUE
                selectedPreset = null
                SleepTimer.startAtQueueBoundary(fadeOut = fadeOutEnabled)
            }
        )

        // Gentle fade-out — resting row + NoirSwitch (ON = white track + near-black thumb).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.row)
                .background(GhaisNoir.cardFillSoft())
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
                .topSpecular(inset = 22.dp)
                .noirClickable { fadeOutEnabled = !fadeOutEnabled }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gentle Fade-out",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GhaisNoir.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Slowly lower volume over the last 30 seconds",
                    fontSize = 12.sp,
                    color = GhaisNoir.TextSecondary
                )
            }
            NoirSwitch(
                checked = fadeOutEnabled,
                onCheckedChange = { fadeOutEnabled = it }
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
    Box(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(if (isSelected) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(
                1.dp,
                if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.pill
            )
            .noirClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BoundaryOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Sort-sheet row recipe: cardFillActive + chrome check when selected.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(
                if (isSelected) GhaisNoir.cardFillActive()
                else GhaisNoir.cardFillSoft()
            )
            .border(
                1.dp,
                if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.row
            )
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(
            icon = icon,
            size = 36.dp,
            iconSize = 18.dp,
            contentDescription = null,
            tint = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = GhaisNoir.TextSecondary
            )
        }

        if (isSelected) {
            ChromeCheckDisc()
        }
    }
}

@Composable
private fun ChromeCheckDisc(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(GhaisNoir.chromeFill(), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = GhaisNoir.OnChrome,
            modifier = Modifier.size(14.dp)
        )
    }
}
