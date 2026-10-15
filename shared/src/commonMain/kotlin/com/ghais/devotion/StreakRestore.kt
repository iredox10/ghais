package com.ghais.devotion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A7 — recoverable-break UI.
 *
 * Shown instead of a zeroed streak: "Pick up where you left off — 3 gentle
 * days restore your N-day devotion", plus a monochrome progress track
 * ([restoreProgress]) driven by [DevotionStreak.restoreDone] /
 * [DEVOTION_RESTORE_GENTLE_DAYS].
 *
 * Wiring: place below [com.ghais.devotion.StreakFlameCard] on Home and show
 * only while `DevotionStreak.isRestoring()` is true. `onBegin` should open
 * whatever starts a gentle session (player / devotional reader); the restore
 * itself advances via `DevotionStreak.recordDevotionDay()` on the same listen
 * event as the streak.
 */

/** Pure restore progress in 0..1 (clamped). */
fun restoreProgress(done: Int, total: Int = DEVOTION_RESTORE_GENTLE_DAYS): Float {
    if (total <= 0) return 1f
    return (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

/** True once enough gentle days are done to complete the restore. */
fun isRestoreComplete(done: Int, total: Int = DEVOTION_RESTORE_GENTLE_DAYS): Boolean =
    done >= total.coerceAtLeast(1)

/**
 * Gentle-day progress copy: "Gentle day 1 of 3", capped so over-completion
 * never reads as "4 of 3".
 */
fun restoreProgressText(done: Int, total: Int = DEVOTION_RESTORE_GENTLE_DAYS): String {
    val safeTotal = total.coerceAtLeast(1)
    val shown = done.coerceIn(0, safeTotal)
    return "Gentle day $shown of $safeTotal"
}

/**
 * Restore headline copy for a pre-break count of [targetDays].
 */
fun restoreHeadlineText(targetDays: Int, total: Int = DEVOTION_RESTORE_GENTLE_DAYS): String =
    "Pick up where you left off — $total gentle days restore your $targetDays-day devotion"

/**
 * Observes [DevotionStreak] and renders the restore card when a restore is
 * in progress, or nothing when it is not.
 */
@Composable
fun StreakRestoreHost(
    modifier: Modifier = Modifier,
    onBegin: () -> Unit,
) {
    val target by DevotionStreak.restoreTarget.collectAsState()
    val done by DevotionStreak.restoreDone.collectAsState()
    if (target > 0) {
        StreakRestoreCard(
            restoreTargetDays = target,
            gentleDone = done,
            onBegin = onBegin,
            modifier = modifier,
        )
    }
}

/**
 * The restore card itself (pure props — previewable and testable via the
 * pure helpers above).
 */
@Composable
fun StreakRestoreCard(
    restoreTargetDays: Int,
    gentleDone: Int,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier,
    total: Int = DEVOTION_RESTORE_GENTLE_DAYS,
) {
    NoirCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWell(
                    icon = Icons.Filled.LocalFireDepartment,
                    size = 44.dp,
                    iconSize = 22.dp,
                    contentDescription = "Restore devotion",
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = restoreHeadlineText(restoreTargetDays, total),
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your $restoreTargetDays days are safe — no starting over.",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            RestoreProgressTrack(progress = restoreProgress(gentleDone, total))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = restoreProgressText(gentleDone, total),
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                ChromePillButton(
                    text = "Begin today",
                    onClick = onBegin,
                )
            }
        }
    }
}

/** Monochrome progress track — depth from fill alpha, never hue. */
@Composable
private fun RestoreProgressTrack(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(GhaisShapes.pill)
                .background(Color.White.copy(alpha = 0.85f)),
        )
    }
}
