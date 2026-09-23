package com.ghais.ui.screens.curated

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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.ghais.data.seed.CuratedTrack
import com.ghais.data.seed.DetailedCuratedPlaylist
import com.ghais.data.seed.QuranDataRepository
import com.ghais.data.seed.toTrackItem
import com.ghais.player.AudioEngine
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
import kotlinx.coroutines.launch

/**
 * Curated Playlist Detail Screen — strict Noir Glass monochrome.
 *
 * True-black canvas, alpha-white fills, ghost hairlines with a top-only
 * specular, grayscale hero art, chrome primary CTA + ghost secondaries,
 * segmented library meter, and [NoirListRow] track rows. Zero hue — state
 * reads through fill elevation, chromium, weight and opacity.
 *
 * Presentation only. Signature, queue/play logic, save toggle, share
 * snackbar, and navigation are preserved.
 */
data class CuratedPlaylistDetailScreen(val playlistId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Look up playlist from QuranDataRepository with fallback to first curated playlist
        val playlist: DetailedCuratedPlaylist = remember(playlistId) {
            QuranDataRepository.getCuratedPlaylistOrDefault(playlistId)
        }

        // Active AudioEngine playback state observation
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()

        var isSavedToLibrary by remember { mutableStateOf(false) }

        // Listening position inside this playlist — drives the hero segmented meter.
        val activeIndex = remember(currentTrack, playlist) {
            playlist.tracks.indexOfFirst { track ->
                currentTrack?.audioUrl == track.audioUrl ||
                    (currentTrack?.surahId == track.surahNumber && currentTrack?.reciterName == track.reciterName)
            }
        }
        val listenProgress: Float = remember(activeIndex, playlist.tracks.size) {
            if (activeIndex < 0 || playlist.tracks.isEmpty()) 0f
            else (activeIndex + 1).toFloat() / playlist.tracks.size.toFloat()
        }

        NoirScreenRoot {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (rootNavigator.size > 1) {
                                        rootNavigator.pop()
                                    } else {
                                        navigator.pop()
                                    }
                                },
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
                        actions = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Collection link copied to clipboard")
                                    }
                                },
                                modifier = Modifier.padding(end = 4.dp)
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
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
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
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Editorial header + hero plate
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURATED COLLECTION",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "${playlist.tracks.size} tracks",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A moment of",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = playlist.title,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playlist.description.ifEmpty { playlist.subtitle },
                                style = GhaisTypography.editorialBody,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            CuratedNoirHeroPlate(
                                playlist = playlist,
                                listenProgress = listenProgress,
                                activeIndex = activeIndex,
                                isSavedToLibrary = isSavedToLibrary,
                                onPlayAll = {
                                    val trackItems = playlist.tracks.map { it.toTrackItem() }
                                    if (trackItems.isNotEmpty()) {
                                        AudioEngine.playQueue(trackItems, startIndex = 0)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                                onShuffle = {
                                    val shuffled = playlist.tracks.shuffled().map { it.toTrackItem() }
                                    if (shuffled.isNotEmpty()) {
                                        AudioEngine.playQueue(shuffled, startIndex = 0)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                                onToggleSave = {
                                    isSavedToLibrary = !isSavedToLibrary
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isSavedToLibrary) "Saved to your Collections" else "Removed from Collections"
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            NoirSectionHeader(
                                label = "Surahs & Recitations",
                                actionLabel = "${playlist.tracks.size} tracks",
                                onAction = {}
                            )
                        }
                    }

                    // Surahs / Tracks list
                    itemsIndexed(
                        items = playlist.tracks,
                        key = { index, track -> "${track.surahNumber}_${track.reciterName}_$index" }
                    ) { index, track ->
                        val isThisTrackActive = currentTrack?.audioUrl == track.audioUrl ||
                            (currentTrack?.surahId == track.surahNumber && currentTrack?.reciterName == track.reciterName)
                        val isCurrentlyPlaying = isThisTrackActive && isPlaying

                        CuratedNoirTrackRow(
                            index = index + 1,
                            track = track,
                            isActive = isThisTrackActive,
                            isPlaying = isCurrentlyPlaying,
                            onPlayClick = {
                                if (isThisTrackActive) {
                                    AudioEngine.togglePlayPause()
                                } else {
                                    val allTracks = playlist.tracks.map { it.toTrackItem() }
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
 * True-grayscale filter — cover art stays recognisable while remaining
 * strictly monochrome.
 */
private val NoirGrayscale: ColorFilter by lazy {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
}

/**
 * Hero plate (mirrors the reciter hero-plate pattern): grayscale cover art
 * with chromium ring + darkening scrim, identity text, stat chips, segmented
 * listening meter, and chrome/ghost actions.
 *
 * Presentation only — all callbacks preserve the original screen logic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CuratedNoirHeroPlate(
    playlist: DetailedCuratedPlaylist,
    listenProgress: Float,
    activeIndex: Int,
    isSavedToLibrary: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onToggleSave: () -> Unit
) {
    NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(GhaisShapes.medium)
                        .background(GhaisNoir.wellFill())
                        .border(1.dp, GhaisNoir.SpecularTop, GhaisShapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.title,
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
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.tag.uppercase(),
                        color = GhaisNoir.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playlist.title,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Curated by ${playlist.curator.ifEmpty { "Ghais" }} • ${playlist.totalDuration.ifEmpty { "42 mins" }}",
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
                NoirStatChip(text = playlist.tag)
                NoirStatChip(text = "${playlist.tracks.size} Tracks")
                NoirStatChip(text = playlist.totalDuration.ifEmpty { "42 mins" })
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented listening meter (engraved track, chrome fill).
            NoirSegmentedProgress(progress = listenProgress, trackHeight = 8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (activeIndex >= 0) "Track ${activeIndex + 1} of ${playlist.tracks.size}" else "${playlist.tracks.size} tracks queued",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${(listenProgress * 100).toInt()}% played",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary CTA: chromium play pill.
            ChromePillButton(
                text = "Play All",
                onClick = onPlayAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = playlist.tracks.isNotEmpty(),
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
                    text = if (isSavedToLibrary) "Saved" else "Save",
                    onClick = onToggleSave,
                    modifier = Modifier.weight(1f),
                    active = isSavedToLibrary
                )
            }
        }
    }
}

/**
 * Single track as a [NoirListRow]: clay icon-well, dual text, and a
 * monochrome trailing cluster (Arabic title + chromium play disc).
 * Live state reads through chromium fill and the white equalizer — never hue.
 */
@Composable
private fun CuratedNoirTrackRow(
    index: Int,
    track: CuratedTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        NoirListRow(
            title = "${track.surahNumber}. ${track.surahNameEn}",
            subtitle = "${track.reciterName} • ${track.duration}",
            icon = Icons.Default.MusicNote,
            chevron = false,
            onClick = onPlayClick,
            trailing = {
                CuratedRowTrailing(
                    index = index,
                    arabicTitle = track.surahNameAr,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    onPlayClick = onPlayClick
                )
            }
        )
    }
}

/**
 * Monochrome trailing cluster: index or Arabic title + chromium play disc.
 */
@Composable
private fun RowScope.CuratedRowTrailing(
    index: Int,
    arabicTitle: String,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Text(
        text = if (arabicTitle.isNotBlank()) arabicTitle else index.toString().padStart(2, '0'),
        color = if (isActive) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
        fontSize = 15.sp,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.width(10.dp))

    // Play disc: chromium while live, clay well otherwise.
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                if (isActive) GhaisNoir.chromeFill() else GhaisNoir.wellFill(),
                GhaisShapes.well
            )
            .border(
                1.dp,
                if (isActive) Color.White.copy(alpha = 0.4f) else GhaisNoir.BorderCard,
                GhaisShapes.well
            )
            .noirClickable(onPlayClick),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            CuratedNoirEqualizer()
        } else {
            Icon(
                imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isActive) "Resume" else "Play",
                tint = if (isActive) GhaisNoir.OnChrome else GhaisNoir.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Animated live equalizer bars in pure white (monochrome live meter).
 */
@Composable
private fun CuratedNoirEqualizer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CuratedNoirEq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqH1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqH2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 7f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqH3"
    )

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h1.dp)
                .clip(CircleShape)
                .background(GhaisNoir.TextPrimary)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .clip(CircleShape)
                .background(GhaisNoir.TextPrimary)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .clip(CircleShape)
                .background(GhaisNoir.TextPrimary)
        )
    }
}
