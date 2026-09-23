package com.ghais.ui.screens.reciters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ghais.ui.components.noir.ChromeFab
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Reciter hero action cluster — minimal icon cluster.
 * Centered 64dp chrome play circle + row of 3 ghost wells (48dp targets).
 * Noir tokens only; active states via fill/specular, zero hue.
 */
@Composable
fun ReciterHeroActions(
    playEnabled: Boolean,
    allDone: Boolean,
    downloadingCount: Int,
    surahCount: Int,
    downloadedCount: Int,
    isFollowing: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onToggleFollow: () -> Unit,
    onDownloadAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChromeFab(
            icon = Icons.Default.PlayArrow,
            onClick = { if (playEnabled) onPlayAll() },
            modifier = Modifier.alpha(if (playEnabled) 1f else 0.45f),
            size = 64.dp,
            contentDescription = "Play all"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostActionWell(
                icon = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                active = false,
                onClick = onShuffle
            )
            GhostActionWell(
                icon = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                contentDescription = if (isFollowing) "Unfollow" else "Follow",
                active = isFollowing,
                onClick = onToggleFollow
            )
            GhostActionWell(
                icon = if (allDone) Icons.Default.Done else Icons.Default.Download,
                contentDescription = if (allDone) "Downloaded" else "Download remaining",
                active = allDone,
                onClick = { if (!allDone) onDownloadAll() },
                progress = if (downloadingCount > 0 && surahCount > 0) {
                    (downloadedCount.toFloat() / surahCount.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun GhostActionWell(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    progress: Float? = null
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(GhaisShapes.well)
            .background(GhaisNoir.wellFill(), GhaisShapes.well)
            .border(
                1.dp,
                if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.well
            )
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        if (progress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(24.dp)
                    .height(3.dp)
                    .background(GhaisNoir.InsetFill, GhaisShapes.pill)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(GhaisNoir.TextPrimary, GhaisShapes.pill)
                )
            }
        }
    }
}
