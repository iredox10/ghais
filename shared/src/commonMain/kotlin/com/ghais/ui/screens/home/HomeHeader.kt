package com.ghais.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.UserUsageRepository
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.ghais_mark
import org.jetbrains.compose.resources.painterResource

/**
 * Phase 4 — Noir Home header.
 *
 * Logo left; search + analytics wells and the user avatar right (avatar opens
 * the profile tab). Editorial headline below stays untouched.
 */
@Composable
fun HomeTopBar(
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    val session by AuthRepository.session.collectAsState()
    val cached by AuthRepository.cachedSession.collectAsState()
    val displayName = session?.name?.ifBlank { null }
        ?: cached?.name?.ifBlank { null }
        ?: ""
    val monogram = displayName.firstOrNull { it.isLetter() }?.uppercase() ?: "G"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo mark in a clay well.
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(GhaisNoir.wellFill(), GhaisShapes.well)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ghais_mark),
                contentDescription = "Ghais",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeHeaderWell(
                contentDescription = "Search",
                onClick = onSearchClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            HomeHeaderWell(
                contentDescription = "Listening stats",
                onClick = onStatsClick
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            // User avatar → profile tab.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.SpecularTop, CircleShape)
                    .noirClickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monogram,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HomeHeaderWell(
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(GhaisNoir.wellFill(), GhaisShapes.well)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well)
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
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
