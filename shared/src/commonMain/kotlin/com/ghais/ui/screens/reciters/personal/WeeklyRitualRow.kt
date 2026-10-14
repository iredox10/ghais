package com.ghais.ui.screens.reciters.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.domain.model.Reciter
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.screens.reciters.ReciterReelCard
import com.ghais.ui.theme.GhaisNoir

/**
 * AGENT P8 — "New for you" weekly ritual rail (Noir).
 *
 * Renders NOTHING — including its own spacing — when [reciters] is empty, so
 * the host layout never gaps (same rule as `TopQarisRail`). Cards reuse the
 * profile reel-card recipe ([ReciterReelCard]): glass fill + ghost border +
 * chrome play disc, so the ritual shelf reads as family with the nation reels.
 *
 * Pure view over [weeklyPicks] output: the coordinator owns the taste/history
 * wiring and passes the resolved lineup in; this file only paints the title
 * ([ritualTitle]), the "Fresh every Monday" appointment caption, and the rail.
 */
@Composable
fun WeeklyRitualRow(
    reciters: List<Reciter>,
    weekSeed: Long,
    photoFor: (String) -> String?,
    onOpenReciter: (String) -> Unit,
    onPlayReciter: (Reciter) -> Unit,
    modifier: Modifier = Modifier,
    isFollowing: (String) -> Boolean = { false },
    followerCount: (String) -> Long? = { null },
    onFollowClick: (String) -> Unit = {},
) {
    if (reciters.isEmpty()) return

    Column(modifier = modifier) {
        NoirSectionHeader(label = ritualTitle(weekSeed))
        Text(
            text = "Fresh every Monday",
            color = GhaisNoir.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(reciters, key = { it.catalogKey() }) { reciter ->
                ReciterReelCard(
                    reciter = reciter,
                    photoUrl = photoFor(reciter.slug),
                    onClick = { onOpenReciter(reciter.slug) },
                    onPlayClick = { onPlayReciter(reciter) },
                    isFollowing = isFollowing(reciter.slug),
                    onFollowClick = { onFollowClick(reciter.slug) },
                    followerCount = followerCount(reciter.slug),
                )
            }
        }
    }
}
