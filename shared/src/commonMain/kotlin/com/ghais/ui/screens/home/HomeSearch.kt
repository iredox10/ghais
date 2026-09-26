package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.SearchHistoryStore
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirCard
import com.ghais.ui.components.noir.NoirInsetField
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.NoirReciterArtwork
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.screens.search.SearchEngine
import com.ghais.ui.screens.search.SearchResults
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlinx.coroutines.delay

/**
 * Phase 4 — Home search.
 *
 * An inline search panel on the Home scroll. Collapsed it is a single
 * [NoirListRow] whose 44dp clay icon-well is pixel-identical to the top-bar
 * search button ([com.ghais.ui.screens.home.HomeTopBar] -> `HomeHeaderWell`:
 * `wellFill()` + `GhaisShapes.well` + `BorderCard`), so the entry point reads as
 * the same control; expanded it becomes the shared engraved [NoirInsetField]
 * with a white cursor, matching the dedicated search screen.
 *
 * Scope: reciters, surahs and the user's own listening history. Matching is
 * NOT reimplemented — [SearchEngine] owns reciter/surah matching (English
 * name, Arabic name, transliteration, slug, surah number) so this panel and
 * the full search screen can never disagree. Only the history filter is local,
 * because no existing search path covers listening history.
 *
 * Not a second search path: the section's "See all" (and the ghost CTA in the
 * empty state) open the dedicated screen, which remains the only place that
 * resolves ayah references such as `2:255`.
 *
 * No new tokens, colours or fonts — [GhaisNoir], [GhaisShapes] and the shared
 * Noir components only.
 */

/** Result caps: this is a fast path, not a replacement for the full screen. */
private const val HOME_SEARCH_RECITER_LIMIT = 4
private const val HOME_SEARCH_SURAH_LIMIT = 5
private const val HOME_SEARCH_HISTORY_LIMIT = 4

/** Mirrors [SearchHistoryStore]'s own persistence floor, so both entry points agree. */
private const val HOME_SEARCH_MIN_QUERY = 2

/** Settle window before a query is persisted — same as the dedicated search screen. */
private const val HOME_SEARCH_RECORD_DEBOUNCE_MS = 600L

/**
 * Home search panel.
 *
 * @param onReciterClick reciter slug -> profile (mirrors the "Qari you follow" row).
 * @param onHistoryPlay surah / history entry -> resume into the player (mirrors "Continue listening").
 * @param onSeeAllHistory overflow of the history group -> history screen.
 * @param onOpenFullSearch "See all" / empty-state CTA -> dedicated search screen.
 */
@Composable
fun HomeSearchSection(
    onReciterClick: (String) -> Unit,
    onHistoryPlay: (JumpBackInItem) -> Unit,
    onSeeAllHistory: () -> Unit,
    onOpenFullSearch: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // Cloud-merged, one row per human — reciters uploaded from the admin panel
    // become searchable as soon as the catalog syncs.
    val browseReciters = rememberBrowseReciters()
    val history by UserUsageRepository.history.collectAsState()
    val recentQueries by SearchHistoryStore.recentQueries.collectAsState()
    val currentTrack by AudioEngine.currentTrack.collectAsState()

    val trimmed = query.trim()

    // Established behaviour: settled, 2+ character queries land in the shared
    // store, so a query typed here shows up under "Recent searches" on the
    // dedicated screen too. Only the store's existing public API is used.
    LaunchedEffect(query) {
        delay(HOME_SEARCH_RECORD_DEBOUNCE_MS)
        if (trimmed.length >= HOME_SEARCH_MIN_QUERY) {
            SearchHistoryStore.record(trimmed)
        }
    }

    val results = remember(trimmed, browseReciters) {
        if (trimmed.isEmpty()) SearchResults(query) else SearchEngine.search(trimmed, browseReciters)
    }
    val historyHits = remember(trimmed, history) { historyMatching(trimmed, history) }

    // The only genuinely async input to this path is catalog resolution. An
    // empty merged catalog means there is nothing to match reciters against
    // yet, so the group says so instead of rendering as if nothing matched.
    val isCatalogResolving = browseReciters.isEmpty()
    val hasAnyHit = results.surahs.isNotEmpty() || results.reciters.isNotEmpty() || historyHits.isNotEmpty()

    // A surah hit has no reciter of its own, so it opens with whatever is
    // already playing (the dedicated screen does the same), falling back to the
    // catalog default. `resumeHistoryEntry` then falls back to the nearest
    // recorded surah when this reciter has no audio for it.
    val playReciter = remember(currentTrack?.reciterSlug) {
        currentTrack?.reciterSlug?.let { QuranDataRepository.getReciterBySlug(it) }
            ?: QuranDataRepository.getFallbackReciter()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
            if (expanded) {
                HomeSearchInsetField(
                    query = query,
                    onQueryChange = { query = it },
                    onCollapse = {
                        query = ""
                        expanded = false
                    }
                )
            } else {
                NoirListRow(
                    title = "Search",
                    subtitle = "Reciters, surahs, your history",
                    icon = Icons.Filled.Search,
                    onClick = { expanded = true }
                )
            }
        }

        if (!expanded) return@Column

        Spacer(Modifier.height(10.dp))
        when {
            // ---------------------------------------------------------- empty
            trimmed.isEmpty() -> HomeSearchRecentBlock(
                recentQueries = recentQueries,
                onPick = { query = it },
                onClearAll = { SearchHistoryStore.clear() },
                onOpenFullSearch = onOpenFullSearch
            )

            // ------------------------------------------------- below the floor
            trimmed.length < HOME_SEARCH_MIN_QUERY -> HomeSearchNoteWell(
                headline = "Keep typing",
                hint = "Two characters is enough — search by reciter, surah name, or surah number."
            )

            // --------------------------------------------------- no results
            !hasAnyHit -> {
                HomeSearchNoteWell(
                    headline = "No results for \"$trimmed\"",
                    hint = if (results.ayahReference != null) {
                        "That reads as an ayah reference — open the full search to jump straight to it."
                    } else {
                        "Try a surah name, a reciter, or a number like 36."
                    }
                )
                if (results.ayahReference != null) {
                    Spacer(Modifier.height(12.dp))
                    GhostPillButton(
                        text = "Open full search",
                        onClick = onOpenFullSearch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ------------------------------------------------------- results
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (results.reciters.isNotEmpty()) {
                        NoirSectionHeader(
                            label = "Reciters • ${results.reciters.size}",
                            modifier = Modifier.padding(top = 8.dp),
                            actionLabel = "See all"
                                .takeIf { results.reciters.size > HOME_SEARCH_RECITER_LIMIT },
                            onAction = onOpenFullSearch
                                .takeIf { results.reciters.size > HOME_SEARCH_RECITER_LIMIT }
                        )
                        results.reciters.take(HOME_SEARCH_RECITER_LIMIT).forEach { reciter ->
                            HomeSearchReciterRow(reciter = reciter) {
                                onReciterClick(reciter.slug)
                            }
                        }
                    } else if (isCatalogResolving) {
                        HomeSearchNoteWell(
                            headline = "Preparing reciters",
                            hint = "The reciter catalog is still loading — surahs and your history are searchable now."
                        )
                    }

                    if (results.surahs.isNotEmpty()) {
                        NoirSectionHeader(
                            label = "Surahs • ${results.surahs.size}",
                            modifier = Modifier.padding(top = 8.dp),
                            actionLabel = "See all"
                                .takeIf { results.surahs.size > HOME_SEARCH_SURAH_LIMIT },
                            onAction = onOpenFullSearch
                                .takeIf { results.surahs.size > HOME_SEARCH_SURAH_LIMIT }
                        )
                        results.surahs.take(HOME_SEARCH_SURAH_LIMIT).forEach { surah ->
                            HomeSearchSurahRow(surah = surah) {
                                onHistoryPlay(surahEntryFor(surah, playReciter))
                            }
                        }
                    }

                    if (historyHits.isNotEmpty()) {
                        NoirSectionHeader(
                            label = "Your history • ${historyHits.size}",
                            modifier = Modifier.padding(top = 8.dp),
                            actionLabel = "See all"
                                .takeIf { historyHits.size > HOME_SEARCH_HISTORY_LIMIT },
                            onAction = onSeeAllHistory
                                .takeIf { historyHits.size > HOME_SEARCH_HISTORY_LIMIT }
                        )
                        historyHits.take(HOME_SEARCH_HISTORY_LIMIT).forEach { entry ->
                            HomeSearchHistoryRow(entry = entry) { onHistoryPlay(entry) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Engraved input. Same recipe as the dedicated search screen: [NoirInsetField]
 * recess, search icon lifting from 38% to 100% once a query is present, white
 * cursor, 24% placeholder. The single trailing disc clears an existing query,
 * or collapses the panel when there is nothing to clear.
 */
@Composable
private fun HomeSearchInsetField(
    query: String,
    onQueryChange: (String) -> Unit,
    onCollapse: () -> Unit
) {
    NoirInsetField(modifier = Modifier.fillMaxWidth()) {
        // fillMaxWidth is required: the weight(1f) text box below is only
        // meaningful against a full-width row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = if (query.isNotEmpty()) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search surahs, reciters, history...",
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
            // One disc, two jobs: clear a typed query, or collapse the panel
            // once there is nothing left to clear.
            Spacer(Modifier.width(8.dp))
            HomeSearchGhostDisc(
                icon = Icons.Filled.Close,
                contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search",
                onClick = { if (query.isNotEmpty()) onQueryChange("") else onCollapse() }
            )
        }
    }
}

/** 22dp ghost disc for clear / close — same fill + rim as the search screen's. */
@Composable
private fun HomeSearchGhostDisc(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(GhaisNoir.Fill2, CircleShape)
            .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
            .noirClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GhaisNoir.TextSecondary,
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * Reciter result: real photo through [NoirReciterArtwork] (cloud lookup +
 * letter-monogram fallback), riwayah chip, Arabic name, ghost chevron.
 * Same row recipe as the search screen, trimmed to a 48dp plate for Home.
 */
@Composable
private fun HomeSearchReciterRow(reciter: Reciter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoirReciterArtwork(
            photoUrl = reciter.imageUrl,
            nameEn = reciter.nameEn,
            slug = reciter.slug,
            shape = CircleShape,
            size = 48.dp,
            monogramSize = 18.sp,
            ring = true,
            scrimAlpha = 0f
        )
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
                HomeSearchChip(
                    text = reciter.riwayah.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                )
                Spacer(Modifier.width(8.dp))
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
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${reciter.nameEn}",
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Surah result: clay monogram well, English name + number/ayah count, Arabic
 * name in white, ghost chevron. Identity is the surah's, not a placeholder.
 */
@Composable
private fun HomeSearchSurahRow(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(GhaisNoir.wellFill(), GhaisShapes.well)
                .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.well),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.nameEn.take(1).uppercase(),
                color = GhaisNoir.TextPrimary,
                fontSize = 18.sp,
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
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (surah.ayahsCount == 1) {
                    "Surah ${surah.id} • 1 ayah"
                } else {
                    "Surah ${surah.id} • ${surah.ayahsCount} ayahs"
                },
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
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Play ${surah.nameEn}",
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Listening-history result: the reciter's real portrait, the surah's English
 * and Arabic identity, and how far in the saved position sits — so the row
 * reads as "where you left off" rather than a bare list item. Tapping resumes
 * exactly like the "Continue listening" section does.
 */
@Composable
private fun HomeSearchHistoryRow(entry: JumpBackInItem, onClick: () -> Unit) {
    val surah = QuranDataRepository.getSurahById(entry.surahId)
    val reciterName = remember(entry.reciterSlug) {
        QuranDataRepository.getReciterBySlug(entry.reciterSlug).nameEn
    }
    val photoUrl = rememberCloudPhoto(entry.reciterSlug)
    val percent = (entry.progress.coerceIn(0f, 1f) * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cloud portrait first (same order as "Continue listening"), then the
        // stored cover, then the monogram inside the shared artwork well.
        NoirArtworkWell(
            coverUrl = photoUrl ?: entry.coverUrl,
            monogram = reciterName.firstOrNull()?.uppercase() ?: "Q",
            shape = CircleShape,
            size = 48.dp,
            monogramSize = 18.sp,
            ring = true,
            grayscale = false,
            scrimAlpha = 0f
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = surah?.nameEn?.takeIf { it.isNotBlank() } ?: entry.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Surah ${entry.surahId} • $reciterName • $percent%",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (surah != null && surah.nameAr.isNotBlank()) {
            Text(
                text = surah.nameAr,
                color = GhaisNoir.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(10.dp))
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, GhaisNoir.BorderGhost, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Resume ${entry.title}",
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Empty state: the persisted recent queries as ghost pills (tapping re-runs the
 * query), or a note well when there is no history yet. Either way the panel
 * ends on a live "Open full search" CTA rather than a dead end.
 */
@Composable
private fun HomeSearchRecentBlock(
    recentQueries: List<String>,
    onPick: (String) -> Unit,
    onClearAll: () -> Unit,
    onOpenFullSearch: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (recentQueries.isEmpty()) {
            HomeSearchNoteWell(
                headline = "Search anything",
                hint = "Find a reciter, jump to a surah, or return to something you listened to."
            )
        } else {
            NoirSectionHeader(
                label = "Recent searches",
                modifier = Modifier.padding(top = 8.dp),
                actionLabel = "Clear all",
                onAction = onClearAll
            )
            LazyRow(
                contentPadding = PaddingValues(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentQueries, key = { "recent-$it" }) { recent ->
                    GhostPillButton(text = recent, onClick = { onPick(recent) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GhostPillButton(
            text = "Open full search",
            onClick = onOpenFullSearch,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Non-blank fallback for every non-result state: soft card, clay icon-well,
 * 100% headline + 62% hint. Same construction the search screen uses for its
 * empty and no-result wells.
 */
@Composable
private fun HomeSearchNoteWell(headline: String, hint: String) {
    NoirCard(soft = true, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconWell(
                icon = Icons.Filled.Search,
                size = 52.dp,
                iconSize = 24.dp,
                contentDescription = null,
                tint = GhaisNoir.TextTertiary
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = headline,
                color = GhaisNoir.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                color = GhaisNoir.TextSecondary,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Resting ghost pill: Fill2 wash + hairline + 62% label. */
@Composable
private fun HomeSearchChip(text: String) {
    Box(
        modifier = Modifier
            .background(GhaisNoir.Fill2, GhaisShapes.pill)
            .border(1.dp, GhaisNoir.BorderGhost, GhaisShapes.pill)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GhaisNoir.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * History matching. `JumpBackInItem.title` is the surah's English name and
 * `subtitle` is "<reciter> • <remaining>" (see `UserUsageRepository`), so both
 * halves of a history row are searchable, plus the raw surah number. The store
 * is already most-recent-first, so results keep that order.
 */
private fun historyMatching(query: String, history: List<JumpBackInItem>): List<JumpBackInItem> {
    if (query.isEmpty()) return emptyList()
    val lower = query.lowercase()
    return history.filter { entry ->
        entry.title.lowercase().contains(lower) ||
            entry.subtitle.lowercase().contains(lower) ||
            entry.surahId.toString() == lower
    }
}

/**
 * Adapts a catalog surah into the history entry shape that the Home resume
 * helper already consumes, so a search hit and a history row share one
 * playback path. Progress starts at 0 — a surah hit is a fresh start, unlike a
 * history row which carries its saved position.
 */
private fun surahEntryFor(surah: Surah, reciter: Reciter): JumpBackInItem = JumpBackInItem(
    title = surah.nameEn,
    subtitle = reciter.nameEn,
    progress = 0f,
    coverUrl = "",
    surahId = surah.id,
    reciterSlug = reciter.slug,
    positionMs = 0L,
    durationMs = surah.ayahsCount * 15_000L,
    lastPlayedTimestampMs = 0L
)
