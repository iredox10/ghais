package com.ghais.ui.screens.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
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
import com.ghais.data.repository.UserUsageRepository
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

        val topSurahs = remember(history) {
            history.filter { it.title.isNotBlank() }
                .groupingBy { it.title }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(3)
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
                    NoirCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "This week",
                                color = GhaisNoir.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "minutes per day",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val maxVal = (weekMinutes.maxOrNull() ?: 0f).coerceAtLeast(1f)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                val barCount = 7
                                val gap = 8.dp.toPx()
                                val barWidth = (size.width - gap * (barCount - 1)) / barCount
                                val corner = 6.dp.toPx()
                                weekMinutes.forEachIndexed { index, value ->
                                    val fraction = (value / maxVal).coerceIn(0f, 1f)
                                    val barHeight = (size.height * fraction)
                                        .coerceAtLeast(if (value > 0f) 8.dp.toPx() else 4.dp.toPx())
                                    val left = index * (barWidth + gap)
                                    val top = size.height - barHeight
                                    drawRoundRect(
                                        color = if (index == 6) {
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
                            val weekTotal = weekMinutes.sum()
                            val weekGoal = (goalMin * 7f).coerceAtLeast(1f)
                            NoirSegmentedProgress(
                                progress = (weekTotal / weekGoal).coerceIn(0f, 1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (weekTotal > 0f) {
                                    "${weekTotal.toInt()} min this week"
                                } else {
                                    "No activity yet this week"
                                },
                                color = GhaisNoir.TextTertiary,
                                fontSize = 12.sp
                            )
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
                                            text = entry.key,
                                            color = GhaisNoir.TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${entry.value} plays",
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
