package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.FavoritesStore

// Self-contained tokens (pure-black design language)
private val DarkCard = Color(0xFF1C1C1E)
private val LinkBlue = Color(0xFF4C8DFF)

private val FocusGradientTop = Color(0xFF2E7CF6)
private val FocusGradientBottom = Color(0xFF0A1F44)
private val NightGradientTop = Color(0xFF3B2E7C)
private val NightGradientBottom = Color(0xFF0B0B18)

@Composable
fun HomePlaylistsRow(onFavourites: () -> Unit, onFocusWork: () -> Unit, onNightSleep: () -> Unit) {
    val favorites by FavoritesStore.favoriteTracks.collectAsState()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PlaylistShell(
                label = "Favourites",
                subtitle = "${favorites.size} saved",
                onClick = onFavourites,
                bgModifier = Modifier.background(DarkCard),
                artwork = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favourites",
                        tint = Color.White,
                        modifier = Modifier.size(104.dp)
                    )
                }
            )
        }
        item {
            PlaylistShell(
                label = "Focus & Work",
                subtitle = "Deep focus",
                onClick = onFocusWork,
                bgModifier = Modifier.background(
                    Brush.verticalGradient(
                        listOf(FocusGradientTop, FocusGradientBottom)
                    )
                ),
                artwork = {
                    ConcentricRings()
                }
            )
        }
        item {
            PlaylistShell(
                label = "Night & Sleep",
                subtitle = "Sleep well",
                onClick = onNightSleep,
                bgModifier = Modifier.background(
                    Brush.verticalGradient(
                        listOf(NightGradientTop, NightGradientBottom)
                    )
                ),
                artwork = {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = "Night and Sleep",
                        tint = Color.White,
                        modifier = Modifier.size(104.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun PlaylistShell(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    bgModifier: Modifier,
    artwork: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 168.dp, height = 200.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
    ) {
        // Centered oversized artwork, lifted slightly to leave room for bottom label
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-14).dp),
            contentAlignment = Alignment.Center,
            content = artwork
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ConcentricRings() {
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(84.dp)
                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
        )
    }
}
