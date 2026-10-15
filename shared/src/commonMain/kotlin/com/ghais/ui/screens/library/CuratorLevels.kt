package com.ghais.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * AGENT A11 — Curator levels.
 *
 * Pure core: [curatorScore] + [curatorLevel]. UI: [CuratorBadge].
 * New file only — LibraryScreen/ProfileScreen wire it via the insertion
 * snippets in the agent report.
 *
 * Scoring: one created playlist weighs 10 saved tracks, so both inputs stay
 * meaningful despite their different magnitudes:
 *
 *   score = playlistsCreated * 10 + tracksSaved   (negatives clamp to 0)
 *
 * Thresholds: BRONZE 0 ("Curator I"), SILVER 25 ("Curator II"),
 * GOLD 75 ("Curator III"), PLATINUM 150 ("Curator IV").
 */
enum class CuratorLevel(val title: String, val minScore: Int) {
    BRONZE("Curator I", 0),
    SILVER("Curator II", 25),
    GOLD("Curator III", 75),
    PLATINUM("Curator IV", 150)
}

/** Combined curator score (see file kdoc). Pure, never throws. */
fun curatorScore(playlistsCreated: Int, tracksSaved: Int): Int {
    val playlists = playlistsCreated.coerceAtLeast(0)
    val tracks = tracksSaved.coerceAtLeast(0)
    return playlists * 10 + tracks
}

/** Level for the given raw counts. Pure, never throws. */
fun curatorLevel(playlistsCreated: Int, tracksSaved: Int): CuratorLevel {
    val score = curatorScore(playlistsCreated, tracksSaved)
    var level = CuratorLevel.BRONZE
    for (candidate in CuratorLevel.entries) {
        if (score >= candidate.minScore) level = candidate
    }
    return level
}

/** Next level above the current one, or null at PLATINUM. Pure. */
fun nextCuratorLevel(playlistsCreated: Int, tracksSaved: Int): CuratorLevel? {
    val score = curatorScore(playlistsCreated, tracksSaved)
    return CuratorLevel.entries.firstOrNull { it.minScore > score }
}

/**
 * 0..1 progress from the current level's floor to the next level's floor.
 * 1f at PLATINUM (nothing above). Pure.
 */
fun curatorProgress(playlistsCreated: Int, tracksSaved: Int): Float {
    val score = curatorScore(playlistsCreated, tracksSaved)
    val current = curatorLevel(playlistsCreated, tracksSaved)
    val next = CuratorLevel.entries.firstOrNull { it.minScore > score } ?: return 1f
    val span = (next.minScore - current.minScore).coerceAtLeast(1)
    return ((score - current.minScore).toFloat() / span).coerceIn(0f, 1f)
}

/**
 * Noir badge chip: chrome star well + "CURATOR II" label + score caption.
 * Zero hue — state reads through elevation + label opacity only.
 */
@Composable
fun CuratorBadge(
    level: CuratorLevel,
    playlistsCreated: Int,
    tracksSaved: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(GhaisNoir.Fill2, GhaisShapes.pill)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(GhaisNoir.chromeFill(), GhaisShapes.well)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = GhaisNoir.OnChrome,
                modifier = Modifier.width(14.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = level.title.uppercase(),
            color = GhaisNoir.TextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${curatorScore(playlistsCreated, tracksSaved)} pts",
            color = GhaisNoir.TextTertiary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
