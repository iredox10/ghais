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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.repository.UserUsageRepository
import com.ghais.ui.components.noir.NoirNavBar
import com.ghais.ui.components.noir.NoirNavButton
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
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
    val goalMinutes by OnboardingStore.dailyGoalMinutes.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Assalamu alaikum",
            color = GhaisNoir.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoirStatChip(text = "${stats.minutesToday}/${goalMinutes}m today")
            if (stats.daysStreak > 0) {
                NoirStatChip(
                    text = if (stats.daysStreak == 1) "1d streak" else "${stats.daysStreak}d streak"
                )
            }
        }
    }
}

/**
 * 1-tap Quran entry points on Home: Read Mushaf, Hifz Practice, and Sunnah (Al-Mulk).
 */
@Composable
fun HomeQuickAccessRow(
    onMushaf: () -> Unit,
    onHifz: () -> Unit,
    onSunnah: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionPill(
            icon = Icons.Filled.MenuBook,
            label = "Mushaf",
            onClick = onMushaf,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Filled.Psychology,
            label = "Hifz",
            onClick = onHifz,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Filled.AutoStories,
            label = "Al-Mulk",
            onClick = onSunnah,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = GhaisNoir.Lavender,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = GhaisNoir.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
