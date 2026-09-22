package com.ghais.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RecitationSchedule
import com.ghais.data.repository.SchedulesStore
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.domain.model.Reciter
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSwitch
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import com.ghais.ui.theme.GhaisTypography

private const val PageCount = 5
private const val LastPage = PageCount - 1

object OnboardingScreen : Screen {
    @Composable
    override fun Content() {
        val savedStep by OnboardingStore.step.collectAsState()
        val goal by OnboardingStore.goal.collectAsState()
        val minutes by OnboardingStore.dailyGoalMinutes.collectAsState()
        val followed by FollowStore.followedSlugs.collectAsState()
        val scope = rememberCoroutineScope()
        val startPage = savedStep.coerceIn(0, LastPage)
        val pagerState = rememberPagerState(initialPage = startPage) { PageCount }
        val currentPage = pagerState.currentPage

        LaunchedEffect(currentPage) {
            OnboardingStore.setStep(currentPage)
        }

        var reminderOn by remember { mutableStateOf(false) }
        var hour by remember { mutableStateOf(7) }
        var minute by remember { mutableStateOf(0) }
        val reciters = remember { QuranDataRepository.getReciters().take(8) }

        fun goTo(page: Int) {
            scope.launch { pagerState.animateScrollToPage(page.coerceIn(0, LastPage)) }
        }

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage > 0) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                .noirClickable { goTo(currentPage - 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(42.dp))
                    }
                    Text(
                        text = "Step ${currentPage + 1} of $PageCount",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    GhostPillButton(text = "Skip", onClick = { OnboardingStore.complete() })
                }
                Spacer(modifier = Modifier.height(4.dp))
                DotsRow(current = currentPage)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    userScrollEnabled = false
                ) { page ->
                    when (page) {
                        0 -> WelcomeStep(onNext = { goTo(1) })
                        1 -> GoalStep(
                            selectedGoal = goal,
                            onPick = { id ->
                                OnboardingStore.setGoal(id)
                                goTo(2)
                            }
                        )
                        2 -> ReciterStep(
                            reciters = reciters,
                            followed = followed,
                            onPick = { slug ->
                                FollowStore.clear()
                                FollowStore.follow(slug)
                                goTo(3)
                            }
                        )
                        3 -> GoalReminderStep(
                            minutes = minutes,
                            onMinutesChange = { OnboardingStore.setDailyGoalMinutes(it) },
                            reminderOn = reminderOn,
                            onReminderToggle = { reminderOn = it },
                            hour = hour,
                            minute = minute,
                            onHourChange = { hour = it },
                            onMinuteChange = { minute = it },
                            onContinue = {
                                if (reminderOn) {
                                    SchedulesStore.add(
                                        RecitationSchedule(
                                            id = SchedulesStore.newId(),
                                            hour = hour,
                                            minute = minute,
                                            reciterSlug = followed.firstOrNull() ?: "mishary",
                                            fromSurah = 1,
                                            toSurah = 114,
                                            durationMin = minutes,
                                            enabled = true
                                        )
                                    )
                                }
                                goTo(4)
                            }
                        )
                        else -> PreviewStep(
                            goalId = goal,
                            reciterSlug = followed.firstOrNull(),
                            minutes = minutes,
                            reminderOn = reminderOn,
                            hour = hour,
                            minute = minute,
                            onDone = { OnboardingStore.complete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DotsRow(current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(PageCount) { index ->
            Box(
                modifier = Modifier
                    .width(if (index == current) 24.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (index == current) Modifier.background(
                            GhaisNoir.chromeFill(),
                            RoundedCornerShape(4.dp)
                        ) else Modifier.background(
                            GhaisNoir.Fill4,
                            RoundedCornerShape(4.dp)
                        )
                    )
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    ChromePillButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepScroll(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        content = { content() }
    )
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(GhaisNoir.wellFill(), RoundedCornerShape(28.dp))
                .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ghais_mark),
                contentDescription = "Ghais",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Listen to the Quran every day",
            style = GhaisTypography.displayEditorialSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Daily recitation, reminders and your favorite qari — in one calm place.",
            style = GhaisTypography.editorialBody,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(label = "Get Started", onClick = onNext)
    }
}

private data class GoalOption(
    val id: String,
    val label: String,
    val desc: String,
    val icon: ImageVector
)

private val GoalOptions = listOf(
    GoalOption("study", "Study", "Steady recitation to boost focus", Icons.Filled.School),
    GoalOption("sleep", "Sleep", "Calm voices to fall asleep to", Icons.Filled.Bedtime),
    GoalOption("work", "Work", "Deep focus for the workday", Icons.Filled.Work),
    GoalOption("memorize", "Memorize", "Repeat and retain verse by verse", Icons.Filled.Psychology),
    GoalOption("consistency", "Consistency", "A small daily habit that sticks", Icons.Filled.Repeat)
)

@Composable
private fun GoalStep(selectedGoal: String?, onPick: (String) -> Unit) {
    StepScroll {
        Text(
            text = "What brings you here?",
            style = GhaisTypography.displayEditorialSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick one — we'll tune your setup around it.",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        GoalOptions.forEach { option ->
            GoalCard(
                option = option,
                selected = selectedGoal == option.id,
                onClick = { onPick(option.id) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GoalCard(option: GoalOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft(),
                RoundedCornerShape(22.dp)
            )
            .border(
                1.dp,
                if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                RoundedCornerShape(22.dp)
            )
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(
            icon = option.icon,
            size = 46.dp,
            iconSize = 24.dp,
            tint = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                color = GhaisNoir.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = option.desc,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ReciterStep(
    reciters: List<Reciter>,
    followed: Set<String>,
    onPick: (String) -> Unit
) {
    StepScroll {
        Text(
            text = "Choose your Qari",
            style = GhaisTypography.displayEditorialSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap a voice to follow — you can change it anytime.",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(reciters, key = { it.slug }) { reciter ->
                val photo = remember(reciter.slug) {
                    resolveFollowedQari(reciter.slug)?.photoUrl
                }
                val selected = followed.contains(reciter.slug)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .noirClickable { onPick(reciter.slug) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) Color.White else GhaisNoir.BorderGhost,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photo != null) {
                            AsyncImage(
                                model = photo,
                                contentDescription = reciter.nameEn,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = reciter.nameEn.take(1).uppercase(),
                                color = GhaisNoir.TextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reciter.nameEn,
                        color = if (selected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        val selectedSlug = followed.firstOrNull()
        if (selectedSlug != null) {
            val name = QuranDataRepository.getReciterBySlug(selectedSlug).nameEn
            NoirCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Selected: $name",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Text(
                text = "No qari selected yet",
                color = GhaisNoir.TextTertiary,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalReminderStep(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    reminderOn: Boolean,
    onReminderToggle: (Boolean) -> Unit,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onContinue: () -> Unit
) {
    StepScroll {
        Text(
            text = "Set your daily goal",
            style = GhaisTypography.displayEditorialSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "How many minutes a day do you want to listen?",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$minutes min",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    StepperButton(icon = Icons.Filled.Remove, desc = "Less") {
                        onMinutesChange(minutes - 5)
                    }
                    StepperButton(icon = Icons.Filled.Add, desc = "More") {
                        onMinutesChange(minutes + 5)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily reminder",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Get a nudge at your chosen time",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 13.sp
                    )
                }
                NoirSwitch(
                    checked = reminderOn,
                    onCheckedChange = onReminderToggle
                )
            }
        }
        if (reminderOn) {
            Spacer(modifier = Modifier.height(12.dp))
            var showClock by remember { mutableStateOf(false) }
            NoirCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showClock = true }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Reminder time",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                        color = GhaisNoir.TextPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to set time",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showClock) {
                val clockState = rememberTimePickerState(
                    initialHour = hour,
                    initialMinute = minute,
                    is24Hour = true
                )
                Dialog(onDismissRequest = { showClock = false }) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GhostPillButton(
                                    text = "Cancel",
                                    onClick = { showClock = false },
                                    modifier = Modifier.weight(1f)
                                )
                                ChromePillButton(
                                    text = "Set time",
                                    onClick = {
                                        onHourChange(clockState.hour)
                                        onMinuteChange(clockState.minute)
                                        showClock = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(label = "Continue", onClick = onContinue)
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, CircleShape)
            .noirClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = GhaisNoir.TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun TimeStepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GhaisNoir.cardFillSoft(), RoundedCornerShape(24.dp))
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(24.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = GhaisNoir.TextTertiary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepperButton(icon = Icons.Filled.Remove, desc = "Down", onClick = onMinus)
            StepperButton(icon = Icons.Filled.Add, desc = "Up", onClick = onPlus)
        }
    }
}

@Composable
private fun PreviewStep(
    goalId: String?,
    reciterSlug: String?,
    minutes: Int,
    reminderOn: Boolean,
    hour: Int,
    minute: Int,
    onDone: () -> Unit
) {
    val goalLabel = GoalOptions.find { it.id == goalId }?.label ?: "Listening"
    val reciterName = if (reciterSlug != null) {
        QuranDataRepository.getReciterBySlug(reciterSlug).nameEn
    } else {
        "Not chosen"
    }
    val reminderText = if (reminderOn) {
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    } else {
        "Off"
    }
    StepScroll {
        Text(
            text = "Your setup",
            style = GhaisTypography.displayEditorialSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Here's what we'll start you with.",
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        NoirCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PreviewRow(label = "Goal", value = goalLabel)
                PreviewRow(label = "Qari", value = reciterName)
                PreviewRow(label = "Daily goal", value = "$minutes min")
                PreviewRow(label = "Reminder", value = reminderText)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(label = "Start listening", onClick = onDone)
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 14.sp,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
