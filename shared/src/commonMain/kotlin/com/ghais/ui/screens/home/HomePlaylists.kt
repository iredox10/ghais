package com.ghais.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.repository.HifzListeningPlannerStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisColors
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Redesign — "Collections" as one slim icon strip (minimal) plus an optional
 * "Today's Plan" quick-launch bar whenever an active listening plan still has
 * remaining sessions today.
 */
@Composable
fun HomePlaylistsRow(
    onFavourites: () -> Unit,
    onPlanner: () -> Unit,
    onNightSleep: () -> Unit,
    onDownloads: () -> Unit,
) {
    val favorites by FavoritesStore.favoriteTracks.collectAsState()
    val downloadedKeys by QuranDownloads.downloadedKeys.collectAsState()
    val listeningPlans by HifzListeningPlannerStore.plans.collectAsState()
    val offlineCount = remember(downloadedKeys) {
        downloadedKeys.count { key -> key.count { it == '/' } == 1 }
    }

    val epochDay = HifzListeningPlannerStore.currentEpochDay()
    val activePlan = remember(listeningPlans) {
        listeningPlans.firstOrNull { it.enabled } ?: listeningPlans.firstOrNull()
    }
    val planProgress = activePlan?.dailyProgressFraction(epochDay) ?: 0f
    val planCompleted = activePlan != null && planProgress >= 1f
    val planStreak = activePlan?.streakDays(epochDay) ?: 0
    val planDone = activePlan?.sessionsDoneOn(epochDay) ?: 0
    val planEstMin = activePlan?.estimatedMinutesPerSession() ?: 0

    val plannerSub = remember(activePlan, planProgress, planStreak, planDone, planEstMin) {
        when {
            activePlan == null -> "Daily plan"
            planCompleted -> "✓ Done today"
            planStreak > 0 -> "🔥${planStreak}d • $planDone/${activePlan.sessionsPerDay}"
            else -> "$planDone/${activePlan.sessionsPerDay} • ~${planEstMin}m"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GhaisNoir.tintFill(), GhaisShapes.cardNoir)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                .topSpecular(inset = 24.dp)
                .padding(horizontal = 6.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirCollectionCell(
                icon = Icons.Filled.Star,
                contentDescription = "Favourites",
                label = "Favourites",
                sub = "${favorites.size} saved",
                onClick = onFavourites,
                modifier = Modifier.weight(1f)
            )
            NoirCollectionCell(
                icon = Icons.Filled.CalendarMonth,
                contentDescription = "Listening Planner",
                label = "Planner",
                sub = plannerSub,
                iconTint = if (planCompleted) GhaisColors.MintGlow else GhaisNoir.Lavender,
                ringProgress = if (activePlan != null) planProgress else null,
                ringCompleted = planCompleted,
                onClick = onPlanner,
                modifier = Modifier.weight(1f)
            )
            NoirCollectionCell(
                icon = Icons.Filled.Bedtime,
                contentDescription = "Night and Sleep",
                label = "Sleep",
                sub = "Night",
                onClick = onNightSleep,
                modifier = Modifier.weight(1f)
            )
            NoirCollectionCell(
                icon = Icons.Filled.Download,
                contentDescription = "Downloads",
                label = "Offline",
                sub = "$offlineCount saved",
                onClick = onDownloads,
                modifier = Modifier.weight(1f)
            )
        }

        if (activePlan != null && !planCompleted) {
            val remaining = (activePlan.sessionsPerDay - planDone).coerceAtLeast(1)
            val planTitle = remember(activePlan.id, activePlan.title, activePlan.surahIds) {
                activePlan.title.trim().takeIf { it.isNotEmpty() } ?: run {
                    val first = activePlan.surahIds.firstOrNull()?.let { id ->
                        QuranDataRepository.getSurahById(id)?.nameEn
                    } ?: "Daily Recitation"
                    if (activePlan.surahIds.size > 1) "$first +${activePlan.surahIds.size - 1}" else first
                }
            }
            TodaysPlanQuickLaunchStrip(
                title = planTitle,
                remainingSessions = remaining,
                estimatedMinutes = planEstMin,
                streakDays = planStreak,
                onOpenPlanner = onPlanner,
                onPlayNow = {
                    val tracks = HifzListeningPlannerStore.buildSessionTracks(activePlan)
                    if (tracks.isNotEmpty()) {
                        AudioEngine.playListeningPlan(activePlan.id, tracks)
                    } else {
                        onPlanner()
                    }
                }
            )
        }
    }
}

@Composable
private fun TodaysPlanQuickLaunchStrip(
    title: String,
    remainingSessions: Int,
    estimatedMinutes: Int,
    streakDays: Int,
    onOpenPlanner: () -> Unit,
    onPlayNow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.cardNoir)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        GhaisNoir.LavenderDeep.copy(alpha = 0.42f),
                        GhaisNoir.CardTintBottom
                    )
                )
            )
            .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.35f), GhaisShapes.cardNoir)
            .topSpecular(inset = 20.dp)
            .noirClickable(onOpenPlanner)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TODAY'S PLAN",
                    color = GhaisNoir.Lavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                if (streakDays > 0) {
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Lavender.copy(alpha = 0.16f))
                            .border(1.dp, GhaisNoir.Lavender.copy(alpha = 0.30f), GhaisShapes.pill)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "🔥 ${streakDays}d",
                            color = GhaisNoir.Lavender,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$remainingSessions ${if (remainingSessions == 1) "session" else "sessions"} left • ~$estimatedMinutes min",
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.lavenderFill())
                .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                .noirClickable(onPlayNow)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play today's plan",
                tint = GhaisNoir.LavenderInk,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Play Now",
                color = GhaisNoir.LavenderInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** Compact strip cell: 44dp icon well (with optional animated progress ring), 11sp label, 11sp live sub-line. */
@Composable
private fun NoirCollectionCell(
    icon: ImageVector,
    contentDescription: String,
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = GhaisNoir.TextPrimary,
    ringProgress: Float? = null,
    ringCompleted: Boolean = false,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (ringProgress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550),
        label = "planner_ring_progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(GhaisShapes.row)
            .noirClickable(onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            if (ringProgress != null) {
                val ringColor = if (ringCompleted) GhaisColors.MintGlow else GhaisNoir.Lavender
                Canvas(modifier = Modifier.size(50.dp)) {
                    val strokePx = 2.5.dp.toPx()
                    if (ringCompleted) {
                        drawCircle(
                            color = GhaisColors.MintGlow.copy(alpha = 0.18f),
                            radius = size.minDimension / 2f
                        )
                    }
                    drawArc(
                        color = GhaisNoir.BorderCard,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokePx)
                    )
                    if (animatedProgress > 0f) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
            }
            IconWell(
                icon = icon,
                size = 44.dp,
                iconSize = 20.dp,
                contentDescription = contentDescription,
                tint = iconTint,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = GhaisNoir.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = sub,
            color = if (ringCompleted) GhaisColors.MintGlow else GhaisNoir.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
