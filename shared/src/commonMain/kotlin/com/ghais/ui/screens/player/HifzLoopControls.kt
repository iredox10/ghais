package com.ghais.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.HifzReciterLock
import com.ghais.domain.model.HifzLoopMode
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.player.components.VoiceCompareSection
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Loop mode options for Hifz range memorization.
 */
enum class HifzLoopModeOption(val label: String) {
    FLAT("Flat"),
    ACCUMULATE("Accumulate"),
    BRIDGE("Bridge"),
    FIRST_WORD("1st-Word")
}

/**
 * Adaptive pause scaling options alongside the manual pause stepper.
 */
enum class AdaptivePauseOption(val label: String, val multiplier: Float?) {
    FIXED("Fixed", null),
    X1_0("1.0×", 1.0f),
    X1_25("1.25×", 1.25f),
    X1_5("1.5×", 1.5f)
}

private fun HifzLoopModeOption.icon(): ImageVector = when (this) {
    HifzLoopModeOption.FLAT -> Icons.Filled.LinearScale
    HifzLoopModeOption.ACCUMULATE -> Icons.Filled.Layers
    HifzLoopModeOption.BRIDGE -> Icons.Filled.Link
    HifzLoopModeOption.FIRST_WORD -> Icons.Filled.RecordVoiceOver
}

private fun HifzLoopModeOption.hint(): String = when (this) {
    HifzLoopModeOption.FLAT -> "Repeats each ayah in order from A to B."
    HifzLoopModeOption.ACCUMULATE -> "Builds gradually: 1, then 1–2, then 1–3."
    HifzLoopModeOption.BRIDGE -> "Plays the end of the previous ayah to cue the next."
    HifzLoopModeOption.FIRST_WORD -> "Plays only the opening word to test memory recall."
}

@Composable
fun HifzLoopRow(
    surahId: Int,
    currentAyah: Int,
    ayahsCount: Int,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    initialLoopMode: HifzLoopModeOption = HifzLoopModeOption.FLAT,
    onLoopModeSelected: (HifzLoopModeOption) -> Unit = {},
    initialAdaptivePause: AdaptivePauseOption = AdaptivePauseOption.FIXED,
    onAdaptivePauseSelected: (AdaptivePauseOption) -> Unit = {}
) {
    val range by AudioEngine.hifzRange.collectAsState()
    val rep by AudioEngine.currentAyahRepetition.collectAsState()
    val engineLoopMode by AudioEngine.hifzLoopMode.collectAsState()
    val enginePauseMultiplier by AudioEngine.hifzPauseMultiplier.collectAsState()
    val waqfRepeatTarget by AudioEngine.waqfRepeatTarget.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val maxAyah = ayahsCount.coerceAtLeast(1)

    val activeForSurah = range?.takeIf { it.surahId == surahId }

    var startAyah by remember(surahId) {
        mutableStateOf((activeForSurah?.startAyah ?: currentAyah).coerceIn(1, maxAyah))
    }
    var endAyah by remember(surahId) {
        mutableStateOf((activeForSurah?.endAyah ?: currentAyah).coerceIn(1, maxAyah))
    }
    var repeatPerAyah by remember(surahId) {
        mutableStateOf((activeForSurah?.repeatPerAyah ?: 1).coerceIn(1, 50))
    }
    var pauseSecs by remember(surahId) {
        mutableStateOf(((activeForSurah?.pauseBetweenMs ?: 0L) / 1000L).toInt().coerceIn(0, 30))
    }
    var targetLoops by remember(surahId, activeForSurah?.targetLoops) {
        mutableStateOf(activeForSurah?.targetLoops?.coerceAtLeast(1) ?: 1)
    }

    fun syncRangeIfActive(
        s: Int = startAyah,
        e: Int = endAyah,
        loops: Int = targetLoops,
        repPerAyah: Int = repeatPerAyah,
        pause: Int = pauseSecs,
    ) {
        if (activeForSurah != null) {
            AudioEngine.setHifzRange(
                surahId = surahId,
                startAyah = s,
                endAyah = e,
                targetLoops = loops,
                repeatPerAyah = repPerAyah,
                pauseBetweenMs = pause * 1000L,
            )
        }
    }

    val loopMode = when (engineLoopMode) {
        HifzLoopMode.FLAT -> HifzLoopModeOption.FLAT
        HifzLoopMode.ACCUMULATION -> HifzLoopModeOption.ACCUMULATE
        HifzLoopMode.BRIDGE -> HifzLoopModeOption.BRIDGE
        HifzLoopMode.FIRST_WORD_PROMPT -> HifzLoopModeOption.FIRST_WORD
    }
    val adaptivePause = when {
        enginePauseMultiplier <= 0.05f -> AdaptivePauseOption.FIXED
        enginePauseMultiplier <= 1.12f -> AdaptivePauseOption.X1_0
        enginePauseMultiplier <= 1.37f -> AdaptivePauseOption.X1_25
        else -> AdaptivePauseOption.X1_5
    }
    var showRecorder by remember(surahId) { mutableStateOf(false) }
    var showAdvanced by remember(surahId) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // 1. Ayah Range (From -> To)
        HifzRangePicker(
            startAyah = startAyah,
            endAyah = endAyah,
            ayahsCount = maxAyah,
            onStartChange = { next ->
                startAyah = next
                if (endAyah < next) endAyah = next
                syncRangeIfActive(s = startAyah, e = endAyah)
            },
            onEndChange = { next ->
                endAyah = next
                if (startAyah > next) startAyah = next
                syncRangeIfActive(s = startAyah, e = endAyah)
            },
            onInteract = onInteract
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Clean 2x2 Grid Steppers: Rep + Waqf, Pause + Loops
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HifzStepper(
                icon = Icons.Filled.RepeatOne,
                label = "Rep / Ayah",
                valueText = "$repeatPerAyah×",
                onMinus = {
                    val next = (repeatPerAyah - 1).coerceAtLeast(1)
                    repeatPerAyah = next
                    syncRangeIfActive(repPerAyah = next)
                },
                onPlus = {
                    val next = (repeatPerAyah + 1).coerceAtMost(50)
                    repeatPerAyah = next
                    syncRangeIfActive(repPerAyah = next)
                },
                minusEnabled = repeatPerAyah > 1,
                plusEnabled = repeatPerAyah < 50,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
            HifzStepper(
                icon = Icons.Filled.Repeat,
                label = "Waqf Repeat",
                valueText = if (waqfRepeatTarget <= 1) "Off" else "$waqfRepeatTarget×",
                onMinus = { AudioEngine.setWaqfRepeatCount((waqfRepeatTarget - 1).coerceAtLeast(1)) },
                onPlus = { AudioEngine.setWaqfRepeatCount((waqfRepeatTarget + 1).coerceAtMost(20)) },
                minusEnabled = waqfRepeatTarget > 1,
                plusEnabled = waqfRepeatTarget < 20,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HifzStepper(
                icon = Icons.Filled.Timer,
                label = "Pause",
                valueText = "${pauseSecs}s",
                onMinus = {
                    val next = (pauseSecs - 1).coerceAtLeast(0)
                    pauseSecs = next
                    syncRangeIfActive(pause = next)
                },
                onPlus = {
                    val next = (pauseSecs + 1).coerceAtMost(30)
                    pauseSecs = next
                    syncRangeIfActive(pause = next)
                },
                minusEnabled = pauseSecs > 0,
                plusEnabled = pauseSecs < 30,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
            HifzStepper(
                icon = Icons.Filled.AllInclusive,
                label = "Loops",
                valueText = "$targetLoops×",
                onMinus = {
                    val next = (targetLoops - 1).coerceAtLeast(1)
                    targetLoops = next
                    syncRangeIfActive(loops = next)
                },
                onPlus = {
                    val next = (targetLoops + 1).coerceAtMost(50)
                    targetLoops = next
                    syncRangeIfActive(loops = next)
                },
                minusEnabled = targetLoops > 1,
                plusEnabled = targetLoops < 50,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Primary Action Bar: Start/Active Loop + Clear (if active) + Compare Mic
        val active = activeForSurah
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val ctaShape = RoundedCornerShape(12.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(ctaShape)
                    .then(
                        if (active != null) {
                            Modifier
                                .background(GhaisNoir.plumFill())
                                .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.45f), ctaShape)
                        } else {
                            Modifier.background(GhaisNoir.lavenderFill())
                        }
                    )
                    .noirClickable {
                        onInteract()
                        AudioEngine.setHifzRange(
                            surahId = surahId,
                            startAyah = startAyah,
                            endAyah = endAyah,
                            targetLoops = targetLoops,
                            repeatPerAyah = repeatPerAyah,
                            pauseBetweenMs = pauseSecs * 1000L,
                        )
                        val slug = currentTrack?.reciterSlug
                        if (slug != null && currentAyah !in startAyah..endAyah) {
                            AudioEngine.playSurahInAyahMode(surahId, slug, startAyahNo = startAyah)
                        }
                    }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = if (active != null) Icons.Filled.Repeat else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = if (active != null) GhaisNoir.Lavender else GhaisNoir.LavenderInk,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (active != null) {
                        "Ayah ${active.startAyah}–${active.endAyah} · rep $rep/${active.repeatPerAyah} · ${active.currentLoop}/${active.targetLoops}"
                    } else {
                        "Start Loop"
                    },
                    color = if (active != null) GhaisNoir.TextPrimary else GhaisNoir.LavenderInk,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (active != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                        .noirClickable {
                            onInteract()
                            AudioEngine.clearHifzRange()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear loop",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Record & compare Mic toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (showRecorder) Modifier.background(GhaisNoir.lavenderFill())
                        else Modifier
                            .background(GhaisNoir.wellFill())
                            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(12.dp))
                    )
                    .noirClickable {
                        onInteract()
                        showRecorder = !showRecorder
                    }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Record & compare",
                    tint = if (showRecorder) GhaisNoir.LavenderInk else GhaisNoir.Lavender,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Compare",
                    color = if (showRecorder) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        if (showRecorder) {
            Spacer(modifier = Modifier.height(10.dp))
            VoiceCompareSection(
                surahId = surahId,
                ayahNo = currentAyah.coerceIn(1, maxAyah)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Collapsible Mode & Smart Pause drawer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GhaisNoir.Fill1)
                .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(10.dp))
                .noirClickable {
                    onInteract()
                    showAdvanced = !showAdvanced
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null,
                tint = GhaisNoir.Lavender,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Mode: ${loopMode.label}  ·  Gap: ${adaptivePause.label}",
                color = GhaisNoir.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (showAdvanced) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (showAdvanced) "Hide mode options" else "Show mode options",
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = showAdvanced) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(10.dp))

                HifzChipSelectorRow(
                    label = "Memorization mode",
                    options = HifzLoopModeOption.entries,
                    selected = loopMode,
                    labelFor = { it.label },
                    iconFor = { it.icon() },
                    onSelect = { mode ->
                        onInteract()
                        val engineMode = when (mode) {
                            HifzLoopModeOption.FLAT -> HifzLoopMode.FLAT
                            HifzLoopModeOption.ACCUMULATE -> HifzLoopMode.ACCUMULATION
                            HifzLoopModeOption.BRIDGE -> HifzLoopMode.BRIDGE
                            HifzLoopModeOption.FIRST_WORD -> HifzLoopMode.FIRST_WORD_PROMPT
                        }
                        AudioEngine.setHifzLoopMode(engineMode)
                        // Auto-arm Accumulate chain when selected so user gets immediate behavior
                        if (mode == HifzLoopModeOption.ACCUMULATE && activeForSurah == null) {
                            if (endAyah <= startAyah && maxAyah > startAyah) {
                                endAyah = (startAyah + 2).coerceAtMost(maxAyah)
                            }
                            AudioEngine.setHifzRange(
                                surahId = surahId,
                                startAyah = startAyah,
                                endAyah = endAyah,
                                targetLoops = targetLoops,
                                repeatPerAyah = repeatPerAyah,
                                pauseBetweenMs = pauseSecs * 1000L,
                                loopMode = engineMode,
                            )
                        }
                        onLoopModeSelected(mode)
                    }
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = loopMode.hint(),
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                HifzChipSelectorRow(
                    label = "Smart pause",
                    options = AdaptivePauseOption.entries,
                    selected = adaptivePause,
                    labelFor = { it.label },
                    onSelect = { option ->
                        onInteract()
                        val multiplier = when (option) {
                            AdaptivePauseOption.FIXED -> 0f
                            AdaptivePauseOption.X1_0 -> 1.0f
                            AdaptivePauseOption.X1_25 -> 1.25f
                            AdaptivePauseOption.X1_5 -> 1.5f
                        }
                        AudioEngine.setHifzPauseMultiplier(multiplier)
                        onAdaptivePauseSelected(option)
                    }
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = if (adaptivePause == AdaptivePauseOption.FIXED) {
                        "Uses your exact Pause seconds between repetitions."
                    } else {
                        "Scales pause to ${adaptivePause.label} of each ayah's duration for mental recall."
                    },
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A–B range picker: start/end ayah steppers bounded by the surah's ayah count. */
@Composable
fun HifzRangePicker(
    startAyah: Int,
    endAyah: Int,
    ayahsCount: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxAyah = ayahsCount.coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ayah range",
                color = GhaisNoir.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "of $maxAyah",
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HifzStepper(
                label = "From",
                valueText = "$startAyah",
                onMinus = { onStartChange((startAyah - 1).coerceIn(1, maxAyah)) },
                onPlus = { onStartChange((startAyah + 1).coerceIn(1, maxAyah)) },
                minusEnabled = startAyah > 1,
                plusEnabled = startAyah < maxAyah,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = GhaisNoir.Lavender.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(14.dp)
            )
            HifzStepper(
                label = "To",
                valueText = "$endAyah",
                onMinus = { onEndChange((endAyah - 1).coerceIn(1, maxAyah)) },
                onPlus = { onEndChange((endAyah + 1).coerceIn(1, maxAyah)) },
                minusEnabled = endAyah > 1,
                plusEnabled = endAyah < maxAyah,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Reciter lock chip: locks to the current reciter slug, tap toggles
 * lock/unlock.
 */
@Composable
fun HifzReciterLockChip(
    currentSlug: String,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked by HifzReciterLock.lockedSlug.collectAsState()
    val isLocked = locked != null
    val differs = isLocked && locked != currentSlug
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLocked) GhaisNoir.tintFill() else GhaisNoir.cardFillSoft())
            .border(
                1.dp,
                if (isLocked) GhaisNoir.Lavender.copy(alpha = 0.35f) else GhaisNoir.BorderGhost,
                RoundedCornerShape(12.dp)
            )
            .noirClickable {
                onInteract()
                if (isLocked) HifzReciterLock.unlock() else HifzReciterLock.lock(currentSlug)
            }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = null,
            tint = if (isLocked) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isLocked) "Reciter locked" else "Lock reciter",
            color = if (isLocked) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = if (isLocked) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (differs) {
            Text(
                text = "differs",
                color = GhaisNoir.Lavender,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HifzStepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GhaisNoir.Lavender.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GhaisNoir.insetFill())
                .border(1.dp, GhaisNoir.InsetBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 3.dp, vertical = 3.dp)
        ) {
            HifzStepButton(
                text = "−",
                enabled = minusEnabled,
                onClick = { onInteract(); onMinus() }
            )
            Text(
                text = valueText,
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            HifzStepButton(
                text = "+",
                enabled = plusEnabled,
                onClick = { onInteract(); onPlus() }
            )
        }
    }
}

@Composable
private fun HifzStepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) GhaisNoir.Fill3 else GhaisNoir.Fill1)
            .then(if (enabled) Modifier.noirClickable(onClick) else Modifier)
    ) {
        Text(
            text = text,
            color = if (enabled) GhaisNoir.TextPrimary else GhaisNoir.TextDisabled,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Toggle pill for the loop-settings card, showing a live active badge when a
 * range is running.
 */
@Composable
fun HifzSettingsToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRange by AudioEngine.hifzRange.collectAsState()
    val waqfRepeatTarget by AudioEngine.waqfRepeatTarget.collectAsState()
    val isBadgeActive = activeRange != null || waqfRepeatTarget > 1
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isBadgeActive) GhaisNoir.tintFill() else GhaisNoir.cardFillSoft())
            .border(
                1.dp,
                if (isBadgeActive) GhaisNoir.Lavender.copy(alpha = 0.35f) else GhaisNoir.BorderGhost,
                RoundedCornerShape(14.dp)
            )
            .noirClickable(onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = null,
            tint = GhaisNoir.Lavender,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Loop & Memorize",
            color = GhaisNoir.TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (waqfRepeatTarget > 1) {
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Lavender.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Waqf ${waqfRepeatTarget}×",
                    color = GhaisNoir.Lavender,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        activeRange?.let { active ->
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Lavender.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${active.startAyah}–${active.endAyah} · ${active.currentLoop}/${active.targetLoops}",
                    color = GhaisNoir.Lavender,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun <T> HifzChipSelectorRow(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    iconFor: ((T) -> ImageVector)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val active = option == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (active) Modifier.background(GhaisNoir.lavenderFill())
                            else Modifier
                                .background(GhaisNoir.Fill1)
                                .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(10.dp))
                        )
                        .noirClickable { onSelect(option) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    if (iconFor != null) {
                        Icon(
                            imageVector = iconFor(option),
                            contentDescription = null,
                            tint = if (active) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Text(
                        text = labelFor(option),
                        color = if (active) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
