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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.rememberMergedReciters
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import kotlin.time.Clock

// Sibling APIs (same package `glass`, created in parallel):
// - ScheduleTimeField(hour24, minute, onChange) + formatScheduleTime(h, m) — ScheduleTimeField.kt
// - ReciterPickerSheet(visible, selectedSlug, onSelect, onDismiss) — ReciterPickerSheet.kt
// - SurahPickerSheet(visible, title, selectedId, onSelect, onDismiss) — SurahPickerSheet.kt
// - ScheduleRowCard(schedule, reciterName, onToggle, onEdit, onDelete) — ScheduleRows.kt

@Composable
fun SchedulesProtonSection() {
    val schedules by SchedulesStore.schedules.collectAsState()
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
                        val reciterName = runCatching {
                            QuranDataRepository.getReciterBySlug(schedule.reciterSlug).nameEn
                        }.getOrNull()?.takeIf { it.isNotBlank() } ?: schedule.reciterSlug
                        ScheduleRowCard(
                            schedule = schedule,
                            reciterName = reciterName,
                            onToggle = { on ->
                                SchedulesStore.update(schedule.copy(enabled = on))
                            },
                            onEdit = {
                                editing = schedule
                                showEditor = true
                            },
                            onDelete = { SchedulesStore.remove(schedule.id) }
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }

    if (showEditor) {
        SchedEditorSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedEditorSheet(
    initial: RecitationSchedule?,
    onDismiss: () -> Unit,
    onSave: (RecitationSchedule) -> Unit
) {
    val reciters = rememberMergedReciters()
    var hour by remember(initial) { mutableStateOf(initial?.hour?.coerceIn(0, 23) ?: 5) }
    var minute by remember(initial) { mutableStateOf(initial?.minute?.coerceIn(0, 59) ?: 30) }
    var reciterSlug by remember(initial) {
        mutableStateOf(initial?.reciterSlug ?: reciters.firstOrNull()?.slug.orEmpty())
    }
    var fromSurah by remember(initial) { mutableStateOf(initial?.fromSurah?.coerceIn(1, 114) ?: 1) }
    var toSurah by remember(initial) { mutableStateOf(initial?.toSurah?.coerceIn(1, 114) ?: 5) }
    var useMinutes by remember(initial) { mutableStateOf(initial?.durationMin != null) }
    var minutes by remember(initial) { mutableStateOf(initial?.durationMin?.coerceIn(5, 180) ?: 30) }
    var showReciterSheet by remember { mutableStateOf(false) }
    var showFromSheet by remember { mutableStateOf(false) }
    var showToSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val reciterName = remember(reciterSlug) {
        runCatching { QuranDataRepository.getReciterBySlug(reciterSlug).nameEn }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: reciterSlug.ifBlank { "Choose reciter" }
    }
    val fromName = remember(fromSurah) {
        QuranDataRepository.getSurahById(fromSurah)?.nameEn ?: "Surah $fromSurah"
    }
    val toName = remember(toSurah) {
        QuranDataRepository.getSurahById(toSurah)?.nameEn ?: "Surah $toSurah"
    }

    // Validation: range mode requires from <= to; duration mode forces toSurah = 114 on save.
    val rangeValid = useMinutes || fromSurah <= toSurah

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GhaisNoir.NoirBlack,
        contentColor = GhaisNoir.TextPrimary,
        shape = com.ghais.ui.theme.GhaisShapes.cardNoir
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initial == null) "New schedule" else "Edit schedule",
                color = GhaisNoir.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            // Clock time — calm field owned by ScheduleTimeField.kt (clock time + AM/PM).
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Time", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                ScheduleTimeField(
                    hour24 = hour,
                    minute = minute,
                    onChange = { h, m ->
                        hour = h.coerceIn(0, 23)
                        minute = m.coerceIn(0, 59)
                    }
                )
                Text(
                    text = "Rings at ${formatScheduleTime(hour, minute)}",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp
                )
            }

            // Reciter selector row → modal.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Reciter", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GhaisNoir.Fill2, com.ghais.ui.theme.GhaisShapes.cardNoir)
                        .border(
                            1.dp,
                            GhaisNoir.BorderCard,
                            com.ghais.ui.theme.GhaisShapes.cardNoir
                        )
                        .noirClickable { showReciterSheet = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(GhaisNoir.Fill4, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reciterName.firstOrNull()?.uppercase() ?: "?",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = reciterName,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = GhaisNoir.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // From / to surah selector rows → modals.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Range", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                SurahSelectorRow(
                    label = "From surah",
                    value = fromName,
                    onClick = { showFromSheet = true }
                )
                if (!useMinutes) {
                    SurahSelectorRow(
                        label = "To surah",
                        value = toName,
                        onClick = { showToSheet = true }
                    )
                }
                if (!rangeValid) {
                    Text(
                        text = "“From” must come before “To” — pick a later ending surah.",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Duration toggle + minutes (simple stepper scoped to duration only).
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
                DurationMinutesStepper(
                    value = minutes,
                    onMinus = { minutes = (minutes - 5).coerceIn(5, 180) },
                    onPlus = { minutes = (minutes + 5).coerceIn(5, 180) }
                )
            }

            // Save / Cancel.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (rangeValid) GhaisNoir.Fill4 else GhaisNoir.Fill2,
                        CircleShape
                    )
                    .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                    .then(
                        if (rangeValid) {
                            Modifier.noirClickable {
                                val h = hour.coerceIn(0, 23)
                                val m = minute.coerceIn(0, 59)
                                val f = fromSurah.coerceIn(1, 114)
                                val t = if (useMinutes) 114 else toSurah.coerceIn(1, 114).coerceAtLeast(f)
                                onSave(
                                    RecitationSchedule(
                                        id = initial?.id
                                            ?: ("sch-" + Clock.System.now().toEpochMilliseconds()),
                                        hour = h,
                                        minute = m,
                                        reciterSlug = reciterSlug,
                                        fromSurah = f,
                                        toSurah = t,
                                        durationMin = if (useMinutes) minutes.coerceIn(5, 180) else null,
                                        enabled = initial?.enabled ?: true
                                    )
                                )
                            }
                        } else Modifier
                    )
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save",
                    color = if (rangeValid) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .noirClickable(onClick = onDismiss)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Cancel", color = GhaisNoir.TextSecondary, fontSize = 13.sp)
            }
        }
    }

    ReciterPickerSheet(
        visible = showReciterSheet,
        selectedSlug = reciterSlug,
        onSelect = { slug ->
            reciterSlug = slug
            showReciterSheet = false
        },
        onDismiss = { showReciterSheet = false }
    )
    SurahPickerSheet(
        visible = showFromSheet,
        title = "From surah",
        selectedId = fromSurah,
        onSelect = { id ->
            fromSurah = id.coerceIn(1, 114)
            showFromSheet = false
        },
        onDismiss = { showFromSheet = false }
    )
    if (!useMinutes) {
        SurahPickerSheet(
            visible = showToSheet,
            title = "To surah",
            selectedId = toSurah,
            onSelect = { id ->
                toSurah = id.coerceIn(1, 114)
                showToSheet = false
            },
            onDismiss = { showToSheet = false }
        )
    }
}

@Composable
private fun SurahSelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhaisNoir.Fill2, com.ghais.ui.theme.GhaisShapes.cardNoir)
            .border(1.dp, GhaisNoir.BorderCard, com.ghais.ui.theme.GhaisShapes.cardNoir)
            .noirClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Simple stepper kept ONLY for duration minutes (time + surahs use modal/clock pickers).
 * Noir tokens only.
 */
@Composable
private fun DurationMinutesStepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Minutes",
            color = GhaisNoir.TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(GhaisNoir.Fill2, CircleShape)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onMinus),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "−", color = GhaisNoir.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = value.toString(),
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(GhaisNoir.Fill2, CircleShape)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", color = GhaisNoir.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
