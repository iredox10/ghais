package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Redesign — "Collections" as a 2x2 tinted grid (AirPods-ref product tiles).
 *
 * The three tiles used to be hue-coded (blue focus / violet sleep / flat grey),
 * then monochrome glass. They are now lavender-washed plates in a two-column
 * grid with a fourth Downloads tile, so the section fills the width instead of
 * hiding content in a horizontal rail.
 */
@Composable
fun HomePlaylistsRow(
    onFavourites: () -> Unit,
    onFocusWork: () -> Unit,
    onNightSleep: () -> Unit,
    onDownloads: () -> Unit,
) {
    val favorites by FavoritesStore.favoriteTracks.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NoirPlaylistTile(
                label = "Favourites",
                subtitle = "${favorites.size} saved",
                onClick = onFavourites,
                modifier = Modifier.weight(1f),
                artwork = {
                    IconWell(
                        icon = Icons.Filled.Star,
                        size = 64.dp,
                        iconSize = 28.dp,
                        contentDescription = "Favourites"
                    )
                }
            )
            NoirPlaylistTile(
                label = "Focus & Work",
                subtitle = "Deep focus",
                onClick = onFocusWork,
                modifier = Modifier.weight(1f),
                artwork = { NoirEngravedRings(ringSize = 72.dp) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NoirPlaylistTile(
                label = "Night & Sleep",
                subtitle = "Sleep well",
                onClick = onNightSleep,
                modifier = Modifier.weight(1f),
                artwork = {
                    IconWell(
                        icon = Icons.Filled.Bedtime,
                        size = 64.dp,
                        iconSize = 28.dp,
                        contentDescription = "Night and Sleep"
                    )
                }
            )
            NoirPlaylistTile(
                label = "Downloads",
                subtitle = "Offline",
                onClick = onDownloads,
                modifier = Modifier.weight(1f),
                artwork = {
                    IconWell(
                        icon = Icons.Filled.Download,
                        size = 64.dp,
                        iconSize = 28.dp,
                        contentDescription = "Downloads"
                    )
                }
            )
        }
    }
}

/** Tinted grid plate: clay artwork above the label block. */
@Composable
private fun NoirPlaylistTile(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(168.dp)
            .background(GhaisNoir.tintFill(), GhaisShapes.cardNoir)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular(inset = 24.dp)
            .noirClickable(onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier.align(Alignment.TopStart),
            contentAlignment = Alignment.Center,
            content = artwork
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = label,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
