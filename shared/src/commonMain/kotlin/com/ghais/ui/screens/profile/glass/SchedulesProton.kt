package com.ghais.ui.screens.profile.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
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
import com.ghais.data.repository.ScheduleEngine
import com.ghais.data.repository.rememberMergedReciters
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import kotlin.time.Clock

// Sibling APIs (same package `glass`, created in parallel):
// - ScheduleTimeField(hour24, minute, onChange) + formatScheduleTime(h, m) — ScheduleTimeField.kt
// - ReciterPickerSheet(visible, selectedSlug, onSelect, onDismiss) — ReciterPickerSheet.kt
//   (opened twice in the editor — stream's reciter AND the offline qari — each
//   with its own state so the two modes never collide)
// - SurahPickerSheet(visible, title, selectedId, onSelect, onDismiss) — SurahPickerSheet.kt
// - ScheduleRowCard(schedule, reciterName, onToggle, onEdit, onDelete) — ScheduleRows.kt

@Composable
fun SchedulesProtonSection() {
    val schedules by SchedulesStore.schedules.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecitationSchedule?>(null) }
    var exactAllowed by remember { mutableStateOf(true) }
    // Re-check on entry and whenever the editor closes (user may have just
    // come back from the system grant screen).
    LaunchedEffect(showEditor) {
        exactAllowed = ScheduleEngine.canScheduleExactAlarms()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!exactAllowed) {
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconWell(icon = Icons.Filled.AlarmOn, size = 32.dp, iconSize = 17.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exact alarms are off",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Schedules may start late or not at all",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .background(GhaisNoir.Fill2, CircleShape)
                            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                            .noirClickable { ScheduleEngine.openExactAlarmSettings() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Allow",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
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
                        // Offline rows resolve the saved slug into the qari's
                        // display name; "Offline downloads" stays only as the
                        // fallback for legacy schedules whose slug doesn't
                        // resolve to a reciter.
                        val reciterName = if (schedule.isOffline) {
                            runCatching {
                                QuranDataRepository.getReciterBySlug(schedule.reciterSlug).nameEn
                            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Offline downloads"
                        } else {
                            runCatching {
                                QuranDataRepository.getReciterBySlug(schedule.reciterSlug).nameEn
                            }.getOrNull()?.takeIf { it.isNotBlank() } ?: schedule.reciterSlug
                        }
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
                        Text(
                            text = if (schedule.autoStart) "Auto-starts" else "Reminder only",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 14.dp, top = 3.dp, bottom = 4.dp)
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
                // On Android 14+ exact alarms start denied; guide the user to
                // the grant screen right after saving so the schedule fires.
                if (!ScheduleEngine.canScheduleExactAlarms()) {
                    ScheduleEngine.openExactAlarmSettings()
                }
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
        mutableStateOf(
            initial?.reciterSlug
                ?: reciters
                    // Default range is surahs 1..5 — pick the first reciter who
                    // actually recorded them, or the alarm would fire with zero
                    // playable tracks (e.g. al-hussaini-al-azzazi has 58..114 only).
                    .firstOrNull { r -> (1..5).any { sid -> r.isSurahAvailable(sid) } }
                    ?.slug
                ?: reciters.firstOrNull()?.slug.orEmpty()
        )
    }
    var fromSurah by remember(initial) { mutableStateOf(initial?.fromSurah?.coerceIn(1, 114) ?: 1) }
    var toSurah by remember(initial) { mutableStateOf(initial?.toSurah?.coerceIn(1, 114) ?: 5) }
    var useMinutes by remember(initial) { mutableStateOf(initial?.durationMin != null) }
    var minutes by remember(initial) { mutableStateOf(initial?.durationMin?.coerceIn(5, 180) ?: 30) }
    var autoStart by remember(initial) { mutableStateOf(initial?.autoStart ?: true) }
    var showReciterSheet by remember { mutableStateOf(false) }
    // Offline's own picker state — a second ReciterPickerSheet instance that
    // never collides with the stream picker's.
    var showOfflineReciterSheet by remember { mutableStateOf(false) }
    var showFromSheet by remember { mutableStateOf(false) }
    var showToSheet by remember { mutableStateOf(false) }
    // Audio source: streamed reciter range, or the user's offline shelf.
    // Offline now mirrors the stream UX — pick ONE qari first (its own slug,
    // so the two modes never fight over `reciterSlug`), then that qari's
    // downloaded surahs become the pick list.
    var useOffline by remember(initial) { mutableStateOf(initial?.isOffline == true) }
    val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
    // Full-surah download keys only (clips can't anchor a schedule).
    val offlineOptions = remember(downloadedKeys) {
        downloadedKeys.filter { it.split("/").size == 2 }.sorted()
    }
    // Offline qaris (slug → keys), groups ordered by reciter display name —
    // that ordering also picks the default qari for a fresh offline schedule.
    val offlineGroups = remember(offlineOptions) {
        offlineOptions.groupBy { it.substringBefore("/") }.toList()
            .sortedBy { (slug, _) -> offlineReciterName(slug).lowercase() }
    }
    // Offline mode's own qari: the edited schedule's slug when that slug still
    // has downloads, else the first qari per display-name order, else "" on an
    // empty shelf. Distinct from `reciterSlug`, which is stream-only.
    var offlineSlug by remember(initial) {
        mutableStateOf(
            initial?.reciterSlug
                ?.takeIf { slug ->
                    initial?.isOffline == true &&
                        offlineOptions.any { k -> k.substringBefore("/") == slug }
                }
                ?: offlineGroups.firstOrNull()?.first
                ?: ""
        )
    }
    // Saved selection, or everything (the pre-select behaviour) when creating
    // new or editing an "all downloads" schedule.
    var selectedKeys by remember(initial) {
        mutableStateOf(
            initial?.offlineKeys?.takeIf { it.isNotEmpty() }?.toSet()
                ?: downloadedKeys.filter { it.split("/").size == 2 }.toSet()
        )
    }
    // Effective selection: deleted-since files drop out, and the rest is
    // scoped to the chosen qari — legacy cross-qari keys neither display,
    // count, nor survive a save.
    val effectiveSelection = remember(selectedKeys, downloadedKeys, offlineSlug) {
        selectedKeys.intersect(downloadedKeys.toSet())
            .filter { it.substringBefore("/") == offlineSlug }
            .toSet()
    }
    // Only the chosen qari's downloads — the offline pick list is one group.
    val offlineQariKeys = remember(offlineOptions, offlineSlug) {
        offlineOptions.filter { it.substringBefore("/") == offlineSlug }
    }
    val offlineSurahCount = offlineOptions.size

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
    // Offline mode ignores the range but is anchored to a qari — it needs ≥1
    // of THAT qari's downloaded surahs.
    val rangeValid = useMinutes || fromSurah <= toSurah
    // Availability: the reciter must have audio for at least one surah of the
    // effective range, else the alarm fires and ScheduleTracks builds 0 tracks.
    val availableIds = remember(reciterSlug) {
        runCatching { QuranDataRepository.getReciterBySlug(reciterSlug).getAvailableSurahIds() }
            .getOrNull()
    }
    val previewFrom = fromSurah.coerceIn(1, 114)
    val previewTo = if (useMinutes) 114 else toSurah.coerceIn(1, 114).coerceAtLeast(previewFrom)
    val overlapMissing = availableIds != null && (previewFrom..previewTo).none { it in availableIds }
    val scheduleValid = if (useOffline) effectiveSelection.isNotEmpty() else rangeValid && !overlapMissing

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
                // Scrollable: on short screens with many downloaded surahs
                // listed (offline mode) or all stream fields visible, the
                // Save/Cancel row used to fall off-screen with no way to
                // reach it — the most confusing tile of the app.
                .verticalScroll(rememberScrollState())
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

            // Audio source: streamed reciter range, or the offline shelf.
            // Offline plays downloads the alarm already has on disk — one
            // picked qari's surahs, no network needed when it fires.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Play from offline downloads",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    NoirSwitch(
                        checked = useOffline,
                        onCheckedChange = { useOffline = it }
                    )
                }
                Text(
                    text = if (useOffline) {
                        val n = effectiveSelection.size
                        when {
                            // The chosen qari has nothing downloaded yet — the
                            // pick list below stays hidden.
                            offlineQariKeys.isEmpty() ->
                                "No downloads for this qari — download a surah first"
                            n == 0 -> "Uncheck: pick at least one download below"
                            n == 1 -> "Plays your 1 picked surah"
                            else -> "Plays your $n picked surahs in order"
                        }
                    } else "Stream a reciter's range as usual",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp
                )
            }

            // Offline mode: pick ONE qari first — the same reciter-row UX as
            // stream mode — then that qari's downloads are the pick list. The
            // old all-qaris multi-group dump is gone.
            if (useOffline) {
                val qariName = remember(offlineSlug) { offlineReciterName(offlineSlug) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Reciter", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                    ReciterSelectorRow(
                        value = qariName,
                        onClick = { showOfflineReciterSheet = true }
                    )
                }

                // Only the chosen qari's downloads. Hidden entirely when the
                // qari has nothing on disk — the sub-line under the switch
                // already explains that state.
                if (offlineQariKeys.isNotEmpty()) {
                    val pickedInQari = offlineQariKeys.count { it in effectiveSelection }
                    val allPicked = pickedInQari == offlineQariKeys.size
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Downloads to play", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Select-all header: name + x/y picked, tap picks
                            // or clears the whole qari.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        GhaisNoir.Fill4,
                                        com.ghais.ui.theme.GhaisShapes.cardNoir
                                    )
                                    .border(
                                        1.dp,
                                        GhaisNoir.BorderCard,
                                        com.ghais.ui.theme.GhaisShapes.cardNoir
                                    )
                                    .noirClickable {
                                        selectedKeys = if (allPicked) {
                                            selectedKeys - offlineQariKeys.toSet()
                                        } else {
                                            selectedKeys + offlineQariKeys.toSet()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (allPicked) GhaisNoir.Fill2 else GhaisNoir.Fill4,
                                            CircleShape
                                        )
                                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (allPicked) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(GhaisNoir.TextPrimary, CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = qariName,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$pickedInQari/${offlineQariKeys.size}",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                            // That qari's downloaded surahs.
                            offlineQariKeys.forEach { key ->
                                val (title, _) = remember(key) { offlineKeyLabel(key) }
                                val checked = key in effectiveSelection
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp)
                                        .background(
                                            GhaisNoir.Fill2,
                                            com.ghais.ui.theme.GhaisShapes.cardNoir
                                        )
                                        .border(
                                            1.dp,
                                            GhaisNoir.BorderCard,
                                            com.ghais.ui.theme.GhaisShapes.cardNoir
                                        )
                                        .noirClickable {
                                            selectedKeys = if (key in selectedKeys) {
                                                selectedKeys - key
                                            } else {
                                                selectedKeys + key
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                if (checked) GhaisNoir.Fill4 else GhaisNoir.Fill2,
                                                CircleShape
                                            )
                                            .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (checked) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .background(GhaisNoir.TextPrimary, CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = title,
                                        color = if (checked) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!useOffline) {
            // Reciter selector row → modal.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Reciter", color = GhaisNoir.TextSecondary, fontSize = 12.sp)
                // Extracted so the offline mode renders the exact same row.
                ReciterSelectorRow(value = reciterName, onClick = { showReciterSheet = true })
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
                if (rangeValid && overlapMissing) {
                    val recordedSpan = availableIds?.let { ids ->
                        val lo = ids.minOrNull()
                        val hi = ids.maxOrNull()
                        if (lo != null && hi != null && ids.size < 114) {
                            " He recorded surahs $lo–$hi."
                        } else ""
                    }.orEmpty()
                    Text(
                        text = "“$reciterName” has no audio for surahs $previewFrom–$previewTo." +
                            recordedSpan,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            } // end if (!useOffline): reciter + range pickers are stream-only.

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

            // Auto-start toggle — saved through the same Save path as duration/enabled.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Start playback automatically",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    NoirSwitch(
                        checked = autoStart,
                        onCheckedChange = { autoStart = it }
                    )
                }
                Text(
                    text = if (autoStart) "Alarm starts playback on its own"
                    else "Off — you only get a reminder notification",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp
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
                        if (scheduleValid) {
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
                                        // Offline schedules carry the OFFLINE
                                        // qari's slug — `reciterSlug` is
                                        // stream-only and stays untouched.
                                        reciterSlug = if (useOffline) offlineSlug else reciterSlug,
                                        fromSurah = f,
                                        toSurah = t,
                                        durationMin = if (useMinutes) minutes.coerceIn(5, 180) else null,
                                        autoStart = autoStart,
                                        enabled = initial?.enabled ?: true,
                                        source = if (useOffline) {
                                            RecitationSchedule.SOURCE_OFFLINE
                                        } else {
                                            RecitationSchedule.SOURCE_STREAM
                                        },
                                        offlineKeys = if (useOffline) {
                                            // effectiveSelection is already
                                            // scoped to offlineSlug: only keys
                                            // of the picked qari survive a save
                                            // — legacy cross-qari keys drop.
                                            effectiveSelection.sorted()
                                        } else {
                                            initial?.offlineKeys ?: emptyList()
                                        }
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
                    color = if (scheduleValid) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
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

    // Offline mode's own picker instance (own state, so the two pickers never
    // collide). Picking a qari resets the selection to that qari's FULL
    // download set — a sane starting point, not a leftover scoped to the
    // previously chosen qari. The list is restricted to qaris that actually
    // have downloads: the shelf is the whole picker's universe here.
    ReciterPickerSheet(
        visible = showOfflineReciterSheet,
        selectedSlug = offlineSlug,
        restrictToSlugs = remember(offlineOptions) {
            offlineOptions.map { it.substringBefore("/") }.toSet()
        },
        onSelect = { slug ->
            offlineSlug = slug
            selectedKeys = offlineOptions.filter { it.substringBefore("/") == slug }.toSet()
            showOfflineReciterSheet = false
        },
        onDismiss = { showOfflineReciterSheet = false }
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
 * Reciter selector row — the exact chrome BOTH stream and offline modes render
 * (label comes from the caller): letter avatar + name + chevron. Extracted so
 * the offline qari row is byte-for-byte the same style as the stream one.
 */
@Composable
private fun ReciterSelectorRow(
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
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(GhaisNoir.Fill4, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.firstOrNull()?.uppercase() ?: "?",
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
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

/**
 * Reciter display name for a download slug, for the schedule pick-list group
 * headers. Falls back to the raw slug; never throws.
 */
private fun offlineReciterName(slug: String): String {
    return try {
        runCatching {
            QuranDataRepository.getBrowseReciterBySlug(slug)
                ?: QuranDataRepository.getReciters().firstOrNull { it.slug == slug }
        }.getOrNull()?.nameEn?.takeIf { it.isNotBlank() }
            ?: slug.ifBlank { "Unknown reciter" }
    } catch (_: Exception) {
        slug.ifBlank { "Unknown reciter" }
    }
}

/**
 * Display label for a download key (`"<slug>/<surahId>"`): surah title +
 * reciter name for the schedule pick list. Falls back to raw ids; never throws.
 */private fun offlineKeyLabel(key: String): Pair<String, String> {
    return try {
        val parts = key.split("/")
        val slug = parts.getOrNull(0).orEmpty()
        val surahId = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val surah = QuranDataRepository.getSurahById(surahId)
        val title = surah?.nameEn?.ifBlank { null }
            ?: if (surahId > 0) "Surah $surahId" else key
        val reciter = runCatching {
            QuranDataRepository.getBrowseReciterBySlug(slug)
                ?: QuranDataRepository.getReciters().firstOrNull { it.slug == slug }
        }.getOrNull()
        val subtitle = reciter?.nameEn?.takeIf { it.isNotBlank() }
            ?: slug.ifBlank { "Unknown reciter" }
        title to subtitle
    } catch (_: Exception) {
        key to ""
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
