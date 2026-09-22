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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.ghais.ui.components.noir.ChromePillButton
import com.ghais.ui.components.noir.GhostPillButton
import com.ghais.ui.components.noir.NoirHeroCard
import com.ghais.ui.components.noir.NoirListRow
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.NoirSectionHeader
import com.ghais.ui.components.noir.NoirSegmentedProgress
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.home.NoirStatChip
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * Screen displaying the profile of a verified reciter, their metadata chips,
 * action controls ("Play All", "Shuffle", "Follow", "Download all"), and their
 * full discography of Surahs.
 *
 * Styled in strict Noir Glass monochrome: true-black canvas, alpha-white fills,
 * ghost hairlines with a top-only specular, chrome CTAs, and grayscale imagery.
 * Zero hue — state reads through fill elevation, chromium, weight and opacity.
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
        val downloadingCount = allKeys.count { dlProgress.containsKey(it) }

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = { navigator.pop() },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GhaisNoir.Fill2)
                                        .border(1.dp, GhaisNoir.BorderCard, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = GhaisNoir.TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp) // 120.dp padding for MiniPlayer & dock
                ) {
                    // Editorial header + hero plate
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Editorial monochrome header (mirrors NoirProfileHeader).
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VERIFIED RECITER",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                )
                                // Live follower count — hidden while unknown.
                                if (followerCount != null) {
                                    Text(
                                        text = FollowStore.formatFollowerCount(followerCount),
                                        color = GhaisNoir.TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The voice of",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = reciter.nameEn,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Hero plate: grayscale portrait + identity + stats +
                            // library meter + chrome/ghost actions.
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
                                playEnabled = allTracks.isNotEmpty(),
                                onPlayAll = {
                                    if (allTracks.isNotEmpty()) {
                                        AudioEngine.playQueue(allTracks, startIndex = 0)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                                onShuffle = {
                                    if (allTracks.isNotEmpty()) {
                                        AudioEngine.playQueue(allTracks.shuffled(), startIndex = 0)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                                onToggleFollow = { FollowStore.toggle(reciter.slug) },
                                onDownloadAll = {
                                    if (!allDone) {
                                        surahs.forEach { surah ->
                                            val key = "${reciter.slug}/${surah.id}"
                                            if (key !in downloaded && !dlProgress.containsKey(key)) {
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

                            // Recitations section header (monochrome, count as ghost action).
                            NoirSectionHeader(
                                label = "Recitations",
                                actionLabel = "${surahs.size} Surahs",
                                onAction = {}
                            )
                        }
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
                        val isDownloaded = downloadKey in downloaded
                        val surahProgress: Float? = dlProgress[downloadKey]
                        val isFailed = downloadKey in failedKeys
                        val audioUrl = reciter.getFullSurahUrl(surah.id)

                        ReciterNoirSurahRow(
                            surah = surah,
                            isCurrentTrack = isCurrentSurah,
                            isPlaying = isCurrentSurahPlaying,
                            isDownloaded = isDownloaded,
                            downloadProgress = surahProgress,
                            isDownloadFailed = isFailed,
                            onDownloadClick = {
                                if (isDownloaded) {
                                    QuranDownloads.delete(reciter.slug, surah.id)
                                } else {
                                    QuranDownloads.download(reciter.slug, surah.id, audioUrl)
                                }
                            },
                            onItemClick = {
                                if (isCurrentSurahPlaying) {
                                    AudioEngine.pause()
                                } else if (isCurrentSurah) {
                                    AudioEngine.resume()
                                } else {
                                    AudioEngine.playQueue(allTracks, startIndex = index)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * True-grayscale filter — reciter portraits stay recognisable while remaining
 * strictly monochrome (mirrors the Home NoirArtworkWell recipe).
 */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Hero plate for the reciter (mirrors the Home "resume plate" pattern):
 * grayscale portrait with chromium ring + monogram fallback, identity text,
 * stat chips, segmented library meter, and chrome/ghost actions.
 *
 * Presentation only — all callbacks preserve the original screen logic.
 */
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
    playEnabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onToggleFollow: () -> Unit,
    onDownloadAll: () -> Unit
) {
    NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReciterNoirAvatar(
                    photoUrl = meta.photoUrl,
                    nameEn = reciter.nameEn,
                    size = 84.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RECITER",
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = reciter.nameAr,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$surahCount Surahs • $downloadedCount offline",
                        color = GhaisNoir.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats as non-interactive NoirStatChip wells (zero hue).
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoirStatChip(text = meta.country)
                NoirStatChip(text = meta.riwayah)
                NoirStatChip(text = meta.style)
                // Live follower count — hidden while unknown.
                if (followerCount != null) {
                    NoirStatChip(text = FollowStore.formatFollowerCount(followerCount))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented library meter (engraved track, chrome fill).
            NoirSegmentedProgress(progress = libraryProgress, trackHeight = 8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Primary CTA: chromium resume pill.
            ChromePillButton(
                text = "Play All",
                onClick = onPlayAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = playEnabled,
                leadingIcon = Icons.Default.PlayArrow
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary actions as ghost pills — fill elevation carries state.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GhostPillButton(
                    text = "Shuffle",
                    onClick = onShuffle,
                    modifier = Modifier.weight(1f)
                )
                GhostPillButton(
                    text = if (isFollowing) "Following" else "Follow",
                    onClick = onToggleFollow,
                    modifier = Modifier.weight(1f),
                    active = isFollowing
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            GhostPillButton(
                text = when {
                    allDone -> "Downloaded"
                    downloadingCount > 0 -> "Downloading ($downloadingCount/$surahCount)"
                    else -> "Download all"
                },
                onClick = onDownloadAll,
                modifier = Modifier.fillMaxWidth(),
                active = allDone
            )
        }
    }
}

/**
 * Grayscale portrait plate: clay well + chromium (specular) ring, darkening
 * scrim so the photo reads engraved, monogram fallback mirroring the profile
 * identity pill, and a chromium verified dot.
 */
@Composable
private fun ReciterNoirAvatar(
    photoUrl: String?,
    nameEn: String,
    size: Dp = 84.dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(GhaisNoir.wellFill())
                .border(1.dp, GhaisNoir.SpecularTop, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = nameEn,
                    contentScale = ContentScale.Crop,
                    colorFilter = NoirGrayscale,
                    modifier = Modifier.fillMaxSize()
                )
                // Darkening scrim: keeps the plate recessed instead of glowing.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                Text(
                    text = nameEn.take(1).uppercase(),
                    color = GhaisNoir.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Chromium verified dot (chrome fill, near-black glyph).
        Box(
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.BottomEnd)
                .background(GhaisNoir.chromeFill(), CircleShape)
                .border(2.dp, GhaisNoir.NoirBlack, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified Reciter",
                tint = GhaisNoir.OnChrome,
                modifier = Modifier.size(13.dp)
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
    onDownloadClick: () -> Unit = {}
) {
    val durationText = remember(surah.ayahsCount) {
        formatSurahDuration(surah.ayahsCount)
    }
    val isDownloading = downloadProgress != null
    val stateSuffix = when {
        isDownloading -> " • Downloading…"
        isDownloaded -> " • Offline"
        isDownloadFailed -> " • Tap to retry"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        NoirListRow(
            title = "${surah.id}. ${surah.nameEn}",
            subtitle = "${surah.nameAr} • ${surah.ayahsCount} Ayahs • $durationText$stateSuffix",
            icon = Icons.Default.MusicNote,
            chevron = false,
            onClick = onItemClick,
            trailing = {
                SurahRowTrailing(
                    isCurrentTrack = isCurrentTrack,
                    isPlaying = isPlaying,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    isDownloadFailed = isDownloadFailed,
                    onDownloadClick = onDownloadClick,
                    onItemClick = onItemClick
                )
            }
        )

        // Segmented determinate progress for active downloads (engraved track).
        val p = downloadProgress
        if (p != null && p in 0f..1f) {
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
    onItemClick: () -> Unit
) {
    // Per-surah download affordance — all states in the monochrome ramp.
    Box(
        modifier = Modifier.size(34.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isDownloaded -> {
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloaded — tap to delete",
                        tint = GhaisNoir.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download failed — tap to retry",
                        tint = GhaisNoir.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
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
