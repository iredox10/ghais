package com.ghais.devotion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.share.ShareSheet
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A7 — streak flame Home card.
 *
 * Noir glass card reading [DevotionStreak]:
 * - flame + "N days with the Quran" (positive devotion framing);
 * - grace-day indicator ("X grace days — your N days are safe", never guilt);
 * - [StreakMilestoneDialog] appears once per milestone (7/30/100/365) with a
 *   Share button backed by [devotionMilestoneShareText].
 *
 * Wiring: see the insertion snippet in the A7 report — this card goes in
 * `HomeScreen` above "Your stats", and `DevotionStreak.recordDevotionDay()`
 * should be called from the playback listen event
 * (`UserUsageRepository`'s play path / `AudioEngine.isPlaying` collector).
 */
@Composable
fun StreakFlameCard(
    modifier: Modifier = Modifier,
    onShare: ((String) -> Unit)? = null,
) {
    val days by DevotionStreak.devotionDays.collectAsState()
    val grace by DevotionStreak.graceDays.collectAsState()
    // Subscribed so celebrating a milestone recomposes the dialog away.
    val celebrated by DevotionStreak.celebratedMilestones.collectAsState()
    val pending = DevotionStreak.pendingMilestone()?.takeIf { it !in celebrated }

    NoirCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconWell(
                icon = Icons.Filled.LocalFireDepartment,
                size = 52.dp,
                iconSize = 26.dp,
                contentDescription = "Devotion streak",
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (days == 1) "1 day with the Quran" else "$days days with the Quran",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = graceIndicatorText(days = days, grace = grace),
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }

    if (pending != null) {
        StreakMilestoneDialog(
            days = pending,
            onDismiss = { DevotionStreak.markMilestoneCelebrated(pending) },
            onShare = onShare,
        )
    }
}

/**
 * Grace-day indicator copy. Always reassuring: banked grace days mean
 * "your N days are safe" — never freeze/guilt language.
 */
fun graceIndicatorText(days: Int, grace: Int): String = when {
    grace > 0 && days > 0 -> {
        val noun = if (grace == 1) "grace day" else "grace days"
        "$grace $noun — your $days days are safe"
    }
    grace > 0 -> {
        val noun = if (grace == 1) "grace day" else "grace days"
        "$grace $noun banked for busy days"
    }
    days > 0 -> "Every day counts — keep it gentle"
    else -> "Start your gentle daily devotion"
}

/**
 * Plain-text share card for a devotion milestone. Shared via
 * [ShareSheet.shareText] (common call; the iOS stub no-ops safely).
 */
fun devotionMilestoneShareText(days: Int): String = buildString {
    appendLine("$days days with the Quran")
    appendLine()
    appendLine("A gentle daily devotion — one day, one verse, one moment at a time.")
    appendLine()
    append("Reflect & listen with Ghaith")
}

/**
 * Milestone celebration dialog (7/30/100/365): chrome Share pill + ghost
 * Keep-going pill, in the Noir dialog language of `SurahCeremonyDialog`.
 */
@Composable
fun StreakMilestoneDialog(
    days: Int,
    onDismiss: () -> Unit,
    onShare: ((String) -> Unit)? = null,
) {
    val cardText = devotionMilestoneShareText(days)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ChromePillButton(
                text = "Share",
                onClick = {
                    if (onShare != null) {
                        onShare(cardText)
                    } else {
                        ShareSheet.shareText(
                            title = "Ghaith — devotion milestone",
                            text = cardText,
                        )
                    }
                },
            )
        },
        dismissButton = {
            GhostPillButton(
                text = "Keep going",
                onClick = onDismiss,
            )
        },
        title = {
            Text(
                text = "$days days with the Quran",
                color = GhaisNoir.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A gentle devotion, kept one day at a time.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Share the milestone, or simply keep going.",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        },
        shape = GhaisShapes.cardNoir,
        containerColor = GhaisNoir.CanvasTop,
        titleContentColor = GhaisNoir.TextPrimary,
        textContentColor = GhaisNoir.TextSecondary,
    )
}
