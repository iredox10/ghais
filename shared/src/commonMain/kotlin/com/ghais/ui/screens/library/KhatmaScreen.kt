package com.ghais.ui.screens.library

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.KhatmaStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.khatmaAyahCountBetween
import com.ghais.data.repository.khatmaDaysLeft
import com.ghais.data.repository.khatmaDoneToday
import com.ghais.data.repository.khatmaEtaDateMs
import com.ghais.data.repository.khatmaEtaLabel
import com.ghais.data.repository.khatmaIsComplete
import com.ghais.data.repository.khatmaPlanProgress
import com.ghais.data.repository.khatmaRemainingAyahs
import com.ghais.data.repository.khatmaShortDate
import com.ghais.data.repository.khatmaSurahAyahCount
import com.ghais.data.repository.khatmaTodayPortion
import com.ghais.data.repository.KhatmaPortion
import com.ghais.data.seed.PageJuzIndex
import com.ghais.domain.model.KhatmaPlan
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import kotlin.time.Clock

object KhatmaScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        // Live data source: the first (most recently started) persisted plan.
        val plans by KhatmaStore.plans.collectAsState()
        val plan = plans.firstOrNull()
        var showCreate by remember { mutableStateOf(false) }
        // Playback reciter: the canonical fallback (Mishary), mirroring
        // ReciterProfileScreen's allTracks TrackItem shape below.
        val reciter = remember { QuranDataRepository.getFallbackReciter() }
        // Pinned per plan revision so day-boundary math doesn't shift mid-frame.
        val nowMs = remember(plan?.id, plan?.lastProgressMs, plan?.totalAyahsRead) {
            Clock.System.now().toEpochMilliseconds()
        }

        // One call site for portion playback + completion. Completion wiring
        // is record-on-play-start guarded to once per epoch day (see
        // KhatmaCreateDialog KDoc / report): the first play of the day
        // records the portion via KhatmaStore.recordProgress; replays the
        // same day play audio only, so aborted sessions can't double-count
        // and finished ones keep streak/ETA moving without a second observer.
        fun playPortion(target: KhatmaPlan, portion: KhatmaPortion, record: Boolean) {
            val surahs = (portion.startSurahId..portion.endSurahId)
                .mapNotNull { QuranDataRepository.getSurahById(it) }
                .filter { reciter.isSurahAvailable(it.id) }
            if (surahs.isEmpty()) return
            val tracks = surahs.map { surah ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(surah.id),
                    textUthmani = "",
                    durationMs = surah.ayahsCount * 15_000L
                )
            }
            AudioEngine.playQueue(tracks, startIndex = 0)
            rootNavigator.push(NowPlayingScreen())
            if (record) KhatmaStore.recordPortionDone(target.id, portion)
        }

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = { navigator.pop() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(GhaisNoir.Fill2, CircleShape)
                                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "SPIRITUAL PLAN",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = "Khatma",
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            if (plan == null) {
                                NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconWell(
                                                icon = Icons.Default.TrackChanges,
                                                size = 64.dp,
                                                iconSize = 28.dp,
                                                contentDescription = "Khatma progress"
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "NO PLAN YET",
                                                    color = GhaisNoir.TextTertiary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 1.2.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Start your first khatma",
                                                    color = GhaisNoir.TextPrimary,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Name it, pick a start point and a target — daily portions follow.",
                                                    color = GhaisNoir.TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        ChromePillButton(
                                            text = "Create a plan",
                                            onClick = { showCreate = true },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                val progress = khatmaPlanProgress(plan)
                                val streak = plan.streak
                                val daysLeft = khatmaDaysLeft(plan, nowMs)
                                val etaMs = khatmaEtaDateMs(plan, nowMs)
                                val etaLabel = khatmaEtaLabel(etaMs)
                                val done = plan.totalAyahsRead.coerceAtLeast(0)
                                val planTotal = done + khatmaRemainingAyahs(plan)
                                val portion = khatmaTodayPortion(plan, nowMs)
                                val doneToday = khatmaDoneToday(plan, nowMs)
                                val complete = khatmaIsComplete(plan)

                                NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconWell(
                                                icon = Icons.Default.TrackChanges,
                                                size = 64.dp,
                                                iconSize = 28.dp,
                                                contentDescription = "Khatma progress"
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = plan.title.uppercase(),
                                                    color = GhaisNoir.TextTertiary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 1.2.sp,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "${(progress * 100).toInt()}% completed",
                                                    color = GhaisNoir.TextPrimary,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "$streak-day streak • $daysLeft days remaining",
                                                    color = GhaisNoir.TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            NoirStatChip(text = "$streak-day streak")
                                            NoirStatChip(text = "$daysLeft days left")
                                            NoirStatChip(text = etaLabel)
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        NoirSegmentedProgress(progress = progress, trackHeight = 8.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$done of $planTotal ayahs",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${(progress * 100).toInt()}% kept",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                NoirSectionHeader(label = "Today's portion")

                                NoirCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (complete || portion == null) {
                                            Text(
                                                text = if (complete) "COMPLETE" else "PLAN NEEDS ATTENTION",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (complete) {
                                                    "The whole Quran is read."
                                                } else {
                                                    "This plan's position is unreadable — delete it and create a fresh one."
                                                },
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            val startName =
                                                QuranDataRepository.getSurahById(portion.startSurahId)?.nameEn
                                                    ?: "Surah ${portion.startSurahId}"
                                            val endName =
                                                QuranDataRepository.getSurahById(portion.endSurahId)?.nameEn
                                                    ?: "Surah ${portion.endSurahId}"
                                            Text(
                                                text = "TODAY'S TARGET • ${portion.ayahCount} AYAHS",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (portion.startSurahId == portion.endSurahId) {
                                                    "Surah $startName"
                                                } else {
                                                    "$startName → $endName"
                                                },
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (portion.startSurahId == portion.endSurahId) {
                                                    "Ayahs ${portion.startAyahNo}–${portion.endAyahNo}"
                                                } else {
                                                    "${portion.startSurahId}:${portion.startAyahNo} – ${portion.endSurahId}:${portion.endAyahNo}"
                                                },
                                                color = GhaisNoir.TextSecondary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (doneToday) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Done for today — come back tomorrow.",
                                                    color = GhaisNoir.TextTertiary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                            ChromePillButton(
                                                text = if (doneToday) "Replay Today's Portion" else "Play Today's Portion",
                                                onClick = {
                                                    playPortion(plan, portion, record = !doneToday)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                leadingIcon = Icons.Default.PlayArrow
                                            )
                                            if (!doneToday) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                GhostPillButton(
                                                    text = "Mark as read",
                                                    onClick = {
                                                        KhatmaStore.recordPortionDone(plan.id, portion)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                NoirSectionHeader(label = "Journey")

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatBox("Streak", "$streak Days", Icons.Filled.LocalFireDepartment, GhaisNoir.TextPrimary)
                                    StatBox("Remaining", "$daysLeft Days", null, GhaisNoir.TextPrimary)
                                    StatBox("Est. Finish", khatmaShortDate(etaMs), null, GhaisNoir.TextPrimary)
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                NoirSectionHeader(label = "Plan")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GhostPillButton(
                                        text = "New plan",
                                        onClick = { showCreate = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                    GhostPillButton(
                                        text = "Delete plan",
                                        onClick = { KhatmaStore.remove(plan.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreate) {
            KhatmaCreateDialog(onDismiss = { showCreate = false })
        }
    }
}

/**
 * Create-plan dialog: name + start surah:ayah + target days. The pages/day
 * preview divides [PageJuzIndex.pageCount] from the start point to the end
 * of the Quran by the target days (ceiling). Confirm persists via
 * [KhatmaStore.create] (blank names fall back to "My Khatma" there).
 */
@Composable
private fun KhatmaCreateDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var surahText by remember { mutableStateOf("1") }
    var ayahText by remember { mutableStateOf("1") }
    var daysText by remember { mutableStateOf("30") }

    val surahNum = surahText.toIntOrNull()?.coerceIn(1, 114) ?: 1
    val maxAyah = khatmaSurahAyahCount(surahNum).coerceAtLeast(1)
    val ayahNum = ayahText.toIntOrNull()?.coerceIn(1, maxAyah) ?: 1
    val daysNum = daysText.toIntOrNull()?.coerceAtLeast(1) ?: 30
    val surahName = QuranDataRepository.getSurahById(surahNum)?.nameEn ?: "Surah $surahNum"
    val lastAyahCount = khatmaSurahAyahCount(114).coerceAtLeast(1)
    val totalPages = PageJuzIndex.pageCount(surahNum, ayahNum, 114, lastAyahCount)
    val pagesPerDay = if (totalPages <= 0) 0 else (totalPages + daysNum - 1) / daysNum
    val totalAyahs = khatmaAyahCountBetween(surahNum, ayahNum, 114, lastAyahCount)
    val ayahsPerDay = if (totalAyahs <= 0) 0 else (totalAyahs + daysNum - 1) / daysNum

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GhaisNoir.NoirBlack,
        title = {
            Text(
                text = "New khatma plan",
                color = GhaisNoir.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                KhatmaField(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Ramadan khatma"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KhatmaField(
                        label = "Surah (1–114)",
                        value = surahText,
                        onValueChange = { surahText = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder = "1",
                        modifier = Modifier.weight(1f)
                    )
                    KhatmaField(
                        label = "Ayah",
                        value = ayahText,
                        onValueChange = { ayahText = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder = "1",
                        modifier = Modifier.weight(1f)
                    )
                    KhatmaField(
                        label = "Days",
                        value = daysText,
                        onValueChange = { daysText = it.filter { c -> c.isDigit() }.take(4) },
                        placeholder = "30",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Start: $surahName $ayahNum",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "≈ $pagesPerDay pages/day • $ayahsPerDay ayahs/day • $totalPages pages total",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    KhatmaStore.create(
                        title = name.trim(),
                        startSurahId = surahNum,
                        startAyahNo = ayahNum,
                        targetDays = daysNum
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = "Create",
                    color = GhaisNoir.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = GhaisNoir.TextSecondary
                )
            }
        }
    )
}

@Composable
private fun KhatmaField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = GhaisNoir.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(GhaisNoir.TextPrimary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = GhaisNoir.TextDisabled,
                        fontSize = 15.sp
                    )
                }
                inner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(GhaisNoir.insetFill(), GhaisShapes.row)
                .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun StatBox(label: String, value: String, icon: ImageVector?, iconTint: Color) {
    // iconTint is accepted for backward compatibility; Noir renders all
    // glyphs in the monochrome text ramp (zero hue).
    val tint = when (iconTint) {
        Color.Unspecified -> GhaisNoir.TextSecondary
        else -> GhaisNoir.TextPrimary
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            IconWell(icon = icon, size = 40.dp, iconSize = 20.dp, tint = tint)
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp
        )
    }
}
