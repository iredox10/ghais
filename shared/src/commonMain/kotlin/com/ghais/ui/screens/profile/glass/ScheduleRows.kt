package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Calm, low-distraction schedule row for the profile Schedules section.
 *
 * NoirListRow recipe (cardFillSoft + hairline + top specular) with:
 * big 12h time + small AM/PM caps, one quiet subtitle line
 * (reciter • range [• N min]), then a whisper row:
 * NoirSwitch + ghost Edit pill + ghost delete well.
 *
 * Strictly presentational — all actions flow out via callbacks.
 * Time text comes from [formatScheduleTime] (glass/ScheduleTimeField.kt,
 * "05:30 AM" format); split here into big time + caps meridiem.
 */
@Composable
fun ScheduleRowCard(
    schedule: RecitationSchedule,
    reciterName: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Resolve surah names inside (QuranDataRepository.getSurahById).
    val fromName = remember(schedule.fromSurah) {
        QuranDataRepository.getSurahById(schedule.fromSurah)?.nameEn
            ?: "Surah ${schedule.fromSurah}"
    }
    val toName = remember(schedule.toSurah) {
        QuranDataRepository.getSurahById(schedule.toSurah)?.nameEn
            ?: "Surah ${schedule.toSurah}"
    }

    // "05:30 AM" -> big "05:30" + caps "AM".
    val formatted = remember(schedule.hour, schedule.minute) {
        formatScheduleTime(schedule.hour, schedule.minute)
    }
    val timePart = formatted.substringBeforeLast(" ", formatted).trim()
    val meridiemPart = if (" " in formatted) {
        formatted.substringAfterLast(" ", "").trim()
    } else {
        ""
    }

    val range = if (schedule.fromSurah == schedule.toSurah) fromName else "$fromName → $toName"
    val subtitle = buildString {
        append(reciterName)
        append(" • ")
        append(range)
        if (schedule.durationMin != null) {
            append(" • ")
            append(schedule.durationMin)
            append(" min")
        }
    }

    // Quiet read when off: dim the time, keep everything monochrome.
    val timeColor = if (schedule.enabled) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
    val subtitleColor = if (schedule.enabled) GhaisNoir.TextSecondary else GhaisNoir.TextTertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Big 12h time + small AM/PM caps, baseline aligned.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = timePart,
                color = timeColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (meridiemPart.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = meridiemPart.uppercase(),
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = subtitleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        // Whisper row: switch + ghost Edit pill + ghost delete well.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirSwitch(
                checked = schedule.enabled,
                onCheckedChange = onToggle
            )
            Spacer(Modifier.weight(1f))
            GhostEditPill(onClick = onEdit)
            Spacer(Modifier.width(8.dp))
            GhostDeleteWell(onClick = onDelete)
        }
    }
}

/** Ghost Edit pill — quiet bordered pill, no chrome. */
@Composable
private fun GhostEditPill(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Edit",
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** Ghost delete well — circular hairline well with a muted trash glyph. */
@Composable
private fun GhostDeleteWell(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = "Delete schedule",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}
