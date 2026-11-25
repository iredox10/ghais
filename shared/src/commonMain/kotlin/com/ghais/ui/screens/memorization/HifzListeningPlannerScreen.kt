package com.ghais.ui.screens.memorization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.data.repository.HifzListeningPlan
import com.ghais.data.repository.HifzListeningPlannerStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

class HifzListeningPlannerScreen(
    private val startInCreator: Boolean = false,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val rootNavigator = LocalRootNavigator.current ?: navigator?.parent ?: navigator
        val plans by HifzListeningPlannerStore.plans.collectAsState()
        val epochDay = remember { HifzListeningPlannerStore.currentEpochDay() }
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val bottomDockPadding = if (currentTrack != null) 200.dp else 112.dp

        var editingPlan by remember(startInCreator, plans.isEmpty()) {
            mutableStateOf(
                if (startInCreator || plans.isEmpty()) {
                    HifzListeningPlan(
                        id = HifzListeningPlannerStore.newId(),
                        reciterSlugs = listOf("husary"),
                        surahIds = listOf(67),
                        timesPerSession = 3,
                        sessionsPerDay = 2,
                        durationDays = 7,
                        startEpochDay = epochDay,
                        reminderEnabled = true,
                        reminderHour = 20,
                        reminderMinute = 0,
                    )
                } else {
                    null
                }
            )
        }

        NoirScreenRoot {
            Column(modifier = Modifier.fillMaxSize()) {
                NoirNavBar(
                    title = if (editingPlan != null) "Create Listening Plan" else "Listening Planner",
                    onBack = {
                        if (editingPlan != null && plans.isNotEmpty()) {
                            editingPlan = null
                        } else {
                            navigator?.pop()
                        }
                    },
                    backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    actions = {
                        if (editingPlan == null) {
                            GhostPillButton(
                                text = "+ New",
                                active = true,
                                onClick = {
                                    editingPlan = HifzListeningPlan(
                                        id = HifzListeningPlannerStore.newId(),
                                        reciterSlugs = listOf("husary"),
                                        surahIds = listOf(67),
                                        timesPerSession = 3,
                                        sessionsPerDay = 2,
                                        durationDays = 7,
                                        startEpochDay = epochDay,
                                        reminderEnabled = true,
                                        reminderHour = 20,
                                        reminderMinute = 0,
                                    )
                                }
                            )
                        }
                    }
                )

                val draft = editingPlan
                if (draft != null) {
                    PlanEditorBody(
                        initialPlan = draft,
                        bottomPadding = bottomDockPadding,
                        onSave = { saved ->
                            HifzListeningPlannerStore.savePlan(saved)
                            editingPlan = null
                        },
                        onCancel = if (plans.isNotEmpty()) {
                            { editingPlan = null }
                        } else null
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = bottomDockPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(plans, key = { it.id }) { plan ->
                            ListeningPlanCard(
                                plan = plan,
                                epochDay = epochDay,
                                onListen = {
                                    val tracks = HifzListeningPlannerStore.buildSessionTracks(plan)
                                    if (tracks.isNotEmpty()) {
                                        AudioEngine.playQueue(tracks, startIndex = 0)
                                        HifzListeningPlannerStore.recordSessionCompleted(plan.id, epochDay)
                                        rootNavigator?.push(NowPlayingScreen())
                                    }
                                },
                                onMarkDone = {
                                    HifzListeningPlannerStore.recordSessionCompleted(plan.id, epochDay)
                                },
                                onEdit = { editingPlan = plan },
                                onDelete = { HifzListeningPlannerStore.deletePlan(plan.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningPlanCard(
    plan: HifzListeningPlan,
    epochDay: Long,
    onListen: () -> Unit,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val doneToday = plan.sessionsDoneOn(epochDay)
    val goalToday = plan.sessionsPerDay.coerceAtLeast(1)
    val dayCompleted = plan.isDayCompleted(epochDay)
    val dayNumber = plan.currentDayNumber(epochDay)
    val progress = (doneToday.toFloat() / goalToday.toFloat()).coerceIn(0f, 1f)

    val reciterNames = remember(plan.reciterSlugs) {
        plan.reciterSlugs.mapNotNull { slug ->
            QuranDataRepository.getReciterBySlug(slug)?.nameEn
                ?: EveryAyahReciters.ALL.firstOrNull { it.slug == slug }?.nameEn
        }.joinToString(", ").ifBlank { "Selected Qari" }
    }
    val surahNames = remember(plan.surahIds) {
        plan.surahIds.mapNotNull { id ->
            QuranDataRepository.getSurahById(id)?.nameEn
        }.joinToString(", ").ifBlank { "Selected Surahs" }
    }
    val reminderLabel = remember(plan.reminderEnabled, plan.reminderHour, plan.reminderMinute, dayCompleted) {
        if (!plan.reminderEnabled) "Reminder off"
        else if (dayCompleted) "Today's goal complete ✓"
        else {
            val hh = plan.reminderHour.toString().padStart(2, '0')
            val mm = plan.reminderMinute.toString().padStart(2, '0')
            "Reminder at $hh:$mm if incomplete"
        }
    }

    NoirCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.displayTitle { id ->
                            QuranDataRepository.getSurahById(id)?.nameEn ?: "Surah $id"
                        },
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Day $dayNumber of ${plan.durationDays} • ${plan.completedDaysCount()} days completed",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .noirClickable(onEdit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit plan",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .noirClickable(onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete plan",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Qaris: $reciterNames",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Surahs: $surahNames",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${plan.timesPerSession}× repeat per session • $doneToday / $goalToday sessions today",
                color = GhaisNoir.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            NoirSegmentedProgress(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = if (dayCompleted) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = reminderLabel,
                    color = if (dayCompleted) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromePillButton(
                    text = "Listen",
                    leadingIcon = Icons.Filled.PlayArrow,
                    onClick = onListen,
                    modifier = Modifier.weight(1f)
                )
                GhostPillButton(
                    text = if (dayCompleted) "Done ✓" else "+1 Session",
                    active = dayCompleted,
                    onClick = onMarkDone
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanEditorBody(
    initialPlan: HifzListeningPlan,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSave: (HifzListeningPlan) -> Unit,
    onCancel: (() -> Unit)?,
) {
    var title by remember(initialPlan.id) { mutableStateOf(initialPlan.title) }
    var selectedReciters by remember(initialPlan.id) { mutableStateOf(initialPlan.reciterSlugs) }
    var selectedSurahs by remember(initialPlan.id) { mutableStateOf(initialPlan.surahIds) }
    var timesPerSession by remember(initialPlan.id) { mutableStateOf(initialPlan.timesPerSession) }
    var sessionsPerDay by remember(initialPlan.id) { mutableStateOf(initialPlan.sessionsPerDay) }
    var durationDays by remember(initialPlan.id) { mutableStateOf(initialPlan.durationDays) }
    var reminderEnabled by remember(initialPlan.id) { mutableStateOf(initialPlan.reminderEnabled) }
    var reminderHour by remember(initialPlan.id) { mutableStateOf(initialPlan.reminderHour) }
    var reminderMinute by remember(initialPlan.id) { mutableStateOf(initialPlan.reminderMinute) }

    var qariQuery by remember { mutableStateOf("") }
    var surahQuery by remember { mutableStateOf("") }

    val allReciters = remember {
        val teacherFirst = EveryAyahReciters.ALL.map { it.toReciter() }
        val existingSlugs = teacherFirst.map { it.slug }.toHashSet()
        teacherFirst + QuranDataRepository.getReciters().filter { it.slug !in existingSlugs }
    }
    val filteredReciters = remember(qariQuery, allReciters) {
        val q = qariQuery.trim().lowercase()
        val list = if (q.isEmpty()) allReciters else allReciters.filter {
            it.nameEn.lowercase().contains(q) ||
                it.nameAr.contains(q) ||
                it.country.lowercase().contains(q)
        }
        list.take(30)
    }

    val allSurahs = remember { QuranDataRepository.getSurahs() }
    val filteredSurahs = remember(surahQuery, allSurahs) {
        val q = surahQuery.trim().lowercase()
        if (q.isEmpty()) allSurahs else allSurahs.filter {
            it.nameEn.lowercase().contains(q) ||
                it.nameAr.contains(q) ||
                it.id.toString() == q
        }
    }

    val canSave = selectedReciters.isNotEmpty() && selectedSurahs.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Plan Title
        item {
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Plan Name (optional)",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    NoirSearchField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "e.g., Daily Al-Mulk & Juz 'Amma",
                        showSearchIcon = false
                    )
                }
            }
        }

        // 2. Select Qaris
        item {
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Select Qaris (${selectedReciters.size} selected)",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    NoirSearchField(
                        value = qariQuery,
                        onValueChange = { qariQuery = it },
                        placeholder = "Search reciters..."
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(190.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredReciters, key = { it.slug }) { reciter ->
                                val isSelected = reciter.slug in selectedReciters
                                SelectableRow(
                                    title = reciter.nameEn,
                                    subtitle = "${reciter.country} • ${reciter.style}",
                                    selected = isSelected,
                                    onClick = {
                                        selectedReciters = if (isSelected) {
                                            selectedReciters - reciter.slug
                                        } else {
                                            selectedReciters + reciter.slug
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Select Surahs
        item {
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "2. Select Surahs (${selectedSurahs.size} selected)",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip("Al-Fatihah") { selectedSurahs = (selectedSurahs + 1).distinct() }
                        PresetChip("Al-Kahf") { selectedSurahs = (selectedSurahs + 18).distinct() }
                        PresetChip("Yasin") { selectedSurahs = (selectedSurahs + 36).distinct() }
                        PresetChip("Al-Mulk") { selectedSurahs = (selectedSurahs + 67).distinct() }
                        PresetChip("Last 10") { selectedSurahs = (selectedSurahs + (105..114)).distinct() }
                        PresetChip("Clear") { selectedSurahs = emptyList() }
                    }
                    Spacer(Modifier.height(8.dp))
                    NoirSearchField(
                        value = surahQuery,
                        onValueChange = { surahQuery = it },
                        placeholder = "Search surah by name or number..."
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSurahs, key = { it.id }) { surah ->
                            val isSelected = surah.id in selectedSurahs
                            SelectableRow(
                                title = "${surah.id}. ${surah.nameEn}",
                                subtitle = "${surah.nameAr} • ${surah.ayahsCount} ayahs",
                                selected = isSelected,
                                onClick = {
                                    selectedSurahs = if (isSelected) {
                                        selectedSurahs - surah.id
                                    } else {
                                        selectedSurahs + surah.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Repetitions & Days
        item {
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Daily Goal & Duration",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StepperRow(
                        label = "Times per session",
                        subtitle = "Repeat each surah",
                        value = timesPerSession,
                        unit = "×",
                        onDecrement = { if (timesPerSession > 1) timesPerSession-- },
                        onIncrement = { if (timesPerSession < 50) timesPerSession++ }
                    )
                    StepperRow(
                        label = "Sessions per day",
                        subtitle = "How many times a day",
                        value = sessionsPerDay,
                        unit = "/day",
                        onDecrement = { if (sessionsPerDay > 1) sessionsPerDay-- },
                        onIncrement = { if (sessionsPerDay < 20) sessionsPerDay++ }
                    )
                    StepperRow(
                        label = "Duration",
                        subtitle = "Total plan length",
                        value = durationDays,
                        unit = "days",
                        onDecrement = { if (durationDays > 1) durationDays-- },
                        onIncrement = { if (durationDays < 365) durationDays++ }
                    )
                }
            }
        }

        // 5. Incomplete-Plan Notification
        item {
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incomplete-Day Reminder",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Notify only if you haven't finished today's $sessionsPerDay sessions",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = GhaisNoir.TextSecondary,
                                uncheckedTrackColor = GhaisNoir.Fill2
                            )
                        )
                    }
                    if (reminderEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StepperRow(
                                label = "Hour",
                                subtitle = "",
                                value = reminderHour,
                                unit = "h",
                                onDecrement = { reminderHour = (reminderHour + 23) % 24 },
                                onIncrement = { reminderHour = (reminderHour + 1) % 24 },
                                modifier = Modifier.weight(1f)
                            )
                            StepperRow(
                                label = "Min",
                                subtitle = "",
                                value = reminderMinute,
                                unit = "m",
                                onDecrement = { reminderMinute = (reminderMinute + 45) % 60 },
                                onIncrement = { reminderMinute = (reminderMinute + 15) % 60 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 6. Save / Cancel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onCancel != null) {
                    GhostPillButton(
                        text = "Cancel",
                        onClick = onCancel
                    )
                }
                ChromePillButton(
                    text = "Save Listening Plan",
                    enabled = canSave,
                    onClick = {
                        onSave(
                            initialPlan.copy(
                                title = title.trim(),
                                reciterSlugs = selectedReciters,
                                surahIds = selectedSurahs,
                                timesPerSession = timesPerSession,
                                sessionsPerDay = sessionsPerDay,
                                durationDays = durationDays,
                                reminderEnabled = reminderEnabled,
                                reminderHour = reminderHour,
                                reminderMinute = reminderMinute,
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NoirSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    showSearchIcon: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSearchIcon) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = GhaisNoir.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = GhaisNoir.TextTertiary, fontSize = 12.sp)
                    }
                    inner()
                }
            )
            if (value.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier
                        .size(16.dp)
                        .noirClickable { onValueChange("") }
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(if (selected) GhaisNoir.Fill4 else GhaisNoir.Fill1)
            .border(
                1.dp,
                if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                GhaisShapes.row
            )
            .noirClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    subtitle: String,
    value: Int,
    unit: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .noirClickable(onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = "${value.toString().padStart(if (unit == "h" || unit == "m") 2 else 1, '0')}$unit",
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .noirClickable(onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
