package com.ghais.ui.screens.reciters

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.ghais.data.seed.StitchAssets
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.player.QuranDownloads
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.player.NowPlayingScreen

/**
 * Screen displaying the profile of a verified reciter, their metadata badges,
 * action controls ("Play All", "Shuffle", "Follow"), and their full discography of Surahs.
 *
 * Fully styled in black-glass theme with LinkBlue accent (#4C8DFF).
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

        Scaffold(
            containerColor = Color(0xFF000000),
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
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
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
                // Header item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Avatar container with glowing blue ring & verified badge
                        ReciterAvatarHeader(
                            photoUrl = meta.photoUrl,
                            nameEn = reciter.nameEn
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Arabic Calligraphy Name in Bold White
                        Text(
                            text = reciter.nameAr,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // English Name in High-Contrast Text
                        Text(
                            text = reciter.nameEn,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Badges: Country, Riwayah, Style in frosted glass pills with white micro-borders
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FrostedGlassBadge(text = meta.country)
                            FrostedGlassBadge(text = meta.riwayah)
                            FrostedGlassBadge(text = meta.style)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Follower Count Text
                        Text(
                            text = meta.followers,
                            color = Color(0xFF9A9AA0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // Action Buttons Row: "Play All", "Shuffle", "Follow"
                        ReciterActionButtonsRow(
                            reciter = reciter,
                            surahs = surahs,
                            allTracks = allTracks,
                            isFollowing = isFollowing,
                            onToggleFollow = { FollowStore.toggle(reciter.slug) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // "Download all" pill next to play-all controls (dark card / glass,
                        // consistent with Shuffle button styling).
                        DownloadAllPill(
                            reciter = reciter,
                            surahs = surahs,
                            downloaded = downloaded,
                            dlProgress = dlProgress
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Recitations Section Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recitations",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4C8DFF).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF4C8DFF).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${surahs.size} Surahs",
                                    color = Color(0xFF4C8DFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Discography / Surahs List
                itemsIndexed(
                    items = surahs,
                    key = { _, it -> it.id }
                ) { index, surah ->
                    val isCurrentSurah = currentTrack?.surahId == surah.id && currentTrack?.reciterSlug == reciter.slug
                    val isCurrentSurahPlaying = isCurrentSurah && isPlaying
                    val downloadKey = "${reciter.slug}/${surah.id}"
                    val isDownloaded = downloadKey in downloaded
                    val surahProgress: Float? = dlProgress[downloadKey]
                    val isFailed = downloadKey in failedKeys
                    val audioUrl = reciter.getFullSurahUrl(surah.id)

                    ReciterSurahListItem(
                        surah = surah,
                        reciter = reciter,
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

/**
 * Avatar with glowing blue ring (#4C8DFF) and verified badge.
 */
@Composable
private fun ReciterAvatarHeader(
    photoUrl: String?,
    nameEn: String
) {
    Box(
        modifier = Modifier.size(118.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing blue ring & frosted halo
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E7CF6).copy(alpha = 0.2f))
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF2E7CF6),
                            Color(0xFF4C8DFF),
                            Color(0xFF7AA8FF),
                            Color(0xFF2E7CF6)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Inner Avatar photo or initials
        Box(
            modifier = Modifier
                .size(106.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = nameEn,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = nameEn.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Verified Badge at bottom end
        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(Color(0xFF4C8DFF))
                .border(2.5.dp, Color(0xFF000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified Reciter",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Frosted glass badge pill with white micro-border.
 */
@Composable
private fun FrostedGlassBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.05f), // subtle white glass tint
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) // white micro-border
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * Action buttons: "Play All" (LinkBlue gradient), "Shuffle" (frosted dark glass),
 * and "Follow" (toggle state with blue outline).
 */
@Composable
private fun ReciterActionButtonsRow(
    reciter: Reciter,
    surahs: List<Surah>,
    allTracks: List<TrackItem>,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Play All" button with LinkBlue gradient
        Button(
            onClick = {
                if (allTracks.isNotEmpty()) {
                    AudioEngine.playQueue(allTracks, startIndex = 0)
                    rootNavigator?.push(NowPlayingScreen())
                }
            },
            modifier = Modifier
                .weight(1.2f)
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(23.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF2E7CF6), Color(0xFF4C8DFF))
                        ),
                        shape = RoundedCornerShape(23.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // "Shuffle" button in frosted dark glass
        Surface(
            onClick = {
                if (allTracks.isNotEmpty()) {
                    AudioEngine.playQueue(allTracks.shuffled(), startIndex = 0)
                    rootNavigator?.push(NowPlayingScreen())
                }
            },
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            color = Color(0xFF1C1C1E),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Shuffle",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // "Follow" toggle button with blue outline
        Surface(
            onClick = onToggleFollow,
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            color = if (isFollowing) Color(0xFF4C8DFF).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color(0xFF4C8DFF))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = if (isFollowing) "Following" else "Follow",
                    tint = if (isFollowing) Color(0xFF4C8DFF) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isFollowing) "Following ✓" else "Follow",
                    color = if (isFollowing) Color(0xFF4C8DFF) else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * "Download all" pill placed next to the play-all controls (dark card / glass,
 * consistent with the Shuffle button). If every surah key is downloaded it shows
 * a static DownloadDone state (tap does nothing harmful); otherwise tapping
 * enqueues all missing surahs via [QuranDownloads.download].
 */
@Composable
private fun DownloadAllPill(
    reciter: Reciter,
    surahs: List<Surah>,
    downloaded: Set<String>,
    dlProgress: Map<String, Float>
) {
    val allKeys = remember(reciter.slug, surahs) {
        surahs.map { "${reciter.slug}/${it.id}" }
    }
    val allDone = allKeys.isNotEmpty() && allKeys.all { it in downloaded }
    val downloadingCount = allKeys.count { dlProgress.containsKey(it) }

    Surface(
        onClick = {
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
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(23.dp),
        color = Color(0xFF1C1C1E),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (allDone) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "All downloaded",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Downloaded",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            } else {
                if (downloadingCount > 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF4C8DFF)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download all",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (downloadingCount > 0) "Downloading ($downloadingCount/${surahs.size})"
                    else "Download all",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Single Surah item displaying Surah number pill, English name, Arabic name,
 * ayah count ("7 Ayahs • 1:45"), and play button or LinkBlue equalizer.
 */
@Composable
private fun ReciterSurahListItem(
    surah: Surah,
    reciter: Reciter,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Surah Number Pill
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isCurrentTrack) Color(0xFF4C8DFF).copy(alpha = 0.15f) else Color(0xFF1C1C1E)
                )
                .border(
                    width = 1.dp,
                    color = if (isCurrentTrack) Color(0xFF4C8DFF) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = surah.id.toString(),
                color = if (isCurrentTrack) Color(0xFF4C8DFF) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // English Name & Ayah Count with Duration
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = surah.nameEn,
                color = if (isCurrentTrack) Color(0xFF4C8DFF) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${surah.ayahsCount} Ayahs • $durationText",
                color = Color(0xFF9A9AA0),
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Arabic Name
        Text(
            text = surah.nameAr,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Per-surah download affordance (placed before play control, 6dp spacing).
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            val isDownloading = downloadProgress != null
            when {
                isDownloaded -> {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Downloaded — tap to delete",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                isDownloading -> {
                    val p = downloadProgress ?: -1f
                    if (p >= 0f && p <= 1f) {
                        CircularProgressIndicator(
                            progress = { p },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF4C8DFF),
                            trackColor = Color(0xFF4C8DFF).copy(alpha = 0.2f)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF4C8DFF)
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
                            tint = Color(0xFFEF4444),
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
                            tint = Color(0xFF9A9AA0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Play Button or Active LinkBlue Equalizer Bar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable { onItemClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                ActiveLinkBlueEqualizer()
            } else if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4C8DFF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color(0xFF4C8DFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Animated live equalizer bars pulsing in LinkBlue (#4C8DFF).
 */
@Composable
private fun ActiveLinkBlueEqualizer(
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
                .background(Color(0xFF4C8DFF), RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .background(Color(0xFF4C8DFF), RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .background(Color(0xFF4C8DFF), RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h4.dp)
                .background(Color(0xFF4C8DFF), RoundedCornerShape(1.5.dp))
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
    val followers: String,
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
        ?: StitchAssets.VerifiedReciters.find {
            it.slug.lowercase() == cleanSlug ||
            (cleanSlug == "alafasy" && it.slug == "mishary") ||
            (cleanSlug == "sudais" && it.slug == "al-sudais") ||
            (cleanSlug == "muaiqly" && it.slug == "al-muaiqly") ||
            (cleanSlug == "dossari" && it.slug == "al-dossari") ||
            (cleanSlug.startsWith("abdulbaset") && it.slug.startsWith("abdul"))
        }?.photoUrl

    val followers = verifiedFromList?.followers ?: run {
        val hash = reciter.nameEn.hashCode().let { if (it < 0) -it else it }
        val fansK = (hash % 850) + 120
        "${(fansK / 100.0).toString().take(3)}M followers"
    }

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
        followers = followers,
        country = country,
        riwayah = riwayah,
        style = style
    )
}
