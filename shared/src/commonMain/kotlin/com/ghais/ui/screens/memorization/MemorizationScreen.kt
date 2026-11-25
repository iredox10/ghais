package com.ghais.ui.screens.memorization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.EveryAyahReciter
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.data.repository.HifzListeningPlannerStore
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.hifz.HifzSessionPlan
import com.ghais.hifz.buildSrsSessions
import com.ghais.hifz.quests.QuestRow
import com.ghais.hifz.quests.questsFor
import kotlin.time.Clock
import com.ghais.player.AudioEngine
import com.ghais.player.DownloadKeys
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

object MemorizationScreen : Tab {

    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Hifz",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current
            ?: LocalNavigator.current?.parent
            ?: LocalNavigator.current

        var searchQuery by remember { mutableStateOf("") }

        val reciters = remember(searchQuery) {
            val ordered = EveryAyahReciters.ALL.sortedBy { reciter ->
                val s = reciter.style.lowercase()
                if (s.contains("teacher") || s.contains("mu'allim") || s.contains("muallim")) 0 else 1
            }
            if (searchQuery.isBlank()) {
                ordered
            } else {
                val q = searchQuery.trim().lowercase()
                ordered.filter { reciter ->
                    reciter.nameEn.lowercase().contains(q) ||
                        reciter.nameAr.contains(q) ||
                        reciter.country.lowercase().contains(q) ||
                        reciter.style.lowercase().contains(q)
                }
            }
        }

        val masteredCount by HifzMasteryStore.totalMasteredCount.collectAsState()
        val todayReviewed by HifzMasteryStore.todayReviewedCount.collectAsState()
        val dailyGoal by HifzMasteryStore.dailyGoalCount.collectAsState()
        val weakAyahs by HifzMasteryStore.weakAyahs.collectAsState()
        val masteryMap by HifzMasteryStore.masteryMap.collectAsState()
        val dueDayMap by HifzMasteryStore.dueDayMap.collectAsState()
        val epochDay = Clock.System.now().toEpochMilliseconds() / 86_400_000L
        val srsSessions = remember(masteryMap, dueDayMap, weakAyahs, epochDay) {
            buildSrsSessions(
                masteryMap = masteryMap,
                dueDayMap = dueDayMap,
                weakAyahs = weakAyahs,
                todayEpochDay = epochDay,
            )
        }
        val dailyQuests = remember(masteryMap, todayReviewed, dailyGoal, epochDay) {
            questsFor(
                mastery = masteryMap,
                todayReviewed = todayReviewed,
                dailyGoal = dailyGoal,
                epochDay = epochDay,
            )
        }

        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isAyahMode by AudioEngine.isAyahMode.collectAsState()
        val isEnginePlaying by AudioEngine.isPlaying.collectAsState()

        val downloaded by QuranDownloads.downloadedKeys.collectAsState()
        val dlProgress by QuranDownloads.progress.collectAsState()
        val failedKeys by QuranDownloads.failedKeys.collectAsState()
        val listeningPlans by HifzListeningPlannerStore.plans.collectAsState()
        val bottomDockPadding = if (currentTrack != null) 200.dp else 112.dp

        fun playReciterAyahMode(reciter: EveryAyahReciter, surahId: Int = 1) {
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = 1
            )
            rootNavigator?.push(NowPlayingScreen())
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = bottomDockPadding
                )
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NoirNavBar(
                            title = "Hifz",
                            actions = {
                                Text(
                                    text = "${reciters.size} reciters",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(GhaisShapes.pill)
                                .background(GhaisNoir.insetFill())
                                .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.pill)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search reciters...",
                                                color = GhaisNoir.TextTertiary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        NoirCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Today's Hifz",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "$masteredCount mastered • $todayReviewed/$dailyGoal reviewed today",
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    val firstWeak = weakAyahs.firstOrNull()
                                    if (firstWeak != null) {
                                        val teacherSlug = reciters.firstOrNull()?.slug ?: "husary"
                                        GhostPillButton(
                                            text = "Review due",
                                            onClick = {
                                                AudioEngine.playSurahInAyahMode(
                                                    surahId = firstWeak.first,
                                                    reciterSlug = teacherSlug,
                                                    startAyahNo = firstWeak.second
                                                )
                                                rootNavigator?.push(NowPlayingScreen())
                                            }
                                        )
                                    }
                                }
                                dailyQuests.firstOrNull()?.let { primaryQuest ->
                                    Spacer(Modifier.height(10.dp))
                                    QuestRow(
                                        quest = primaryQuest,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                val teacherSlug = reciters.firstOrNull()?.slug ?: "husary"
                                HifzSrsTierRow(
                                    sessions = srsSessions,
                                    onSelectPlan = { plan ->
                                        val firstTarget = plan.targets.firstOrNull() ?: return@HifzSrsTierRow
                                        AudioEngine.playSurahInAyahMode(
                                            surahId = firstTarget.first,
                                            reciterSlug = teacherSlug,
                                            startAyahNo = firstTarget.second
                                        )
                                        rootNavigator?.push(NowPlayingScreen())
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        val activePlan = listeningPlans.firstOrNull { it.enabled } ?: listeningPlans.firstOrNull()
                        NoirCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Listening Planner",
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        val reciterSummary = remember(activePlan?.reciterSlugs) {
                                            activePlan?.reciterSlugs?.mapNotNull { slug ->
                                                QuranDataRepository.getReciterBySlug(slug).nameEn
                                            }?.joinToString(", ") ?: ""
                                        }
                                        Text(
                                            text = if (activePlan != null) {
                                                val done = activePlan.sessionsDoneOn(epochDay)
                                                val dayNo = activePlan.currentDayNumber(epochDay)
                                                val qariPart = if (reciterSummary.isNotBlank()) " • $reciterSummary" else ""
                                                "${activePlan.displayTitle { id -> QuranDataRepository.getSurahById(id)?.nameEn ?: "Surah $id" }}$qariPart • $done/${activePlan.sessionsPerDay} today (Day $dayNo/${activePlan.durationDays})"
                                            } else {
                                                "Pick Qaris, Surahs, daily repeats & days with incomplete-day reminders"
                                            },
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    GhostPillButton(
                                        text = if (activePlan != null) "Manage" else "Create",
                                        active = activePlan == null,
                                        onClick = {
                                            rootNavigator?.push(
                                                HifzListeningPlannerScreen(startInCreator = activePlan == null)
                                            )
                                        }
                                    )
                                }
                                if (activePlan != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GhostPillButton(
                                            text = "Listen Session",
                                            active = true,
                                            onClick = {
                                                val tracks = HifzListeningPlannerStore.buildSessionTracks(activePlan)
                                                if (tracks.isNotEmpty()) {
                                                    AudioEngine.playListeningPlan(activePlan.id, tracks)
                                                    rootNavigator?.push(NowPlayingScreen())
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        GhostPillButton(
                                            text = "+ New Plan",
                                            onClick = {
                                                rootNavigator?.push(HifzListeningPlannerScreen(startInCreator = true))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                    }
                }

                if (reciters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reciters found for \"$searchQuery\"",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = reciters,
                        key = { it.slug }
                    ) { reciter ->
                        val isCurrentReciter = currentTrack?.reciterSlug == reciter.slug && isAyahMode
                        val isPlayingThisReciter = isCurrentReciter && isEnginePlaying

                        // Starter-pack bundle: Al-Fatihah surah file + every ayah
                        // clip + verse text, so the bundle plays and reads fully
                        // offline in the player.
                        val bundleKeys = remember(reciter.slug) {
                            val count = QuranDataRepository.getSurahById(1)?.ayahsCount ?: 7
                            DownloadKeys.bundleKeys(reciter.slug, 1, count)
                        }
                        val isDownloaded = bundleKeys.all { it in downloaded }
                        val isDownloading = !isDownloaded &&
                            bundleKeys.any { it in dlProgress }
                        val isFailed = !isDownloaded && !isDownloading &&
                            bundleKeys.any { it in failedKeys }
                        val doneCount = bundleKeys.count { it in downloaded }
                        val reciterProgress: Float? = if (!isDownloading) null else
                            doneCount.toFloat() / bundleKeys.size.toFloat()
                        val starterAudioUrl = reciter.toReciter().getFullSurahUrl(1)

                        MemorizationReciterCard(
                            reciter = reciter,
                            isPlaying = isPlayingThisReciter,
                            isDownloaded = isDownloaded,
                            downloadProgress = reciterProgress,
                            isDownloadFailed = isFailed,
                            onDownloadClick = {
                                // Finished bundles stay finished; partial/failed
                                // bundles resume (engine skips done/in-flight keys).
                                if (isDownloaded) return@MemorizationReciterCard
                                val full = reciter.toReciter()
                                val count = QuranDataRepository.getSurahById(1)?.ayahsCount ?: 7
                                val ayahUrls = (1..count).associateWith { ayahNo ->
                                    full.getAyahAudioUrl(1, ayahNo)
                                }
                                QuranDownloads.downloadSurahBundle(
                                    reciter.slug, 1, starterAudioUrl, ayahUrls
                                )
                                QuranAyahRepository.prefetchSurah(1)
                            },
                            onPlayClick = {
                                if (isPlayingThisReciter) {
                                    AudioEngine.pause()
                                } else if (isCurrentReciter) {
                                    AudioEngine.resume()
                                    rootNavigator?.push(NowPlayingScreen())
                                } else {
                                    playReciterAyahMode(reciter, 1)
                                }
                            },
                            onCardClick = { rootNavigator?.push(MemorizationReciterScreen(reciter.slug)) }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorizationReciterCard(
    reciter: EveryAyahReciter,
    isPlaying: Boolean,
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    isDownloadFailed: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val isDownloading = downloadProgress != null
    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoirReciterAvatar(
                    photoUrl = photoForSlug(reciter.slug),
                    nameEn = reciter.nameEn,
                    size = 52.dp,
                    shape = RoundedCornerShape(16.dp),
                    ring = isPlaying,
                    slug = reciter.slug,
                    grayscale = false,
                    scrimAlpha = 0f
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reciter.nameEn,
                        color = if (isPlaying) Color.White else GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = reciter.nameAr,
                        style = GhaisTypography.arabicBody,
                        fontFamily = GhaisTypography.quranFont,
                        fontSize = 14.sp,
                        lineHeight = 25.sp,
                        letterSpacing = 0.sp,
                        color = GhaisNoir.TextSecondary
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "${reciter.style} • ${reciter.tempo}",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                when {
                    isDownloaded -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.chromeFill())
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Downloaded for offline",
                                tint = GhaisNoir.OnChrome,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    isDownloading -> {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Downloading",
                                tint = GhaisNoir.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    isDownloadFailed -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.wellFill())
                                .border(1.dp, GhaisNoir.SpecularTop, CircleShape)
                                .noirClickable { onDownloadClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download failed — tap to retry",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                .noirClickable { onDownloadClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download Al-Fatihah for offline",
                                tint = GhaisNoir.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color.White else GhaisNoir.Fill2)
                        .border(
                            1.dp,
                            if (isPlaying) Color.White else GhaisNoir.BorderCard,
                            CircleShape
                        )
                        .noirClickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause ${reciter.nameEn}" else "Play ${reciter.nameEn} in Ayah Mode",
                        tint = if (isPlaying) Color.Black else GhaisNoir.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val p = downloadProgress
            if (!isDownloaded && p != null && p in 0f..1f) {
                Spacer(modifier = Modifier.height(8.dp))
                NoirSegmentedProgress(
                    progress = p,
                    trackHeight = 3.dp
                )
            }
        }
    }
}

@Composable
private fun HifzSrsTierRow(
    sessions: List<HifzSessionPlan>,
    onSelectPlan: (HifzSessionPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sessions.forEach { plan ->
            val hasTargets = plan.targets.isNotEmpty()
            val tileShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(tileShape)
                    .background(if (hasTargets) GhaisNoir.Fill2 else GhaisNoir.InsetFill)
                    .border(
                        1.dp,
                        if (hasTargets) GhaisNoir.BorderCard else GhaisNoir.BorderGhost,
                        tileShape
                    )
                    .then(
                        if (hasTargets) Modifier.noirClickable { onSelectPlan(plan) }
                        else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = plan.title,
                            color = if (hasTargets) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = plan.targets.size.toString(),
                            color = if (hasTargets) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = plan.subtitle,
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
