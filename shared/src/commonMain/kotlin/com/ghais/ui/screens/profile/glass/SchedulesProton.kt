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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import kotlin.time.Clock

// Self-contained NOIR monochrome palette (GhaisNoir tokens).
private val SchedDialogBg = GhaisNoir.NoirBlack

@Composable
fun SchedulesProtonSection() {
    val schedules by SchedulesStore.schedules.collectAsState()
    val reciters = remember { QuranDataRepository.getReciters() }
    val surahs = remember { QuranDataRepository.getSurahs() }
    val surahName = remember(surahs) { surahs.associate { it.id to it.nameEn } }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecitationSchedule?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Group header inside the card.
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconWell(icon = Icons.Filled.AlarmOn, size = 32.dp, iconSize = 17.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Recitation schedules",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .background(GhaisNoir.Fill2, CircleShape)
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                            .noirClickable {
                                editing = null
                                showEditor = true
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = GhaisNoir.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (schedules.isEmpty()) {
                    Text(
                        text = "No schedules — add one to wake up to Quran",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                } else {
                    schedules.forEachIndexed { index, schedule ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = GhaisNoir.BorderGhost,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        val reciterName = remember(schedule.reciterSlug, reciters) {
                            runCatching { QuranDataRepository.getReciterBySlug(schedule.reciterSlug).nameEn }
                                .getOrNull() ?: schedule.reciterSlug
                        }
                        val fromName = surahName[schedule.fromSurah] ?: "Surah ${schedule.fromSurah}"
                        val toName = surahName[schedule.toSurah] ?: "Surah ${schedule.toSurah}"
                        val range =
                            if (schedule.fromSurah == schedule.toSurah) fromName else "$fromName – $toName"
                        val durationSuffix = schedule.durationMin?.let { " • $it min" } ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "%02d:%02d".format(schedule.hour, schedule.minute),
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "$reciterName • $range$durationSuffix",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            NoirSwitch(
                                checked = schedule.enabled,
                                onCheckedChange = { on ->
                                    SchedulesStore.update(schedule.copy(enabled = on))
                                }
                            )
                            TextButton(
                                onClick = {
                                    editing = schedule
                                    showEditor = true
                                }
                            ) {
                                Text(text = "Edit", color = GhaisNoir.TextPrimary, fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { SchedulesStore.remove(schedule.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete schedule",
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }

    if (showEditor) {
        SchedEditorDialog(
            initial = editing,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = { built ->
                if (editing == null) SchedulesStore.add(built) else SchedulesStore.update(built)
                showEditor = false
                editing = null
            }
        )
    }
}

@Composable
private fun SchedEditorDialog(
    initial: RecitationSchedule?,
    onDismiss: () -> Unit,
    onSave: (RecitationSchedule) -> Unit
) {
    val reciters = remember { QuranDataRepository.getReciters() }
    var hour by remember(initial) { mutableStateOf(initial?.hour ?: 5) }
    var minute by remember(initial) { mutableStateOf(initial?.minute ?: 30) }
    var reciterSlug by remember(initial) {
        mutableStateOf(initial?.reciterSlug ?: reciters.firstOrNull()?.slug.orEmpty())
    }
    var fromSurah by remember(initial) { mutableStateOf(initial?.fromSurah ?: 1) }
    var toSurah by remember(initial) { mutableStateOf(initial?.toSurah ?: 5) }
    var useMinutes by remember(initial) { mutableStateOf(initial?.durationMin != null) }
    var minutes by remember(initial) { mutableStateOf(initial?.durationMin ?: 30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val f = fromSurah.coerceIn(1, 114)
                    var t = if (useMinutes) 114 else toSurah.coerceIn(1, 114)
                    if (!useMinutes && f > t) t = f
                    onSave(
                        RecitationSchedule(
                            id = initial?.id
                                ?: ("sch-" + Clock.System.now().toEpochMilliseconds()),
                            hour = hour.coerceIn(0, 23),
                            minute = minute.coerceIn(0, 59),
                            reciterSlug = reciterSlug,
                            fromSurah = f,
                            toSurah = t,
                            durationMin = if (useMinutes) minutes.coerceIn(5, 180) else null,
                            enabled = initial?.enabled ?: true
                        )
                    )
                }
            ) {
                Text(text = "Save", color = GhaisNoir.TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = GhaisNoir.TextSecondary)
            }
        },
        title = {
            Text(
                text = if (initial == null) "New schedule" else "Edit schedule",
                color = GhaisNoir.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SchedStepperRow(
                    label = "Hour",
                    valueText = "%02d".format(hour),
                    onMinus = { hour = (hour - 1).mod(24) },
                    onPlus = { hour = (hour + 1).mod(24) }
                )
                SchedStepperRow(
                    label = "Minute",
                    valueText = "%02d".format(minute),
                    onMinus = { minute = (minute - 1).mod(60) },
                    onPlus = { minute = (minute + 1).mod(60) }
                )
                Text(text = "Reciter", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reciters, key = { it.slug }) { reciter ->
                        val selected = reciter.slug == reciterSlug
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) GhaisNoir.Fill4 else GhaisNoir.Fill2,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    CircleShape
                                )
                                .noirClickable { reciterSlug = reciter.slug }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = reciter.nameEn,
                                color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
                SchedStepperRow(
                    label = "From surah",
                    valueText = fromSurah.toString(),
                    onMinus = { fromSurah = (fromSurah - 1).coerceIn(1, 114) },
                    onPlus = { fromSurah = (fromSurah + 1).coerceIn(1, 114) }
                )
                if (!useMinutes) {
                    SchedStepperRow(
                        label = "To surah",
                        valueText = toSurah.toString(),
                        onMinus = { toSurah = (toSurah - 1).coerceIn(1, 114) },
                        onPlus = { toSurah = (toSurah + 1).coerceIn(1, 114) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Limit duration",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    NoirSwitch(
                        checked = useMinutes,
                        onCheckedChange = { useMinutes = it }
                    )
                }
                if (useMinutes) {
                    SchedStepperRow(
                        label = "Minutes",
                        valueText = minutes.toString(),
                        onMinus = { minutes = (minutes - 5).coerceIn(5, 180) },
                        onPlus = { minutes = (minutes + 5).coerceIn(5, 180) }
                    )
                }
            }
        },
        containerColor = SchedDialogBg,
        shape = com.ghais.ui.theme.GhaisShapes.cardNoir
    )
}

@Composable
private fun SchedStepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = GhaisNoir.TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        SchedStepButton(text = "−", onClick = onMinus)
        Spacer(Modifier.width(10.dp))
        Text(
            text = valueText,
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )
        Spacer(Modifier.width(10.dp))
        SchedStepButton(text = "+", onClick = onPlus)
    }
}

@Composable
private fun SchedStepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(GhaisNoir.Fill2, CircleShape)
            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = GhaisNoir.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
