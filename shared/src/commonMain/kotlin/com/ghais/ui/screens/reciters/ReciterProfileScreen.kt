package com.ghais.ui.screens.reciters

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RelatedQaris
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.share.ShareSheet
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.player.ReciterRadio
import com.ghais.ui.components.noir.NoirHeaderSearch
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.bidiIsolate
import com.ghais.ui.util.realImageUrlOrNull

private val ReciterExpandedHeight = 420.dp
private val ReciterCollapsedHeight = 64.dp
// Small overscan purely so the parallax travel never exposes an edge —
// width stays 1.0x so photo pixels are never upscaled (upscaling blurs).
private val ReciterArtworkOverscan = 64.dp
private const val ReciterArtworkOverscanWidth = 1.0f

/**
 * Screen displaying the profile of a verified reciter, their metadata chips,
 * action controls ("Play All", "Shuffle", "Follow", "Download all"), and their
 * full discography of Surahs.
 *
 * Styled in strict Noir Glass monochrome: true-black canvas, alpha-white fills,
 * ghost hairlines with a top-only specular, chrome CTAs, and natural-colour artwork.
 * Surrounding chrome and state remain monochrome; artwork retains its own colour.
 *
 * Presentation only. Playback / queue / download / follow logic is untouched.
 */
data class ReciterProfileScreen(val reciterSlug: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator

        // 1 & 2: Look up reciter with fallback to Mishary
        val reciter: Reciter = remember(reciterSlug) {
            QuranDataRepository.getReciterBySlug(reciterSlug)
        }
        val surahs: List<Surah> = remember(reciter) {
            QuranDataRepository.getSurahsForReciter(reciter)
        }

        val allTracks: List<TrackItem> = remember(reciter, surahs) {
            surahs.map { surah ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(surah.id),
                    textUthmani = "",
                    durationMs = surah.ayahsCount * 15_000L
                )
            }
        }

        // Playback-only queue: excludes surahs the reciter never recorded so
        // known-404 URLs never enqueue. The surahs list display / downloads
        // stay complete — only playback queues filter through here.
        val playableTracks: List<TrackItem> = remember(reciter, allTracks) {
            allTracks.filter { reciter.isSurahAvailable(it.surahId) }
        }

        val meta = remember(reciter) {
            resolveReciterMetadata(reciter)
        }

        // Playback state observation from AudioEngine
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()

        // Download state observation from QuranDownloads (key = "$slug/$surahId")
        val downloaded by QuranDownloads.downloadedKeys.collectAsState()
        val dlProgress by QuranDownloads.progress.collectAsState()
        val failedKeys by QuranDownloads.failedKeys.collectAsState()

        // Follow state (persisted global store)
        val followedSlugs by FollowStore.followedSlugs.collectAsState()
        val isFollowing = reciter.slug in followedSlugs

        // Live follower count from public reciter_stats (null = unknown = hidden).
        val followerCounts by FollowStore.followerCounts.collectAsState()
        val followerCount = followerCounts[reciter.slug]
        LaunchedEffect(reciter.slug) {
            FollowStore.refreshCount(reciter.slug)
        }

        // Discovery: radio station seeded by this reciter (empty = no CTA).
        val stationTracks: List<TrackItem> = remember(reciter) {
            ReciterRadio.stationQueue(reciter.slug)
        }

        // Discovery: voices most like this one (empty rail stays hidden).
        val related: List<Reciter> = remember(reciter) {
            RelatedQaris.relatedTo(reciter, QuranDataRepository.getBrowseReciters(), 8)
        }
        LaunchedEffect(related) {
            if (related.isNotEmpty()) {
                FollowStore.refreshCounts(related.map { it.slug })
            }
        }

        // Library offline fraction — drives the hero segmented meter.
        val downloadedCount = remember(reciter.slug, surahs, downloaded) {
            surahs.count { "${reciter.slug}/${it.id}" in downloaded }
        }
        val libraryProgress: Float = remember(downloadedCount, surahs.size) {
            if (surahs.isEmpty()) 0f else downloadedCount.toFloat() / surahs.size.toFloat()
        }
        val allKeys = remember(reciter.slug, surahs) {
            surahs.map { "${reciter.slug}/${it.id}" }
        }
        val allDone = allKeys.isNotEmpty() && allKeys.all { it in downloaded }
        // Strict precedence: a completed key is never counted as downloading,
        // so a stale progress entry can't resurrect the download affordance.
        val downloadingCount = allKeys.count { it in dlProgress && it !in downloaded }

        val listState = rememberLazyListState()
        val density = LocalDensity.current
        val collapseRangePx = remember(density) {
            with(density) {
                (ReciterExpandedHeight - ReciterCollapsedHeight).toPx()
            }
        }
        val progressProvider = remember(listState, collapseRangePx) {
            {
                if (listState.firstVisibleItemIndex == 0) {
                    if (collapseRangePx > 0f) {
                        (listState.firstVisibleItemScrollOffset.toFloat() / collapseRangePx).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                } else {
                    1f
                }
            }
        }
        val totalDurationText = remember(surahs) { formatTotalDuration(surahs) }

        // Personal progress with this voice: distinct catalog surahs present
        // in listening history — real identities, never estimates.
        val history by UserUsageRepository.history.collectAsState()
        val heardCount = remember(history, reciter.slug, surahs) {
            val ids = surahs.map { it.id }.toSet()
            history.filter { it.reciterSlug == reciter.slug }
                .mapNotNull { it.surahId.takeIf { id -> id in ids } }
                .toSet().size
        }

        // Surah search lives entirely in the header now: `surahQuery` drives
        // the same filterSurahs screening it always did; `searchExpanded`
        // is screen-owned (not component-internal) so the collapsing bar can
        // choreograph its title / share-well siblings from the same boolean.
        // English, Arabic (diacritic-tolerant), transliteration, meaning,
        // number, revelation type + Makki/Madani aliases. Filters the
        // recitations list below; playback indexing stays surahId-based so
        // filtering can never misqueue.
        var surahQuery by remember { mutableStateOf("") }
        var searchExpanded by remember { mutableStateOf(false) }
        val visibleSurahs = remember(surahs, surahQuery) {
            filterSurahs(surahs, surahQuery)
        }

        NoirScreenRoot(edgeToEdge = true) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    ReciterCollapsingBar(
                        progressProvider = progressProvider,
                        name = reciter.nameEn,
                        onBack = { navigator.pop() },
                        onShare = {
                            ShareSheet.shareText(
                                "Listen to ${reciter.nameEn}",
                                "Listen to ${reciter.nameEn} (${meta.riwayah}) on Quranify — ${surahs.size} surahs."
                            )
                        },
                        // Expansion is screen-owned: the bar only mirrors it.
                        // Collapse always clears the query too — while collapsed
                        // the list must read unfiltered, otherwise a filtered
                        // subset would silently persist with no visible field. The
                        // field's clear circle wipes text and STAYS expanded instead.
                        searchExpanded = searchExpanded,
                        onSearchExpandedChange = { expanded ->
                            searchExpanded = expanded
                            if (!expanded) surahQuery = ""
                        },
                        searchQuery = surahQuery,
                        onSearchQueryChange = { surahQuery = it }
                    )
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item(key = "reciter-hero") {
                        ReciterNoirHeroPlate(
                            reciter = reciter,
                            meta = meta,
                            surahCount = surahs.size,
                            downloadedCount = downloadedCount,
                            libraryProgress = libraryProgress,
                            allDone = allDone,
                            downloadingCount = downloadingCount,
                            isFollowing = isFollowing,
                            followerCount = followerCount,
                            totalDurationText = totalDurationText,
                            playEnabled = playableTracks.isNotEmpty(),
                            heardCount = heardCount,
                            progressProvider = progressProvider,
                            collapseRangePx = collapseRangePx,
                            onPlayAll = {
                                if (playableTracks.isNotEmpty()) {
                                    AudioEngine.playQueue(playableTracks, startIndex = 0)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            },
                            onShuffle = {
                                if (playableTracks.isNotEmpty()) {
                                    AudioEngine.playQueue(playableTracks.shuffled(), startIndex = 0)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            },
                            radioEnabled = stationTracks.isNotEmpty(),
                            onStartRadio = {
                                if (stationTracks.isNotEmpty()) {
                                    AudioEngine.playStation("${reciter.nameEn} Radio", stationTracks)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            },
                            onToggleFollow = { FollowStore.toggle(reciter.slug) },
                            onDownloadAll = {
                                if (!allDone) {
                                    surahs.forEach { surah ->
                                        val key = "${reciter.slug}/${surah.id}"
                                        if (key !in downloaded && !dlProgress.containsKey(key) && key !in failedKeys) {
                                            QuranDownloads.download(
                                                reciter.slug,
                                                surah.id,
                                                reciter.getFullSurahUrl(surah.id)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    item(key = "recitations-header") {
                        Spacer(modifier = Modifier.height(18.dp))

                        // Count folded into the label: the old trailing action
                        // rendered a tappable affordance with a no-op handler.
                        NoirSectionHeader(
                            label = if (surahQuery.isBlank()) "Recitations • ${surahs.size}"
                            else "Recitations • ${visibleSurahs.size} of ${surahs.size}",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Search is header-only since the NoirHeaderSearch migration:
                    // no inline field item. The empty state below still matters —
                    // while the header search is expanded, live filtering can
                    // legitimately reduce the list to zero rows.
                    if (visibleSurahs.isEmpty()) {
                        item(key = "surah-empty") {
                            Text(
                                text = "No surahs match “${surahQuery.trim()}”.",
                                color = GhaisNoir.TextTertiary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 24.dp)
                            )
                        }
                    }

                    // Discography / Surahs list
                    itemsIndexed(
                        items = visibleSurahs,
                        key = { _, it -> it.id }
                    ) { index, surah ->
                        val isCurrentSurah =
                            currentTrack?.surahId == surah.id && currentTrack?.reciterSlug == reciter.slug
                        val isCurrentSurahPlaying = isCurrentSurah && isPlaying
                        val downloadKey = "${reciter.slug}/${surah.id}"
                        // Strict precedence: downloaded > downloading(progress) > failed > idle.
                        // A completed key can never render the idle download affordance,
                        // even if a stale progress/failed entry lingers for the same key.
                        val isDownloaded = downloadKey in downloaded
                        val rawProgress: Float? = dlProgress[downloadKey]
                        val surahProgress: Float? = if (isDownloaded) null else rawProgress
                        val isDownloading = surahProgress != null
                        val isFailed = !isDownloaded && !isDownloading && downloadKey in failedKeys
                        val audioUrl = reciter.getFullSurahUrl(surah.id)
                        val isAvailable = reciter.isSurahAvailable(surah.id)

                        ReciterNoirSurahRow(
                            surah = surah,
                            isCurrentTrack = isCurrentSurah,
                            isPlaying = isCurrentSurahPlaying,
                            isDownloaded = isDownloaded,
                            downloadProgress = surahProgress,
                            isDownloadFailed = isFailed,
                            isAvailable = isAvailable,
                            onDownloadClick = {
                                // Guard: finished keys stay finished (no re-download affordance),
                                // in-flight keys ignore double-taps (no duplicate enqueue).
                                // Failed/idle keys fall through to an explicit (re)try.
                                if (isDownloaded) return@ReciterNoirSurahRow
                                if (dlProgress.containsKey(downloadKey)) return@ReciterNoirSurahRow
                                QuranDownloads.download(reciter.slug, surah.id, audioUrl)
                            },
                            onItemClick = {
                                if (isCurrentSurahPlaying) {
                                    AudioEngine.pause()
                                } else if (isCurrentSurah) {
                                    AudioEngine.resume()
                                } else {
                                    // Index-safe: rows are indexed in `surahs` but the
                                    // queue is `playableTracks` (filtered) — map via
                                    // surahId so an unrecorded row never enqueues a
                                    // known-404 URL (tap becomes a no-op for it).
                                    val queueIndex =
                                        playableTracks.indexOfFirst { it.surahId == surah.id }
                                    if (queueIndex != -1) {
                                        AudioEngine.playQueue(playableTracks, startIndex = queueIndex)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                }
                            }
                        )
                    }

                    // Discovery rail (A5), relocated to the BOTTOM: voices most
                    // like this one render after the full discography instead of
                    // above it, so the 114-surah list starts right after the hero
                    // actions. Same content, header, keys and callbacks — moved
                    // verbatim; the list must be scrolled to reach it now.
                    // Hidden when empty. The Start Radio action lives in the hero
                    // rail over the photo, it does not compete with this rail.
                    if (related.isNotEmpty()) {
                        item(key = "more-like") {
                            Spacer(modifier = Modifier.height(18.dp))
                            NoirSectionHeader(
                                label = "More like ${reciter.nameEn}",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = related,
                                    key = { it.slug }
                                ) { relatedReciter ->
                                    val relatedPhoto =
                                        rememberCloudPhoto(relatedReciter.slug)
                                            ?: photoForSlug(relatedReciter.slug)
                                    // Cards carry their own identity + meta; the
                                    // similarity caption is dropped for a clean rail.
                                    Column(
                                        modifier = Modifier.width(180.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        ReciterReelCard(
                                            reciter = relatedReciter,
                                            photoUrl = relatedPhoto,
                                            onClick = {
                                                navigator.push(
                                                    ReciterProfileScreen(relatedReciter.slug)
                                                )
                                            },
                                            onPlayClick = {
                                                val relatedSurahs =
                                                    QuranDataRepository.getSurahsForReciter(relatedReciter)
                                                        .filter { relatedReciter.isSurahAvailable(it.id) }
                                                val relatedTracks = relatedSurahs.map { surah ->
                                                    TrackItem(
                                                        reciterSlug = relatedReciter.slug,
                                                        reciterName = relatedReciter.nameEn,
                                                        surahId = surah.id,
                                                        surahNameEn = surah.nameEn,
                                                        surahNameAr = surah.nameAr,
                                                        ayahNo = 0,
                                                        audioUrl = relatedReciter.getFullSurahUrl(surah.id),
                                                        textUthmani = "",
                                                        durationMs = surah.ayahsCount * 15_000L
                                                    )
                                                }
                                                if (relatedTracks.isNotEmpty()) {
                                                    AudioEngine.playQueue(relatedTracks, startIndex = 0)
                                                    rootNavigator.push(NowPlayingScreen())
                                                }
                                            },
                                            isFollowing = relatedReciter.slug in followedSlugs,
                                            onFollowClick = { FollowStore.toggle(relatedReciter.slug) },
                                            followerCount = followerCounts[relatedReciter.slug]
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
}

/**
 * The reciter's collapsing top bar. Identity row: 38dp back glass well,
 * weighted centered title, trailing action wells; the drawBehind scrim and
 * hairline follow the scroll progress inside [progressProvider] exactly as
 * before.
 *
 * Search choreography here: today — while collapsed — the row is unchanged
 * except that the old 38dp "Search surahs" well is now the collapsed state of
 * [NoirHeaderSearch] (40dp, its own NoirNavButton-matched well recipe; the 2dp
 * size drift next to the 38dp siblings is accepted because building a second
 * well surface to run the component's in-place morph would need the component
 * to skip its own animation). When [searchExpanded] flips true the component
 * grows into the remaining row width between the back well and the right
 * inset; the weighted title is squeezed smoothly to zero by the Row, so it
 * disappears under the expanding pill rather than popping. The share well and
 * its spacer are REMOVED while expanded, not faded: Compose measures this
 * Row's children in child order, so an expanded pill that claims the full
 * remaining width still leaves the share well measured afterwards to report
 * its fixed 38dp and overshoot the row edge — collapsing to nothing cleanly
 * avoids that overlap (share is secondary; the reappear on collapse is a
 * simple, honest restore). The scrim keeps painting regardless, so the
 * collapsed↔expanded transition stays visually anchored to the same scroll
 * progress.
 *
 * @param searchExpanded         screen-owned expansion state, mirrored in.
 * @param onSearchExpandedChange open from the collapsed well, close from the
 *   component's trailing X. Closing deliberately also clears the query (the
 *   screen wires that), so a collapsed bar always means an unfiltered list —
 *   keeping the query across close would leave the recitations subset
 *   filtered with no visible field, which reads like a bug.
 * @param searchQuery            live text routed to the screen's filter.
 * @param onSearchQueryChange    keystroke / clear-circle callback.
 */
@Composable
private fun ReciterCollapsingBar(
    progressProvider: () -> Float,
    name: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 10f * smoothStep(0.70f, 0.95f, progressProvider())
            }
            .drawBehind {
                val scrimProgress = smoothStep(0.55f, 0.95f, progressProvider())
                val borderProgress = smoothStep(0.78f, 0.96f, progressProvider())
                drawRect(
                    color = GhaisNoir.NoirBlack.copy(alpha = scrimProgress)
                )
                drawLine(
                    color = GhaisNoir.BorderCard.copy(alpha = borderProgress),
                    start = Offset(0f, size.height - 1f),
                    end = Offset(size.width, size.height - 1f),
                    strokeWidth = 1f
                )
            }
            .statusBarsPadding()
            .height(ReciterCollapsedHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GhaisNoir.Fill2)
                .border(1.dp, GhaisNoir.BorderCard, CircleShape)
                .noirClickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GhaisNoir.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        // Search — the header-search component itself, collapsed well + expanded
        // pill in one morphing surface. Placed per its contract so the Row
        // measures it before the weighted title: expanded, its pill claims all
        // remaining width and squeezes the title out; collapsed, it is a 40dp
        // well — 2dp larger than its 38dp siblings, the accepted trade for not
        // hand-rolling a second well surface alongside the component's morph.
        NoirHeaderSearch(
            expanded = searchExpanded,
            onExpandedChange = onSearchExpandedChange,
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search surahs…"
        )
        // Share — mirrors the back well's glass circle. Only composed while
        // collapsed: when the search pill owns the row this slot must not be
        // measured at all, or it would be measured into the consumed space and
        // overshoot the row edge (see the KDoc above for the full why).
        if (!searchExpanded) {
            Spacer(modifier = Modifier.width(8.dp))
            // Share — glass circle over the photo, mirroring the back well.
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .noirClickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ReciterNoirHeroPlate(
    reciter: Reciter,
    meta: ReciterDisplayMeta,
    surahCount: Int,
    downloadedCount: Int,
    libraryProgress: Float,
    allDone: Boolean,
    downloadingCount: Int,
    isFollowing: Boolean,
    followerCount: Long?,
    totalDurationText: String,
    playEnabled: Boolean,
    heardCount: Int,
    progressProvider: () -> Float,
    collapseRangePx: Float,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    radioEnabled: Boolean,
    onStartRadio: () -> Unit,
    onToggleFollow: () -> Unit,
    onDownloadAll: () -> Unit
) {
    val photoUrl = realImageUrlOrNull(rememberReciterPhoto(reciter.slug) ?: meta.photoUrl)
    var photoFailed by remember(photoUrl) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Clean portrait: square top under the status bar, rounded where the
        // info begins. No text sits on the photo — the face stays fully
        // visible (dating-ref detail treatment).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ReciterExpandedHeight)
                .clip(
                    RoundedCornerShape(
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    )
                )
                .background(GhaisNoir.wellFill())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ReciterArtworkOverscanWidth)
                    .height(ReciterExpandedHeight + ReciterArtworkOverscan)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        val progress = progressProvider()
                        // Translate only — scaling down would resample the
                        // portrait every frame and soften it.
                        translationY = progress * collapseRangePx * 0.3f
                    }
            ) {
                if (!photoUrl.isNullOrBlank() && !photoFailed) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = reciter.nameEn,
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            if (state is coil3.compose.AsyncImagePainter.State.Error) photoFailed = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NoirReciterAvatar(
                            photoUrl = null,
                            nameEn = reciter.nameEn,
                            size = 160.dp,
                            shape = CircleShape,
                            ring = true,
                            grayscale = true
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        1.dp,
                        GhaisNoir.BorderCard,
                        RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
            )

            // Floating action rail — dating-ref vertical actions over the
            // photo's right edge: follow, shuffle, radio, download.
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReciterRailButton(
                    icon = if (isFollowing) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFollowing) "Following" else "Follow",
                    active = isFollowing,
                    onClick = onToggleFollow
                )
                if (playEnabled) {
                    ReciterRailButton(
                        icon = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        onClick = onShuffle
                    )
                }
                if (radioEnabled) {
                    ReciterRailButton(
                        icon = Icons.Filled.Radio,
                        contentDescription = "Start radio",
                        onClick = onStartRadio
                    )
                }
                ReciterRailButton(
                    icon = if (allDone) Icons.Filled.Check else Icons.Filled.Download,
                    contentDescription = when {
                        allDone -> "Downloaded for offline"
                        downloadingCount > 0 -> "Downloading $downloadingCount of $surahCount"
                        else -> "Download all"
                    },
                    dimmed = allDone || downloadingCount > 0,
                    onClick = onDownloadAll
                )
            }
        }

        // Identity lives BELOW the photo on the canvas — nothing covers
        // the portrait. Deliberately minimal: name, one meta line, CTA.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp)
        ) {
            Text(
                text = reciter.nameEn,
                style = GhaisTypography.displayEditorialBold,
                color = GhaisNoir.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reciter.nameAr,
                style = GhaisTypography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = GhaisNoir.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // One quiet meta line replaces the four pills + follower badge:
            // country • riwayah • style. Count and duration live in the
            // Play-all CTA below, so nothing repeats.
            val metaLine = remember(meta) {
                listOf(meta.country, meta.riwayah, meta.style)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" • ")
            }
            if (metaLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = metaLine,
                    color = GhaisNoir.TextTertiary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Minimal CTA — compact auto-width Noir pill in the hero action row:
        // play glyph + "Play all" only. The "• N surahs" suffix was dropped
        // because the count already lives in the Recitations header
        // ("Recitations • N"), and the old "You heard X of Y" caption row +
        // standalone percentage were pure duplication of the fraction — the
        // offline meter below keeps its own library read, so the heard story
        // shrinks to a 2dp hairline under the pill. No new state: the
        // fraction reuses the same heardCount/surahCount values the caption
        // was computed from. Click handler, playEnabled gate and disabled
        // alpha behave exactly as the old pill did.
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
            // Deliberately NO fillMaxWidth: the Column hugs the pill, so the
            // hairline below inherits exactly the pill's own width.
        ) {
            Row(
                modifier = Modifier
                    .clip(GhaisShapes.pill)
                    .background(GhaisNoir.wellFill())
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.pill)
                    .then(if (playEnabled) Modifier.noirClickable(onClick = onPlayAll) else Modifier)
                    .height(40.dp)
                    .padding(horizontal = 14.dp)
                    .alpha(if (playEnabled) 1f else 0.45f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = GhaisNoir.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Play all",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            // Heard-progress hairline: only when something was actually heard.
            // TextSecondary track + lavender fill — the briefest possible echo
            // of the removed caption, sized to the pill, not the full width.
            if (heardCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(GhaisNoir.BorderCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(heardCount.toFloat() / surahCount.coerceAtLeast(1).toFloat())
                            .height(2.dp)
                            .background(GhaisNoir.Lavender)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (downloadedCount > 0 || downloadingCount > 0) {
            NoirSegmentedProgress(
                progress = libraryProgress,
                modifier = Modifier.padding(horizontal = 16.dp),
                trackHeight = 4.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$downloadedCount of $surahCount offline",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${(libraryProgress * 100).toInt()}% kept",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Arabic folding for search: strip diacritics, unify alef forms. */
private fun arabicFold(s: String): String =
    s.replace(Regex("[\\u064B-\\u0652\\u0670]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')

/**
 * Matches a query against every surah criterion that matters: English name,
 * Arabic name (folded), transliteration, meaning, revelation type
 * (Makki/Madani aliases included), and raw surah number.
 */
private fun filterSurahs(surahs: List<Surah>, rawQuery: String): List<Surah> {
    val q = rawQuery.trim()
    if (q.isEmpty()) return surahs
    val folded = arabicFold(q)
    val number = q.toIntOrNull()
    // "2" also matches surah 2's number prefix? No — exact number only,
    // so "1" never floods the list with 10x/11x/100x.
    val revelation = when (q.lowercase()) {
        "makki", "makkiy", "makkah", "meccan" -> "Meccan"
        "madani", "madina", "madinah", "medinan" -> "Medinan"
        else -> null
    }
    return surahs.filter { s ->
        s.nameEn.contains(q, ignoreCase = true) ||
            arabicFold(s.nameAr).contains(folded) ||
            s.transliteration.contains(q, ignoreCase = true) ||
            s.meaning.contains(q, ignoreCase = true) ||
            s.revelationType.contains(q, ignoreCase = true) ||
            (revelation != null && s.revelationType == revelation) ||
            (number != null && s.id == number)
    }
}

/**
 * Floating rail action — 48dp glass circle over the photo edge, lavender
 * fill when active (follow), dimmed while its work is done/in-flight.
 */
@Composable
private fun ReciterRailButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    dimmed: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                when {
                    active -> GhaisNoir.lavenderFill()
                    else -> Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                }
            )
            .border(
                1.dp,
                if (active) Color.Transparent else Color.White.copy(alpha = 0.25f),
                CircleShape
            )
            .noirClickable(onClick = onClick)
            .alpha(if (dimmed && !active) 0.55f else 1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) GhaisNoir.LavenderInk else GhaisNoir.TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Single Surah row as a [NoirListRow]: clay icon-well, dual text, and a
 * monochrome trailing cluster (download affordance + chromium play disc).
 * Determinate downloads render a thin [NoirSegmentedProgress] under the row.
 */
@Composable
private fun ReciterNoirSurahRow(
    surah: Surah,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onItemClick: () -> Unit,
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    isDownloadFailed: Boolean = false,
    isAvailable: Boolean = true,
    onDownloadClick: () -> Unit = {}
) {
    val durationText = remember(surah.ayahsCount) {
        formatSurahDuration(surah.ayahsCount)
    }
    val isDownloading = downloadProgress != null
    // Unavailable takes precedence: the file was never recorded, so no
    // downloaded/downloading/failed state can apply — file can't exist.
    // Strict precedence for AVAILABLE surahs is untouched:
    // downloaded > downloading > failed > idle.
    val stateSuffix = when {
        !isAvailable -> " • Not recorded"
        isDownloaded -> " • Offline"
        isDownloading -> " • Downloading…"
        isDownloadFailed -> " • Failed — tap to retry"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isAvailable) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        NoirListRow(
            title = "${surah.id}. ${surah.nameEn}",
            subtitle = "${bidiIsolate(surah.nameAr)} • ${surah.ayahsCount} Ayahs • $durationText$stateSuffix",
            icon = Icons.Default.MusicNote,
            chevron = false,
            // Unrecorded rows are dimmed + non-clickable (tertiary read via alpha).
            onClick = if (isAvailable) onItemClick else null,
            trailing = {
                SurahRowTrailing(
                    isCurrentTrack = isCurrentTrack,
                    isPlaying = isPlaying,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    isDownloadFailed = isDownloadFailed,
                    isAvailable = isAvailable,
                    onDownloadClick = onDownloadClick,
                    onItemClick = onItemClick
                )
            }
        )

        // Segmented determinate progress for active downloads (engraved track).
        // Suppressed for downloaded rows so stale progress can't linger under Offline.
        val p = downloadProgress
        if (!isDownloaded && p != null && p in 0f..1f) {
            Spacer(modifier = Modifier.height(6.dp))
            NoirSegmentedProgress(
                progress = p,
                modifier = Modifier.padding(horizontal = 4.dp),
                trackHeight = 4.dp
            )
        }
    }
}

/**
 * Monochrome trailing cluster: download affordance + chromium play disc.
 * Live state reads through chromium fill and the white equalizer — never hue.
 */
@Composable
private fun RowScope.SurahRowTrailing(
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isDownloadFailed: Boolean,
    onDownloadClick: () -> Unit,
    onItemClick: () -> Unit,
    isAvailable: Boolean = true
) {
    // Unrecorded surahs: no file can exist, so the download slot is hidden
    // entirely (no dead affordance). Available-surah logic below untouched.
    if (!isAvailable) {
        // Play disc only: dimmed (tertiary), non-clickable — no live state.
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    GhaisNoir.wellFill(),
                    GhaisShapes.well
                )
                .border(
                    1.dp,
                    GhaisNoir.BorderCard,
                    GhaisShapes.well
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Not recorded",
                tint = GhaisNoir.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        return
    }
    // Per-surah download affordance — downloaded rows render no slot so the
    // play disc sits clean; failed/idle/downloading keep their affordances.
    if (!isDownloaded) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloading",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                isDownloadFailed -> {
                    // Failed is visually distinct (engraved well + specular hairline +
                    // primary glyph) with an explicit retry tap — never auto-downloaded.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(GhaisNoir.wellFill(), CircleShape)
                            .border(1.dp, GhaisNoir.SpecularTop, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download failed — tap to retry",
                                tint = GhaisNoir.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                else -> {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = GhaisNoir.TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))
    }

    // Play disc: chromium while live, clay well otherwise.
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                if (isCurrentTrack) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                GhaisShapes.well
            )
            .border(
                1.dp,
                if (isCurrentTrack) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                GhaisShapes.well
            )
            .noirClickable(onItemClick),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            ActiveNoirEqualizer()
        } else {
            Icon(
                imageVector = if (isCurrentTrack) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isCurrentTrack) "Resume" else "Play",
                tint = if (isCurrentTrack) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Animated live equalizer bars in pure white (monochrome live meter).
 */
@Composable
private fun ActiveNoirEqualizer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ReciterEq")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 21f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 17f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar4"
    )

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h1.dp)
                .background(GhaisNoir.TextPrimary, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .background(GhaisNoir.TextPrimary, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .background(GhaisNoir.TextPrimary, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h4.dp)
                .background(GhaisNoir.TextPrimary, CircleShape)
        )
    }
}

/**
 * Helper to compute formatted duration from ayah count (e.g. 7 ayahs -> "1:45").
 */
private fun formatSurahDuration(ayahsCount: Int): String {
    val totalSeconds = ayahsCount * 15
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun formatTotalDuration(surahs: List<Surah>): String {
    val totalMinutes = surahs.sumOf { it.ayahsCount } * 15 / 60
    return if (totalMinutes >= 60) {
        "${totalMinutes / 60} hr ${totalMinutes % 60} min"
    } else {
        "$totalMinutes min"
    }
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun smoothStep(start: Float, end: Float, value: Float): Float {
    return smoothStep((value - start) / (end - start))
}

/**
 * Helper metadata class for reciter profile presentation.
 */
private data class ReciterDisplayMeta(
    val photoUrl: String?,
    val country: String,
    val riwayah: String,
    val style: String
)

/**
 * Resolves full profile presentation metadata including avatar photo, follower count,
 * country, Riwayah, and recitation style.
 */
private fun resolveReciterMetadata(reciter: Reciter): ReciterDisplayMeta {
    val cleanSlug = reciter.slug.lowercase()

    val verifiedFromList = ALL_VERIFIED_RECITERS.find {
        it.slug.lowercase() == cleanSlug ||
        (cleanSlug == "alafasy" && it.slug == "mishary") ||
        (cleanSlug == "sudais" && it.slug == "al-sudais") ||
        (cleanSlug == "muaiqly" && it.slug == "al-muaiqly") ||
        (cleanSlug == "dossari" && it.slug == "al-dossari") ||
        (cleanSlug.startsWith("abdulbaset") && it.slug.startsWith("abdul")) ||
        (cleanSlug == "shuraym" && it.slug == "shuraim") ||
        (cleanSlug.contains("minshawi") && it.slug.contains("minshawi"))
    }

    val photoUrl = verifiedFromList?.photoUrl
        ?: reciter.imageUrl
        ?: GhaisAssets.VerifiedReciters.find {
            it.slug.lowercase() == cleanSlug ||
            (cleanSlug == "alafasy" && it.slug == "mishary") ||
            (cleanSlug == "sudais" && it.slug == "al-sudais") ||
            (cleanSlug == "muaiqly" && it.slug == "al-muaiqly") ||
            (cleanSlug == "dossari" && it.slug == "al-dossari") ||
            (cleanSlug.startsWith("abdulbaset") && it.slug.startsWith("abdul"))
        }?.photoUrl

    val country = reciter.country.ifBlank {
        verifiedFromList?.country ?: when {
            cleanSlug.contains("alafasy") || cleanSlug == "mishary" -> "Kuwait"
            cleanSlug.contains("husary") || cleanSlug.contains("minshawi") ||
                cleanSlug.startsWith("abdul") || cleanSlug.contains("sobhi") ||
                cleanSlug.contains("hisham") -> "Egypt"
            else -> "Saudi Arabia"
        }
    }

    val riwayah = if (reciter.riwayah.isNotBlank()) reciter.riwayah else "Hafs"

    val style = verifiedFromList?.style ?: when {
        reciter.style.contains("mujawwad", ignoreCase = true) -> "Mujawwad"
        reciter.style.contains("taraweeh", ignoreCase = true) -> "Taraweeh"
        else -> "Murattal"
    }

    return ReciterDisplayMeta(
        photoUrl = photoUrl,
        country = country,
        riwayah = riwayah,
        style = style
    )
}
