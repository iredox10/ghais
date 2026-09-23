package com.ghais.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.MasteryStatus
import com.ghais.domain.model.HifzRange
import com.ghais.player.AudioEngine
import com.ghais.player.VoiceRecorderBridge
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Verse Repetition Pill: Displays current target and loop iteration.
 * Tapping opens dropdown menu to select (1x, 3x, 5x, 7x, 10x, ∞).
 */
@Composable
fun AyahRepetitionPill(
    repetitionTarget: Int,
    currentRepetition: Int,
    onTargetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = when {
        repetitionTarget == 1 -> "Loop: 1x"
        repetitionTarget == -1 -> "Loop: ∞ ($currentRepetition)"
        else -> "Loop: ${repetitionTarget}x ($currentRepetition/$repetitionTarget)"
    }

    val isActive = repetitionTarget != 1

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(if (isActive) GhaisNoir.Fill4 else GhaisNoir.Fill2)
                .border(
                    1.dp,
                    if (isActive) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                    GhaisShapes.pill
                )
                .noirClickable { expanded = true }
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (repetitionTarget == 1) Icons.Default.Repeat else Icons.Default.RepeatOne,
                contentDescription = "Ayah Repetition",
                tint = if (isActive) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = displayText,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isActive) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(GhaisNoir.CanvasTop)
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(12.dp))
        ) {
            listOf(1, 3, 5, 7, 10, -1).forEach { target ->
                val label = if (target == -1) "Continuous (∞)" else "$target times (${target}x)"
                val selected = repetitionTarget == target
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GhaisNoir.TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onTargetSelected(target)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Recitation Gap Banner: Animates when Talaqqi silence gap is active between verses.
 */
@Composable
fun RecitationGapBanner(
    countdownSeconds: Int,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gapPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillActive())
            .border(1.dp, GhaisNoir.SpecularTop.copy(alpha = 0.4f), GhaisShapes.row)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recite Aloud",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Recite aloud now (${countdownSeconds}s)",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Talaqqi student repetition window",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill3)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                    .noirClickable { onSkip() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Skip Gap",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Range Loop Badge: Displays active bounded range and current loop index.
 */
@Composable
fun RangeLoopBadge(
    range: HifzRange,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loopStr = if (range.targetLoops == -1) "Loop ${range.currentLoop}/∞"
    else "Loop ${range.currentLoop}/${range.targetLoops}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill3)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = "RANGE: ${range.startAyah}–${range.endAyah} ($loopStr)",
                color = GhaisNoir.TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(GhaisNoir.Fill2)
                .noirClickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Range Loop",
                tint = GhaisNoir.TextSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * Quick Mastery Status Pill: ⚪ Learning -> 🔘 Review Needed -> ⚫ Mastered.
 */
@Composable
fun QuickMasteryPill(
    status: MasteryStatus,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, symbol, borderColor) = when (status) {
        MasteryStatus.NEW -> Triple("Learning", "⚪", GhaisNoir.BorderCard)
        MasteryStatus.REVIEW_NEEDED -> Triple("Review", "🔘", GhaisNoir.SpecularTop.copy(alpha = 0.5f))
        MasteryStatus.MASTERED -> Triple("Mastered", "⚫", Color.White.copy(alpha = 0.7f))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, borderColor, GhaisShapes.pill)
            .noirClickable { onCycle() }
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = symbol,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GhaisNoir.TextPrimary
        )
    }
}

/**
 * Voice Recording & Instant Side-by-Side Playback Panel.
 */
@Composable
fun VoiceCompareSection(
    surahId: Int,
    ayahNo: Int,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val isRecording by VoiceRecorderBridge.isRecording.collectAsState()
    val recordingDurationMs by VoiceRecorderBridge.recordingDurationMs.collectAsState()
    val isPlayingRecording by VoiceRecorderBridge.isPlayingRecording.collectAsState()

    val isAudioEnginePlaying by AudioEngine.isPlaying.collectAsState()

    val recordingPath = remember(surahId, ayahNo, isRecording) {
        VoiceRecorderBridge.getRecordingPath(surahId, ayahNo)
    }
    val hasRecording = recordingPath != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFill())
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Toggle Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noirClickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color(0xFFEF4444) else GhaisNoir.Fill3),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Compare",
                            tint = if (isRecording) Color.White else GhaisNoir.TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Record & Compare",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                        Text(
                            text = if (hasRecording) "Recitation recorded • Tap to compare"
                            else "Record your voice to test Tajweed",
                            fontSize = 10.5.sp,
                            color = GhaisNoir.TextTertiary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Open",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextSecondary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (isRecording) {
                        // State 1B: Recording in progress
                        val seconds = (recordingDurationMs / 1000).toInt()
                        val timeStr = "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Fill4)
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), GhaisShapes.pill)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Text(
                                    text = "Recording ($timeStr)...",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.chromeFill())
                                    .noirClickable {
                                        VoiceRecorderBridge.stopRecording()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = GhaisNoir.OnChrome,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Done",
                                        color = GhaisNoir.OnChrome,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else if (!hasRecording) {
                        // State 1A: Not recorded yet
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.chromeFill())
                                .noirClickable {
                                    if (isAudioEnginePlaying) {
                                        AudioEngine.pause()
                                    }
                                    VoiceRecorderBridge.startRecording(surahId, ayahNo)
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record",
                                tint = GhaisNoir.OnChrome,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Record My Recitation",
                                color = GhaisNoir.OnChrome,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // State 2: Recorded — Dual Playback Toggle Pills
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sheikh Recitation Pill
                                val sheikhActive = isAudioEnginePlaying && !isPlayingRecording
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(GhaisShapes.pill)
                                        .background(if (sheikhActive) GhaisNoir.chromeFill() else GhaisNoir.wellFill())
                                        .border(
                                            1.dp,
                                            if (sheikhActive) Color.White.copy(alpha = 0.5f) else GhaisNoir.BorderCard,
                                            GhaisShapes.pill
                                        )
                                        .noirClickable {
                                            if (isPlayingRecording) {
                                                VoiceRecorderBridge.stopPlayback()
                                            }
                                            if (sheikhActive) {
                                                AudioEngine.pause()
                                            } else {
                                                AudioEngine.resume()
                                            }
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (sheikhActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Sheikh",
                                            tint = if (sheikhActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Sheikh",
                                            color = if (sheikhActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // My Recitation Pill
                                val userActive = isPlayingRecording
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(GhaisShapes.pill)
                                        .background(if (userActive) GhaisNoir.chromeFill() else GhaisNoir.wellFill())
                                        .border(
                                            1.dp,
                                            if (userActive) Color.White.copy(alpha = 0.5f) else GhaisNoir.BorderCard,
                                            GhaisShapes.pill
                                        )
                                        .noirClickable {
                                            if (isAudioEnginePlaying) {
                                                AudioEngine.pause()
                                            }
                                            if (userActive) {
                                                VoiceRecorderBridge.stopPlayback()
                                            } else {
                                                recordingPath?.let { VoiceRecorderBridge.playRecording(it) }
                                            }
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (userActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "My Recitation",
                                            tint = if (userActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "My Voice",
                                            color = if (userActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Re-record action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .noirClickable {
                                        VoiceRecorderBridge.stopPlayback()
                                        VoiceRecorderBridge.deleteRecording(surahId, ayahNo)
                                        if (isAudioEnginePlaying) {
                                            AudioEngine.pause()
                                        }
                                        VoiceRecorderBridge.startRecording(surahId, ayahNo)
                                    },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-record",
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Re-record Voice",
                                    fontSize = 11.sp,
                                    color = GhaisNoir.TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
