package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.FavoritesStore
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Redesign — "Collections" as one slim icon strip (minimal).
 *
 * Four equal cells (icon well + label + live sub-line) inside a single
 * tinted plate: ~90dp tall instead of a 348dp tile wall. Same four
 * destinations, nothing hidden, nothing repeated.
 */
@Composable
fun HomePlaylistsRow(
    onFavourites: () -> Unit,
    onFocusWork: () -> Unit,
    onNightSleep: () -> Unit,
    onDownloads: () -> Unit,
) {
    val favorites by FavoritesStore.favoriteTracks.collectAsState()

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
            icon = Icons.Filled.Headphones,
            contentDescription = "Focus and Work",
            label = "Focus",
            sub = "Deep work",
            onClick = onFocusWork,
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
            sub = "Saved",
            onClick = onDownloads,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Compact strip cell: 44dp icon well, 11sp label, 10sp live sub-line. */
@Composable
private fun NoirCollectionCell(
    icon: ImageVector,
    contentDescription: String,
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(GhaisShapes.row)
            .noirClickable(onClick)
            .padding(vertical = 4.dp)
    ) {
        IconWell(
            icon = icon,
            size = 44.dp,
            iconSize = 20.dp,
            contentDescription = contentDescription
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = GhaisNoir.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = sub,
            color = GhaisNoir.TextTertiary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
