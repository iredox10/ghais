package com.ghais.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlin.time.Clock

private val PureBlack = Color(0xFF000000)
private val MutedGrey = Color(0xFF9A9AA0)
private val GlassFill = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.08f)
private val AccentGreen = Color(0xFF4CAF7D)
private val AccentBlue = Color(0xFF4C8DFF)
private val FlameOrange = Color(0xFFFF9F43)

private const val DAILY_GOAL_MINUTES = 30f
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
            val hours = stats.totalSecondsListened / 3600.0
            if (hours < 0.1 && stats.totalSecondsListened > 0L) {
                "${stats.totalSecondsListened / 60L}m"
            } else {
                "${((hours * 10).toInt() / 10.0)}h"
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

        val ringProgress = (stats.minutesToday / DAILY_GOAL_MINUTES).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "Your Stats",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Track your journey",
                                color = MutedGrey,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text(
                                text = totalHoursText,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(text = "Listening time", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (stats.totalSecondsListened > 0L) "total" else "total • start listening",
                                color = MutedGrey,
                                fontSize = 12.sp
                            )
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${stats.daysStreak}",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = FlameOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(text = "Day streak", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "days", color = MutedGrey, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text(text = "Top Qari", color = MutedGrey, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (topQariName != null && topQari != null) {
                                Text(
                                    text = topQariName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${topQari.value} plays",
                                    color = MutedGrey,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "—",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "No plays yet",
                                    color = MutedGrey,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Canvas(modifier = Modifier.size(96.dp)) {
                                val stroke = 10.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.12f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = AccentGreen,
                                    startAngle = -90f,
                                    sweepAngle = 360f * ringProgress,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${stats.minutesToday} min",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "today's goal", color = MutedGrey, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "This week", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "minutes per day", color = MutedGrey, fontSize = 12.sp)
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
                                val barHeight = (size.height * fraction).coerceAtLeast(if (value > 0f) 8.dp.toPx() else 4.dp.toPx())
                                val left = index * (barWidth + gap)
                                val top = size.height - barHeight
                                drawRoundRect(
                                    color = if (index == 6) AccentGreen else AccentBlue.copy(alpha = 0.75f),
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(corner, corner)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val totalWeek = weekMinutes.sum()
                        Text(
                            text = if (totalWeek > 0f) "${totalWeek.toInt()} min this week" else "No activity yet this week",
                            color = MutedGrey,
                            fontSize = 12.sp
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${favorites.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "Favorites", color = MutedGrey, fontSize = 12.sp)
                        }
                        GlassCard(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${followed.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "Following", color = MutedGrey, fontSize = 12.sp)
                        }
                        GlassCard(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${downloaded.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = "Downloads", color = MutedGrey, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Top Surahs", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (topSurahs.isEmpty()) {
                            Text(text = "No surahs played yet", color = MutedGrey, fontSize = 13.sp)
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
                                        color = MutedGrey,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Text(
                                        text = entry.key,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${entry.value} plays",
                                        color = MutedGrey,
                                        fontSize = 12.sp
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

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}
