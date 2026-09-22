package com.ghais.ui.screens.curated

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import com.ghais.data.seed.GhaisAssets
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/** True-grayscale filter — cover art stays recognisable while strictly monochrome. */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

private val NoirPlaylistCardShape = RoundedCornerShape(20.dp)
private val NoirPlaylistArtShape = RoundedCornerShape(14.dp)

/**
 * All Curated Playlists Screen — strict Noir Glass monochrome.
 *
 * Canvas #050506 via [NoirScreenRoot] (glow zone -> absolute black + grain).
 * Surfaces are alpha-white (cardFillSoft) + 1px [GhaisNoir.BorderCard] +
 * 22% top-only specular + diagonal sheen. Artwork is saturation-0 with a
 * black scrim so it reads engraved. Text ladder 100/62/38/24%. Zero hue.
 *
 * Signature, filter logic and navigation preserved:
 * `navigator.pop()` back, `rootNavigator.push(CuratedPlaylistDetailScreen(id))`
 * on card tap, `AudioEngine.playQueue(...)` + `NowPlayingScreen` on play.
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
            GhaisAssets.AllCuratedPlaylists
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

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                // Top bar — IconWell back + title + monochrome count chip
                CuratedCollectionsTopBar(
                    totalCount = filteredPlaylists.size,
                    onBackClick = { navigator.pop() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Editorial headline — light / bold two-line rhythm (home + reciters pattern)
                Text(
                    text = "Curated",
                    style = GhaisTypography.displayEditorialSmall,
                    maxLines = 1
                )
                Text(
                    text = "Collections",
                    style = GhaisTypography.displayEditorialBold.copy(fontSize = 30.sp, lineHeight = 36.sp),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Handpicked journeys for every mood",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search — engraved inset field
                CuratedSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category pills — chrome (selected) / ghost (resting)
                CategoryChipsRow(
                    categories = categoryOptions,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section label — shared Noir rhythm
                NoirSectionHeader(
                    label = "Playlists",
                    actionLabel = "${filteredPlaylists.size} shown",
                    onAction = {}
                )

                // 2-Column grid of Noir playlist cards with 120.dp bottom padding
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 2.dp,
                        bottom = 120.dp // MiniPlayer / dock clearance
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

/**
 * Top bar: clay [IconWell] back affordance, bold title,
 * monochrome count chip (Fill2 wash + ghost hairline, 62% text).
 */
@Composable
private fun CuratedCollectionsTopBar(
    totalCount: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.noirClickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            IconWell(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 40.dp,
                iconSize = 20.dp,
                contentDescription = "Back"
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Curated Collections",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GhaisNoir.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Monochrome count chip — informational, no press affordance.
        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$totalCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.TextSecondary
            )
        }
    }
}

/**
 * Search field in engraved inset style: carved-in recessed surface
 * (black 45% gradient + 5% rim), white cursor, 24% placeholder.
 */
@Composable
private fun CuratedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search playlists, moods, or tags...",
                    color = GhaisNoir.TextDisabled,
                    fontSize = 13.5.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = GhaisNoir.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(GhaisNoir.TextPrimary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .noirClickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = GhaisNoir.TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Filter pills: selected = chrome gradient fill + near-black label
 * (primary action); resting = Fill2 wash + card hairline + 62% label.
 */
@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable { onCategorySelected(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.OnChrome
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .noirClickable { onCategorySelected(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GhaisNoir.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * 2-column Noir playlist card:
 * - cardFillSoft + 1px BorderCard + top-only specular + diagonal sheen.
 * - Grayscale artwork in an engraved well + black scrim, mood tag pill
 *   top-end, chrome play disc bottom-end.
 * - Title 100%, subtitle 62%, meta pill ghost. Zero hue.
 *
 * Tapping the card pushes `CuratedPlaylistDetailScreen(playlist.id)`.
 */
@Composable
private fun CuratedPlaylistCard(
    playlist: CuratedPlaylist,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirPlaylistCardShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirPlaylistCardShape)
            .topSpecular(inset = 18.dp)
            .noirClickable(onClick = onCardClick)
            .padding(10.dp)
    ) {
        // Diagonal glass sheen swept across the card (monochrome only).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Artwork well — grayscale image + darkening scrim, engraved read
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(NoirPlaylistArtShape)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, NoirPlaylistArtShape)
            ) {
                if (playlist.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        colorFilter = NoirGrayscale,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Darkening scrim keeps the plate recessed instead of glowing.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = playlist.title.firstOrNull()?.uppercase() ?: "C",
                            color = GhaisNoir.TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom scrim for control legibility (monochrome only).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // Mood tag pill in top-end — smoked glass, 100% label.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(GhaisShapes.pill)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = playlist.tag,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Chrome play disc in bottom-end — raised chrome, dark glyph.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .noirClickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playlist title in 100% white
            Text(
                text = playlist.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Description in 62% grey
            Text(
                text = playlist.subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Track count and duration pill — ghost, informational only.
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${playlist.trackCount} tracks • ${playlist.durationText}",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Empty state — clay [IconWell], 100% headline + 62% hint,
 * chrome reset pill (primary action).
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
        IconWell(
            icon = Icons.Default.Search,
            size = 64.dp,
            iconSize = 30.dp,
            contentDescription = null,
            tint = GhaisNoir.TextTertiary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (query.isNotBlank()) "No playlists found for \"$query\"" else "No playlists found",
            color = GhaisNoir.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Try searching for a different mood, title, or resetting category filter",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.chromeFill())
                .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                .noirClickable(onClick = onResetFilters)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Reset Filters",
                color = GhaisNoir.OnChrome,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
