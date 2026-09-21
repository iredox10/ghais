package com.ghais.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private val PureBlack = Color(0xFF000000)
private val LinkBlue = Color(0xFF4C8DFF)
private val MutedGrey = Color(0xFF9A9AA0)
private val GlassFill = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
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
                                .clickable { goTo(currentPage - 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(42.dp))
                    }
                    Text(
                        text = "Step ${currentPage + 1} of $PageCount",
                        color = MutedGrey,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { OnboardingStore.complete() }) {
                        Text(
                            text = "Skip",
                            color = MutedGrey,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                    .background(
                        if (index == current) LinkBlue
                        else Color.White.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LinkBlue,
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
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
                .background(GlassFill, RoundedCornerShape(28.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
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
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Daily recitation, reminders and your favorite qari — in one calm place.",
            color = MutedGrey,
            fontSize = 15.sp,
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
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick one — we'll tune your setup around it.",
            color = MutedGrey,
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
            .background(if (selected) LinkBlue.copy(alpha = 0.14f) else GlassFill)
            .border(
                1.dp,
                if (selected) LinkBlue else GlassBorder,
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (selected) LinkBlue else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = option.desc,
                color = MutedGrey,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = LinkBlue,
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
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap a voice to follow — you can change it anytime.",
            color = MutedGrey,
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
                        .clickable { onPick(reciter.slug) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                if (selected) 3.dp else 1.dp,
                                if (selected) LinkBlue else GlassBorder,
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
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reciter.nameEn,
                        color = if (selected) Color.White else MutedGrey,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassFill)
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = LinkBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Selected: $name",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = "No qari selected yet",
                color = MutedGrey,
                fontSize = 13.sp
            )
        }
    }
}

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
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "How many minutes a day do you want to listen?",
            color = MutedGrey,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$minutes min",
                    color = Color.White,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily reminder",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Get a nudge at your chosen time",
                    color = MutedGrey,
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = reminderOn,
                onCheckedChange = onReminderToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LinkBlue,
                    uncheckedThumbColor = MutedGrey,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                )
            )
        }
        if (reminderOn) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeStepper(
                    label = "Hour",
                    value = hour.toString().padStart(2, '0'),
                    onMinus = { onHourChange((hour + 23) % 24) },
                    onPlus = { onHourChange((hour + 1) % 24) },
                    modifier = Modifier.weight(1f)
                )
                TimeStepper(
                    label = "Minute",
                    value = minute.toString().padStart(2, '0'),
                    onMinus = { onMinuteChange((minute + 59) % 60) },
                    onPlus = { onMinuteChange((minute + 1) % 60) },
                    modifier = Modifier.weight(1f)
                )
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
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = Color.White,
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
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = MutedGrey, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
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
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Here's what we'll start you with.",
            color = MutedGrey,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PreviewRow(label = "Goal", value = goalLabel)
            PreviewRow(label = "Qari", value = reciterName)
            PreviewRow(label = "Daily goal", value = "$minutes min")
            PreviewRow(label = "Reminder", value = reminderText)
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
            color = MutedGrey,
            fontSize = 14.sp,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
