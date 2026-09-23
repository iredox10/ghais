package com.ghais.ui.screens.reciters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton

/**
 * Reciter hero action cluster — Play All (chrome), Shuffle + Follow (ghost),
 * download-remaining (ghost). Noir tokens only; no captions.
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChromePillButton(
            text = "Play All",
            onClick = onPlayAll,
            modifier = Modifier.fillMaxWidth(),
            enabled = playEnabled,
            leadingIcon = Icons.Default.PlayArrow
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GhostPillButton(
                text = "Shuffle",
                onClick = onShuffle,
                modifier = Modifier.weight(1f)
            )
            GhostPillButton(
                text = if (isFollowing) "Following" else "Follow",
                onClick = onToggleFollow,
                modifier = Modifier.weight(1f),
                active = isFollowing
            )
        }

        GhostPillButton(
            text = when {
                allDone -> "Downloaded"
                downloadingCount > 0 -> "Downloading ($downloadingCount/$surahCount)"
                else -> "Download ${surahCount - downloadedCount} remaining"
            },
            onClick = { if (!allDone) onDownloadAll() },
            modifier = Modifier.fillMaxWidth(),
            active = allDone
        )
    }
}
