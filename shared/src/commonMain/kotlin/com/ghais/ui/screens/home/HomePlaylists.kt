package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
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
 * Phase 4 — "Collections".
 *
 * The three tiles used to be hue-coded (blue focus / violet sleep / flat grey).
 * They are now identical glass plates distinguished purely by claymorphic
 * artwork — a star well, engraved concentric rings, a moon well — so the
 * language stays strictly monochrome.
 */
@Composable
fun HomePlaylistsRow(onFavourites: () -> Unit, onFocusWork: () -> Unit, onNightSleep: () -> Unit) {
    val favorites by FavoritesStore.favoriteTracks.collectAsState()

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            NoirPlaylistTile(
                label = "Favourites",
                subtitle = "${favorites.size} saved",
                onClick = onFavourites,
                artwork = {
                    IconWell(
                        icon = Icons.Filled.Star,
                        size = 76.dp,
                        iconSize = 34.dp,
                        contentDescription = "Favourites"
                    )
                }
            )
        }
        item {
            NoirPlaylistTile(
                label = "Focus & Work",
                subtitle = "Deep focus",
                onClick = onFocusWork,
                artwork = { NoirEngravedRings(ringSize = 92.dp) }
            )
        }
        item {
            NoirPlaylistTile(
                label = "Night & Sleep",
                subtitle = "Sleep well",
                onClick = onNightSleep,
                artwork = {
                    IconWell(
                        icon = Icons.Filled.Bedtime,
                        size = 76.dp,
                        iconSize = 34.dp,
                        contentDescription = "Night and Sleep"
                    )
                }
            )
        }
    }
}

/** Glass playlist plate: oversized clay artwork lifted above the label block. */
@Composable
private fun NoirPlaylistTile(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    artwork: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 164.dp, height = 196.dp)
            .background(GhaisNoir.cardFill(), GhaisShapes.cardNoir)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular(inset = 28.dp)
            .noirClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp),
            contentAlignment = Alignment.Center,
            content = artwork
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = label,
                color = GhaisNoir.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
