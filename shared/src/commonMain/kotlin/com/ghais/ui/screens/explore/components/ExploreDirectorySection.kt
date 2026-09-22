package com.ghais.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

enum class SurahSortType(val label: String, val icon: ImageVector) {
    MUSHAF("Standard Mushaf Order", Icons.Default.FormatListNumbered),
    CHRONOLOGICAL("Chronological Revelation Order", Icons.Default.History),
    LENGTH("Length (Longest to Shortest)", Icons.Default.SwapVert),
    ALPHABETICAL("Alphabetical Name", Icons.Default.SortByAlpha)
}

private val CHRONOLOGICAL_ORDER = listOf(
    96, 68, 73, 74, 1, 111, 81, 87, 92, 89,
    93, 94, 103, 100, 108, 102, 107, 109, 105, 113,
    114, 112, 53, 80, 97, 91, 85, 95, 106, 101,
    75, 104, 77, 50, 90, 86, 54, 38, 7, 72,
    36, 25, 35, 19, 20, 56, 26, 27, 28, 17,
    10, 11, 12, 15, 6, 37, 31, 34, 39, 40,
    41, 42, 43, 44, 45, 46, 51, 88, 18, 16,
    71, 14, 21, 23, 32, 52, 67, 69, 70, 78,
    79, 82, 84, 30, 29, 83, 2, 8, 3, 33,
    60, 4, 99, 57, 47, 13, 55, 76, 65, 98,
    59, 24, 22, 63, 58, 49, 66, 64, 61, 62,
    48, 5, 9, 110
)

/** Short pill labels for the sort strip (full sentence kept in the sheet). */
private fun SurahSortType.shortLabel(): String = when (this) {
    SurahSortType.MUSHAF -> "Mushaf"
    SurahSortType.CHRONOLOGICAL -> "Chronological"
    SurahSortType.LENGTH -> "Length"
    SurahSortType.ALPHABETICAL -> "A–Z"
}

/** True-grayscale filter — qari portraits stay recognisable, strictly monochrome. */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreDirectorySection(
    surahs: List<Surah> = QuranData.SURAHS,
    onSurahClick: (Surah) -> Unit = {},
    onReciterClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var sortType by remember { mutableStateOf(SurahSortType.MUSHAF) }
    var showSortModal by remember { mutableStateOf(false) }
    var bookmarkedSurahIds by remember { mutableStateOf(setOf(1, 18, 36, 55, 67)) }
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    val sortedSurahs = remember(surahs, sortType) {
        when (sortType) {
            SurahSortType.MUSHAF -> surahs.sortedBy { it.id }
            SurahSortType.CHRONOLOGICAL -> {
                val orderMap = CHRONOLOGICAL_ORDER.mapIndexed { idx, id -> id to idx }.toMap()
                surahs.sortedBy { orderMap[it.id] ?: 999 }
            }
            SurahSortType.LENGTH -> surahs.sortedByDescending { it.ayahsCount }
            SurahSortType.ALPHABETICAL -> surahs.sortedBy { it.nameEn }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header — shared Noir rhythm (bright label + ghost action).
        NoirSectionHeader(
            label = "Surah Directory • ${surahs.size}",
            actionLabel = sortType.shortLabel(),
            onAction = { showSortModal = true },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Sort strip — chrome pill (selected) / ghost pill (resting).
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SurahSortType.entries) { option ->
                val isSelected = option == sortType
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.chromeFill())
                            .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
                            .noirClickable { sortType = option }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.shortLabel(),
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
                            .noirClickable { sortType = option }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.shortLabel(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Surah rows — NoirListRow recipe (soft fill + ghost border + top specular).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sortedSurahs.forEach { surah ->
                val isBookmarked = bookmarkedSurahIds.contains(surah.id)
                SurahDirectoryItem(
                    surah = surah,
                    isBookmarked = isBookmarked,
                    onClick = { onSurahClick(surah) },
                    onPlayClick = {
                        val reciter = QuranDataRepository.getReciterBySlug("mishary")
                        val allTracks = sortedSurahs.map { s ->
                            TrackItem(
                                reciterSlug = reciter.slug,
                                reciterName = reciter.nameEn,
                                surahId = s.id,
                                surahNameEn = s.nameEn,
                                surahNameAr = s.nameAr,
                                ayahNo = 0,
                                audioUrl = reciter.getFullSurahUrl(s.id),
                                durationMs = s.ayahsCount * 15_000L
                            )
                        }
                        val startIndex = allTracks.indexOfFirst { it.surahId == surah.id }.coerceAtLeast(0)
                        AudioEngine.playQueue(allTracks, startIndex = startIndex)
                        rootNavigator?.push(NowPlayingScreen())
                    },
                    onBookmarkClick = {
                        bookmarkedSurahIds = if (isBookmarked) {
                            bookmarkedSurahIds - surah.id
                        } else {
                            bookmarkedSurahIds + surah.id
                        }
                    }
                )
            }
        }

        // Popular Qaris strip (grayscale reel).
        PopularQarisStrip(onReciterClick = onReciterClick)
    }

    // Sort sheet — Noir canvas + ghost rows, state reads through fill elevation.
    if (showSortModal) {
        ModalBottomSheet(
            onDismissRequest = { showSortModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GhaisNoir.CanvasTop,
            scrimColor = GhaisNoir.Scrim,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconWell(
                            icon = Icons.Default.FormatListNumbered,
                            size = 40.dp,
                            iconSize = 20.dp,
                            contentDescription = null
                        )
                        Text(
                            text = "Sort Surahs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhaisNoir.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                            .noirClickable { showSortModal = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GhaisNoir.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurahSortType.entries.forEach { option ->
                        val isSelected = option == sortType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GhaisShapes.row)
                                .background(
                                    if (isSelected) GhaisNoir.cardFillActive()
                                    else GhaisNoir.cardFillSoft()
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    GhaisShapes.row
                                )
                                .topSpecular(inset = 22.dp)
                                .noirClickable {
                                    sortType = option
                                    showSortModal = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconWell(
                                    icon = option.icon,
                                    size = 36.dp,
                                    iconSize = 18.dp,
                                    contentDescription = null,
                                    tint = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary
                                )
                                Text(
                                    text = option.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextSecondary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(GhaisNoir.chromeFill(), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = GhaisNoir.OnChrome,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SurahDirectoryItem(
    surah: Surah,
    isBookmarked: Boolean = false,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {}
) {
    val isMeccan = surah.revelationType.equals("Meccan", ignoreCase = true)
    val isFirst = surah.id == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(
                1.dp,
                if (isFirst) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                GhaisShapes.row
            )
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: number well + dual text (NoirListRow recipe).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Engraved number well — state reads through border brightness, never hue.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GhaisNoir.wellFill())
                    .border(
                        1.dp,
                        if (isFirst) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.id.toString().padStart(2, '0'),
                    fontSize = 13.sp,
                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.SemiBold,
                    color = GhaisNoir.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = surah.nameEn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Revelation ghost chip — informational only (Makki / Madani).
                    Box(
                        modifier = Modifier
                            .clip(GhaisShapes.pill)
                            .background(GhaisNoir.Fill2)
                            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isMeccan) "Makki" else "Madani",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhaisNoir.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${surah.meaning} • ${surah.ayahsCount} verses",
                    fontSize = 12.sp,
                    color = GhaisNoir.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right: Arabic + monochrome circular affordances.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = surah.nameAr,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GhaisNoir.TextPrimary.copy(alpha = 0.95f),
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Play ghost button.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(GhaisNoir.Fill1)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .noirClickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Surah ${surah.nameEn}",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bookmark — active reads through stronger wash + bright rim.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isBookmarked) GhaisNoir.Fill4 else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isBookmarked) GhaisNoir.SpecularTop else GhaisNoir.BorderGhost,
                        CircleShape
                    )
                    .noirClickable(onClick = onBookmarkClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PopularQarisStrip(
    onReciterClick: (String) -> Unit = {}
) {
    val qaris = GhaisAssets.VerifiedReciters

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp)
    ) {
        NoirSectionHeader(
            label = "Popular Qaris • ${qaris.size}",
            actionLabel = "Explore",
            onAction = { onReciterClick("explore") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            text = "World-renowned Quranic voices",
            fontSize = 12.sp,
            color = GhaisNoir.TextSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(qaris) { qari ->
                val isFeatured = qari.slug == "mishary"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .noirClickable { onReciterClick(qari.slug) }
                ) {
                    Box(modifier = Modifier.size(60.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(GhaisNoir.wellFill())
                                .border(
                                    if (isFeatured) 2.dp else 1.dp,
                                    if (isFeatured) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = qari.photoUrl,
                                contentDescription = qari.name,
                                contentScale = ContentScale.Crop,
                                colorFilter = NoirGrayscale,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Darkening scrim keeps the portrait recessed, never glowing.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        }

                        // Verified dot — chrome disc, near-black glyph (zero hue).
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .background(GhaisNoir.chromeFill(), CircleShape)
                                .border(1.5.dp, GhaisNoir.NoirBlack, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verified",
                                tint = GhaisNoir.OnChrome,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = qari.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhaisNoir.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
