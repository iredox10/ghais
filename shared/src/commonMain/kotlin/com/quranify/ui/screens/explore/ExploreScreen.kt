package com.quranify.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.playlists.MoodPlaylist
import com.quranify.ui.screens.playlists.MoodPlaylists
import com.quranify.ui.screens.playlists.PlaylistCover
import com.quranify.ui.screens.playlists.PlaylistDetailsScreen

private val SystemBlack = Color(0xFF000000)
private val SystemGrey = Color(0xFF8E8E93)

/** Bento cell shape: true = wide banner (spans both columns). */
private fun MoodPlaylist.isWide(): Boolean =
    id == "favourites" || id == "most-beautiful"

object ExploreScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Explore",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        var query by remember { mutableStateOf("") }

        val cards = remember(query) {
            MoodPlaylists.filter {
                query.isBlank() ||
                    it.title.contains(query, true) ||
                    it.description.contains(query, true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBlack)
        ) {
            Text(
                text = "Explore",
                color = Color.White,
                fontSize = 33.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Search filters the cards
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = SystemGrey,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text(text = "Search playlists", color = SystemGrey, fontSize = 16.sp)
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards, key = { it.id }, span = { SpanFor(it) }) { playlist ->
                    BentoCard(
                        playlist = playlist,
                        onClick = { rootNavigator?.push(PlaylistDetailsScreen(playlist.id)) }
                    )
                }
            }
        }
    }
}

private fun SpanFor(playlist: MoodPlaylist): GridItemSpan =
    if (playlist.isWide()) GridItemSpan(2) else GridItemSpan(1)

@Composable
private fun BentoCard(playlist: MoodPlaylist, onClick: () -> Unit) {
    val wide = playlist.isWide()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (wide) Modifier.aspectRatio(2.15f)
                else Modifier.aspectRatio(0.80f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        PlaylistCover(art = playlist.art, modifier = Modifier.fillMaxSize())
        // Strong diagonal glass sheen over the artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.07f)
                        )
                    )
                )
        )
        // Top specular hairline inside the card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 20.dp)
                .offset(y = 8.dp)
                .background(Color.White.copy(alpha = 0.35f))
                .align(Alignment.TopCenter)
        )
        // Frosted glass title panel
        if (wide) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = playlist.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${playlist.surahIds.size} surahs",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = playlist.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
