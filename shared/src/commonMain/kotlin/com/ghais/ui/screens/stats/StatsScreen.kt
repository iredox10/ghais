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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography
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

        val topQari = remember(history) {
            history.filter { it.reciterSlug.isNotBlank() }
                .groupingBy { it.reciterSlug }
                .eachCount()
                .maxByOrNull { it.value }
        }
        val topQariName = remember(topQari) {
            if (topQari == null) null
            else resolveFollowedQari(topQari.key)?.reciter?.nameEn ?: topQari.key
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
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GhaisNoir.Fill2)
                                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                                .noirClickable(onClick = { navigator.pop() }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Your", style = GhaisTypography.displayEditorial)
                    Text(text = "Stats", style = GhaisTypography.displayEditorialBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Track your journey",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NoirStatChip(text = "${favorites.size} Favorites")
                        NoirStatChip(text = "${followed.size} Following")
                        NoirStatChip(text = "${downloaded.size} Downloads")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NoirCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = totalHoursText,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Listening time",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (stats.totalSecondsListened > 0L) "total" else "total • start listening",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        NoirCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${stats.daysStreak}",
                                        color = GhaisNoir.TextPrimary,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = GhaisNoir.TextTertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Day streak",
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(text = "days", color = GhaisNoir.TextTertiary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NoirSectionHeader(label = "Today's goal")
                            Canvas(modifier = Modifier.size(96.dp)) {
                                val stroke = 10.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.12f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                if (ringProgress > 0f) {
                                    drawArc(
                                        brush = GhaisNoir.chromeFill(),
                                        startAngle = -90f,
                                        sweepAngle = 360f * ringProgress,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.minutesToday} min",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "of $goalMin min daily goal",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            NoirSegmentedProgress(progress = ringProgress)
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
                            Spacer(modifier = Modifier.height(12.dp))
                            val maxVal = (shownBars.maxOrNull() ?: 0f).coerceAtLeast(1f)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                val barCount = shownBars.size.coerceAtLeast(1)
                                val gap = (if (barCount > 12) 3.dp else 8.dp).toPx()
                                val barWidth = (size.width - gap * (barCount - 1)) / barCount
                                val corner = 6.dp.toPx()
                                shownBars.forEachIndexed { index, value ->
                                    val fraction = (value / maxVal).coerceIn(0f, 1f)
                                    val barHeight = (size.height * fraction)
                                        .coerceAtLeast(if (value > 0f) 8.dp.toPx() else 4.dp.toPx())
                                    val left = index * (barWidth + gap)
                                    val top = size.height - barHeight
                                    drawRoundRect(
                                        color = if (index == shownBars.lastIndex) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.28f)
                                        },
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(corner, corner)
                                    )
                                }
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
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (topQariName != null && topQari != null) {
                                Text(
                                    text = topQariName,
                                    color = GhaisNoir.TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${topQari.value} plays",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 12.sp
                                )
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
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = GhaisNoir.TextTertiary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(20.dp)
                                        )
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

/** Epoch day 0 (1970-01-01) was a Thursday. */
private fun weekdayShort(epochDay: Long): String {
    val idx = ((epochDay + 3) % 7 + 7) % 7
    return WEEKDAY_SHORT[idx.toInt()]
}

/** Range pill: chrome fill when selected, ghost wash otherwise. Zero hue. */
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
            .then(
                if (selected) {
                    Modifier.background(GhaisNoir.chromeFill())
                } else {
                    Modifier.background(GhaisNoir.Fill2)
                }
            )
            .border(
                1.dp,
                if (selected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                shape
            )
            .noirClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) GhaisNoir.OnChrome else GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
