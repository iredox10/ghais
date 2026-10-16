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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.UserUsageRepository
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisTypography

/**
 * Standard nav bar: avatar left (profile tab), centered "Home",
 * search + stats wells right.
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

    NoirNavBar(
        title = "Home",
        navigation = {
            // User avatar → profile tab.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.SpecularTop, CircleShape)
                    .noirClickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monogram,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            NoirNavButton(
                icon = Icons.Filled.Search,
                contentDescription = "Search",
                onClick = onSearchClick
            )
            Spacer(Modifier.width(8.dp))
            NoirNavButton(
                icon = Icons.Filled.BarChart,
                contentDescription = "Listening stats",
                onClick = onStatsClick
            )
        }
    )
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
