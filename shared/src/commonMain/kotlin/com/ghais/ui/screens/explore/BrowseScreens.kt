package com.ghais.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Noir Glass — strict monochrome redesign of the browse directory screens.
 *
 * Canvas #050506 via [NoirScreenRoot] (glow zone -> absolute black + grain).
 * Rows speak the [com.ghais.ui.components.noir.NoirListRow] language: soft card
 * fill + 1px [GhaisNoir.BorderCard] + 22% top-only specular. Number plates are
 * typographic clay wells (no artwork on these screens, so grayscale is vacuous
 * — zero hue by construction). Text ladder 100/62/38/24%. State reads through
 * fill elevation + chromium, never hue.
 *
 * Preserved: public signatures (`JuzInfo`, `SurahsScreen`, `JuzBrowserScreen`),
 * navigation (`navigator.pop()` back, `SurahDetailScreen` pushes,
 * `AudioEngine.playTrack` + `NowPlayingScreen`), bookmark state, and the
 * juzId -> startSurahId mapping.
 */
data class JuzInfo(
    val id: Int,
    val nameAr: String,
    val startVerse: String,
    val surahsSummary: String
)

object SurahsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        var bookmarkedSurahs by remember { mutableStateOf(setOf(1, 18, 36, 55, 67)) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }

        val filterOptions = remember { listOf("All", "Makki", "Madani", "Saved") }

        val filteredSurahs = remember(searchQuery, selectedFilter, bookmarkedSurahs) {
            QuranData.SURAHS.filter { surah ->
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim()
                    surah.nameEn.contains(q, ignoreCase = true) ||
                        surah.nameAr.contains(q, ignoreCase = true) ||
                        surah.meaning.contains(q, ignoreCase = true)
                }
                val isMeccan = surah.revelationType.equals("Meccan", ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    "Makki" -> isMeccan
                    "Madani" -> !isMeccan
                    "Saved" -> bookmarkedSurahs.contains(surah.id)
                    else -> true
                }
                matchesQuery && matchesFilter
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
                BrowseTopBar(
                    title = "All Surahs",
                    count = filteredSurahs.size,
                    onBackClick = { navigator.pop() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                BrowseSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by surah, Arabic name, meaning..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                BrowseFilterPills(
                    filters = filterOptions,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                NoirSectionHeader(
                    label = "Surahs",
                    actionLabel = "${filteredSurahs.size} shown",
                    onAction = {}
                )

                Spacer(modifier = Modifier.height(2.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredSurahs.isEmpty()) {
                        item {
                            BrowseEmptyState(
                                headline = if (searchQuery.isNotBlank()) {
                                    "No surahs found for \"$searchQuery\""
                                } else {
                                    "No surahs here yet"
                                },
                                hint = "Try searching by name, Arabic spelling, or meaning"
                            )
                        }
                    } else {
                        items(items = filteredSurahs, key = { it.id }) { surah ->
                            val isBookmarked = bookmarkedSurahs.contains(surah.id)
                            SurahNoirRow(
                                surah = surah,
                                isBookmarked = isBookmarked,
                                onClick = { navigator.push(SurahDetailScreen(surah.id)) },
                                onPlayClick = {
                                    val track = TrackItem(
                                        reciterSlug = "mishary",
                                        reciterName = "Sheikh Mishary Rashid Alafasy",
                                        surahId = surah.id,
                                        surahNameEn = surah.nameEn,
                                        surahNameAr = surah.nameAr,
                                        ayahNo = 0,
                                        audioUrl = "https://server8.mp3quran.net/afs/${surah.id.toString().padStart(3, '0')}.mp3",
                                        durationMs = 300000L
                                    )
                                    AudioEngine.playTrack(track)
                                    rootNavigator.push(NowPlayingScreen())
                                },
                                onBookmarkClick = {
                                    bookmarkedSurahs = if (isBookmarked) {
                                        bookmarkedSurahs - surah.id
                                    } else {
                                        bookmarkedSurahs + surah.id
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

object JuzBrowserScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        var searchQuery by remember { mutableStateOf("") }
        var selectedRange by remember { mutableStateOf("All") }
        val rangeOptions = remember { listOf("All", "1-10", "11-20", "21-30") }

        val juzList = remember {
            listOf(
                JuzInfo(1, "الجزء الأول", "Al-Fatihah 1 - Al-Baqarah 141", "Surahs 1 - 2"),
                JuzInfo(2, "الجزء الثاني", "Al-Baqarah 142 - Al-Baqarah 252", "Surah 2"),
                JuzInfo(3, "الجزء الثالث", "Al-Baqarah 253 - Ali 'Imran 92", "Surahs 2 - 3"),
                JuzInfo(4, "الجزء الرابع", "Ali 'Imran 93 - An-Nisa 23", "Surahs 3 - 4"),
                JuzInfo(5, "الجزء الخامس", "An-Nisa 24 - An-Nisa 147", "Surah 4"),
                JuzInfo(6, "الجزء السادس", "An-Nisa 148 - Al-Ma'idah 81", "Surahs 4 - 5"),
                JuzInfo(7, "الجزء السابع", "Al-Ma'idah 82 - Al-An'am 110", "Surahs 5 - 6"),
                JuzInfo(8, "الجزء الثامن", "Al-An'am 111 - Al-A'raf 87", "Surahs 6 - 7"),
                JuzInfo(9, "الجزء التاسع", "Al-A'raf 88 - Al-Anfal 40", "Surahs 7 - 8"),
                JuzInfo(10, "الجزء العاشر", "Al-Anfal 41 - At-Tawbah 92", "Surahs 8 - 9"),
                JuzInfo(11, "الجزء الحادي عشر", "At-Tawbah 93 - Hud 5", "Surahs 9 - 11"),
                JuzInfo(12, "الجزء الثاني عشر", "Hud 6 - Yusuf 52", "Surahs 11 - 12"),
                JuzInfo(13, "الجزء الثالث عشر", "Yusuf 53 - Ibrahim 52", "Surahs 12 - 14"),
                JuzInfo(14, "الجزء الرابع عشر", "Al-Hijr 1 - An-Nahl 128", "Surahs 15 - 16"),
                JuzInfo(15, "الجزء الخامس عشر", "Al-Isra 1 - Al-Kahf 74", "Surahs 17 - 18"),
                JuzInfo(16, "الجزء السادس عشر", "Al-Kahf 75 - Ta-Ha 135", "Surahs 18 - 20"),
                JuzInfo(17, "الجزء السابع عشر", "Al-Anbiya 1 - Al-Hajj 78", "Surahs 21 - 22"),
                JuzInfo(18, "الجزء الثامن عشر", "Al-Mu'minun 1 - Al-Furqan 20", "Surahs 23 - 25"),
                JuzInfo(19, "الجزء التاسع عشر", "Al-Furqan 21 - An-Naml 55", "Surahs 25 - 27"),
                JuzInfo(20, "الجزء العشرون", "An-Naml 56 - Al-Ankabut 45", "Surahs 27 - 29"),
                JuzInfo(21, "الجزء الحادي والعشرون", "Al-Ankabut 46 - Al-Ahzab 30", "Surahs 29 - 33"),
                JuzInfo(22, "الجزء الثاني والعشرون", "Al-Ahzab 31 - Ya-Sin 27", "Surahs 33 - 36"),
                JuzInfo(23, "الجزء الثالث والعشرون", "Ya-Sin 28 - Az-Zumar 31", "Surahs 36 - 39"),
                JuzInfo(24, "الجزء الرابع والعشرون", "Az-Zumar 32 - Fussilat 46", "Surahs 39 - 41"),
                JuzInfo(25, "الجزء الخامس والعشرون", "Fussilat 47 - Al-Jathiyah 37", "Surahs 41 - 45"),
                JuzInfo(26, "الجزء السادس والعشرون", "Al-Ahqaf 1 - Adh-Dhariyat 30", "Surahs 46 - 51"),
                JuzInfo(27, "الجزء السابع والعشرون", "Adh-Dhariyat 31 - Al-Hadid 29", "Surahs 51 - 57"),
                JuzInfo(28, "الجزء الثامن والعشرون", "Al-Mujadila 1 - At-Tahrim 12", "Surahs 58 - 66"),
                JuzInfo(29, "تبارك", "Al-Mulk 1 - Al-Mursalat 50", "Surahs 67 - 77"),
                JuzInfo(30, "عمّ", "An-Naba 1 - An-Nas 6", "Surahs 78 - 114")
            )
        }

        val filteredJuz = remember(searchQuery, selectedRange) {
            juzList.filter { juz ->
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim()
                    "juz ${juz.id}".contains(q, ignoreCase = true) ||
                        juz.startVerse.contains(q, ignoreCase = true) ||
                        juz.nameAr.contains(q, ignoreCase = true) ||
                        juz.surahsSummary.contains(q, ignoreCase = true)
                }
                val matchesRange = when (selectedRange) {
                    "1-10" -> juz.id in 1..10
                    "11-20" -> juz.id in 11..20
                    "21-30" -> juz.id in 21..30
                    else -> true
                }
                matchesQuery && matchesRange
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
                BrowseTopBar(
                    title = "Juz Index",
                    count = filteredJuz.size,
                    onBackClick = { navigator.pop() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                BrowseSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by juz number, verse, surah..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                BrowseFilterPills(
                    filters = rangeOptions,
                    selectedFilter = selectedRange,
                    onFilterSelected = { selectedRange = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                NoirSectionHeader(
                    label = "Ajza",
                    actionLabel = "${filteredJuz.size} shown",
                    onAction = {}
                )

                Spacer(modifier = Modifier.height(2.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredJuz.isEmpty()) {
                        item {
                            BrowseEmptyState(
                                headline = if (searchQuery.isNotBlank()) {
                                    "No ajza found for \"$searchQuery\""
                                } else {
                                    "No ajza here yet"
                                },
                                hint = "Try searching by juz number or verse range"
                            )
                        }
                    } else {
                        items(items = filteredJuz, key = { it.id }) { juz ->
                            JuzNoirRow(
                                juz = juz,
                                onClick = { navigator.push(SurahDetailScreen(juzStartSurah(juz.id))) },
                                onPlayClick = {
                                    val track = TrackItem(
                                        reciterSlug = "mishary",
                                        reciterName = "Sheikh Mishary Rashid Alafasy",
                                        surahId = if (juz.id == 30) 78 else if (juz.id == 29) 67 else 1,
                                        surahNameEn = "Juz ${juz.id}",
                                        surahNameAr = juz.nameAr,
                                        ayahNo = 0,
                                        audioUrl = "https://server8.mp3quran.net/afs/${if (juz.id == 30) "078" else if (juz.id == 29) "067" else "001"}.mp3",
                                        durationMs = 300000L
                                    )
                                    AudioEngine.playTrack(track)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Juz -> entry surah mapping (navigation logic preserved verbatim). */
private fun juzStartSurah(juzId: Int): Int = when (juzId) {
    1 -> 1
    2 -> 2
    3 -> 2
    4 -> 3
    5 -> 4
    6 -> 4
    7 -> 5
    8 -> 6
    9 -> 7
    10 -> 8
    11 -> 9
    12 -> 11
    13 -> 12
    14 -> 15
    15 -> 17
    16 -> 18
    17 -> 21
    18 -> 23
    19 -> 25
    20 -> 27
    21 -> 29
    22 -> 33
    23 -> 36
    24 -> 39
    25 -> 41
    26 -> 46
    27 -> 51
    28 -> 58
    29 -> 67
    30 -> 78
    else -> 1
}

/**
 * Top bar: clay [IconWell] back affordance, bold white title,
 * monochrome count chip (Fill2 wash + ghost hairline, 62% text).
 */
@Composable
private fun BrowseTopBar(
    title: String,
    count: Int,
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

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GhaisNoir.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.TextSecondary
            )
        }
    }
}

/**
 * Search field in [NoirInsetField] style: engraved recessed surface
 * (black 45% gradient + 5% rim), white cursor, 24% placeholder.
 */
@Composable
private fun BrowseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    NoirInsetField(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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
                        text = placeholder,
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
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape)
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
}

/**
 * Filter pills: selected = chrome gradient fill + near-black label
 * (primary action); resting = Fill2 wash + ghost hairline + 62% label.
 */
@Composable
private fun BrowseFilterPills(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.chromeFill())
                        .border(1.dp, GhaisNoir.ChromeInnerHighlight.copy(alpha = 0.35f), GhaisShapes.pill)
                        .noirClickable { onFilterSelected(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                        .noirClickable { onFilterSelected(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
 * Surah row in the NoirListRow language: soft card fill + 1px card border +
 * 22% top-only specular, typographic number well (clay fill, white numeral),
 * ghost revelation chip, Arabic title, ghost play + bookmark affordances.
 * The old emerald/amber Makki/Madani badges collapse to one ghost chip —
 * state reads through text, not hue.
 */
@Composable
private fun SurahNoirRow(
    surah: Surah,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val isMeccan = surah.revelationType.equals("Meccan", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.id.toString().padStart(2, '0'),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GhaisNoir.TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = surah.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${surah.meaning} • ${surah.ayahsCount} Verses",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(GhaisNoir.Fill2)
                        .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMeccan) "Makki" else "Madani",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (surah.transliteration.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = surah.transliteration,
                        color = GhaisNoir.TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = surah.nameAr,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GhaisNoir.TextPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Right,
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                        .background(GhaisNoir.Fill2, CircleShape)
                        .noirClickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play ${surah.nameEn}",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                val bookmarkFill = if (isBookmarked) {
                    GhaisNoir.chromeFill()
                } else {
                    null
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .then(
                            if (bookmarkFill != null) {
                                Modifier.background(bookmarkFill)
                            } else {
                                Modifier.background(GhaisNoir.Fill2, CircleShape)
                            }
                        )
                        .border(
                            1.dp,
                            if (isBookmarked) {
                                GhaisNoir.ChromeInnerHighlight.copy(alpha = 0.35f)
                            } else {
                                GhaisNoir.BorderGhost
                            },
                            CircleShape
                        )
                        .noirClickable(onClick = onBookmarkClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                        tint = if (isBookmarked) GhaisNoir.OnChrome else GhaisNoir.TextTertiary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Juz row in the NoirListRow language. The old emerald "prominent" treatment
 * (Juz 1/29/30) is now fill elevation: active card wash + specular ring on the
 * number well, bold white numeral — no hue. Tap navigates to the entry surah;
 * play streams the juz head surah (logic preserved).
 */
@Composable
private fun JuzNoirRow(
    juz: JuzInfo,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val isProminent = juz.id == 1 || juz.id == 29 || juz.id == 30
    val rowFill = if (isProminent) GhaisNoir.cardFillActive() else GhaisNoir.cardFillSoft()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(rowFill, GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(
                    1.dp,
                    if (isProminent) GhaisNoir.SpecularTop else GhaisNoir.BorderCard,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = juz.id.toString().padStart(2, '0'),
                fontSize = 13.sp,
                fontWeight = if (isProminent) FontWeight.Bold else FontWeight.SemiBold,
                color = GhaisNoir.TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = "Juz ${juz.id}",
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = juz.startVerse,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.Fill2)
                    .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = juz.surahsSummary,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = juz.nameAr,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GhaisNoir.TextPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Right,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .background(GhaisNoir.Fill2, CircleShape)
                    .noirClickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Juz ${juz.id}",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/** Empty state — clay [IconWell], 100% headline + 62% hint. */
@Composable
private fun BrowseEmptyState(
    headline: String,
    hint: String
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
            text = headline,
            color = GhaisNoir.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = hint,
            color = GhaisNoir.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
