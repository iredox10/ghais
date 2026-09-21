package com.ghais.ui.screens.curated

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.seed.CuratedPlaylist
import com.ghais.data.seed.QuranDataRepository
import com.ghais.data.seed.StitchAssets
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.QuranifyColors

/**
 * All Curated Playlists Screen.
 * Opened when the user clicks "See All" in the Curated for Peace section.
 * Designed in Black & White + Trending Purple palette.
 */
class AllCuratedPlaylistsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        var searchQuery by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }

        val categoryOptions = remember {
            listOf("All", "Focus", "Peace", "Morning", "Night", "Healing")
        }

        val allPlaylists = remember {
            StitchAssets.AllCuratedPlaylists
        }

        val filteredPlaylists = remember(searchQuery, selectedCategory, allPlaylists) {
            allPlaylists.filter { playlist ->
                // Category filtering
                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Focus" -> playlist.category.equals("Focus", ignoreCase = true) ||
                               playlist.tag.contains("Focus", ignoreCase = true) ||
                               playlist.tag.contains("Tartil", ignoreCase = true) ||
                               playlist.title.contains("Focus", ignoreCase = true) ||
                               playlist.title.contains("Study", ignoreCase = true)
                    "Peace" -> playlist.category.equals("Peace", ignoreCase = true) ||
                               playlist.tag.contains("Peace", ignoreCase = true) ||
                               playlist.tag.contains("Emotional", ignoreCase = true) ||
                               playlist.title.contains("Peace", ignoreCase = true) ||
                               playlist.title.contains("Soothing", ignoreCase = true) ||
                               playlist.title.contains("Tranquility", ignoreCase = true)
                    "Morning" -> playlist.category.equals("Morning", ignoreCase = true) ||
                                 playlist.tag.contains("Morning", ignoreCase = true) ||
                                 playlist.title.contains("Morning", ignoreCase = true) ||
                                 playlist.title.contains("Adhkar", ignoreCase = true) ||
                                 playlist.title.contains("Sunrise", ignoreCase = true)
                    "Night" -> playlist.category.equals("Night", ignoreCase = true) ||
                               playlist.tag.contains("Night", ignoreCase = true) ||
                               playlist.tag.contains("Sleep", ignoreCase = true) ||
                               playlist.title.contains("Night", ignoreCase = true) ||
                               playlist.title.contains("Bedtime", ignoreCase = true) ||
                               playlist.title.contains("Tahajjud", ignoreCase = true) ||
                               playlist.title.contains("Qiyam", ignoreCase = true)
                    "Healing" -> playlist.category.equals("Healing", ignoreCase = true) ||
                                 playlist.tag.contains("Healing", ignoreCase = true) ||
                                 playlist.tag.contains("Shifa", ignoreCase = true) ||
                                 playlist.title.contains("Healing", ignoreCase = true) ||
                                 playlist.title.contains("Shifa", ignoreCase = true) ||
                                 playlist.title.contains("Relief", ignoreCase = true)
                    else -> true
                }

                // Instant text query filtering (title, description/subtitle, or mood tag)
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim().lowercase()
                    playlist.title.lowercase().contains(q) ||
                    playlist.subtitle.lowercase().contains(q) ||
                    playlist.tag.lowercase().contains(q) ||
                    playlist.category.lowercase().contains(q)
                }

                matchesCategory && matchesQuery
            }
        }

        Scaffold(
            containerColor = QuranifyColors.PitchBlack
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(QuranifyColors.PitchBlack)
            ) {
                // Ambient Top Aura: Electric Violet fading into deep Pitch Black
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    QuranifyColors.TrendingPurple.copy(alpha = 0.20f),
                                    QuranifyColors.ElectricViolet.copy(alpha = 0.08f),
                                    QuranifyColors.PitchBlack.copy(alpha = 0.0f)
                                ),
                                center = Offset(250f, -40f),
                                radius = 480f
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top bar with Back button and Title "Curated Collections"
                    CuratedCollectionsTopBar(
                        totalCount = filteredPlaylists.size,
                        onBackClick = { navigator.pop() }
                    )

                    // Search bar with instant text filtering
                    CuratedSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    // Category chips row: "All", "Focus", "Peace", "Morning", "Night", "Healing"
                    CategoryChipsRow(
                        categories = categoryOptions,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2-Column Grid of curated playlist cards with 120.dp bottom padding
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 120.dp // Requirement: 120.dp bottom padding
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (filteredPlaylists.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                EmptyCuratedPlaylistsState(
                                    query = searchQuery,
                                    onResetFilters = {
                                        searchQuery = ""
                                        selectedCategory = "All"
                                    }
                                )
                            }
                        } else {
                            items(
                                items = filteredPlaylists,
                                key = { it.id }
                            ) { playlist ->
                                CuratedPlaylistCard(
                                    playlist = playlist,
                                    onCardClick = {
                                        rootNavigator.push(CuratedPlaylistDetailScreen(playlist.id))
                                    },
                                    onPlayClick = {
                                        // Enqueue full list of tracks for this curated playlist
                                        val playlistTracks = QuranDataRepository.getCuratedTracksForPlaylist(playlist.id)
                                        if (playlistTracks.isNotEmpty()) {
                                            AudioEngine.playQueue(playlistTracks, startIndex = 0)
                                            rootNavigator.push(NowPlayingScreen())
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top bar with Back button (`navigator.pop()`), Title "Curated Collections" in bold white,
 * and collection count pill badge.
 */
@Composable
private fun CuratedCollectionsTopBar(
    totalCount: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF161822))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title: "Curated Collections" in bold white
        Text(
            text = "Curated Collections",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Dynamic Count Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(QuranifyColors.TrendingPurple.copy(alpha = 0.16f))
                .border(1.dp, QuranifyColors.TrendingPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$totalCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuranifyColors.TrendingPurple
            )
        }
    }
}

/**
 * Search bar with instant text filtering (by playlist title, description, or mood tag)
 * in frosted dark glass container with Trending Purple / white micro-border.
 */
@Composable
private fun CuratedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF13151D).copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        QuranifyColors.TrendingPurple.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.15f),
                        QuranifyColors.TrendingPurple.copy(alpha = 0.30f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) QuranifyColors.TrendingPurple else Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search playlists, moods, or tags...",
                        color = Color(0xFF6B7280),
                        fontSize = 13.5.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(QuranifyColors.TrendingPurple),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262835))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Category chips row: "All", "Focus", "Peace", "Morning", "Night", "Healing"
 * with active chip highlighted in Trending Purple.
 */
@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    QuranifyColors.ElectricViolet,
                                    QuranifyColors.TrendingPurple
                                )
                            )
                        } else {
                            SolidColor(Color(0xFF14161F))
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            Color(0xFFC084FC).copy(alpha = 0.5f)
                        } else {
                            Color.White.copy(alpha = 0.10f)
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = category,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF9CA3AF)
                )
            }
        }
    }
}

/**
 * 2-column Grid Curated Playlist Card:
 * - Artwork image with rounded corners (16.dp) and frosted glass border.
 * - Mood tag pill in top-right.
 * - Circular floating play button in Trending Purple in bottom-right.
 * - Playlist title in bold white, description in muted grey.
 * - Track count and duration pill.
 * - Clicking the card pushes `CuratedPlaylistDetailScreen(playlist.id)` via `navigator.push(...)`.
 */
@Composable
private fun CuratedPlaylistCard(
    playlist: CuratedPlaylist,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF12141C))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        QuranifyColors.TrendingPurple.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onCardClick() }
            .padding(10.dp)
    ) {
        // Artwork container with rounded corners (16.dp) and frosted glass border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181A24))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            QuranifyColors.TrendingPurple.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            AsyncImage(
                model = playlist.coverUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient shadow overlay at the bottom for high contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.70f)
                            )
                        )
                    )
            )

            // Mood tag pill in top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD0B0C0E))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                QuranifyColors.TrendingPurple.copy(alpha = 0.75f),
                                Color.White.copy(alpha = 0.35f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = playlist.tag,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Circular floating play button in Trending Purple in bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                QuranifyColors.ElectricViolet,
                                QuranifyColors.TrendingPurple
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.40f),
                        shape = CircleShape
                    )
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Playlist title in bold white
        Text(
            text = playlist.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Description in muted grey
        Text(
            text = playlist.subtitle,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Track count and duration pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${playlist.trackCount} tracks • ${playlist.durationText}",
                color = Color(0xFFD1D5DB),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Empty state displayed when search or category filter has no matches.
 */
@Composable
private fun EmptyCuratedPlaylistsState(
    query: String,
    onResetFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF161824)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = QuranifyColors.TrendingPurple,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (query.isNotBlank()) "No playlists found for \"$query\"" else "No playlists found",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Try searching for a different mood, title, or resetting category filter",
            color = Color(0xFF9CA3AF),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(QuranifyColors.TrendingPurple.copy(alpha = 0.16f))
                .border(1.dp, QuranifyColors.TrendingPurple.copy(alpha = 0.40f), RoundedCornerShape(20.dp))
                .clickable { onResetFilters() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Reset Filters",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
