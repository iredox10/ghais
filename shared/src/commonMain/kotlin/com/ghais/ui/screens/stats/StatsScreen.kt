package com.ghais.ui.screens.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.StatsRange as RepoStatsRange
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.barsForRange
import com.ghais.data.repository.formatDuration
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.data.share.ShareSheet
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.screens.home.moments.LocalWallClock
import com.ghais.ui.screens.home.moments.WeekRecapShelf
import com.ghais.ui.screens.reciters.rememberReciterPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object StatsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stats by UserUsageRepository.stats.collectAsState()
        val history by UserUsageRepository.history.collectAsState()
        val favorites by FavoritesStore.favoriteTracks.collectAsState()
        val followed by FollowStore.followedSlugs.collectAsState()
        val downloaded by QuranDownloads.downloadedKeys.collectAsState()

        val totalHoursText = remember(stats.totalSecondsListened) {
            formatDuration(stats.totalSecondsListened)
        }

        val qariPlayCounts = remember(history) {
            history.filter { it.reciterSlug.isNotBlank() && it.surahId in 1..114 }
                .groupingBy { it.reciterSlug }
                .eachCount()
        }
        val topQaris = remember(qariPlayCounts) {
            qariPlayCounts.map { (slug, plays) ->
                val name = resolveFollowedQari(slug)?.reciter?.nameEn ?: slug
                Triple(slug, name, plays)
            }.sortedWith(
                compareByDescending<Triple<String, String, Int>> { it.third }
                    .thenBy { it.second }
            ).take(5)
        }

        val surahPlays by UserUsageRepository.surahPlays.collectAsState()
        val topSurahs = remember(surahPlays) {
            surahPlays.entries
                .sortedByDescending { it.value }
                .take(5)
                .mapNotNull { (surahId, plays) ->
                    val surah = QuranDataRepository.getSurahById(surahId)
                        ?: return@mapNotNull null
                    val name = surah.nameEn.ifBlank {
                        surah.transliteration.ifBlank { "Surah $surahId" }
                    }
                    Triple(surahId, name, plays)
                }
        }
        val maxSurahPlays = remember(topSurahs) {
            (topSurahs.firstOrNull()?.third ?: 1L).coerceAtLeast(1L)
        }

        val goalMin by com.ghais.data.repository.OnboardingStore.dailyGoalMinutes.collectAsState()

        val dailyMap by UserUsageRepository.dailySeconds.collectAsState()
        val remoteDailyMap by UserUsageRepository.remoteDailySeconds.collectAsState()
        var range by remember { mutableStateOf(StatsRange.Week) }
        val todayEpochDay = rememberStatsEpochDay()

        val combinedDailySeconds = remember(dailyMap, remoteDailyMap) {
            combinedStatsDays(dailyMap, remoteDailyMap)
        }
        val todaySeconds = combinedDailySeconds[todayEpochDay] ?: 0L
        val todayMinutes = todaySeconds / 60f
        val rawRingProgress = (todayMinutes / goalMin.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
        val ringProgress by animateFloatAsState(
            targetValue = rawRingProgress,
            animationSpec = tween(durationMillis = 700),
            label = "dailyGoalRing"
        )

        val rangeSecondsList = remember(range, combinedDailySeconds, todayEpochDay) {
            List(range.days) { i ->
                combinedDailySeconds[todayEpochDay - (range.days - 1 - i)] ?: 0L
            }
        }
        val rangeTotalSeconds = remember(rangeSecondsList) { rangeSecondsList.sum() }
        val rangeTotalMin = rangeTotalSeconds / 60f
        val rangeAvgSeconds = if (range.days > 0) rangeTotalSeconds / range.days else 0L
        val rangeActiveDays = rangeSecondsList.count { it > 0L }
        val bestIndex = remember(rangeSecondsList) {
            val max = rangeSecondsList.maxOrNull() ?: 0L
            if (max > 0L) rangeSecondsList.indexOfFirst { it == max } else null
        }
        val bestSeconds = bestIndex?.let { rangeSecondsList[it] } ?: 0L
        val bestDayLabel = bestIndex?.let {
            weekdayShort(todayEpochDay - (range.days - 1 - it))
        } ?: "—"

        val chartBars = remember(range, combinedDailySeconds, todayEpochDay) {
            val repoRange = when (range) {
                StatsRange.Today -> RepoStatsRange.TODAY
                StatsRange.Week -> RepoStatsRange.WEEK
                StatsRange.Month -> RepoStatsRange.DAYS_30
                StatsRange.Quarter -> RepoStatsRange.MONTHS_3
                StatsRange.Year -> RepoStatsRange.YEAR
            }
            barsForRange(repoRange, combinedDailySeconds, todayEpochDay)
        }
        val shownBars = remember(chartBars) { chartBars.map { it.second / 60f } }
        val shownLabels = remember(range, chartBars) {
            when (range) {
                StatsRange.Today -> listOf("Today")
                StatsRange.Week -> chartBars.map { it.first }
                StatsRange.Month, StatsRange.Quarter, StatsRange.Year -> emptyList()
            }
        }
        val dailyBreakdownUnavailable = combinedDailySeconds.isEmpty() && stats.totalSecondsListened > 0L
        val remoteMinutesInRange = remember(range, remoteDailyMap, todayEpochDay) {
            var sum = 0L
            var day = todayEpochDay - (range.days - 1)
            while (day <= todayEpochDay) {
                sum += remoteDailyMap[day] ?: 0L
                day++
            }
            sum / 60f
        }
        val showRemoteCaption = remoteMinutesInRange >= 1f

        val rangeProgress = remember(range, rangeTotalMin, rangeActiveDays, goalMin) {
            when (range) {
                StatsRange.Today -> (rangeTotalMin / goalMin.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                StatsRange.Week -> (rangeTotalMin / (goalMin * 7f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                else -> if (range.days > 0) (rangeActiveDays / range.days.toFloat()).coerceIn(0f, 1f) else 0f
            }
        }
        val rangeCaption = when (range) {
            StatsRange.Today -> "${rangeTotalMin.toInt()} min of $goalMin min goal"
            StatsRange.Week -> if (rangeTotalSeconds > 0L) {
                "${formatDuration(rangeTotalSeconds)} this week"
            } else {
                "No activity yet this week"
            }
            else -> "Active $rangeActiveDays of ${range.days} days"
        }

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Navigation + Quick Library Pills
                item {
                    NoirNavBar(
                        title = "Stats",
                        onBack = { navigator.pop() },
                        backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        actions = {
                            NoirNavButton(
                                icon = Icons.Filled.Share,
                                contentDescription = "Share your stats",
                                onClick = {
                                    ShareSheet.shareText(
                                        title = "Ghaith — My Quran Journey",
                                        text = buildWrappedCard(
                                            totalHoursText = totalHoursText,
                                            dayStreak = stats.daysStreak,
                                            topQariName = topQaris.firstOrNull()?.second,
                                            topQariPlays = topQaris.firstOrNull()?.third?.toLong(),
                                            topSurahName = topSurahs.firstOrNull()?.second,
                                            topSurahPlays = topSurahs.firstOrNull()?.third
                                        )
                                    )
                                }
                            )
                        },
                        titleAlignLeft = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryQuickPill(
                            icon = Icons.Filled.Favorite,
                            value = "${favorites.size}",
                            label = "Favorites",
                            modifier = Modifier.weight(1f)
                        )
                        LibraryQuickPill(
                            icon = Icons.Filled.People,
                            value = "${followed.size}",
                            label = "Following",
                            modifier = Modifier.weight(1f)
                        )
                        LibraryQuickPill(
                            icon = Icons.Filled.DownloadDone,
                            value = "${downloaded.size}",
                            label = "Offline",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Hero Journey Card (Plum Aura + Glowing Goal Ring + 3-Col Footer)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GhaisShapes.cardLarge)
                            .background(GhaisNoir.plumFill())
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        GhaisNoir.Lavender.copy(alpha = 0.55f),
                                        GhaisNoir.Lavender.copy(alpha = 0.16f)
                                    )
                                ),
                                GhaisShapes.cardLarge
                            )
                    ) {
                        // Top-right soft radial aura behind the goal ring
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GhaisNoir.Lavender.copy(alpha = 0.22f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.82f, size.height * 0.34f),
                                    radius = size.width * 0.55f
                                ),
                                radius = size.width * 0.55f,
                                center = Offset(size.width * 0.82f, size.height * 0.34f)
                            )
                        }

                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "YOUR QURAN JOURNEY",
                                    color = GhaisNoir.Lavender.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.28f))
                                        .border(
                                            1.dp,
                                            GhaisNoir.Lavender.copy(alpha = 0.35f),
                                            CircleShape
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = GhaisNoir.Lavender,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stats.daysStreak}-day streak",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = totalHoursText,
                                        color = Color.White,
                                        fontSize = 46.sp,
                                        lineHeight = 50.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (stats.totalSecondsListened > 0L) "total listening"
                                        else "start your first recitation",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 13.sp
                                    )
                                }

                                // Glowing Daily Goal Ring
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(112.dp)
                                ) {
                                    Canvas(modifier = Modifier.size(112.dp)) {
                                        val stroke = 10.dp.toPx()
                                        val pad = stroke / 2f
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.16f),
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                        if (ringProgress > 0f) {
                                            drawArc(
                                                brush = Brush.sweepGradient(
                                                    listOf(
                                                        Color(0xFFE2DCFF),
                                                        GhaisNoir.Lavender,
                                                        Color(0xFF9E8CF0)
                                                    )
                                                ),
                                                startAngle = -90f,
                                                sweepAngle = 360f * ringProgress,
                                                useCenter = false,
                                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                                            )
                                            // Specular tip dot on the arc head
                                            val angleRad = ((-90f + 360f * ringProgress) * PI / 180.0)
                                            val radius = (size.minDimension - pad * 2f) / 2f
                                            val tipX = center.x + (radius * cos(angleRad)).toFloat()
                                            val tipY = center.y + (radius * sin(angleRad)).toFloat()
                                            drawCircle(
                                                color = Color.White,
                                                radius = stroke * 0.28f,
                                                center = Offset(tipX, tipY)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${(rawRingProgress * 100).toInt()}%",
                                            color = Color.White,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (todaySeconds in 1L..59L) "${todaySeconds}s/${goalMin}m"
                                            else "${todayMinutes.toInt()}/${goalMin}m",
                                            color = Color.White.copy(alpha = 0.72f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.12f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                HeroFooterCell(
                                    label = "Qaris heard",
                                    value = "${stats.uniqueRecitersCount}",
                                    modifier = Modifier.weight(1f)
                                )
                                HeroFooterCell(
                                    label = "Surahs heard",
                                    value = "${stats.uniqueSurahsCount}",
                                    modifier = Modifier.weight(1f)
                                )
                                HeroFooterCell(
                                    label = "Daily goal",
                                    value = "${goalMin}m",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. Monday Weekly Recap Shelf
                item {
                    WeekRecapShelf(todayDow = LocalWallClock.mondayBasedDow())
                }

                // 4. Time Window Selector
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsRange.values().forEach { r ->
                            StatsRangePill(
                                label = r.label,
                                selected = r == range,
                                onClick = { range = r }
                            )
                        }
                    }
                }

                // 5. Interactive Rhythm & Chart Card
                item {
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = range.header,
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${range.window} • UTC",
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                                if (range == StatsRange.Today || range == StatsRange.Week) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(GhaisNoir.Lavender.copy(alpha = 0.14f))
                                            .border(
                                                1.dp,
                                                GhaisNoir.Lavender.copy(alpha = 0.35f),
                                                CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "goal ${goalMin}m/day",
                                            color = GhaisNoir.Lavender,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatDuration(rangeTotalSeconds),
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (rangeTotalSeconds > 0L) "listened" else "start listening",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3-Tile Tinted Bento Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BentoMetricTile(
                                    label = "Daily avg",
                                    value = formatDuration(rangeAvgSeconds),
                                    modifier = Modifier.weight(1f)
                                )
                                BentoMetricTile(
                                    label = if (bestSeconds > 0L) "Best ($bestDayLabel)" else "Best day",
                                    value = if (bestSeconds > 0L) formatDuration(bestSeconds) else "—",
                                    highlight = bestSeconds > 0L,
                                    modifier = Modifier.weight(1f)
                                )
                                BentoMetricTile(
                                    label = "Active",
                                    value = "$rangeActiveDays/${range.days}d",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val bestBarIdx = remember(shownBars) {
                                val max = shownBars.maxOrNull() ?: 0f
                                if (max > 0f) shownBars.indexOfFirst { it == max }
                                else shownBars.lastIndex.coerceAtLeast(0)
                            }
                            var selectedBar by remember(range, bestBarIdx) {
                                mutableStateOf(bestBarIdx.coerceIn(0, shownBars.lastIndex.coerceAtLeast(0)))
                            }
                            InsightBars(
                                values = shownBars,
                                labels = shownLabels,
                                goalMin = if (range == StatsRange.Today || range == StatsRange.Week) {
                                    goalMin.toFloat()
                                } else null,
                                selected = if (shownBars.isEmpty()) 0 else selectedBar.coerceIn(shownBars.indices),
                                onSelect = { selectedBar = it }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val selIdx = if (shownBars.isEmpty()) 0 else selectedBar.coerceIn(shownBars.indices)
                            val selLabel = when (range) {
                                StatsRange.Today -> "Today (UTC)"
                                StatsRange.Week -> shownLabels.getOrNull(selIdx) ?: "—"
                                StatsRange.Month -> {
                                    val daysAgo = shownBars.lastIndex - selIdx
                                    val dayName = chartBars.getOrNull(selIdx)?.first.orEmpty()
                                    if (daysAgo == 0) "Today ($dayName)" else "${daysAgo}d ago ($dayName)"
                                }
                                StatsRange.Quarter, StatsRange.Year -> chartBars.getOrNull(selIdx)?.first ?: "—"
                            }
                            val selSeconds = chartBars.getOrNull(selIdx)?.second ?: 0L

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GhaisNoir.InsetFill)
                                    .border(1.dp, GhaisNoir.InsetBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Lavender)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$selLabel • ${formatDuration(selSeconds)}",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            NoirSegmentedProgress(progress = rangeProgress)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rangeCaption,
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            if (dailyBreakdownUnavailable) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Daily breakdown unavailable for older listening",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            if (showRemoteCaption) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${remoteMinutesInRange.toInt()}m synced from other devices",
                                    color = GhaisNoir.Lavender.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 6. Top Qaris (Spotlight #1 + Ranked List)
                item {
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Top Qaris",
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Distinct saved surahs • recent history",
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (topQaris.isNotEmpty()) {
                                topQaris.forEachIndexed { index, entry ->
                                    TopQariRow(
                                        rank = index + 1,
                                        slug = entry.first,
                                        name = entry.second,
                                        plays = entry.third
                                    )
                                    if (index > 0 && index < topQaris.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(GhaisNoir.BorderGhost)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No recent history yet",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // 7. Top Surahs with Proportional Bars
                item {
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Top Surahs",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Most played • all time",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (topSurahs.isEmpty()) {
                                Text(
                                    text = "No surahs played yet",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 13.sp
                                )
                            } else {
                                topSurahs.forEachIndexed { index, entry ->
                                    val surahRank = index + 1
                                    val barFraction = (entry.third.toFloat() / maxSurahPlays.toFloat())
                                        .coerceIn(0.08f, 1f)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        brush = if (surahRank == 1) GhaisNoir.lavenderFill()
                                                        else Brush.verticalGradient(
                                                            listOf(GhaisNoir.Fill2, GhaisNoir.Fill2)
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "#$surahRank",
                                                    color = if (surahRank == 1) GhaisNoir.LavenderInk
                                                    else GhaisNoir.TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = entry.second,
                                                color = GhaisNoir.TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${entry.third} plays",
                                                color = if (surahRank == 1) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                                                fontSize = 12.sp,
                                                fontWeight = if (surahRank == 1) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(GhaisNoir.Fill1)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(barFraction)
                                                    .height(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (surahRank == 1) GhaisNoir.lavenderFill()
                                                        else Brush.horizontalGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.35f),
                                                                Color.White.copy(alpha = 0.14f)
                                                            )
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                    if (index < topSurahs.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(GhaisNoir.BorderGhost)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryQuickPill(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GhaisNoir.Lavender,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HeroFooterCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun BentoMetricTile(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GhaisNoir.tintFill())
            .border(
                1.dp,
                if (highlight) GhaisNoir.Lavender.copy(alpha = 0.35f) else GhaisNoir.BorderGhost,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = if (highlight) GhaisNoir.Lavender else GhaisNoir.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun TopQariRow(
    rank: Int,
    slug: String,
    name: String,
    plays: Int
) {
    val photo = rememberReciterPhoto(slug)
    if (rank == 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GhaisNoir.tintFill())
                .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirArtworkWell(
                coverUrl = photo.orEmpty(),
                monogram = name.firstOrNull()?.uppercase() ?: "Q",
                shape = RoundedCornerShape(14.dp),
                size = 52.dp,
                monogramSize = 18.sp,
                ring = false,
                grayscale = false,
                scrimAlpha = 0f
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#1 MOST LISTENED",
                    color = GhaisNoir.Lavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = name,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GhaisNoir.Lavender.copy(alpha = 0.18f))
                    .border(
                        1.dp,
                        GhaisNoir.Lavender.copy(alpha = 0.4f),
                        CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$plays surahs",
                    color = GhaisNoir.Lavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            color = GhaisNoir.TextTertiary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        NoirArtworkWell(
            coverUrl = photo.orEmpty(),
            monogram = name.firstOrNull()?.uppercase() ?: "Q",
            shape = CircleShape,
            size = 36.dp,
            monogramSize = 14.sp,
            ring = true,
            grayscale = false,
            scrimAlpha = 0f
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            color = GhaisNoir.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$plays surahs",
            color = GhaisNoir.TextTertiary,
            fontSize = 12.sp
        )
    }
}

private enum class StatsRange(
    val label: String,
    val days: Int,
    val header: String,
    val window: String
) {
    Today("Today", 1, "Your day in Quran", "today so far"),
    Week("Week", 7, "Your week in Quran", "last 7 days"),
    Month("30 days", 30, "Your month in Quran", "last 30 days"),
    Quarter("3 months", 90, "Your season in Quran", "last 90 days"),
    Year("Year", 365, "Your year in Quran", "last 365 days")
}

private val WEEKDAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun buildWrappedCard(
    totalHoursText: String,
    dayStreak: Int,
    topQariName: String?,
    topQariPlays: Long?,
    topSurahName: String?,
    topSurahPlays: Long?
): String = buildString {
    appendLine("Ghaith — My Quran Journey")
    appendLine()
    appendLine("$totalHoursText listened • $dayStreak-day streak")
    appendLine()
    if (topQariName != null && topQariPlays != null) {
        appendLine("Recent-history Qari: $topQariName ($topQariPlays saved surahs)")
    } else {
        appendLine("Top Qari: —")
    }
    if (topSurahName != null && topSurahPlays != null) {
        appendLine("Top Surah: $topSurahName ($topSurahPlays plays)")
    } else {
        appendLine("Top Surah: —")
    }
    appendLine()
    append("Listen, memorize & reflect with Ghaith")
}

private fun weekdayShort(epochDay: Long): String {
    val idx = ((epochDay + 3) % 7 + 7) % 7
    return WEEKDAY_SHORT[idx.toInt()]
}

@Composable
private fun InsightBars(
    values: List<Float>,
    labels: List<String>,
    goalMin: Float?,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    val maxVal = ((values.maxOrNull() ?: 0f).coerceAtLeast(goalMin ?: 0f)).coerceAtLeast(1f)
    val bestIdx = values.indices.maxByOrNull { values[it] }
    val maxH = 118.dp
    val hasLabels = labels.size == values.size
    val labelH = if (hasLabels) 20.dp else 0.dp
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxH + labelH),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { i, v ->
                val isSel = i == selected
                val isBest = i == bestIdx && v > 0f
                val isToday = hasLabels && i == values.lastIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .noirClickable { onSelect(i) }
                ) {
                    Box(
                        modifier = Modifier
                            .height(maxH)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val barH = if (v <= 0f) 4.dp
                        else 10.dp + (maxH - 10.dp) * (v / maxVal).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.58f)
                                .height(barH)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    brush = when {
                                        isSel -> GhaisNoir.lavenderFill()
                                        isBest -> Brush.verticalGradient(
                                            listOf(
                                                GhaisNoir.Lavender.copy(alpha = 0.58f),
                                                GhaisNoir.Lavender.copy(alpha = 0.22f)
                                            )
                                        )
                                        isToday -> Brush.verticalGradient(
                                            listOf(Color.White, Color.White.copy(alpha = 0.85f))
                                        )
                                        else -> Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.28f),
                                                Color.White.copy(alpha = 0.14f)
                                            )
                                        )
                                    }
                                )
                        )
                    }
                    if (hasLabels) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = labels[i],
                            color = if (isSel || isToday) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        if (goalMin != null && goalMin > 0f) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = labelH)
            ) {
                val y = size.height * (1f - (goalMin / maxVal).coerceIn(0f, 1f))
                drawLine(
                    color = GhaisNoir.Lavender.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                        phase = 0f
                    )
                )
            }
        }
    }
}

@Composable
private fun StatsRangePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                brush = if (selected) GhaisNoir.lavenderFill()
                else Brush.verticalGradient(
                    listOf(GhaisNoir.Fill2, GhaisNoir.Fill2)
                ),
                shape = shape
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else GhaisNoir.BorderCard,
                shape
            )
            .noirClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
