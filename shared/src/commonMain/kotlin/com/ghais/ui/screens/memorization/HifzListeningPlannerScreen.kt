package com.ghais.ui.screens.memorization

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.data.repository.HifzListeningPlan
import com.ghais.data.repository.HifzListeningPlannerStore
import com.ghais.data.repository.HifzListeningPlannerStore.PlanCompletionEvent
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
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisColors
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
        val completionEvent by HifzListeningPlannerStore.lastCompletionEvent.collectAsState()
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (plans.isNotEmpty()) {
                                item(key = "planner_hero_streak") {
                                    PlannerHeroStreakCard(
                                        plans = plans,
                                        epochDay = epochDay
                                    )
                                }
                            }
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

                val event = completionEvent
                if (event != null) {
                    PlanCompletionCelebrationOverlay(
                        event = event,
                        onDismiss = { HifzListeningPlannerStore.dismissCompletionEvent() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannerHeroStreakCard(
    plans: List<HifzListeningPlan>,
    epochDay: Long,
) {
    val currentStreak = remember(plans, epochDay) {
        plans.maxOfOrNull { it.streakDays(epochDay) } ?: 0
    }
    val bestStreak = remember(plans) {
        plans.maxOfOrNull { it.bestStreakDays() } ?: 0
    }
    val totalDoneToday = remember(plans, epochDay) {
        plans.sumOf { it.sessionsDoneOn(epochDay) }
    }
    val totalGoalToday = remember(plans) {
        plans.sumOf { it.sessionsPerDay.coerceAtLeast(1) }.coerceAtLeast(1)
    }
    val progress = (totalDoneToday.toFloat() / totalGoalToday.toFloat()).coerceIn(0f, 1f)
    val allDoneToday = progress >= 1f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "hero_ring_progress"
    )
    val last7Days = remember(epochDay) {
        (6L downTo 0L).map { offset -> epochDay - offset }
    }

    val accentBorder = if (allDoneToday) {
        GhaisColors.MintGlow.copy(alpha = 0.45f)
    } else {
        GhaisNoir.Lavender.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        GhaisNoir.PlumTop.copy(alpha = 0.36f),
                        GhaisNoir.CardTintBottom
                    )
                )
            )
            .border(1.dp, accentBorder, GhaisShapes.cardLarge)
            .topSpecular(inset = 28.dp)
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (allDoneToday) "TODAY'S GOAL COMPLETE" else "DAILY HABIT",
                        color = if (allDoneToday) GhaisColors.MintGlow else GhaisNoir.Lavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (currentStreak > 0) "🔥 $currentStreak Day Streak" else "Start Your Streak",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Best streak: $bestStreak ${if (bestStreak == 1) "day" else "days"}",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringColor = if (allDoneToday) GhaisColors.MintGlow else GhaisNoir.Lavender
                    Canvas(modifier = Modifier.size(64.dp)) {
                        val strokePx = 5.dp.toPx()
                        if (allDoneToday) {
                            drawCircle(
                                color = GhaisColors.MintGlow.copy(alpha = 0.14f),
                                radius = size.minDimension / 2f
                            )
                        }
                        drawArc(
                            color = GhaisNoir.Fill3,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokePx)
                        )
                        if (animatedProgress > 0f) {
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalDoneToday/$totalGoalToday",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "today",
                            color = GhaisNoir.TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 7-Day Habit Chain
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                last7Days.forEach { day ->
                    val completed = plans.any { it.isDayCompleted(day) }
                    val isToday = day == epochDay
                    val hasPartial = !completed && plans.any { it.sessionsDoneOn(day) > 0 }
                    val dayLabel = if (isToday) "Today" else "-${epochDay - day}d"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completed -> GhaisColors.MintGlow.copy(alpha = 0.22f)
                                        isToday -> GhaisNoir.Lavender.copy(alpha = 0.16f)
                                        else -> GhaisNoir.Fill2
                                    }
                                )
                                .border(
                                    width = if (isToday || completed) 1.5.dp else 1.dp,
                                    color = when {
                                        completed -> GhaisColors.MintGlow
                                        isToday -> GhaisNoir.Lavender
                                        else -> GhaisNoir.BorderGhost
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                completed -> Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Completed",
                                    tint = GhaisColors.MintGlow,
                                    modifier = Modifier.size(15.dp)
                                )
                                hasPartial || isToday -> Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Lavender)
                                )
                            }
                        }
                        Text(
                            text = dayLabel,
                            color = when {
                                completed -> GhaisColors.MintGlow
                                isToday -> GhaisNoir.Lavender
                                else -> GhaisNoir.TextTertiary
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        )
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
    val progress = plan.dailyProgressFraction(epochDay).coerceIn(0f, 1f)
    val streak = plan.streakDays(epochDay)
    val estMinutes = plan.estimatedMinutesPerSession()

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

    val cardGlowBorder = if (dayCompleted) {
        GhaisColors.MintGlow.copy(alpha = 0.45f)
    } else {
        GhaisNoir.Lavender.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(GhaisNoir.cardFill())
            .border(1.dp, cardGlowBorder, GhaisShapes.cardNoir)
            .topSpecular()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: title + status badges + compact edit/delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    .background(GhaisColors.MintGlow.copy(alpha = 0.16f))
                                    .border(1.dp, GhaisColors.MintGlow.copy(alpha = 0.45f), GhaisShapes.pill)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✓ Goal Hit",
                                    color = GhaisColors.MintGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (streak > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(GhaisShapes.pill)
                                    .background(GhaisNoir.Lavender.copy(alpha = 0.14f))
                                    .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.30f), GhaisShapes.pill)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔥 ${streak}d",
                                    color = GhaisNoir.Lavender,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Day $dayNumber of ${plan.durationDays} • ~${estMinutes} min/session",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reciterList.take(2).joinToString(", ") +
                            if (reciterList.size > 2) " +${reciterList.size - 2}" else "",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
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

            // Progress + prominent Listen Now CTA
            Spacer(Modifier.height(12.dp))
            NoirSegmentedProgress(
                progress = progress,
                trackHeight = 5.dp,
                lavender = !dayCompleted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
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
                        .background(
                            if (dayCompleted) GhaisNoir.chromeFill() else GhaisNoir.lavenderFill()
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.40f), GhaisShapes.pill)
                        .noirClickable(onListen)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (dayCompleted) GhaisNoir.OnChrome else GhaisNoir.LavenderInk,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (dayCompleted) "Listen Again (~${estMinutes}m)" else "Listen Now (~${estMinutes}m)",
                        color = if (dayCompleted) GhaisNoir.OnChrome else GhaisNoir.LavenderInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
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
    var autoPlayOnSchedule by remember(initialPlan.id) { mutableStateOf(initialPlan.autoPlayOnSchedule) }
    var escalateReminder by remember(initialPlan.id) { mutableStateOf(initialPlan.escalateReminder) }

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

    val previewEstMin = remember(selectedSurahs, timesPerSession) {
        HifzListeningPlan(
            id = "preview",
            surahIds = selectedSurahs,
            timesPerSession = timesPerSession,
        ).estimatedMinutesPerSession()
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
                                textAlign = TextAlign.Center,
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

        // 4. Schedule, Habit Anchors & Automation
        item(key = "editor_schedule") {
            Column(modifier = Modifier.fillMaxWidth()) {
                NoirSectionHeader(label = "Schedule")
                NoirCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Commitment Summary badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.Lavender.copy(alpha = 0.14f))
                                .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.30f), GhaisShapes.pill)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "~$previewEstMin min/session • ${sessionsPerDay}x/day • $durationDays days",
                                color = GhaisNoir.Lavender,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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

                        EditorToggleRow(
                            title = "Daily reminder",
                            subtitle = "Alert if today's goal isn't done",
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )

                        if (reminderEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "1-Tap Habit Anchors",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val anchors = listOf(
                                    Triple("After Fajr 05:30", 5, 30),
                                    Triple("Morning 08:30", 8, 30),
                                    Triple("After Asr 16:00", 16, 0),
                                    Triple("Before Sleep 22:00", 22, 0),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(anchors, key = { it.first }) { (label, h, m) ->
                                        val selected = reminderHour == h && reminderMinute == m
                                        PresetChip(
                                            label = label,
                                            selected = selected,
                                            onClick = {
                                                reminderHour = h
                                                reminderMinute = m
                                            }
                                        )
                                    }
                                }
                            }

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

                            EditorToggleRow(
                                title = "Auto-start playback",
                                subtitle = "Start recitation automatically at reminder time",
                                checked = autoPlayOnSchedule,
                                onCheckedChange = { autoPlayOnSchedule = it }
                            )

                            EditorToggleRow(
                                title = "Last-call escalation",
                                subtitle = "Send a second nudge before midnight if skipped",
                                checked = escalateReminder,
                                onCheckedChange = { escalateReminder = it }
                            )
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
                                autoPlayOnSchedule = autoPlayOnSchedule,
                                escalateReminder = escalateReminder,
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
private fun EditorToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = GhaisNoir.TextSecondary,
                uncheckedTrackColor = GhaisNoir.Fill2
            )
        )
    }
}

@Composable
private fun PlanCompletionCelebrationOverlay(
    event: PlanCompletionEvent,
    onDismiss: () -> Unit,
) {
    val accent = if (event.dayCompleted) GhaisColors.MintGlow else GhaisNoir.Lavender

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GhaisNoir.Scrim)
            .noirClickable(onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.cardLarge)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            GhaisNoir.PlumTop.copy(alpha = 0.55f),
                            GhaisNoir.CardTintBottom
                        )
                    )
                )
                .border(1.5.dp, accent.copy(alpha = 0.60f), GhaisShapes.cardLarge)
                .topSpecular(inset = 28.dp)
                .noirClickable {}
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.20f))
                        .border(2.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (event.dayCompleted) Icons.Filled.LocalFireDepartment else Icons.Filled.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (event.dayCompleted) "Day Goal Crushed! 🔥" else "Session Complete!",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = event.planTitle,
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "${event.sessionsDoneToday} / ${event.sessionsPerDay} today • ${event.streak} day streak",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                ChromePillButton(
                    text = "Alhamdulillah / Continue",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
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
