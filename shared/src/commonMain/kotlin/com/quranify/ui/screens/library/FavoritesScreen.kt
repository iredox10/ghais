package com.quranify.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranify.domain.model.TrackItem
import com.quranify.ui.theme.QuranifyColors

object FavoritesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val favoriteTracks = listOf(
            TrackItem("alafasy", "Mishary Alafasy", 2, "Al-Baqarah", "البقرة", 255, "url", "Ayatul Kursi", 45000),
            TrackItem("sudais", "Al-Sudais", 36, "Ya-Sin", "يس", 1, "url", "Yasin", 3000)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QuranifyColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Favorites",
                    color = QuranifyColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Play All Button
            if (favoriteTracks.isNotEmpty()) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = QuranifyColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All", color = Color.White)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(favoriteTracks) { track ->
                    FavoriteTrackRow(track)
                }
            }
        }
    }
}

@Composable
fun FavoriteTrackRow(track: TrackItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(QuranifyColors.Card),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = QuranifyColors.Primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${track.surahNameEn} • Ayah ${track.ayahNo}",
                color = QuranifyColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = track.reciterName,
                color = QuranifyColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
