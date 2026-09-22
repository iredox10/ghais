package com.ghais.ui.screens.profile.glass

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Schedule clock field — replaces +/- steppers with a real clock picker + AM/PM.
 *
 * Model stays 24h ([com.ghais.data.repository.RecitationSchedule.hour] 0..23);
 * this component converts 24h <-> 12h + period internally and writes back 24h.
 */

/** "05:30 AM" — 12h, zero-padded, AM/PM caps. */
fun formatScheduleTime(hour24: Int, minute: Int): String {
    val h = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val period = if (h < 12) "AM" else "PM"
    val h12 = when (val r = h % 12) {
        0 -> 12
        else -> r
    }
    return h12.toString().padStart(2, '0') + ":" + m.toString().padStart(2, '0') + " " + period
}

/** "5:30 AM" — compact row form, hour not padded. */
fun formatScheduleTimeShort(hour24: Int, minute: Int): String {
    val h = hour24.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val period = if (h < 12) "AM" else "PM"
    val h12 = when (val r = h % 12) {
        0 -> 12
        else -> r
    }
    return "$h12:${m.toString().padStart(2, '0')} $period"
}

private fun hour12Padded(hour24: Int): String {
    val h12 = when (val r = hour24.coerceIn(0, 23) % 12) {
        0 -> 12
        else -> r
    }
    return h12.toString().padStart(2, '0')
}

private fun periodOf(hour24: Int): String =
    if (hour24.coerceIn(0, 23) < 12) "AM" else "PM"

/**
 * Noir row showing big "05:30" + "AM" pill; tap opens a Material3 [TimePicker]
 * dialog (12-hour clock face) + AM/PM selector. Save writes back converted 24h.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimeField(
    hour24: Int,
    minute: Int,
    onChange: (hour24: Int, minute: Int) -> Unit
) {
    val safeHour = hour24.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    // Re-derive display parts whenever the 24h model params change.
    val (clockText, period) = remember(safeHour, safeMinute) {
        hour12Padded(safeHour) + ":" + safeMinute.toString().padStart(2, '0') to periodOf(safeHour)
    }

    var showDialog by remember { mutableStateOf(false) }

    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = clockText,
                color = GhaisNoir.TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDialog) {
        // Re-create picker state from the latest 24h model each time params change.
        key(safeHour, safeMinute) {
            val clockState = rememberTimePickerState(
                initialHour = safeHour,
                initialMinute = safeMinute,
                is24Hour = false
            )
            Dialog(onDismissRequest = { showDialog = false }) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(GhaisNoir.CanvasTop)
                        .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(28.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = clockState)
                        Spacer(modifier = Modifier.height(12.dp))
                        // Explicit AM/PM selector (zero hue) — mutates picker hour.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isAm = clockState.hour < 12
                            GhostPillButton(
                                text = "AM",
                                active = isAm,
                                onClick = {
                                    if (!isAm) clockState.hour = (clockState.hour - 12).coerceIn(0, 23)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            GhostPillButton(
                                text = "PM",
                                active = !isAm,
                                onClick = {
                                    if (isAm) clockState.hour = (clockState.hour + 12).coerceIn(0, 23)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GhostPillButton(
                                text = "Cancel",
                                onClick = { showDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                            ChromePillButton(
                                text = "Set",
                                onClick = {
                                    onChange(
                                        clockState.hour.coerceIn(0, 23),
                                        clockState.minute.coerceIn(0, 59)
                                    )
                                    showDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
