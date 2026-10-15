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
import androidx.compose.material.icons.filled.Stop
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
import com.ghais.data.repository.ChartsRepository
import com.ghais.data.repository.DevotionalRepository
import com.ghais.data.repository.EditorialPlaylist
import com.ghais.data.repository.EditorialRepository
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranAyahRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.SearchHistoryStore
import com.ghais.data.repository.TranslationPrefs
import com.ghais.data.repository.reciterCountryKey
import com.ghais.data.repository.rememberBrowseReciters
import com.ghais.data.repository.resolveFollowedQari
import com.ghais.data.repository.toTrackItems
import com.ghais.data.search.AyahSearchIndex
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.DevotionalItem
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.flags.HookFlags
import com.ghais.player.AudioEngine
import com.ghais.player.ReciterPreview
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
import com.ghais.ui.screens.mood.AVAILABLE_MOODS
import com.ghais.ui.screens.mood.MoodItem
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.playlists.PlaylistDetailsScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.reciters.RegionRecitersScreen
import com.ghais.ui.screens.surah.SurahDetailScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import kotlinx.coroutines.delay
import kotlin.time.Clock
import com.ghais.ui.util.realImageUrlOrNull

/**
 * Arabic is a one-line teaser on a result row only; the devotional reader owns
 * the full passage. Same budget the devotional list screen uses.
 */
private const val ARABIC_SNIPPET_MAX: Int = 88

/** Lookback window for the hub's "New playlists" section. */
private const val NEW_PLAYLIST_WINDOW_MS: Long = 30L * 24L * 60L * 60L * 1000L

/** Nations shown in the hub's "Browse by nation" chip row. */
private const val HUB_NATION_COUNT: Int = 8

/** Rows shown in the hub's "Top qaris this week" / "New playlists" sections. */
private const val HUB_TOP_ROW_COUNT: Int = 5

/**
 * A12 exploratory search.
 *
 * [EXPLORE_TRY_QUERIES] are the blank-state "Try:" chips — tapping one fills
 * the query, which flips the screen into results and runs it (plain state
 * assignment, no separate "run" path). [EXPLORE_FALLBACK_COUNT] caps how many
 * intent-ordered browse rows may stand in for a group the literal engine
 * missed, so an exploratory phrase never dumps the whole catalog.
 */
private val EXPLORE_TRY_QUERIES: List<String> = listOf(
    "calm recitation for night",
    "short powerful surahs",
    "new voices like Mishary",
    "rainy-day murattal"
)
private const val EXPLORE_FALLBACK_COUNT: Int = 8

/** Followed-qari shortcut chips shown in the hub's taste section. */
private const val HUB_TASTE_ROW_COUNT: Int = 5

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

        // A12 taste-aware shortcuts: the follow set is read-only here — this
        // screen never follows/unfollows (the reciter profile owns writes).
        val followedSlugs by FollowStore.followedSlugs.collectAsState()

        // A12 inline 30s preview, via Agent A3's ReciterPreview
        // (previewUrl/playPreview/stopPreview — Ayat al-Kursi 2:255 sample,
        // session save/restore, 30s auto-stop all live there). This screen
        // only observes previewSlug for the row's stop-state and stops the
        // sample best-effort on navigate-away (restore = false: ending the
        // sample must never autoplay a session the user already left).
        // The Unit-keyed DisposableEffect reads through rememberUpdatedState
        // so it always sees the live slug instead of the first-composition
        // null it would otherwise capture.
        val previewSlug by ReciterPreview.previewSlug.collectAsState()
        val latestPreviewSlug by rememberUpdatedState(previewSlug)
        DisposableEffect(Unit) {
            onDispose {
                if (latestPreviewSlug != null) {
                    try {
                        ReciterPreview.stopPreview(restore = false)
                    } catch (_: Exception) {
                    }
                }
            }
        }

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

        // Ayah-text search (AyahSearchIndex wave): edition-scoped, local-only.
        // SurahDetailScreen takes only surahId (verified) — there is no
        // scroll-to-ayah target, so rows below push the surah top and the play
        // disc starts ayah-mode playback from the hit ayah instead.
        val selectedEdition by TranslationPrefs.selectedEdition.collectAsState()
        val ayahCacheGen by QuranAyahRepository.cacheGen.collectAsState()

        // Edition invalidation: a switch retires the previous edition's verses
        // out of the repository (onTranslationChanged), so the index must
        // forget them too rather than serving stale-edition hits until the
        // first surah of the new edition is indexed (indexSurah's backstop
        // only fires then).
        LaunchedEffect(selectedEdition) { AyahSearchIndex.clear() }

        val trimmedQuery = query.trim()
        // A12 exploratory intent: natural phrases ("short powerful surahs")
        // carry no literal match, so when the engine draws a blank for a
        // group the intent-ordered browse list stands in (capped) instead of
        // "No results". Engine hits always win when present — the fallbacks
        // below are read only in that case.
        val exploreIntent = if (!HookFlags.exploreEnabled) null
        else remember(trimmedQuery) { parseExploreIntent(trimmedQuery) }
        val exploreOutcome = remember(exploreIntent, browseReciters) {
            val intent = exploreIntent ?: return@remember null
            applyIntent(
                reciters = browseReciters.map { r ->
                    ExploreReciterMeta(
                        slug = r.slug,
                        name = r.nameEn,
                        style = r.style,
                        tempo = r.tempo,
                        // The catalog carries no added-at timestamp, so
                        // NEW_FIRST currently preserves browse order until
                        // that metadata lands (see ExploreIntents docs).
                        isNew = false
                    )
                },
                surahs = QuranData.SURAHS.map { s ->
                    ExploreSurahMeta(id = s.id, ayahsCount = s.ayahsCount)
                },
                intent = intent
            )
        }
        val fallbackSurahs = remember(exploreOutcome) {
            exploreOutcome?.surahIds
                ?.mapNotNull { id -> QuranData.SURAHS.firstOrNull { it.id == id } }
                ?.take(EXPLORE_FALLBACK_COUNT)
                .orEmpty()
        }
        val fallbackReciters = remember(exploreOutcome, browseReciters) {
            exploreOutcome?.reciterSlugs
                ?.mapNotNull { slug -> browseReciters.firstOrNull { it.slug == slug } }
                ?.take(EXPLORE_FALLBACK_COUNT)
                .orEmpty()
        }
        // Mirrors AyahSearchIndex script routing: any Arabic-script char
        // searches Arabic text; otherwise Latin needs length>=3 to be worth
        // indexing/matching (shorter Latin substrings match everything).
        val ayahTextRelevant = trimmedQuery.isNotEmpty() &&
            (trimmedQuery.any { isArabicScript(it) } || trimmedQuery.length >= 3)
        val ayahFeedKey = if (ayahTextRelevant) trimmedQuery else null
        var ayahIndexTick by remember { mutableIntStateOf(0) }
        // Feed ONLY locally cached + bundled verses — never a network fan-out
        // from the UI. isAyahAvailable is a side-effect-free synchronous cache
        // read (no getCachedSurahIds exists on the repository); the per-ayah
        // gate means getAyahImmediate is called on cache hits only, so no
        // background fetch is ever triggered from here. Coverage: bundled
        // seeds (surahs 1, 108, 112, 113, 114 fully + 18:1-5 partial, default
        // edition only) plus any surah previously downloaded/persisted on
        // device; non-default editions search downloaded surahs only.
        // Side effect: feeding marks fed surahs as requested in the repository
        // (bounded set of 8 — only affects which surahs refetch on an edition
        // switch, which is desirable here).
        LaunchedEffect(ayahFeedKey, ayahCacheGen, selectedEdition) {
            if (ayahFeedKey == null) return@LaunchedEffect
            feedCachedAyahs(selectedEdition)
            ayahIndexTick++
        }
        val ayahTextHits = remember(trimmedQuery, ayahIndexTick, selectedEdition) {
            if (!ayahTextRelevant) emptyList()
            else AyahSearchIndex.search(trimmedQuery)
        }
        val ayahIndexedSurahs = remember(trimmedQuery, ayahIndexTick, selectedEdition) {
            if (!ayahTextRelevant) 0 else AyahSearchIndex.indexedSurahCount()
        }

        // Browse-hub sources (blank-query state only; search execution below
        // never reads them). Charts publish instantly from disk cache, then
        // refetch when stale; editorial "new" derives from the synced shelf.
        val chartsSnapshot by ChartsRepository.snapshot.collectAsState()
        val editorialPlaylists by EditorialRepository.playlists.collectAsState()
        LaunchedEffect(Unit) { ChartsRepository.refresh() }

        // Top nations by reciter count, grouped on the flag-insensitive
        // country key with the richest raw spelling as the label — the same
        // grouping RecitersScreen uses, so a chip pushes a region screen that
        // contains exactly these reciters.
        val topNations = remember(browseReciters) {
            browseReciters
                .groupBy { reciterCountryKey(it.country).ifBlank { "other" } }
                .map { (key, reciters) ->
                    val label = reciters
                        .map { it.country.trim() }
                        .filter { it.isNotBlank() }
                        .maxByOrNull { it.count { ch -> !ch.isLetterOrDigit() && ch != ' ' } }
                        ?.takeIf { reciterCountryKey(it) == key }
                        ?: reciters.first().country.ifBlank { "Other" }
                    label to reciters.size
                }
                .sortedByDescending { (_, count) -> count }
                .take(HUB_NATION_COUNT)
        }
        val topQaris = remember(chartsSnapshot) {
            chartsSnapshot?.topQaris7d.orEmpty()
                .take(HUB_TOP_ROW_COUNT)
                .mapNotNull { QuranDataRepository.getBrowseReciterBySlug(it) }
        }
        val newPlaylists = remember(editorialPlaylists) {
            EditorialRepository.newSince(
                Clock.System.now().toEpochMilliseconds() - NEW_PLAYLIST_WINDOW_MS
            ).take(HUB_TOP_ROW_COUNT)
        }
        // A12 taste-aware shortcuts, resolved against the live browse catalog
        // so cloud-added reciters work the moment they sync. Unresolvable
        // slugs (removed or cloud-pending rows) are skipped, never crashed on.
        val followedReciters = remember(followedSlugs, browseReciters) {
            followedSlugs
                .mapNotNull { slug ->
                    browseReciters.firstOrNull { it.slug.equals(slug, ignoreCase = true) }
                }
                .distinctBy { it.catalogKey() }
                .take(HUB_TASTE_ROW_COUNT)
        }

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

        // Ayah-mode playback of a surah starting FROM a hit ayah — same
        // current/fallback reciter resolution as playWithCurrentReciter, but
        // playSurahInAyahMode (per-ayah queue from startAyahNo) instead of the
        // full-surah playSurahQueue. Pushes NowPlaying like every other play
        // entry point in this file.
        fun playAyahHit(surahId: Int, ayahNo: Int) {
            val reciter = AudioEngine.currentTrack.value?.let {
                QuranDataRepository.getReciterBySlug(it.reciterSlug)
            } ?: QuranDataRepository.getFallbackReciter()
            AudioEngine.playSurahInAyahMode(
                surahId = surahId,
                reciterSlug = reciter.slug,
                startAyahNo = ayahNo
            )
            rootNavigator.push(NowPlayingScreen())
        }

        // Single-surah playback for a hub mood tap — the exact wiring
        // MoodScreen uses (Mishary queue entry, guarded on audio
        // availability), minus the sheet-pop which has no meaning here.
        fun playMood(mood: MoodItem) {
            val reciter = QuranDataRepository.getReciterBySlug("mishary")
            if (reciter.isSurahAvailable(mood.surahId)) {
                AudioEngine.playTrack(
                    TrackItem(
                        reciterSlug = "mishary",
                        reciterName = "Sheikh Mishary Rashid Alafasy",
                        surahId = mood.surahId,
                        surahNameEn = mood.surahNameEn,
                        surahNameAr = mood.surahNameAr,
                        ayahNo = 0,
                        audioUrl = mood.audioUrl,
                        durationMs = 300000L
                    )
                )
            }
        }

        // A12 inline 30s reciter preview toggle, straight through
        // ReciterPreview (owns the sample URL, session save/restore and the
        // 30s auto-stop). At most one preview plays: playPreview captures the
        // session only when not already previewing, and re-tapping the active
        // row stops it. Errors are swallowed — a preview must never break
        // search (playPreview already no-ops null on unresolvable URLs).
        fun toggleSearchPreview(reciter: Reciter) {
            try {
                if (previewSlug == reciter.slug) ReciterPreview.stopPreview()
                else ReciterPreview.playPreview(reciter)
            } catch (_: Exception) {
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
                    // Recents stay on top; the browse hub lives below in the
                    // same scroll. Every hub section hides when its data is
                    // empty. Order: moods, nations, top qaris, new playlists.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (recentQueries.isEmpty()) {
                            item(key = "search-empty") {
                                SearchEmptyWell(
                                    headline = "Search anything",
                                    hint = "Surahs, reciters, duas, or ayah refs like 2:255"
                                )
                            }
                        } else {
                            items(recentQueries, key = { "recent-$it" }) { recentQuery ->
                                RecentSearchRow(
                                    query = recentQuery,
                                    onClick = { query = recentQuery },
                                    onRemove = { SearchHistoryStore.remove(recentQuery) }
                                )
                            }
                        }

                        // A12 exploratory entry points (gated): natural-language
                        // chips that fill the query and run it — a non-blank
                        // query flips this screen into results, so plain state
                        // assignment IS the run path (no separate submit needed).
                        if (HookFlags.exploreEnabled) {
                            item(key = "hub-try-gap") { Spacer(Modifier.height(8.dp)) }
                            item(key = "hub-try-header") {
                                NoirSectionHeader(label = "Try")
                            }
                            item(key = "hub-try-chips") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(EXPLORE_TRY_QUERIES, key = { "hub-try-$it" }) { suggestion ->
                                        ExploreTryChip(
                                            label = suggestion,
                                            onClick = { query = suggestion }
                                        )
                                    }
                                }
                            }
                        }

                        // A12 taste-aware shortcuts (gated), read-only off
                        // FollowStore: one chip per followed qari, filling the
                        // query with their name so the results below answer
                        // it. Hidden when the follow set is empty or
                        // unresolvable.
                        if (HookFlags.exploreEnabled && followedReciters.isNotEmpty()) {
                            item(key = "hub-taste-gap") { Spacer(Modifier.height(8.dp)) }
                            item(key = "hub-taste-header") {
                                NoirSectionHeader(
                                    label = "Because you follow ${followedReciters.first().nameEn}"
                                )
                            }
                            item(key = "hub-taste-chips") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(
                                        followedReciters,
                                        key = { "hub-taste-${it.catalogKey()}" }
                                    ) { reciter ->
                                        ExploreTryChip(
                                            label = "More like ${shortReciterName(reciter.nameEn)}",
                                            onClick = { query = reciter.nameEn }
                                        )
                                    }
                                }
                            }
                        }

                        if (AVAILABLE_MOODS.isNotEmpty()) {
                            item(key = "hub-mood-gap") { Spacer(Modifier.height(8.dp)) }
                            item(key = "hub-mood-header") {
                                NoirSectionHeader(label = "Browse by mood")
                            }
                            items(
                                AVAILABLE_MOODS.chunked(2),
                                key = { "hub-mood-${it.first().id}" }
                            ) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { mood ->
                                        MoodHubTile(
                                            mood = mood,
                                            onClick = { playMood(mood) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        if (topNations.isNotEmpty()) {
                            item(key = "hub-nation-gap") { Spacer(Modifier.height(8.dp)) }
                            item(key = "hub-nation-header") {
                                NoirSectionHeader(label = "Browse by nation")
                            }
                            item(key = "hub-nation-chips") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(topNations, key = { "hub-nation-${it.first}" }) { nation ->
                                        NationHubChip(
                                            label = nation.first,
                                            count = nation.second,
                                            onClick = { navigator.push(RegionRecitersScreen(nation.first)) }
                                        )
                                    }
                                }
                            }
                        }

                        if (topQaris.isNotEmpty()) {
                            item(key = "hub-qaris-gap") { Spacer(Modifier.height(8.dp)) }
                            item(key = "hub-qaris-header") {
                                NoirSectionHeader(label = "Top qaris this week")
                            }
                            items(topQaris, key = { "hub-qari-${it.catalogKey()}" }) { reciter ->
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

                        if (newPlaylists.isNotEmpty()) {
                            item(key = "hub-playlists-gap") { Spacer(Modifier.height(8.dp)) }
                            item(key = "hub-playlists-header") {
                                NoirSectionHeader(label = "New playlists")
                            }
                            items(newPlaylists, key = { "hub-playlist-${it.id}" }) { playlist ->
                                PlaylistHubRow(
                                    playlist = playlist,
                                    // Tap-to-play: PlaylistDetailsScreen only knows mood ids and
                                    // would show the wrong content for an editorial id.
                                    onOpen = {
                                        val tracks = playlist.toTrackItems()
                                        if (tracks.isNotEmpty()) {
                                            AudioEngine.playQueue(tracks, startIndex = 0)
                                            navigator.push(NowPlayingScreen())
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {

                    val ayahRef = searchResults.ayahReference
                    val showAyah = ayahRef != null && (selectedFilter == "All" || selectedFilter == "Ayahs")
                    // Ayah-text hits live alongside (never replacing) the
                    // single-ref quick-play card above: the card answers
                    // "2:255", the section below answers "mercy" / pasted
                    // Arabic. Same All/Ayahs filter gating as the card.
                    val showAyahHits = ayahTextHits.isNotEmpty() &&
                        (selectedFilter == "All" || selectedFilter == "Ayahs")
                    // A12: intent fallbacks fill groups the literal engine
                    // missed, so "short powerful surahs" still answers. The
                    // using*Fallback flags feed only the honest-coverage note
                    // under the header — ranking itself is pure applyIntent.
                    val effectiveSurahs = if (searchResults.surahs.isNotEmpty()) {
                        searchResults.surahs
                    } else {
                        fallbackSurahs
                    }
                    val usingSurahFallback = searchResults.surahs.isEmpty() &&
                        fallbackSurahs.isNotEmpty()
                    val effectiveReciters = if (searchResults.reciters.isNotEmpty()) {
                        searchResults.reciters
                    } else {
                        fallbackReciters
                    }
                    val usingReciterFallback = searchResults.reciters.isEmpty() &&
                        fallbackReciters.isNotEmpty()
                    val showSurahs = effectiveSurahs.isNotEmpty() &&
                        (selectedFilter == "All" || selectedFilter == "Surahs")
                    val showReciters = effectiveReciters.isNotEmpty() &&
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
                    if (!showAyah && !showAyahHits && !showSurahs && !showReciters && !showDevotionals) {
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

                            // Ayah-text section: snippet + owning surah + S:A
                            // ref per row. Row tap pushes SurahDetailScreen for
                            // the hit's surah — scroll-to-ayah is unavailable
                            // (that screen takes only surahId), so the row
                            // lands at the surah top; the play disc is the
                            // precise target (ayah-mode from the hit ayah).
                            // The coverage line is honest by construction: the
                            // index only ever holds locally cached + bundled
                            // verses (see the feed above), so N is exactly the
                            // searchable set.
                            if (showAyahHits) {
                                item(key = "header-ayah-hits") {
                                    NoirSectionHeader(
                                        label = "Ayahs",
                                        actionLabel = "${ayahTextHits.size} shown",
                                        onAction = {}
                                    )
                                }
                                item(key = "ayah-hits-coverage") {
                                    Text(
                                        text = if (ayahIndexedSurahs == 1) {
                                            "Searches 1 downloaded surah"
                                        } else {
                                            "Searches $ayahIndexedSurahs downloaded surahs"
                                        },
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                items(
                                    ayahTextHits,
                                    key = { "ayah-hit-${it.surahId}-${it.ayahNo}" }
                                ) { hit ->
                                    AyahTextHitRow(
                                        hit = hit,
                                        onOpen = { navigator.push(SurahDetailScreen(hit.surahId)) },
                                        onPlay = { playAyahHit(hit.surahId, hit.ayahNo) }
                                    )
                                }
                            }

                            if (showSurahs) {
                                item(key = "header-surahs") {
                                    NoirSectionHeader(
                                        label = "Surahs",
                                        actionLabel = "${effectiveSurahs.size} shown",
                                        onAction = {}
                                    )
                                }
                                if (usingSurahFallback) {
                                    item(key = "surah-explore-note") {
                                        ExploreFallbackNote(text = "Exploratory picks for \"$query\"")
                                    }
                                }
                                items(effectiveSurahs, key = { "surah-${it.id}" }) { surah ->
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
                                        actionLabel = "${effectiveReciters.size} shown",
                                        onAction = {}
                                    )
                                }
                                if (usingReciterFallback) {
                                    item(key = "reciter-explore-note") {
                                        ExploreFallbackNote(text = "Exploratory picks for \"$query\"")
                                    }
                                }
                                items(effectiveReciters, key = { "reciter-${it.catalogKey()}" }) { reciter ->
                                    ReciterNoirRow(
                                        reciter = reciter,
                                        onOpen = { navigator.push(ReciterProfileScreen(reciter.slug)) },
                                        onPlay = {
                                            playSurahQueue(
                                                entrySurahId = 1,
                                                reciterSlug = reciter.slug,
                                                reciterName = reciter.nameEn
                                            )
                                        },
                                        previewing = previewSlug == reciter.slug,
                                        onPreview = { toggleSearchPreview(reciter) }
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
 *
 * A12 adds the inline 30s preview disc (optional, results only — hub callers
 * omit it and the row renders exactly as before): a "30s" ghost disc that
 * inverts to a stop disc while its preview plays. Playback itself lives in
 * ReciterPreview (Agent A3); the row only reflects previewing/onPreview.
 */
@Composable
private fun ReciterNoirRow(
    reciter: Reciter,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    previewing: Boolean = false,
    onPreview: (() -> Unit)? = null
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
        if (onPreview != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, GhaisNoir.BorderGhost, CircleShape)
                    .background(
                        if (previewing) GhaisNoir.TextPrimary else GhaisNoir.Fill2,
                        CircleShape
                    )
                    .noirClickable(onPreview),
                contentAlignment = Alignment.Center
            ) {
                if (previewing) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop preview ${reciter.nameEn}",
                        tint = GhaisNoir.OnChrome,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = "30s",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
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
 * Browse-hub mood tile: NoirListRow language (soft card fill + 1px card
 * border + 22% top-only specular, [GhaisShapes.row] clipping) with an
 * [IconWell] and dual text, sized to share a two-column row. Tap runs the
 * same single-surah playback MoodScreen uses.
 */
@Composable
private fun MoodHubTile(
    mood: MoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(GhaisShapes.row)
            .background(GhaisNoir.cardFillSoft(), GhaisShapes.row)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.row)
            .topSpecular(inset = 22.dp)
            .noirClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconWell(
            icon = mood.icon,
            size = 40.dp,
            iconSize = 20.dp,
            contentDescription = null,
            tint = GhaisNoir.TextSecondary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = mood.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = mood.subtitle,
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Browse-hub nation chip: resting ghost pill (Fill2 wash + card hairline +
 * 62% label), same recipe as an unselected filter chip. Label carries the
 * reciter count so the ordering reads as a ranking.
 */
@Composable
private fun NationHubChip(
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label • $count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GhaisNoir.TextSecondary
        )
    }
}

/**
 * A12 exploratory suggestion chip: resting ghost pill, same recipe as
 * [NationHubChip] minus the count. Used by both the "Try" row and the
 * "Because you follow" shortcut row — both fill the query on tap.
 */
@Composable
private fun ExploreTryChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(GhaisShapes.pill)
            .background(GhaisNoir.Fill2)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
            .noirClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GhaisNoir.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A12 honest-coverage note under a header served from the intent fallback,
 * so an exploratory answer never masquerades as a literal match.
 */
@Composable
private fun ExploreFallbackNote(text: String) {
    Text(
        text = text,
        color = GhaisNoir.TextTertiary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Compact chip label from a display name ("Mishary Rashid Alafasy" ->
 * "Mishary"). Blank-safe: falls back to the trimmed whole string.
 */
private fun shortReciterName(nameEn: String): String {
    val first = nameEn.trim().substringBefore(' ')
    return first.takeIf { it.isNotEmpty() } ?: nameEn.trim()
}

/**
 * Browse-hub playlist row in the NoirListRow language: soft card fill + 1px
 * card border + 22% top-only specular, book IconWell, dual text, circular
 * chevron. The row opens [PlaylistDetailsScreen], mirroring the Collections
 * tile navigation.
 */
@Composable
private fun PlaylistHubRow(
    playlist: EditorialPlaylist,
    onOpen: () -> Unit
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
        IconWell(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            size = 40.dp,
            iconSize = 18.dp,
            contentDescription = null,
            tint = GhaisNoir.TextSecondary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = playlist.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${playlist.items.size} tracks",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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

/**
 * Ayah-text hit row in the NoirListRow language, copy-adapted from
 * [SurahNoirRow]: same soft card fill + 1px card border + 22% top-only
 * specular, same [GhaisShapes.row] clipping, same 52.dp well, same horizontal
 * 14.dp / vertical 12.dp padding rhythm, same trailing 32.dp ghost play disc.
 * Content only: the matched-field snippet (authentic text, tashkeel kept for
 * Arabic) over an owning-surah + "S:A" ref line.
 *
 * Two tap targets, mirroring [SurahNoirRow]: the ROW opens
 * [SurahDetailScreen] for the hit's surah (scroll-to-ayah unavailable — that
 * screen takes only `surahId`), the DISC starts ayah-mode playback of that
 * surah from the hit ayah. The disc is a separate `noirClickable` nested
 * inside the row's, and Compose does not bubble a click consumed by a child,
 * so a disc tap fires exactly one of them.
 */
@Composable
private fun AyahTextHitRow(
    hit: AyahSearchIndex.Hit,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val surah = remember(hit.surahId) {
        QuranData.SURAHS.firstOrNull { it.id == hit.surahId }
    }
    val snippetIsArabic = remember(hit.snippet) { hit.snippet.any { isArabicScript(it) } }
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
                text = "${hit.ayahNo}",
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
            if (snippetIsArabic) {
                // Arabic never rides a Latin display style: the ambient
                // MaterialTheme style carries negative tracking, and non-zero
                // tracking breaks the cursive joins. arabicSmall plus an
                // explicit letterSpacing = 0 is the fix, inside an RTL provider
                // — same rule as the devotional rows' Arabic teaser.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = hit.snippet,
                        style = GhaisTypography.arabicSmall,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp,
                        color = GhaisNoir.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = hit.snippet,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${surah?.nameEn ?: "Surah ${hit.surahId}"} • ${hit.surahId}:${hit.ayahNo}",
                color = GhaisNoir.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
                contentDescription = "Play ayah ${hit.surahId}:${hit.ayahNo}",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Arabic-script test mirroring [AyahSearchIndex]'s private routing: the same
 * four ranges (basic Arabic, supplement, presentation forms A/B). Kept local
 * because the index's copy is private and the UI's relevance gate must agree
 * with it exactly.
 */
private fun isArabicScript(c: Char): Boolean {
    val v = c.code
    return v in 0x0600..0x06FF ||
        v in 0x0750..0x077F ||
        v in 0xFB50..0xFDFF ||
        v in 0xFE70..0xFEFF
}

/**
 * Feeds [AyahSearchIndex] from synchronously cached verses ONLY — no network.
 *
 * No synchronous cached-surah enumeration exists on [QuranAyahRepository], so
 * [QuranAyahRepository.isAyahAvailable] (a side-effect-free cache read) is the
 * gate: the surah-1 probe skips uncached surahs, and the per-ayah probe skips
 * uncached verses, so [QuranAyahRepository.getAyahImmediate] is called on
 * cache hits only and never triggers its miss-path background fetch. The
 * per-ayah (not just per-surah) gate matters for the partial bundled seed:
 * surah 18 ships ayahs 1-5 only, and probing 1..110 through getAyahImmediate
 * would fan out a fetch for the 105 missing verses.
 *
 * Coverage limit: bundled seeds (surahs 1, 108, 112, 113, 114 fully +
 * 18:1-5 partial — default `en.sahih` edition only, the repository never
 * serves seed text under another edition) plus any surah previously
 * downloaded/persisted on device (in-memory cache or Settings blobs restored
 * at startup, both visible to isAyahAvailable). Total function, never throws.
 */
private fun feedCachedAyahs(editionId: String) {
    try {
        for (surah in QuranData.SURAHS) {
            if (!QuranAyahRepository.isAyahAvailable(surah.id, 1)) continue
            val verses = (1..surah.ayahsCount).mapNotNull { ayahNo ->
                if (!QuranAyahRepository.isAyahAvailable(surah.id, ayahNo)) null
                else QuranAyahRepository.getAyahImmediate(surah.id, ayahNo)
            }
            if (verses.isEmpty()) continue
            AyahSearchIndex.indexSurah(surah.id, verses, editionId)
        }
    } catch (_: Exception) {
        // Best-effort: a bad cache entry must never break search.
    }
}
