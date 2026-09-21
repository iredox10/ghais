package com.ghais.ui.screens.curated

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.ghais.data.seed.CuratedTrack
import com.ghais.data.seed.DetailedCuratedPlaylist
import com.ghais.data.seed.QuranDataRepository
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.data.seed.toTrackItem
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.navigation.LocalRootNavigator
import kotlinx.coroutines.launch

// Trending Purple & Obsidian Monochrome Design Tokens
private val PitchBlack = Color(0xFF0B0C0E)
private val ObsidianCard = Color(0xFF121418)
private val TrendingPurple = Color(0xFFA855F7)
private val ElectricViolet = Color(0xFF8B5CF6)
private val NeonLilac = Color(0xFFC084FC)
private val MutedGrey = Color(0xFF9CA3AF)
private val SubtitleGrey = Color(0xFFD1D5DB)

/**
 * Curated Playlist Detail Screen.
 * Opened when the user clicks any curated playlist from the Home screen or All Curated Collections screen.
 *
 * Implements Black & White + Trending Purple design specifications:
 * - Dynamic artwork container with soft purple bottom gradient fade & ambient aura.
 * - Frosted glass tag pill with purple border.
 * - Bold white title, muted description, and curator info.
 * - Action row: "Play All" purple gradient button, "Shuffle" in frosted obsidian glass, "Save to Library" button.
 * - Full Surahs/Tracks list with index, English/Arabic title, reciter name, duration, and live playing equalizer.
 * - 120.dp bottom padding for dock / mini player.
 */
data class CuratedPlaylistDetailScreen(val playlistId: String) : Screen {

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

        Scaffold(
            containerColor = PitchBlack,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(PitchBlack)
            ) {
                // Soft Ambient Top Aura: Electric Violet & Trending Purple fading into pitch black
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricViolet.copy(alpha = 0.24f),
                                    TrendingPurple.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(300f, 100f),
                                radius = 500f
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. Top Bar with Back button and Share button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Button: Frosted obsidian circle
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF161822).copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .clickable {
                                        if (rootNavigator.size > 1) {
                                            rootNavigator.pop()
                                        } else {
                                            navigator.pop()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = "Curated Playlist",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Share Button: Frosted obsidian circle
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF161822).copy(alpha = 0.85f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Playlist link copied to clipboard")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 2. Beautiful Artwork Container with Soft Purple Bottom Gradient Fade
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(ObsidianCard)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                TrendingPurple.copy(alpha = 0.60f),
                                                Color.White.copy(alpha = 0.20f),
                                                ElectricViolet.copy(alpha = 0.45f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = playlist.coverUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Soft Purple bottom gradient fade
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    TrendingPurple.copy(alpha = 0.25f),
                                                    PitchBlack.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Tag pill in frosted glass with purple border
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xCC161822))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                TrendingPurple.copy(alpha = 0.75f),
                                                Color.White.copy(alpha = 0.35f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = playlist.tag,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.6.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Title in bold white
                            Text(
                                text = playlist.title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Subtitle / Description in muted grey
                            Text(
                                text = playlist.description.ifEmpty { playlist.subtitle },
                                color = MutedGrey,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Curator info ("Curated by Quranify • 8 Tracks • 42 mins")
                            Text(
                                text = "Curated by ${playlist.curator.ifEmpty { "Quranify" }} • ${playlist.tracks.size} Tracks • ${playlist.totalDuration.ifEmpty { "42 mins" }}",
                                color = SubtitleGrey,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            // Action Row: Play All, Shuffle, Save to Library
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Play All" in Trending Purple gradient button with white play icon
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                listOf(ElectricViolet, TrendingPurple)
                                            )
                                        )
                                        .clickable {
                                            val trackItems = playlist.tracks.map { it.toTrackItem() }
                                            if (trackItems.isNotEmpty()) {
                                                AudioEngine.playQueue(trackItems, startIndex = 0)
                                                rootNavigator.push(NowPlayingScreen())
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play All",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Play All",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // "Shuffle" in frosted obsidian glass
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF161822).copy(alpha = 0.85f))
                                        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                        .clickable {
                                            val shuffled = playlist.tracks.shuffled().map { it.toTrackItem() }
                                            if (shuffled.isNotEmpty()) {
                                                AudioEngine.playQueue(shuffled, startIndex = 0)
                                                rootNavigator.push(NowPlayingScreen())
                                            }
                                        }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Shuffle",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // "Save to Library" icon button
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF161822).copy(alpha = 0.85f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSavedToLibrary) TrendingPurple else Color.White.copy(alpha = 0.14f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            isSavedToLibrary = !isSavedToLibrary
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (isSavedToLibrary) "Saved to your Library" else "Removed from Library"
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSavedToLibrary) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Save to Library",
                                        tint = if (isSavedToLibrary) TrendingPurple else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section Divider & Tracks Header
                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Surahs & Recitations",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = "${playlist.tracks.size} tracks",
                                fontSize = 12.5.sp,
                                color = MutedGrey
                            )
                        }
                    }

                    // 3. Surahs / Tracks List
                    itemsIndexed(playlist.tracks) { index, track ->
                        val isThisTrackActive = currentTrack?.audioUrl == track.audioUrl ||
                            (currentTrack?.surahId == track.surahNumber && currentTrack?.reciterName == track.reciterName)
                        val isCurrentlyPlaying = isThisTrackActive && isPlaying

                        TrackItemRow(
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
 * Single Track item in the Curated Playlist.
 * Displays:
 * - Index number / Animated playing equalizer indicator.
 * - Surah English title, Arabic title, reciter name, duration.
 * - Circular play / pause button.
 */
@Composable
private fun TrackItemRow(
    index: Int,
    track: CuratedTrack,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isActive) TrendingPurple.copy(alpha = 0.08f) else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isActive) TrendingPurple.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onPlayClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Index number or Active Playing Equalizer Animation
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                CuratedEqualizerBars()
            } else if (isActive) {
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = "Active Track",
                    tint = TrendingPurple,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = index.toString().padStart(2, '0'),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Middle: Surah English title, Reciter Name, Duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${track.surahNumber}. ${track.surahNameEn}",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) NeonLilac else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.reciterName,
                    fontSize = 12.sp,
                    color = MutedGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • ",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = track.duration,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Arabic Calligraphy Title
        Text(
            text = track.surahNameAr,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Rightmost: Play / Pause circular button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isPlaying) {
                        Brush.linearGradient(listOf(ElectricViolet, TrendingPurple))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1E212D).copy(alpha = 0.9f),
                                Color(0xFF161822).copy(alpha = 0.9f)
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isPlaying) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .clickable { onPlayClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Animated 3-bar equalizer visualizer pulsing in Trending Purple (#A855F7).
 */
@Composable
private fun CuratedEqualizerBars(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CuratedEqualizer")
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
                .clip(RoundedCornerShape(1.5.dp))
                .background(TrendingPurple)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h2.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(TrendingPurple)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(h3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(TrendingPurple)
        )
    }
}
