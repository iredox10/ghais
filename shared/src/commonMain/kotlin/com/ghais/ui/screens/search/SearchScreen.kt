package com.ghais.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.repository.DevotionalRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.SearchHistoryStore
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.DevotionalItem
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
import com.ghais.ui.screens.devotional.DevotionPlayerScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import kotlinx.coroutines.delay
import com.ghais.ui.util.realImageUrlOrNull

/**
 * Arabic is a one-line teaser on a result row only; the devotional reader owns
 * the full passage. Same budget the devotional list screen uses.
 */
private const val ARABIC_SNIPPET_MAX: Int = 88

/**
 * Noir Glass — strict monochrome redesign.
 *
 * Canvas #050506 via [NoirScreenRoot] (glow zone -> absolute black + grain).
 * Engraved [NoirInsetField] search (white cursor, 24% hint, ghost clear),
 * chrome/ghost filter pills, [NoirCard] ayah plate with chrome play,
 * result rows in the NoirListRow language with reciter photos in color, ghost-well
 * empty states. Zero hue — state reads through fill, weight and opacity.
 *
 * Searchable content is surahs, reciters, ayah references and the devotional
 * catalogues (duas + ruqiyah, one relevance-ordered group). Signatures and the
 * existing search/filter/play logic and navigation are preserved.
 */
class SearchScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        var query by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf("All") }
        val filters = listOf("All", "Surahs", "Reciters", "Ayahs", "Duas")
        val recentQueries by SearchHistoryStore.recentQueries.collectAsState()

        LaunchedEffect(query) {
            delay(600)
            val meaningfulQuery = query.trim()
            if (meaningfulQuery.length >= 2) {
                SearchHistoryStore.record(meaningfulQuery)
            }
        }

        // Keyed on the cloud-merged catalog so reciters uploaded from the
        // admin panel become searchable as soon as the catalog syncs.
        val browseReciters = rememberBrowseReciters()
        val searchResults = remember(query, browseReciters) { SearchEngine.search(query, browseReciters) }

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
                            hint = "Surahs, reciters, duas, or ayah refs like 2:255"
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
                    // Both devotional kinds live in one group, so the single
                    // "Duas" chip narrows the group rather than selecting one
                    // collection. It is named for the bigger, better-known of
                    // the two rather than inventing a second chip row.
                    val showDevotionals = searchResults.devotionals.isNotEmpty() &&
                        (selectedFilter == "All" || selectedFilter == "Duas")
                    // showDevotionals MUST be in this guard: without it a query
                    // that only matches a devotional (e.g. "istighfar" or a
                    // pasted Arabic phrase) falls through to "No results for …"
                    // with the results sitting right underneath it in memory.
                    if (!showAyah && !showSurahs && !showReciters && !showDevotionals) {
                        SearchEmptyWell(
                            headline = "No results for \"$query\"",
                            hint = "Try a surah name, reciter, dua, or ayah ref like 2:255"
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

                            if (showDevotionals) {
                                item(key = "header-devotionals") {
                                    NoirSectionHeader(
                                        label = "Duas & Ruqiyah",
                                        actionLabel = "${searchResults.devotionals.size} shown",
                                        onAction = {}
                                    )
                                }
                                // Key is `id#index`, not `id`, unlike the surah
                                // and reciter groups above. The devotional seed
                                // reuses short ids across categories (`dua-1`
                                // exists under Daily Dua, Morning, Evening and
                                // After Prayer) and a devotional hit is never a
                                // whole collection, so duplicate ids inside one
                                // result page are the normal case, not the
                                // exception. A duplicate LazyColumn key throws
                                // at runtime; the index suffix only
                                // disambiguates, and ordering still comes from
                                // SearchEngine's relevance sort.
                                itemsIndexed(
                                    items = searchResults.devotionals,
                                    key = { index, item -> "devotional-${item.id}#$index" }
                                ) { _, item ->
                                    DevotionalNoirRow(
                                        item = item,
                                        onOpen = { navigator.push(DevotionPlayerScreen(itemId = item.id)) },
                                        onPlay = { navigator.push(DevotionPlayerScreen(itemId = item.id)) }
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
                            text = "Search surahs, reciters, duas, ayahs...",
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
 * Devotional result row, copy-adapted from [SurahNoirRow]: same soft card fill
 * + 1px card border + 22% top-only specular, same [GhaisShapes.row] clipping,
 * same 52.dp clay monogram well, same horizontal 14.dp / vertical 12.dp padding
 * rhythm, same trailing 32.dp ghost play disc. The differences are content
 * only — a citation subtitle plus a one-line Arabic teaser instead of a bare
 * Arabic title.
 *
 * Two tap targets, mirroring [SurahNoirRow] and [ReciterNoirRow]: the ROW opens
 * the reader, the DISC is the explicit "start the recitation" target. The disc
 * is a separate `noirClickable` nested inside the row's, and Compose does not
 * bubble a click consumed by a child, so a disc tap fires exactly one of them —
 * there is no path that pushes twice. Both currently resolve to the same
 * destination because `DevotionPlayerScreen` has no `startPlaying` parameter
 * yet; the disc's `contentDescription` already names it as the play action, so
 * the day that parameter lands the only edit is `onPlay` gaining
 * `startPlaying = true`.
 *
 * The play disc is OMITTED for an entry whose audio URL does not resolve — that
 * is every dua today. A disabled or dimmed triangle still reads as "broken
 * play" and invites a tap that cannot play, so a read-only row gets a book
 * glyph with a null contentDescription instead, which is what
 * `DevotionListScreen` does. Playability is read from
 * [DevotionalRepository.audioUrlFor], NOT [DevotionalItem.hasAudio]: ruqiyah
 * ships `audioUrl = null` and gets an everyayah URL built at read time, so
 * keying off `hasAudio` would wrongly mute all 20 ruqiyah rows.
 */
@Composable
private fun DevotionalNoirRow(
    item: DevotionalItem,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val playUrl = remember(item) {
        DevotionalRepository.audioUrlFor(item)?.takeIf { it.isNotBlank() }
    }
    val playable = playUrl != null
    val label = item.title.ifBlank { item.kind.label }

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
                text = devotionalMonogram(item),
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
                text = label,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Blank-safe subtitle, same join as the devotional list screen:
            // "Category • Reference" with only the present parts, so a dua with
            // no citation can never render a dangling "• ".
            val subtitle = remember(item) { devotionalSubtitle(item) }
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = GhaisNoir.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val arabic = remember(item) { devotionalArabicSnippet(item.arabic) }
            if (arabic.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                // Arabic never rides a Latin display style: the ambient
                // MaterialTheme style carries negative tracking, and non-zero
                // tracking breaks the cursive joins. arabicSmall plus an
                // explicit letterSpacing = 0 is the fix, inside an RTL provider
                // so the teaser reads right-to-left. Deliberately NOT
                // quranScript/quranFont — that face is for Uthmani Quran text,
                // and dua text is ordinary Naskh.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = arabic,
                        style = GhaisTypography.arabicSmall,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp,
                        color = GhaisNoir.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (playable) {
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
                    contentDescription = "Play $label",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            // No audio: the play affordance is OMITTED, not disabled. The row
            // stays fully tappable — reading is all a dua can do.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .background(GhaisNoir.Fill2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = GhaisNoir.TextDisabled,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Latin-initial monogram for the leading well. Arabic-initial titles fall back
 * to the collection's letter, so the plate is never a glyph the Latin face has
 * no coverage for. Same rule as the devotional list screen's row.
 */
private fun devotionalMonogram(item: DevotionalItem): String =
    item.title.trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.takeIf { it in 'A'..'Z' }
        ?.toString()
        ?: item.kind.label.take(1).uppercase()

/**
 * Citation line: "Category • Reference", joining only the parts that are
 * present, and falling back to the head of the translation when both are
 * missing. Copied from the devotional list screen so the same entry never
 * shows two different subtitles in two places.
 */
private fun devotionalSubtitle(item: DevotionalItem): String {
    val parts = listOfNotNull(
        item.category.trim().takeIf { it.isNotEmpty() },
        item.reference.trim().takeIf { it.isNotEmpty() }
    )
    if (parts.isNotEmpty()) return parts.joinToString(" • ")
    return item.translation.trim().takeIf { it.isNotEmpty() }
        ?.let { if (it.length > 72) it.take(72).trimEnd() + "…" else it }
        .orEmpty()
}

/** Whitespace-collapsed one-line Arabic teaser; the reader owns the full text. */
private fun devotionalArabicSnippet(raw: String): String {
    val flat = raw.replace(Regex("\\s+"), " ").trim()
    if (flat.isEmpty()) return ""
    if (flat.length <= ARABIC_SNIPPET_MAX) return flat
    val cut = flat.take(ARABIC_SNIPPET_MAX)
    val lastSpace = cut.lastIndexOf(' ')
    val head = if (lastSpace > ARABIC_SNIPPET_MAX / 2) cut.take(lastSpace) else cut
    return "$head…"
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
    val photoUrl = realImageUrlOrNull(reciter.imageUrl) ?: followedPhoto

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
