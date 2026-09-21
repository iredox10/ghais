package com.ghais.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.QuranData
import com.ghais.data.seed.StitchAssets
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import cafe.adriel.voyager.navigator.LocalNavigator
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.QuranifyColors

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
        // Section Header Row with Sort Dropdown Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // "Surah Directory" + Count Pill Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Surah Directory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${surahs.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.Primary
                    )
                }
            }

            // Sort Menu Button (Glass Pill)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(QuranifyColors.SurfaceLow.copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .clickable { showSortModal = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (sortType) {
                        SurahSortType.MUSHAF -> "Mushaf Order"
                        SurahSortType.CHRONOLOGICAL -> "Chronological"
                        SurahSortType.LENGTH -> "Length"
                        SurahSortType.ALPHABETICAL -> "Alphabetical"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = QuranifyColors.TextSecondary
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Sort Options",
                    tint = QuranifyColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Surah Items List with Glass Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

        // Popular Qaris Section (Master Reciter Strip)
        PopularQarisStrip(onReciterClick = onReciterClick)
    }

    // Interactive Sort Modal Sheet
    if (showSortModal) {
        ModalBottomSheet(
            onDismissRequest = { showSortModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = QuranifyColors.SurfaceContainer,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = QuranifyColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sort Surahs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuranifyColors.TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { showSortModal = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(QuranifyColors.SurfaceHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = QuranifyColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Options list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurahSortType.entries.forEach { option ->
                        val isSelected = option == sortType
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) QuranifyColors.SurfaceHighest
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) QuranifyColors.Primary.copy(alpha = 0.35f)
                                    else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    sortType = option
                                    showSortModal = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = option.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextPrimary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = QuranifyColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
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
            .clip(RoundedCornerShape(16.dp))
            .background(QuranifyColors.SurfaceLow.copy(alpha = 0.7f))
            .border(
                1.dp,
                if (isFirst) QuranifyColors.Primary.copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Index Disc + Title & Meta
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Rub el Hizb / Squircle Number Disc
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isFirst) {
                            Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(1.dp, QuranifyColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        } else {
                            Modifier
                                .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.7f))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Rotated Diamond Accent
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(45f)
                        .background(
                            if (isFirst) QuranifyColors.Primary.copy(alpha = 0.1f)
                            else Color.White.copy(alpha = 0.03f)
                        )
                )

                Text(
                    text = surah.id.toString().padStart(2, '0'),
                    fontSize = 12.sp,
                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isFirst) QuranifyColors.Primary else QuranifyColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = surah.nameEn,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuranifyColors.TextPrimary
                    )

                    // Revelation Badge (Makki = Emerald, Madani = Amber)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isMeccan) Color(0xFF10B981).copy(alpha = 0.12f)
                                else Color(0xFFFFB95F).copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isMeccan) QuranifyColors.Primary.copy(alpha = 0.3f)
                                else QuranifyColors.Secondary.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isMeccan) "Makki" else "Madani",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMeccan) QuranifyColors.Primary else QuranifyColors.Secondary
                        )
                    }
                }

                Text(
                    text = "${surah.meaning} • ${surah.ayahsCount} Verses",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }
        }

        // Right side: Arabic Calligraphy + Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = surah.nameAr,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFirst) QuranifyColors.Primary else QuranifyColors.TextPrimary.copy(alpha = 0.95f),
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Play Circular Glass Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(QuranifyColors.SurfaceHigh.copy(alpha = 0.8f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Surah ${surah.nameEn}",
                    tint = QuranifyColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bookmark Circular Glass Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isBookmarked) QuranifyColors.Secondary.copy(alpha = 0.12f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isBookmarked) QuranifyColors.Secondary.copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.06f),
                        CircleShape
                    )
                    .clickable { onBookmarkClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) QuranifyColors.Secondary else QuranifyColors.TextTertiary,
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
    val qaris = StitchAssets.VerifiedReciters

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Popular Qaris",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuranifyColors.TextPrimary
                )
                Text(
                    text = "World-renowned Quranic voices",
                    fontSize = 12.sp,
                    color = QuranifyColors.TextSecondary
                )
            }

            Text(
                text = "Explore",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = QuranifyColors.Primary,
                modifier = Modifier.clickable { onReciterClick("explore") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Reciters Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(qaris) { qari ->
                val isFeatured = qari.slug == "mishary"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable { onReciterClick(qari.slug) }
                ) {
                    Box(modifier = Modifier.size(60.dp)) {
                        AsyncImage(
                            model = qari.photoUrl,
                            contentDescription = qari.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .then(
                                    if (isFeatured) {
                                        Modifier.border(
                                            2.dp,
                                            Brush.sweepGradient(
                                                listOf(
                                                    QuranifyColors.Primary,
                                                    Color(0xFF68DBA9),
                                                    Color.White.copy(alpha = 0.4f),
                                                    QuranifyColors.Primary
                                                )
                                            ),
                                            CircleShape
                                        )
                                    } else {
                                        Modifier.border(
                                            1.5.dp,
                                            Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                    }
                                )
                                .background(QuranifyColors.SurfaceHigh)
                        )

                        // Verified checkmark badge
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(QuranifyColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verified",
                                tint = QuranifyColors.OnPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = qari.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuranifyColors.TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
