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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.GhaisAssets
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.components.noir.ChromeFab
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography
import com.ghais.ui.util.bidiIsolate
import com.ghais.ui.util.realImageUrlOrNull

private val ReciterExpandedHeight = 360.dp
private val ReciterCollapsedHeight = 64.dp
private val ReciterArtworkOverscan = 176.dp
private const val ReciterArtworkOverscanWidth = 1.08f

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

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    ReciterCollapsingBar(
                        progressProvider = progressProvider,
                        name = reciter.nameEn,
                        onBack = { navigator.pop() }
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

                        Spacer(modifier = Modifier.height(18.dp))

                        NoirSectionHeader(
                            label = "Recitations",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            actionLabel = "${surahs.size} Surahs",
                            onAction = {}
                        )
                    }

                    // Discography / Surahs list
                    itemsIndexed(
                        items = surahs,
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
                }
            }
        }
    }
}

@Composable
private fun ReciterCollapsingBar(
    progressProvider: () -> Float,
    name: String,
    onBack: () -> Unit
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
                .graphicsLayer {
                    alpha = smoothStep(0.70f, 0.94f, progressProvider())
                }
        ) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = GhaisNoir.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    progressProvider: () -> Float,
    collapseRangePx: Float,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onToggleFollow: () -> Unit,
    onDownloadAll: () -> Unit
) {
    val photoUrl = realImageUrlOrNull(rememberReciterPhoto(reciter.slug) ?: meta.photoUrl)
    var photoFailed by remember(photoUrl) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ReciterExpandedHeight)
                .clip(GhaisShapes.cardLarge)
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
                        translationY = progress * collapseRangePx * 0.45f
                        val scale = 1f - 0.02f * progress
                        scaleX = scale
                        scaleY = scale
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
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to GhaisNoir.NoirBlack.copy(alpha = 0.7f),
                            0.75f to GhaisNoir.NoirBlack.copy(alpha = 0.88f),
                            1f to GhaisNoir.NoirBlack.copy(alpha = 0.92f)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardLarge)
                    .topSpecular(inset = 30.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                alpha = 1f - smoothStep(0.58f, 0.82f, progressProvider())
                            }
                    ) {
                        Text(
                            text = reciter.nameEn,
                            style = GhaisTypography.displayEditorialBold,
                            color = GhaisNoir.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = reciter.nameAr,
                            style = GhaisTypography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = GhaisNoir.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = meta.country,
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$surahCount surahs",
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = GhaisNoir.TextTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = totalDurationText,
                                    color = GhaisNoir.TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    ChromeFab(
                        icon = Icons.Filled.PlayArrow,
                        onClick = { if (playEnabled) onPlayAll() },
                        contentDescription = "Play all",
                        modifier = Modifier.alpha(if (playEnabled) 1f else 0.45f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReciterNoirAction(
                icon = if (isFollowing) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = if (isFollowing) "Following" else "Follow",
                onClick = onToggleFollow,
                active = isFollowing,
                modifier = Modifier.weight(1f)
            )
            ReciterNoirAction(
                icon = Icons.Filled.Shuffle,
                label = "Shuffle",
                onClick = onShuffle,
                enabled = playEnabled,
                modifier = Modifier.weight(1f)
            )
            ReciterNoirAction(
                icon = if (allDone) Icons.Filled.Check else Icons.Filled.Download,
                label = when {
                    allDone -> "Offline"
                    downloadingCount > 0 -> "Downloading $downloadingCount/$surahCount"
                    downloadedCount > 0 -> "Download ${surahCount - downloadedCount}"
                    else -> "Download all"
                },
                onClick = onDownloadAll,
                enabled = !allDone && downloadingCount == 0,
                modifier = Modifier.weight(1f)
            )
        }

        NoirSegmentedProgress(
            progress = libraryProgress,
            modifier = Modifier.padding(horizontal = 16.dp),
            trackHeight = 6.dp
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

@Composable
private fun ReciterNoirAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true
) {
    val contentTint = when {
        active -> GhaisNoir.TextPrimary
        enabled -> GhaisNoir.TextSecondary
        else -> GhaisNoir.TextTertiary
    }
    Box(
        modifier = modifier
            .height(62.dp)
            .clip(GhaisShapes.row)
            .background(if (active) GhaisNoir.Fill4 else GhaisNoir.Fill2)
            .border(1.dp, if (active) GhaisNoir.SpecularTop else GhaisNoir.BorderCard, GhaisShapes.row)
            .then(if (enabled) Modifier.noirClickable(onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                color = contentTint,
                style = GhaisTypography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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
