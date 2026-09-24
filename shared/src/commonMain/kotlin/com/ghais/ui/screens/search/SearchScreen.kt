package com.ghais.ui.screens.search

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.SearchHistoryStore
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.ChromeFab
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlinx.coroutines.delay

/**
 * Noir Glass — strict monochrome redesign.
 *
 * Canvas #050506 via [NoirScreenRoot] (glow zone -> absolute black + grain).
 * Engraved [NoirInsetField] search (white cursor, 24% hint, ghost clear),
 * chrome/ghost filter pills, [NoirCard] ayah plate with chrome play,
 * result rows in the NoirListRow language with reciter photos in color, ghost-well
 * empty states. Zero hue — state reads through fill, weight and opacity.
 *
 * Signatures, search/filter/play logic and navigation preserved.
 */
class SearchScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        var query by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }
        val filters = listOf("All", "Surahs", "Reciters", "Ayahs")
        val recentQueries by SearchHistoryStore.recentQueries.collectAsState()

        LaunchedEffect(query) {
            delay(600)
            val meaningfulQuery = query.trim()
            if (meaningfulQuery.length >= 2) {
                SearchHistoryStore.record(meaningfulQuery)
            }
        }

        val searchResults = remember(query) { SearchEngine.search(query) }

        // Shared queue builder — unchanged playback logic, surah entry point varies.
        // Filtered to surahs the reciter actually has audio for; entry falls
        // back to the nearest available surah so we never queue a known-404.
        fun playSurahQueue(entrySurahId: Int, reciterSlug: String, reciterName: String) {
            val reciter = QuranDataRepository.getReciterBySlug(reciterSlug)
            val allSurahs = QuranDataRepository.getSurahs()
                .filter { reciter.isSurahAvailable(it.id) }
            val allTracks = allSurahs.map { s ->
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
            if (allTracks.isEmpty()) return
            val startIndex = allTracks.indexOfFirst { it.surahId == entrySurahId }
                .takeIf { it >= 0 }
                ?: allTracks.indices.minByOrNull {
                    kotlin.math.abs(allTracks[it].surahId - entrySurahId)
                } ?: 0
            AudioEngine.playQueue(allTracks, startIndex = startIndex)
            rootNavigator.push(NowPlayingScreen())
        }

        fun playWithCurrentReciter(entrySurahId: Int) {
            val reciter = AudioEngine.currentTrack.value?.let {
                QuranDataRepository.getReciterBySlug(it.reciterSlug)
            } ?: QuranDataRepository.getFallbackReciter()
            playSurahQueue(entrySurahId, reciter.slug, reciter.nameEn)
        }

        NoirScreenRoot {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                // Top bar — IconWell back + engraved inset search.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.noirClickable { navigator.pop() },
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
                    SearchInsetField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filters — chrome pill (selected) / ghost pill (resting).
                SearchFilterChipsRow(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (query.isBlank()) {
                    NoirSectionHeader(
                        label = "Recent searches",
                        actionLabel = "Clear all".takeIf { recentQueries.isNotEmpty() },
                        onAction = { SearchHistoryStore.clear() }
                            .takeIf { recentQueries.isNotEmpty() }
                    )
                    if (recentQueries.isEmpty()) {
                        SearchEmptyWell(
                            headline = "Search anything",
                            hint = "Surahs, reciters, or ayah refs like 2:255"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentQueries, key = { "recent-$it" }) { recentQuery ->
                                RecentSearchRow(
                                    query = recentQuery,
                                    onClick = { query = recentQuery },
                                    onRemove = { SearchHistoryStore.remove(recentQuery) }
                                )
                            }
                        }
                    }
                } else {

                    val ayahRef = searchResults.ayahReference
                    val showAyah = ayahRef != null && (selectedFilter == "All" || selectedFilter == "Ayahs")
                    val showSurahs = searchResults.surahs.isNotEmpty() &&
                        (selectedFilter == "All" || selectedFilter == "Surahs")
                    val showReciters = searchResults.reciters.isNotEmpty() &&
                        (selectedFilter == "All" || selectedFilter == "Reciters")
                    if (!showAyah && !showSurahs && !showReciters) {
                        SearchEmptyWell(
                            headline = "No results for \"$query\"",
                            hint = "Try a surah name, reciter, or ayah ref like 2:255"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (showAyah && ayahRef != null) {
                                item(key = "ayah-${ayahRef.surahId}-${ayahRef.ayahNo}") {
                                    AyahQuickPlayCard(
                                        ayahRef = ayahRef,
                                        onClick = { navigator.push(SurahDetailScreen(ayahRef.surahId)) },
                                        onPlay = { playWithCurrentReciter(ayahRef.surahId) }
                                    )
                                }
                            }

                            if (showSurahs) {
                                item(key = "header-surahs") {
                                    NoirSectionHeader(
                                        label = "Surahs",
                                        actionLabel = "${searchResults.surahs.size} shown",
                                        onAction = {}
                                    )
                                }
                                items(searchResults.surahs, key = { "surah-${it.id}" }) { surah ->
                                    SurahNoirRow(
                                        surah = surah,
                                        onOpen = { playWithCurrentReciter(surah.id) },
                                        onPlay = { playWithCurrentReciter(surah.id) }
                                    )
                                }
                            }

                            if (showReciters) {
                                item(key = "header-reciters") {
                                    NoirSectionHeader(
                                        label = "Reciters",
                                        actionLabel = "${searchResults.reciters.size} shown",
                                        onAction = {}
                                    )
                                }
                                items(searchResults.reciters, key = { "reciter-${it.catalogKey()}" }) { reciter ->
                                    ReciterNoirRow(
                                        reciter = reciter,
                                        onOpen = { navigator.push(ReciterProfileScreen(reciter.slug)) },
                                        onPlay = {
                                            playSurahQueue(
                                                entrySurahId = 1,
                                                reciterSlug = reciter.slug,
                                                reciterName = reciter.nameEn
                                            )
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
}

/**
 * Engraved inset search: carved recess ([NoirInsetField]), white cursor,
 * 24% hint ([GhaisNoir.TextDisabled]), ghost clear disc. Search icon lifts
 * from 38% to 100% once a query is present.
 */
@Composable
private fun SearchInsetField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NoirInsetField(modifier = modifier) {
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
                        text = "Search surahs, reciters, ayahs...",
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
 * Filter pills: selected = chrome gradient + near-black label;
 * resting = Fill2 wash + card hairline + 62% label.
 */
@Composable
private fun SearchFilterChipsRow(
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
                        .border(1.dp, Color.White.copy(alpha = 0.35f), GhaisShapes.pill)
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
 * Surah result row in the NoirListRow language: soft card fill + 1px card
 * border + 22% top-only specular, clay monogram well, dual text, Arabic
 * title in white, ghost circular play affordance.
 */
@Composable
private fun SurahNoirRow(
    surah: Surah,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(GhaisShapes.well)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.nameEn.take(1).uppercase(),
                color = GhaisNoir.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
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
                text = "Surah ${surah.id} • ${surah.ayahsCount} ayahs",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = surah.nameAr,
            color = GhaisNoir.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                .background(GhaisNoir.Fill2, CircleShape)
                .noirClickable(onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Surah ${surah.nameEn}",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Reciter result row in the NoirListRow language: color photo well
 * (monogram fallback), dual text, ghost style chip,
 * circular chevron to the profile. Trailing play keeps the queue-start logic.
 */
@Composable
private fun ReciterNoirRow(
    reciter: Reciter,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val followedPhoto = remember(reciter.slug) {
        resolveFollowedQari(reciter.slug)?.photoUrl
    }
    val photoUrl = reciter.imageUrl?.takeIf { it.isNotBlank() } ?: followedPhoto

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.BorderCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = reciter.nameEn,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0f))
                )
            } else {
                Text(
                    text = reciter.nameEn.take(1).uppercase(),
                    color = GhaisNoir.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = reciter.nameEn,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
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
                        text = reciter.style.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        },
                        color = GhaisNoir.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reciter.nameAr,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                .noirClickable(onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play reciter ${reciter.nameEn}",
                tint = GhaisNoir.TextSecondary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RecentSearchRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
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
        IconWell(
            icon = Icons.Default.Search,
            size = 40.dp,
            iconSize = 18.dp,
            contentDescription = null,
            tint = GhaisNoir.TextSecondary
        )
        Text(
            text = query,
            color = GhaisNoir.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                .background(GhaisNoir.Fill2, CircleShape)
                .noirClickable(onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove $query",
                tint = GhaisNoir.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Ghost-well empty state: soft card + clay icon-well, 100% headline + 62% hint.
 */
@Composable
private fun SearchEmptyWell(
    headline: String,
    hint: String
) {
    NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconWell(
                icon = Icons.Default.Search,
                size = 56.dp,
                iconSize = 26.dp,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = headline,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Ayah quick-play plate (signature preserved): elevated Noir card, 38% kicker,
 * white identity, chrome play disc with near-black glyph.
 */
@Composable
fun AyahQuickPlayCard(
    ayahRef: AyahReference,
    onClick: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    NoirCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val surah = QuranData.SURAHS.find { it.id == ayahRef.surahId }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ayah reference found",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${surah?.nameEn} • Ayah ${ayahRef.ayahNo}",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Surah ${ayahRef.surahId} • Ayah ${ayahRef.ayahNo}",
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            ChromeFab(
                icon = Icons.Default.PlayArrow,
                onClick = onPlay,
                size = 48.dp,
                contentDescription = "Play Ayah"
            )
        }
    }
}
