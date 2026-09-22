package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.UserUsageRepository
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Phase 4 — Noir Home header.
 *
 * Editorial two-line headline ("Return to" light / "Your Quran" bold) matching
 * the Profile screen's "Your Space" treatment. The chart action is a clay well
 * instead of the old flat #1C1C1E circle, and the Premium entry is a ghost pill
 * so chromium stays reserved for the primary Resume CTA.
 */
@Composable
fun HomeTopBar(
    onPremiumClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostPillButton(text = "Premium", onClick = onPremiumClick)

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(GhaisNoir.wellFill(), GhaisShapes.well)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
                .noirClickable(onStatsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = "Listening stats",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Editorial greeting + today's listening chips. Streak/minutes are live from
 * UserUsageRepository so the header doubles as the at-a-glance summary.
 */
@Composable
fun NoirHomeGreeting() {
    val stats by UserUsageRepository.stats.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Return to",
            style = GhaisTypography.displayEditorial,
            maxLines = 1
        )
        Text(
            text = "Your Quran",
            style = GhaisTypography.displayEditorialBold,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Assalamu alaikum",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoirStatChip(
                text = if (stats.minutesToday == 1) "1 min today" else "${stats.minutesToday} min today"
            )
            NoirStatChip(
                text = if (stats.daysStreak == 1) "1 day streak" else "${stats.daysStreak} day streak"
            )
        }
    }
}
