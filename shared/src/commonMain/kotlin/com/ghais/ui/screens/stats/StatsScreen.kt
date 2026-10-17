package com.ghais.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import com.ghais.data.share.ShareSheet
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
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.seed.JumpBackInItem
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirArtworkWell
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.reciters.rememberReciterPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.time.Clock

private const val DAY_MS = 86_400_000L

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
            val total = stats.totalSecondsListened
            when {
                total <= 0L -> "0s"
                total < 60L -> "${total}s"
                total < 3600L -> "${total / 60L}m"
                else -> {
                    val hours = total / 3600.0
                    "${((hours * 10).toInt() / 10.0)}h"
                }
            }
        }

        val qariPlayCounts = remember(history) {
            history.filter { it.reciterSlug.isNotBlank() }
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
        // All-time per-surah play counts (see UserUsageRepository.surahPlays
        // completion-or-30s rule). NOT range-filtered — the label below says
        // "Most played • all time" honestly instead of pretending per-range.
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

        val weekMinutes = remember(history) {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val todayStart = (nowMs / DAY_MS) * DAY_MS
            List(7) { offset ->
                val dayIndex = 6 - offset
                val dayStart = todayStart - dayIndex * DAY_MS
                val dayEnd = dayStart + DAY_MS
                history.filter { it.lastPlayedTimestampMs in dayStart until dayEnd }
                    .sumOf { item ->
                        val listenedMs = if (item.positionMs > 0L) {
                            item.positionMs
                        } else if (item.durationMs > 0L) {
                            (item.durationMs * item.progress).toLong()
                        } else {
                            0L
                        }
                        listenedMs / 60_000L
                    }.toFloat()
            }
        }

        val goalMin by com.ghais.data.repository.OnboardingStore.dailyGoalMinutes.collectAsState()
        val ringProgress = (stats.minutesToday / goalMin.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

        // Time-range selection. Contracts (sibling-owned):
        // UserUsageRepository.dailySeconds: StateFlow<Map<epochDay, seconds>> (this device).
        // UserUsageRepository.remoteDailySeconds: StateFlow<Map<epochDay, seconds>>
        //   (other devices only, own device excluded -> plain per-day sum, no double count).
        // Empty maps = contracts not yet populated -> fall back to history-derived
        // per-day minutes (same math as weekMinutes below).
        val dailyMap by UserUsageRepository.dailySeconds.collectAsState()
        val remoteDailyMap by UserUsageRepository.remoteDailySeconds.collectAsState()
        var range by remember { mutableStateOf(StatsRange.Week) }
        val todayEpochDay = remember { Clock.System.now().toEpochMilliseconds() / DAY_MS }

        // Combined per-day seconds: this device + other devices. Plain sum is
        // correct because the remote map excludes this device.
        val combinedDailySeconds: Map<Long, Long> = remember(dailyMap, remoteDailyMap) {
            if (remoteDailyMap.isEmpty()) dailyMap
            else {
                val merged = dailyMap.toMutableMap()
                for ((day, secs) in remoteDailyMap) {
                    if (secs > 0L) merged[day] = (merged[day] ?: 0L) + secs
                }
                merged
            }
        }

        val rangeMinutes: List<Float> = remember(range, combinedDailySeconds, history, weekMinutes) {
            if (range == StatsRange.Week && combinedDailySeconds.isEmpty()) {
                weekMinutes
            } else {
                List(range.days) { i ->
                    val epochDay = todayEpochDay - (range.days - 1 - i)
                    val mapped = combinedDailySeconds[epochDay]
                    if (mapped != null) mapped / 60f
                    else historyMinutesForEpochDay(history, epochDay)
                }
            }
        }

        val rangeTotalMin = rangeMinutes.sum()
        val rangeAvgMin = if (range.days > 0) rangeTotalMin / range.days else 0f
        val rangeActiveDays = rangeMinutes.count { it > 0f }
        val bestIndex = rangeMinutes.indices.maxByOrNull { rangeMinutes[it] }
        val bestMin = bestIndex?.let { rangeMinutes[it] } ?: 0f
        val bestDayLabel = bestIndex?.let {
            weekdayShort(todayEpochDay - (range.days - 1 - it))
        } ?: "—"
        val shownBars = remember(rangeMinutes) { rangeMinutes.takeLast(30) }
        val barsTruncated = rangeMinutes.size > shownBars.size
        val usingHistoryFallback = combinedDailySeconds.isEmpty()
        // Quiet multi-device note: remote minutes inside the current window.
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
            StatsRange.Week -> if (rangeTotalMin > 0f) {
                "${rangeTotalMin.toInt()} min this week"
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                        title = "Ghaith — My Quran Year",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoirStatChip(text = "${favorites.size} Favorites")
                        NoirStatChip(text = "${followed.size} Following")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    // Hero scorecard: lifetime total + today ring + streak in
                    // one plum plate (fitness-app summary language).
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GhaisNoir.plumFill(), GhaisShapes.cardLarge)
                            .border(
                                1.dp,
                                GhaisNoir.Lavender.copy(alpha = 0.35f),
                                GhaisShapes.cardLarge
                            )
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "YOUR QURAN JOURNEY",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = totalHoursText,
                                    color = Color.White,
                                    fontSize = 44.sp,
                                    lineHeight = 48.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (stats.totalSecondsListened > 0L) "total listening"
                                    else "total • start listening",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = GhaisNoir.Lavender,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${stats.daysStreak}-day streak",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // Today goal ring with center readout.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(104.dp)
                            ) {
                                Canvas(modifier = Modifier.size(104.dp)) {
                                    val stroke = 10.dp.toPx()
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.22f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    if (ringProgress > 0f) {
                                        drawArc(
                                            color = Color(0xFFC9BFF2),
                                            startAngle = -90f,
                                            sweepAngle = 360f * ringProgress,
                                            useCenter = false,
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(ringProgress * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${stats.minutesToday}/${goalMin}m",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

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

                item {
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = range.header,
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = range.window,
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                                // Goal tag — the chart's reference line, labeled.
                                if (range == StatsRange.Today || range == StatsRange.Week) {
                                    Text(
                                        text = "goal ${goalMin}m/day",
                                        color = GhaisNoir.Lavender,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = formatCompactMinutes(rangeTotalMin),
                                color = GhaisNoir.TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (rangeTotalMin > 0f) "listened" else "listened • start listening",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NoirStatChip(text = "${rangeAvgMin.toInt()}m / day")
                                NoirStatChip(
                                    text = if (bestMin > 0f) {
                                        "Best $bestDayLabel • ${bestMin.toInt()}m"
                                    } else {
                                        "Best —"
                                    }
                                )
                                // Streak is only meaningful for short windows.
                                if (range == StatsRange.Today || range == StatsRange.Week) {
                                    NoirStatChip(text = "${stats.daysStreak}-day streak")
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            var selectedBar by remember(range) {
                                mutableStateOf(bestIndex ?: (shownBars.size - 1).coerceAtLeast(0))
                            }
                            val weekLabels = remember(range, todayEpochDay) {
                                if (range == StatsRange.Week) {
                                    List(7) { i -> weekdayShort(todayEpochDay - (6 - i)) }
                                } else emptyList()
                            }
                            InsightBars(
                                values = shownBars,
                                labels = weekLabels,
                                goalMin = if (range == StatsRange.Today || range == StatsRange.Week) {
                                    goalMin.toFloat()
                                } else null,
                                selected = if (shownBars.isEmpty()) 0 else selectedBar.coerceIn(shownBars.indices),
                                onSelect = { selectedBar = it }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Selected-bar readout: the chart's one takeaway.
                            val selIdx = if (shownBars.isEmpty()) 0 else selectedBar.coerceIn(shownBars.indices)
                            val selLabel = weekLabels.getOrNull(selIdx)
                                ?: if (range == StatsRange.Today) "Today" else "Day ${selIdx + 1}"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Lavender)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$selLabel • ${shownBars.getOrNull(selIdx)?.toInt() ?: 0}m",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            NoirSegmentedProgress(progress = rangeProgress)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = rangeCaption,
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            if (barsTruncated) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Showing last 30 of ${range.days} days",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            if (usingHistoryFallback) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Daily breakdown pending — estimated from history",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            if (showRemoteCaption) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${remoteMinutesInRange.toInt()}m from other devices",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                item {
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Top Qari",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Most played • all time",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (topQaris.isNotEmpty()) {
                                topQaris.forEachIndexed { index, entry ->
                                    TopQariRow(
                                        rank = index + 1,
                                        slug = entry.first,
                                        name = entry.second,
                                        plays = entry.third
                                    )
                                    if (index < topQaris.lastIndex) {
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
                                    text = "—",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No plays yet",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

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
                            Spacer(modifier = Modifier.height(8.dp))
                            if (topSurahs.isEmpty()) {
                                Text(
                                    text = "No surahs played yet",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 13.sp
                                )
                            } else {
                                topSurahs.forEachIndexed { index, entry ->
                                    val surahRank = index + 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
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
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "#$surahRank",
                                                color = if (surahRank == 1) GhaisNoir.LavenderInk
                                                else GhaisNoir.TextSecondary,
                                                fontSize = 10.sp,
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
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 12.sp
                                        )
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
private fun TopQariRow(
    rank: Int,
    slug: String,
    name: String,
    plays: Int
) {
    val photo = rememberReciterPhoto(slug)
    if (rank == 1) {
        // Leader row: larger rounded artwork + lavender plays pill.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirArtworkWell(
                coverUrl = photo.orEmpty(),
                monogram = name.firstOrNull()?.uppercase() ?: "Q",
                shape = RoundedCornerShape(16.dp),
                size = 48.dp,
                monogramSize = 18.sp,
                ring = false,
                grayscale = false,
                scrimAlpha = 0f
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = name,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    text = "$plays plays",
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
            .padding(vertical = 6.dp),
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
        Spacer(modifier = Modifier.width(8.dp))
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
            text = "$plays plays",
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

/** History-derived per-day minutes (fallback while `dailySeconds` is unpopulated). */
private fun historyMinutesForEpochDay(history: List<JumpBackInItem>, epochDay: Long): Float {
    val dayStart = epochDay * DAY_MS
    val dayEnd = dayStart + DAY_MS
    return history.filter { it.lastPlayedTimestampMs in dayStart until dayEnd }
        .sumOf { item ->
            val listenedMs = if (item.positionMs > 0L) {
                item.positionMs
            } else if (item.durationMs > 0L) {
                (item.durationMs * item.progress).toLong()
            } else {
                0L
            }
            listenedMs / 60_000L
        }.toFloat()
}

private fun formatCompactMinutes(minutes: Float): String {
    val totalMin = minutes.toLong()
    if (totalMin <= 0L) return "0m"
    if (totalMin < 60L) return "${totalMin}m"
    val hours = totalMin / 60L
    val rem = totalMin % 60L
    return if (rem == 0L) "${hours}h" else "${hours}h ${rem}m"
}

private val WEEKDAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * Wrapped-style share card: total hours, day streak, top qari + top surah.
 * Shared via [ShareSheet.shareText] (common call; the iOS stub no-ops safely).
 */
private fun buildWrappedCard(
    totalHoursText: String,
    dayStreak: Int,
    topQariName: String?,
    topQariPlays: Long?,
    topSurahName: String?,
    topSurahPlays: Long?
): String = buildString {
    appendLine("Ghaith — My Quran Year")
    appendLine()
    appendLine("$totalHoursText listened • $dayStreak-day streak")
    appendLine()
    if (topQariName != null && topQariPlays != null) {
        appendLine("Top Qari: $topQariName ($topQariPlays plays)")
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

/** Epoch day 0 (1970-01-01) was a Thursday. */
private fun weekdayShort(epochDay: Long): String {
    val idx = ((epochDay + 3) % 7 + 7) % 7
    return WEEKDAY_SHORT[idx.toInt()]
}

/**
 * Interactive insight bars — the 2026 weekly-chart language: native layout
 * bars (labels align for free), a dashed goal reference line, gradient
 * best-day fill, and tap-to-inspect selection with a readout below.
 * Single takeaway per chart: the selected bar's day + minutes.
 */
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
    val maxH = 110.dp
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
                                .fillMaxWidth(0.55f)
                                .height(barH)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    brush = when {
                                        isSel -> GhaisNoir.lavenderFill()
                                        isBest -> Brush.verticalGradient(
                                            listOf(
                                                GhaisNoir.Lavender.copy(alpha = 0.55f),
                                                GhaisNoir.Lavender.copy(alpha = 0.22f)
                                            )
                                        )
                                        isToday -> Brush.verticalGradient(
                                            listOf(Color.White, Color.White)
                                        )
                                        else -> Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.28f),
                                                Color.White.copy(alpha = 0.28f)
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
                            fontSize = 10.sp,
                            fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        // Dashed goal reference line across the bar field.
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

/** Range pill: lavender fill when selected, ghost wash otherwise. */
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
