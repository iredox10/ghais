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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.HifzListeningPlannerStore
import com.ghais.data.repository.HifzMasteryStore
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.EveryAyahReciter
import com.ghais.data.seed.EveryAyahReciters
import com.ghais.hifz.HifzSessionPlan
import com.ghais.hifz.buildSrsSessions
import com.ghais.hifz.quests.QuestRow
import com.ghais.hifz.quests.questsFor
import com.ghais.player.AudioEngine
import com.ghais.player.DownloadKeys
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirHeaderSearch
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.NoirReciterAvatar
import com.ghais.ui.screens.reciters.photoForSlug
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import kotlin.time.Clock

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
        var searchExpanded by remember { mutableStateOf(false) }

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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = bottomDockPadding
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "hifz_nav", span = { GridItemSpan(2) }) {
                    NoirNavBar(
                        title = "Hifz",
                        actions = {
                            NoirHeaderSearch(
                                expanded = searchExpanded,
                                onExpandedChange = { expanded ->
                                    searchExpanded = expanded
                                    if (!expanded) searchQuery = ""
                                },
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = "Search Hifz reciters…"
                            )
                        }
                    )
                }

                item(key = "hifz_hero", span = { GridItemSpan(2) }) {
                    val teacherSlug = reciters.firstOrNull()?.slug ?: "husary"
                    val goalRatio = (todayReviewed.toFloat() / dailyGoal.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f)

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
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "$masteredCount mastered • $todayReviewed/$dailyGoal today" +
                                            if (weakAyahs.isNotEmpty()) " • ${weakAyahs.size} due" else "",
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val firstWeak = weakAyahs.firstOrNull()
                                if (firstWeak != null) {
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

                            Spacer(Modifier.height(8.dp))
                            NoirSegmentedProgress(
                                progress = goalRatio,
                                trackHeight = 3.dp
                            )

                            Spacer(Modifier.height(8.dp))
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
                }

                item(key = "hifz_reciters_header", span = { GridItemSpan(2) }) {
                    NoirSectionHeader(
                        label = "Reciters • ${reciters.size}",
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (reciters.isEmpty()) {
                    item(key = "hifz_empty", span = { GridItemSpan(2) }) {
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

                        MemorizationGridCard(
                            reciter = reciter,
                            isPlaying = isPlayingThisReciter,
                            isDownloaded = isDownloaded,
                            downloadProgress = reciterProgress,
                            isDownloadFailed = isFailed,
                            onDownloadClick = {
                                if (isDownloaded) return@MemorizationGridCard
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
                    }
                }
            }
        }
    }
}

@Composable
private fun HifzStatTile(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.BorderGhost, shape)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun MemorizationGridCard(
    reciter: EveryAyahReciter,
    isPlaying: Boolean,
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    isDownloadFailed: Boolean = false,
    onDownloadClick: () -> Unit = {},
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val isTeacher = reciter.style.lowercase().let {
        it.contains("teacher") || it.contains("mu'allim") || it.contains("muallim")
    }
    val cardFill = if (isPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFill()
    val cardBorder = if (isPlaying) GhaisNoir.Lavender else GhaisNoir.BorderCard

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(cardFill)
            .border(1.dp, cardBorder, GhaisShapes.cardNoir)
            .topSpecular()
            .noirClickable(onCardClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: style badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(if (isTeacher) GhaisNoir.Fill3 else GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isTeacher) "Teacher" else reciter.style,
                        color = if (isTeacher) GhaisNoir.Lavender else GhaisNoir.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            NoirReciterAvatar(
                photoUrl = photoForSlug(reciter.slug),
                nameEn = reciter.nameEn,
                size = 76.dp,
                shape = CircleShape,
                ring = isPlaying,
                slug = reciter.slug,
                grayscale = false,
                scrimAlpha = 0f
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = reciter.nameEn,
                color = if (isPlaying) Color.White else GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = reciter.nameAr,
                style = GhaisTypography.arabicBody,
                fontFamily = GhaisTypography.quranFont,
                fontSize = 13.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
                color = GhaisNoir.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${reciter.tempo} • ${reciter.country}".trim(' ', '•'),
                color = GhaisNoir.TextTertiary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = plan.title,
                        color = if (hasTargets) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = plan.targets.size.toString(),
                        color = if (hasTargets) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
