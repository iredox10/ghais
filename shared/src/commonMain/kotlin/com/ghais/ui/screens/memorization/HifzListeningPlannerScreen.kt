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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
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
                        reciterSlugs = emptyList(),
                        surahIds = emptyList(),
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
                    title = if (editingPlan != null) "Create Plan" else "Listening Planner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            NoirNavButton(
                                icon = Icons.Filled.Add,
                                contentDescription = "Create new listening plan",
                                onClick = {
                                    editingPlan = HifzListeningPlan(
                                        id = HifzListeningPlannerStore.newId(),
                                        reciterSlugs = emptyList(),
                                        surahIds = emptyList(),
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
                        item(key = "planner_overview") {
                            val completedToday = plans.count { it.isDayCompleted(epochDay) }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                NoirSectionHeader(
                                    label = if (plans.size == 1) "Active Plan" else "Active Plans • ${plans.size}",
                                    actionLabel = "$completedToday/${plans.size} done today",
                                    onAction = null
                                )
                            }
                        }
                        items(plans, key = { it.id }) { plan ->
                            ListeningPlanCard(
                                plan = plan,
                                epochDay = epochDay,
                                onListen = {
                                    val tracks = HifzListeningPlannerStore.buildSessionTracks(plan)
                                    if (tracks.isNotEmpty()) {
                                        AudioEngine.playListeningPlan(plan.id, tracks)
                                        rootNavigator?.push(NowPlayingScreen())
                                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeningPlanCard(
    plan: HifzListeningPlan,
    epochDay: Long,
    onListen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val doneToday = plan.sessionsDoneOn(epochDay)
    val goalToday = plan.sessionsPerDay.coerceAtLeast(1)
    val dayCompleted = plan.isDayCompleted(epochDay)
    val dayNumber = plan.currentDayNumber(epochDay)
    val progress = (doneToday.toFloat() / goalToday.toFloat()).coerceIn(0f, 1f)

    val reciterList = remember(plan.reciterSlugs) {
        plan.reciterSlugs.mapNotNull { slug ->
            QuranDataRepository.getBrowseReciterBySlug(slug)?.nameEn
                ?: QuranDataRepository.getReciterBySlug(slug).nameEn.takeIf { it.isNotBlank() }
                ?: EveryAyahReciters.ALL.firstOrNull { it.slug == slug }?.nameEn
        }.ifEmpty { listOf("Selected Qari") }
    }
    val surahList = remember(plan.surahIds) {
        plan.surahIds.mapNotNull { id ->
            QuranDataRepository.getSurahById(id)?.nameEn
        }.ifEmpty { listOf("Selected Surahs") }
    }
    val cardTitle = remember(plan.title, surahList) {
        plan.title.trim().takeIf { it.isNotEmpty() } ?: run {
            val first = surahList.firstOrNull() ?: "Quran"
            if (surahList.size > 1) "$first +${surahList.size - 1}" else first
        }
    }
    val reminderValue = remember(plan.reminderEnabled, plan.reminderHour, plan.reminderMinute) {
        if (!plan.reminderEnabled) "Off"
        else {
            val hh = plan.reminderHour.toString().padStart(2, '0')
            val mm = plan.reminderMinute.toString().padStart(2, '0')
            "$hh:$mm"
        }
    }

    NoirCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: title + day/qari subtitle + compact edit/delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = cardTitle,
                            color = GhaisNoir.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (dayCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.Fill3)
                                    .border(1.dp, GhaisNoir.SpecularTop, GhaisShapes.pill)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    color = GhaisNoir.Lavender,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Day $dayNumber/${plan.durationDays} • ${reciterList.take(2).joinToString(", ")}" +
                            if (reciterList.size > 2) " +${reciterList.size - 2}" else "",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .noirClickable(onEdit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit plan",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .noirClickable(onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete plan",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Clean surah summary line
            Text(
                text = surahList.take(4).joinToString(" · ") +
                    if (surahList.size > 4) " +${surahList.size - 4} more" else "",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Compact 3-stat strip
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlanStatTile(
                    label = "REPEAT",
                    value = "${plan.timesPerSession}×",
                    modifier = Modifier.weight(1f)
                )
                PlanStatTile(
                    label = "TODAY",
                    value = "$doneToday / $goalToday",
                    modifier = Modifier.weight(1f)
                )
                PlanStatTile(
                    label = "REMINDER",
                    value = reminderValue,
                    modifier = Modifier.weight(1f)
                )
            }

            // Progress + compact Listen action
            Spacer(Modifier.height(12.dp))
            NoirSegmentedProgress(
                progress = progress,
                trackHeight = 4.dp,
                lavender = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${plan.completedDaysCount()} of ${plan.durationDays} days completed",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable(onListen)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (dayCompleted) "Listen Again" else "Listen",
                        color = GhaisNoir.OnChrome,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(GhaisShapes.row)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlanMetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        QuranDataRepository.getBrowseReciters()
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Plan Name (minimal inset pill)
        item(key = "editor_name") {
            NoirSearchField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Plan name (optional, e.g. Daily Al-Mulk)",
                showSearchIcon = false
            )
        }

        // 2. Select Qaris — horizontal portrait rail (no nested vertical scroll)
        item(key = "editor_qaris") {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoirSectionHeader(
                    label = "Qaris",
                    actionLabel = "${selectedReciters.size} selected",
                    onAction = if (selectedReciters.isNotEmpty()) {
                        { selectedReciters = emptyList() }
                    } else null
                )
                NoirSearchField(
                    value = qariQuery,
                    onValueChange = { qariQuery = it },
                    placeholder = "Search reciters…"
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(filteredReciters, key = { it.slug }) { reciter ->
                        val isSelected = reciter.slug in selectedReciters
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(84.dp)
                                .clip(GhaisShapes.row)
                                .background(if (isSelected) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft())
                                .border(
                                    1.dp,
                                    if (isSelected) GhaisNoir.Lavender else GhaisNoir.BorderGhost,
                                    GhaisShapes.row
                                )
                                .noirClickable {
                                    selectedReciters = if (isSelected) {
                                        selectedReciters - reciter.slug
                                    } else {
                                        selectedReciters + reciter.slug
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            NoirReciterAvatar(
                                photoUrl = photoForSlug(reciter.slug) ?: reciter.imageUrl,
                                nameEn = reciter.nameEn,
                                size = 54.dp,
                                shape = CircleShape,
                                ring = isSelected,
                                slug = reciter.slug,
                                grayscale = false,
                                scrimAlpha = 0f
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = reciter.nameEn,
                                color = if (isSelected) Color.White else GhaisNoir.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Select Surahs — single-row presets + horizontal surah rail
        item(key = "editor_surahs") {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoirSectionHeader(
                    label = "Surahs",
                    actionLabel = if (selectedSurahs.isNotEmpty()) "${selectedSurahs.size} selected • Clear" else "0 selected",
                    onAction = if (selectedSurahs.isNotEmpty()) {
                        { selectedSurahs = emptyList() }
                    } else null
                )
                val portions = listOf(
                    "Juz 'Amma" to (78..114).toList(),
                    "Juz Tabarak" to (67..77).toList(),
                    "Juz Qad Sami'" to (58..66).toList(),
                    "Juz Adh-Dhariyat" to (51..57).toList(),
                    "Last 10" to (105..114).toList(),
                    "Al-Fatihah" to listOf(1),
                    "Al-Baqarah" to listOf(2),
                    "Al-Kahf" to listOf(18),
                    "Yasin" to listOf(36),
                    "Ar-Rahman" to listOf(55),
                    "Al-Waqi'ah" to listOf(56),
                    "Al-Mulk" to listOf(67),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(portions, key = { it.first }) { (label, ids) ->
                        val allIn = ids.all { it in selectedSurahs }
                        PresetChip(
                            label = label,
                            selected = allIn,
                            onClick = {
                                selectedSurahs = if (allIn) {
                                    selectedSurahs - ids.toSet()
                                } else {
                                    (selectedSurahs + ids).distinct()
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                NoirSearchField(
                    value = surahQuery,
                    onValueChange = { surahQuery = it },
                    placeholder = "Search surah by name or number…"
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(filteredSurahs, key = { it.id }) { surah ->
                        val isSelected = surah.id in selectedSurahs
                        Column(
                            modifier = Modifier
                                .width(126.dp)
                                .clip(GhaisShapes.row)
                                .background(if (isSelected) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft())
                                .border(
                                    1.dp,
                                    if (isSelected) GhaisNoir.Lavender else GhaisNoir.BorderGhost,
                                    GhaisShapes.row
                                )
                                .noirClickable {
                                    selectedSurahs = if (isSelected) {
                                        selectedSurahs - surah.id
                                    } else {
                                        selectedSurahs + surah.id
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${surah.id}",
                                    color = if (isSelected) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = GhaisNoir.Lavender,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = surah.nameEn,
                                color = GhaisNoir.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${surah.ayahsCount} ayahs",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4. Schedule & Reminder
        item(key = "editor_schedule") {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoirSectionHeader(label = "Schedule")
                NoirCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StepperRow(
                            label = "Repeat",
                            subtitle = "Times per session",
                            value = timesPerSession,
                            unit = "×",
                            onDecrement = { if (timesPerSession > 1) timesPerSession-- },
                            onIncrement = { if (timesPerSession < 50) timesPerSession++ }
                        )
                        StepperRow(
                            label = "Daily goal",
                            subtitle = "Sessions per day",
                            value = sessionsPerDay,
                            unit = "/day",
                            onDecrement = { if (sessionsPerDay > 1) sessionsPerDay-- },
                            onIncrement = { if (sessionsPerDay < 20) sessionsPerDay++ }
                        )
                        StepperRow(
                            label = "Duration",
                            subtitle = "Plan length",
                            value = durationDays,
                            unit = "days",
                            onDecrement = { if (durationDays > 1) durationDays-- },
                            onIncrement = { if (durationDays < 365) durationDays++ }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily reminder",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Alert if today's goal isn't done",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp
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
        }

        // 5. Save / Cancel
        item(key = "editor_actions") {
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
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(if (selected) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(
                1.dp,
                if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                GhaisShapes.pill
            )
            .noirClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
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
