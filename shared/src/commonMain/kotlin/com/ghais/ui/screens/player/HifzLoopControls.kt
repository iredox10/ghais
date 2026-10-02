package com.ghais.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Contract owned by the parallel data agent: HifzReciterLock exposes
// lockedSlug: StateFlow<String?>, lock(slug), unlock().
import com.ghais.data.repository.HifzReciterLock
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir

/**
 * Phase 1 hifz loop chrome (ayah mode only). Every tap routes through
 * [onInteract] (the screen's poke()) so the 10s auto-hide clock restarts.
 *
 * Confirm calls the extended AudioEngine.setHifzRange overload
 * setHifzRange(surahId, startAyah, endAyah, targetLoops, repeatPerAyah,
 * pauseBetweenMs), wiring the Rep stepper to repeatPerAyah and the Pause
 * stepper (seconds) to pauseBetweenMs.
 */
@Composable
fun HifzLoopRow(
    surahId: Int,
    currentAyah: Int,
    ayahsCount: Int,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val range by AudioEngine.hifzRange.collectAsState()
    val rep by AudioEngine.currentAyahRepetition.collectAsState()
    val maxAyah = ayahsCount.coerceAtLeast(1)

    var startAyah by remember(surahId, currentAyah) {
        mutableStateOf(currentAyah.coerceIn(1, maxAyah))
    }
    var endAyah by remember(surahId, currentAyah) {
        mutableStateOf(currentAyah.coerceIn(1, maxAyah))
    }
    var repeatPerAyah by remember(surahId) { mutableStateOf(1) }
    var pauseSecs by remember(surahId) { mutableStateOf(0) }
    var targetLoops by remember(surahId, range?.targetLoops) {
        mutableStateOf(range?.targetLoops?.coerceAtLeast(1) ?: 1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GhaisNoir.Fill1)
            .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        HifzRangePicker(
            startAyah = startAyah,
            endAyah = endAyah,
            ayahsCount = maxAyah,
            onStartChange = { startAyah = it },
            onEndChange = { endAyah = it },
            onInteract = onInteract
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HifzStepper(
                label = "Rep",
                valueText = "$repeatPerAyah×",
                onMinus = { repeatPerAyah = (repeatPerAyah - 1).coerceAtLeast(1) },
                onPlus = { repeatPerAyah = (repeatPerAyah + 1).coerceAtMost(50) },
                minusEnabled = repeatPerAyah > 1,
                plusEnabled = repeatPerAyah < 50,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
            HifzStepper(
                label = "Pause",
                valueText = "${pauseSecs}s",
                onMinus = { pauseSecs = (pauseSecs - 1).coerceAtLeast(0) },
                onPlus = { pauseSecs = (pauseSecs + 1).coerceAtMost(30) },
                minusEnabled = pauseSecs > 0,
                plusEnabled = pauseSecs < 30,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
            HifzStepper(
                label = "Loops",
                valueText = "$targetLoops×",
                onMinus = { targetLoops = (targetLoops - 1).coerceAtLeast(1) },
                onPlus = { targetLoops = (targetLoops + 1).coerceAtMost(50) },
                minusEnabled = targetLoops > 1,
                plusEnabled = targetLoops < 50,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val active = range
        Text(
            text = if (active != null) {
                "Ayah ${active.startAyah}–${active.endAyah} · rep $rep/$repeatPerAyah · loop ${active.currentLoop}/${active.targetLoops}"
            } else {
                "Loop A–B to memorize"
            },
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GhaisNoir.Fill4)
                    .border(1.dp, GhaisNoir.SpecularTop, RoundedCornerShape(10.dp))
                    .noirClickable {
                        onInteract()
                        AudioEngine.setHifzRange(surahId, startAyah, endAyah, targetLoops, repeatPerAyah, pauseSecs * 1000L)
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Loop range",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GhaisNoir.Fill1)
                    .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(10.dp))
                    .noirClickable {
                        onInteract()
                        AudioEngine.clearHifzRange()
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Clear",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        HifzStepper(
            label = "A",
            valueText = "$startAyah",
            onMinus = { onStartChange((startAyah - 1).coerceIn(1, maxAyah)) },
            onPlus = { onStartChange((startAyah + 1).coerceIn(1, maxAyah)) },
            minusEnabled = startAyah > 1,
            plusEnabled = startAyah < maxAyah,
            onInteract = onInteract,
            modifier = Modifier.weight(1f)
        )
        HifzStepper(
            label = "B",
            valueText = "$endAyah",
            onMinus = { onEndChange((endAyah - 1).coerceIn(1, maxAyah)) },
            onPlus = { onEndChange((endAyah + 1).coerceIn(1, maxAyah)) },
            minusEnabled = endAyah > 1,
            plusEnabled = endAyah < maxAyah,
            onInteract = onInteract,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "of $maxAyah",
            color = GhaisNoir.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Reciter lock chip: locks to the current reciter slug, tap toggles
 * lock/unlock. A locked-but-different track only shows a warning state —
 * playback is never blocked here (enforcement belongs to the data agent).
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
            .clip(RoundedCornerShape(10.dp))
            .background(if (isLocked) GhaisNoir.Fill4 else GhaisNoir.Fill1)
            .border(
                1.dp,
                if (isLocked) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                RoundedCornerShape(10.dp)
            )
            .noirClickable {
                onInteract()
                if (isLocked) HifzReciterLock.unlock() else HifzReciterLock.lock(currentSlug)
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isLocked) "Locked" else "Lock reciter",
            color = if (isLocked) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            fontSize = 13.sp,
            fontWeight = if (isLocked) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (differs) {
            Text(
                text = "differs",
                color = GhaisNoir.TextPrimary,
                fontSize = 12.sp,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp)
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) GhaisNoir.Fill4 else GhaisNoir.Fill1)
            .noirClickable(onClick)
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
 * The single switch for the whole loop-settings card. Always mounted while
 * the chrome is up (it is the way back when the card is collapsed), so it
 * lives OUTSIDE the card's own visibility gate. Chevron shows the state:
 * "Loop settings ⌄" collapsed, "Loop settings ⌃" expanded.
 */
@Composable
fun HifzSettingsToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GhaisNoir.Fill1)
            .border(1.dp, GhaisNoir.BorderGhost, RoundedCornerShape(10.dp))
            .noirClickable(onToggle)
            .padding(vertical = 9.dp)
    ) {
        Text(
            text = "Loop settings",
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (expanded) "▴" else "▾",
            color = GhaisNoir.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
