package com.ghais.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.explore.components.isWideCard
import com.ghais.ui.screens.playlists.MoodPlaylist
import com.ghais.ui.screens.playlists.MoodPlaylists
import com.ghais.ui.screens.playlists.PlaylistDetailsScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Phase 4 — Noir Explore.
 *
 * Strict Noir Glass monochrome on the shared canvas (glow zone -> #050506 +
 * ambient glow + film grain). Search/filter logic, category mapping, grid spans
 * and navigation targets are unchanged; only the surface language changed:
 * zero accent hue, alpha-white glass cards, top-only specular hairlines,
 * chrome pills and grayscale artwork. State reads through fill elevation,
 * weight and opacity.
 */
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

    private val Categories = listOf(
        "All",
        "Focus",
        "Work",
        "Sleep",
        "Peace",
        "Healing",
        "Night",
        "Barakah",
        "Reflection"
    )

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        var query by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }

        val filteredCards = remember(query, selectedCategory) {
            MoodPlaylists.filter { playlist ->
                val matchesQuery = query.isBlank() ||
                    playlist.title.contains(query, ignoreCase = true) ||
                    playlist.description.contains(query, ignoreCase = true)

                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Focus" -> playlist.id in listOf("study-focus", "focus-work")
                    "Work" -> playlist.id in listOf("focus-work", "study-focus")
                    "Sleep" -> playlist.id in listOf("sleep-mode", "tahajjud")
                    "Peace" -> playlist.id in listOf("heart-soothing", "most-beautiful")
                    "Healing" -> playlist.id in listOf("duaa-ruqia", "heart-soothing")
                    "Night" -> playlist.id in listOf("tahajjud", "sleep-mode")
                    "Barakah" -> playlist.id in listOf("sunrise-barakah", "favourites")
                    "Reflection" -> playlist.id in listOf("emotional", "most-beautiful")
                    else -> true
                }

                matchesQuery && matchesCategory
            }
        }

        NoirScreenRoot {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ---------------------------------------------------------
                // Editorial header + engraved search + chrome category chips
                // ---------------------------------------------------------
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NoirExploreHeader(
                            totalCount = MoodPlaylists.size,
                            visibleCount = filteredCards.size
                        )
                        Spacer(Modifier.height(16.dp))
                        NoirExploreSearch(
                            query = query,
                            onQueryChange = { query = it }
                        )
                        Spacer(Modifier.height(14.dp))
                        NoirCategoryReel(
                            categories = Categories,
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it }
                        )
                    }
                }

                if (filteredCards.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        NoirExploreEmpty()
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        NoirSectionHeader(label = "Collections • ${filteredCards.size}")
                    }
                    items(
                        items = filteredCards,
                        key = { it.id },
                        span = { playlist ->
                            if (playlist.isWideCard()) GridItemSpan(2) else GridItemSpan(1)
                        }
                    ) { playlist ->
                        NoirExploreCard(
                            playlist = playlist,
                            wide = playlist.isWideCard(),
                            onClick = { rootNavigator?.push(PlaylistDetailsScreen(playlist.id)) }
                        )
                    }
                }
            }
        }
    }
}

private val NoirExploreCardShape = RoundedCornerShape(28.dp)
private val NoirExploreAvatarShape = RoundedCornerShape(22.dp)

/** True-grayscale filter — artwork stays recognisable, strictly monochrome. */
private val NoirExploreGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Editorial two-line headline ("Explore" light / "Collections" bold) matching
 * the Home "Return to / Your Quran" and Reciters "Browse / Reciters"
 * treatment, with ghost count chips.
 */
@Composable
private fun NoirExploreHeader(
    totalCount: Int,
    visibleCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Explore",
            style = GhaisTypography.displayEditorial,
            maxLines = 1
        )
        Text(
            text = "Collections",
            style = GhaisTypography.displayEditorialBold,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Curated collections, deep focus routines & sacred themes",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoirExploreChip(
                text = if (totalCount == 1) "1 collection" else "$totalCount collections"
            )
            NoirExploreChip(
                text = if (visibleCount == 1) "1 shown" else "$visibleCount shown"
            )
        }
    }
}

/** Non-interactive ghost count chip — informational, no press affordance. */
@Composable
private fun NoirExploreChip(text: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Engraved search field — carved-in recessed surface (dark inset fill +
 * ~5% rim), search glyph at 38%, typed text at 100%, hint at 24%.
 */
@Composable
private fun NoirExploreSearch(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(GhaisShapes.row)
            .background(GhaisNoir.insetFill())
            .border(1.dp, GhaisNoir.InsetBorder, GhaisShapes.row)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(GhaisNoir.TextPrimary),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search collections, surahs & moods...",
                        color = GhaisNoir.TextDisabled,
                        fontSize = 15.sp
                    )
                }
                inner()
            },
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Category reel — selected chip is an extruded chrome pill (near-black
 * label), unselected chips are Fill2 washes with ghost rims. State reads
 * through elevation + label opacity, never hue.
 */
@Composable
private fun NoirCategoryReel(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable { onSelect(category) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
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
                        .heightIn(min = 36.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .noirClickable { onSelect(category) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
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
 * Noir bento card: soft glass fill + 1px card border + top-only specular.
 * Playing state reads through stronger wash + bright hairline, never hue.
 * Artwork is grayscale under a darkening scrim; monogram fallback when empty.
 */
@Composable
private fun NoirExploreCard(
    playlist: MoodPlaylist,
    wide: Boolean,
    onClick: () -> Unit
) {
    val currentTrack by AudioEngine.currentTrack.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val isCurrent = currentTrack?.let { it.surahId in playlist.surahIds } == true
    val activelyPlaying = isCurrent && isPlaying

    val fill = if (activelyPlaying) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    val border = if (activelyPlaying) GhaisNoir.SpecularTop else GhaisNoir.BorderCard

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirExploreCardShape)
            .background(fill)
            .border(1.dp, border, NoirExploreCardShape)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        // Sheen sweep + bottom scrim for depth (monochrome only).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )
        if (wide) {
            NoirExploreWideContent(
                playlist = playlist,
                activelyPlaying = activelyPlaying,
                isCurrent = isCurrent
            )
        } else {
            NoirExploreGridContent(
                playlist = playlist,
                activelyPlaying = activelyPlaying,
                isCurrent = isCurrent
            )
        }
    }
}

@Composable
private fun NoirExploreWideContent(
    playlist: MoodPlaylist,
    activelyPlaying: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoirExploreArtwork(
            playlist = playlist,
            size = 84.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoirExploreCategoryBadge(categoryForPlaylist(playlist.id))
                Spacer(modifier = Modifier.width(8.dp))
                NoirExplorePlayButton(
                    playlist = playlist,
                    activelyPlaying = activelyPlaying,
                    isCurrent = isCurrent,
                    size = 42.dp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = playlist.description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                color = GhaisNoir.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${playlist.surahIds.size} Surahs • ${styleForPlaylist(playlist.id)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NoirExploreGridContent(
    playlist: MoodPlaylist,
    activelyPlaying: Boolean,
    isCurrent: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoirExploreCategoryBadge(categoryForPlaylist(playlist.id))
            NoirExplorePlayButton(
                playlist = playlist,
                activelyPlaying = activelyPlaying,
                isCurrent = isCurrent,
                size = 38.dp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            NoirExploreArtwork(
                playlist = playlist,
                size = 76.dp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = playlist.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            color = GhaisNoir.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = playlist.description,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.5.sp,
            color = GhaisNoir.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${playlist.surahIds.size} Surahs • ${styleForPlaylist(playlist.id)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = GhaisNoir.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Recessed grayscale artwork plate with monogram fallback. */
@Composable
private fun NoirExploreArtwork(
    playlist: MoodPlaylist,
    size: androidx.compose.ui.unit.Dp
) {
    val coverUrl = coverForPlaylist(playlist.id)
    Box(
        modifier = Modifier
            .size(size)
            .clip(NoirExploreAvatarShape)
            .background(GhaisNoir.wellFill())
            .border(1.dp, GhaisNoir.BorderCard, NoirExploreAvatarShape),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                colorFilter = NoirExploreGrayscale,
                modifier = Modifier.fillMaxSize()
            )
            // Darkening scrim: keeps the plate recessed instead of glowing.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        } else {
            Icon(
                imageVector = Icons.Filled.AutoStories,
                contentDescription = playlist.title,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

/** Ghost category badge — informational, no press affordance, zero hue. */
@Composable
private fun NoirExploreCategoryBadge(category: String) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.uppercase(),
            color = GhaisNoir.TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
    }
}

/**
 * Play / pause pill: chrome when actively playing, smoked glass otherwise.
 * Starts a full-surah queue for the playlist; toggles pause/resume for the
 * current queue.
 */
@Composable
private fun NoirExplorePlayButton(
    playlist: MoodPlaylist,
    activelyPlaying: Boolean,
    isCurrent: Boolean,
    size: androidx.compose.ui.unit.Dp
) {
    val playModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (activelyPlaying) Modifier.background(GhaisNoir.chromeFill())
            else Modifier.background(Color.Black.copy(alpha = 0.70f))
        )
    Box(
        modifier = playModifier
            .border(
                1.dp,
                if (activelyPlaying) Color.White.copy(alpha = 0.35f)
                else GhaisNoir.BorderCard,
                CircleShape
            )
            .noirClickable {
                if (activelyPlaying) {
                    AudioEngine.pause()
                } else if (isCurrent) {
                    AudioEngine.resume()
                } else {
                    val tracks = tracksForPlaylist(playlist)
                    if (tracks.isNotEmpty()) {
                        AudioEngine.playQueue(tracks, startIndex = 0)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (activelyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (activelyPlaying) "Pause" else "Play",
            tint = if (activelyPlaying) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/** Ghost empty state — muted well + tertiary copy, no hue. */
@Composable
private fun NoirExploreEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirExploreCardShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirExploreCardShape)
            .topSpecular()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SelfImprovement,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No collections found",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = GhaisNoir.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try adjusting your search query or category filter.",
            fontSize = 13.sp,
            color = GhaisNoir.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------------
// Local playlist metadata (mirrors the legacy ExploreModularCard mapping
// minus accent hues — category/style/cover only, all grayscale at render).
// ------------------------------------------------------------------

private fun categoryForPlaylist(id: String): String = when (id) {
    "study-focus" -> "Focus"
    "focus-work" -> "Work"
    "sleep-mode" -> "Sleep"
    "heart-soothing" -> "Peace"
    "duaa-ruqia" -> "Healing"
    "tahajjud" -> "Night"
    "sunrise-barakah" -> "Barakah"
    "emotional" -> "Reflection"
    "most-beautiful" -> "Tranquility"
    "favourites" -> "Devotion"
    else -> "Quran"
}

private fun styleForPlaylist(id: String): String = when (id) {
    "study-focus" -> "Tartil"
    "focus-work" -> "Flow"
    "sleep-mode" -> "Calm"
    "heart-soothing" -> "Solace"
    "duaa-ruqia" -> "Shifa"
    "tahajjud" -> "Qiyam"
    "sunrise-barakah" -> "Barakah"
    "emotional" -> "Mujawwad"
    "most-beautiful" -> "Murattal"
    "favourites" -> "Curated"
    else -> "Recitation"
}

private fun coverForPlaylist(id: String): String = when (id) {
    "study-focus" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "mindful-hifz" }?.coverUrl
        ?: GhaisAssets.LibraryMorningCover
    "focus-work" -> GhaisAssets.AllCuratedPlaylists.firstOrNull()?.coverUrl
        ?: GhaisAssets.LibraryMorningCover
    "sleep-mode" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "tahajjud-night-qiyam" }?.coverUrl
        ?: GhaisAssets.LibraryTahajjudCover
    "heart-soothing" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "anxiety-relief-inshirah" }?.coverUrl
        ?: GhaisAssets.LibraryMorningCover
    "duaa-ruqia" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "ayat-ash-shifa" }?.coverUrl
        ?: GhaisAssets.LibraryTahajjudCover
    "tahajjud" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "tahajjud-night-qiyam" }?.coverUrl
        ?: GhaisAssets.LibraryTahajjudCover
    "sunrise-barakah" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "sunrise-barakah" }?.coverUrl
        ?: GhaisAssets.LibraryMorningCover
    "emotional" -> GhaisAssets.AllCuratedPlaylists.getOrNull(1)?.coverUrl
        ?: GhaisAssets.LibraryMorningCover
    "most-beautiful" -> GhaisAssets.AllCuratedPlaylists.find { it.id == "garden-of-tranquility" }?.coverUrl
        ?: GhaisAssets.NowPlayingVinylArtUrl
    "favourites" -> GhaisAssets.ProfileAvatarUrl
    else -> GhaisAssets.LibraryMorningCover
}

private fun tracksForPlaylist(
    playlist: MoodPlaylist,
    reciterSlug: String = "mishary"
): List<TrackItem> {
    val reciter = QuranDataRepository.getReciterBySlug(reciterSlug)
    return playlist.surahIds.mapNotNull { surahId ->
        val surah = QuranDataRepository.getSurahById(surahId) ?: return@mapNotNull null
        TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = 0,
            audioUrl = reciter.getFullSurahUrl(surah.id),
            textUthmani = "",
            durationMs = 0L
        )
    }
}
