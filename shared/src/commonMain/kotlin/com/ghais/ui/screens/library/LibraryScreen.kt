package com.ghais.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.playlists.MoodPlaylist
import com.ghais.ui.screens.playlists.MoodPlaylists
import com.ghais.ui.screens.playlists.PlaylistArt
import com.ghais.ui.screens.playlists.PlaylistDetailsScreen
import com.ghais.ui.screens.routines.MyRoutinesScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase 4 — Noir Library.
 *
 * Strict Noir Glass monochrome on the shared canvas (glow zone -> #050506 +
 * ambient glow + film grain). Tab signature, search/filter state and navigation
 * targets are unchanged; only the surface language changed: zero accent hue,
 * alpha-white glass tiles, top-only specular hairlines, chrome pills and
 * monochrome playlist artwork. State reads through fill elevation, weight
 * and opacity.
 */
object LibraryScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 3u,
                title = "Collections",
                icon = null
            )
        }

    private val Filters = listOf("All", "Night", "Calm", "Focus", "Healing", "Devotion")

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        var query by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        val filteredPlaylists = remember(query, selectedFilter) {
            MoodPlaylists.filter { playlist ->
                val matchesQuery = query.isBlank() ||
                    playlist.title.contains(query, ignoreCase = true) ||
                    playlist.description.contains(query, ignoreCase = true)
                val matchesFilter = selectedFilter == "All" ||
                    libraryCategory(playlist.id) == selectedFilter
                matchesQuery && matchesFilter
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
                // Editorial header + engraved search + chrome filter pills
                // ---------------------------------------------------------
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NoirLibraryHeader(
                            totalCount = MoodPlaylists.size,
                            visibleCount = filteredPlaylists.size
                        )
                        Spacer(Modifier.height(16.dp))
                        NoirLibrarySearch(
                            query = query,
                            onQueryChange = { query = it }
                        )
                        Spacer(Modifier.height(14.dp))
                        NoirLibraryReel(
                            filters = Filters,
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it }
                        )
                    }
                }

                if (filteredPlaylists.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        NoirLibraryEmpty()
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        NoirSectionHeader(label = "Collections • ${filteredPlaylists.size}")
                    }
                    item {
                        NoirRoutinesTile(
                            onClick = { rootNavigator?.push(MyRoutinesScreen) }
                        )
                    }
                    items(filteredPlaylists, key = { it.id }) { playlist ->
                        NoirLibraryTile(
                            playlist = playlist,
                            onClick = { rootNavigator?.push(PlaylistDetailsScreen(playlist.id)) }
                        )
                    }
                }
            }
        }
    }
}

private val NoirLibraryTileShape = RoundedCornerShape(28.dp)
private val NoirLibraryArtShape = RoundedCornerShape(22.dp)

/**
 * Editorial two-line headline ("Your" light / "Collections" bold) matching
 * the Home "Return to / Your Quran" treatment, with ghost count chips.
 */
@Composable
private fun NoirLibraryHeader(
    totalCount: Int,
    visibleCount: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your",
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
            text = "Routines, collections & recitations saved for you",
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoirLibraryChip(
                text = if (totalCount == 1) "1 collection" else "$totalCount collections"
            )
            NoirLibraryChip(
                text = if (visibleCount == 1) "1 shown" else "$visibleCount shown"
            )
        }
    }
}

/** Non-interactive ghost count chip — informational, no press affordance. */
@Composable
private fun NoirLibraryChip(text: String) {
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
private fun NoirLibrarySearch(
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
                        text = "Search routines & collections...",
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
 * Filter reel — selected chip is an extruded chrome pill (near-black
 * label), unselected chips are Fill2 washes with ghost rims. State reads
 * through elevation + label opacity, never hue.
 */
@Composable
private fun NoirLibraryReel(
    filters: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable { onSelect(filter) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
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
                        .noirClickable { onSelect(filter) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
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
 * My Routines tile — soft glass fill + 1px card border + top-only specular,
 * clay icon-well + dual text + circular chevron affordance.
 */
@Composable
private fun NoirRoutinesTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirLibraryTileShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirLibraryTileShape)
            .topSpecular()
            .noirClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GhaisNoir.sheen())
        )
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
                IconWell(icon = Icons.Filled.QueueMusic, size = 44.dp, iconSize = 22.dp)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "My Routines",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Your custom recitation routines",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.5.sp,
                color = GhaisNoir.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Routines • Personal",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Noir bento tile: soft glass fill + 1px card border + top-only specular.
 * Artwork is monochrome line-work on a recessed plate — zero hue.
 */
@Composable
private fun NoirLibraryTile(
    playlist: MoodPlaylist,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirLibraryTileShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirLibraryTileShape)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                NoirLibraryBadge(libraryCategory(playlist.id))
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                NoirLibraryArtwork(art = playlist.art, plateSize = 76.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = playlist.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = playlist.description,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.5.sp,
                color = GhaisNoir.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${playlist.surahIds.size} Surahs • ${libraryStyle(playlist.id)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = GhaisNoir.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Recessed monochrome artwork plate — white line-work, never hue. */
@Composable
private fun NoirLibraryArtwork(
    art: PlaylistArt,
    plateSize: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(plateSize)
            .clip(NoirLibraryArtShape)
            .background(GhaisNoir.wellFill())
            .border(1.dp, GhaisNoir.BorderCard, NoirLibraryArtShape),
        contentAlignment = Alignment.Center
    ) {
        if (art == PlaylistArt.FAVOURITES) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(plateSize * 0.42f)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 2.5f
                val white = Color.White.copy(alpha = 0.32f)
                when (art) {
                    PlaylistArt.FOCUS_WORK, PlaylistArt.STUDY -> {
                        val cx = size.width / 2f
                        val cy = size.height * 0.42f
                        val maxR = size.width * 0.44f
                        for (i in 4 downTo 1) {
                            drawCircle(
                                color = white,
                                radius = maxR * i / 4f,
                                center = Offset(cx, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                            )
                        }
                    }
                    PlaylistArt.BEAUTIFUL, PlaylistArt.EMOTIONAL -> {
                        val cx = size.width / 2f
                        val cy = size.height * 0.44f
                        val petalR = size.width * 0.17f
                        val orbitR = size.width * 0.17f
                        repeat(8) { k ->
                            val a = k * (PI / 4.0)
                            drawCircle(
                                color = white,
                                radius = petalR,
                                center = Offset(
                                    cx + (orbitR * cos(a)).toFloat(),
                                    cy + (orbitR * sin(a)).toFloat()
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                            )
                        }
                    }
                    PlaylistArt.SLEEP, PlaylistArt.TAHAJJUD -> {
                        drawArc(
                            color = white,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(-size.width * 0.25f, size.height * 0.12f),
                            size = androidx.compose.ui.geometry.Size(size.width * 1.5f, size.height * 0.9f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f),
                            radius = size.width * 0.10f,
                            center = Offset(size.width * 0.32f, size.height * 0.60f)
                        )
                    }
                    else -> {
                        val origin = Offset(size.width * 0.15f, size.height * 1.05f)
                        for (i in 0..10) {
                            val a = -90.0 + i * (80.0 / 10.0)
                            val rad = a * PI / 180.0
                            drawLine(
                                color = white,
                                start = origin,
                                end = Offset(
                                    origin.x + (size.width * 1.4f * cos(rad)).toFloat(),
                                    origin.y + (size.width * 1.4f * sin(rad)).toFloat()
                                ),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
        }
        // Darkening scrim: keeps the plate recessed instead of glowing.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )
    }
}

/** Ghost category badge — informational, no press affordance, zero hue. */
@Composable
private fun NoirLibraryBadge(category: String) {
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

/** Ghost empty state — muted well + tertiary copy, no hue. */
@Composable
private fun NoirLibraryEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoirLibraryTileShape)
            .background(GhaisNoir.cardFillSoft())
            .border(1.dp, GhaisNoir.BorderCard, NoirLibraryTileShape)
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
                imageVector = Icons.Filled.QueueMusic,
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
            text = "Try adjusting your search query or filter.",
            fontSize = 13.sp,
            color = GhaisNoir.TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------------
// Local collection metadata (category/style only, all monochrome at render).
// ------------------------------------------------------------------

private fun libraryCategory(id: String): String = when (id) {
    "tahajjud", "sleep-mode" -> "Night"
    "heart-soothing", "most-beautiful" -> "Calm"
    "study-focus", "focus-work" -> "Focus"
    "duaa-ruqia", "emotional" -> "Healing"
    else -> "Devotion"
}

private fun libraryStyle(id: String): String = when (id) {
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
