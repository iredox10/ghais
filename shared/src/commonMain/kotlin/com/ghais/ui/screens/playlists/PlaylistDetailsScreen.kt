package com.ghais.ui.screens.playlists

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.ghais.data.repository.FavoritesStore
import com.ghais.data.seed.QuranData
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
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
import com.ghais.ui.screens.surah.SurahDetailScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import com.ghais.ui.theme.GhaisTypography

/**
 * One playlist's own screen: cover art, name, description,
 * then its surahs ready to play.
 *
 * Styled in strict Noir Glass monochrome: true-black canvas, alpha-white
 * fills, ghost hairlines with a top-only specular, chrome CTAs, and
 * grayscale artwork. Zero hue — state reads through fill elevation,
 * chromium, weight and opacity.
 *
 * Presentation only. Queue / play / favorites logic and navigation
 * (SurahDetailScreen, NowPlayingScreen) are untouched.
 */
data class PlaylistDetailsScreen(val playlistId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val rootNavigator = LocalRootNavigator.current ?: navigator.parent ?: navigator
        val playlist = remember(playlistId) { moodPlaylistById(playlistId) }
        val reciter = remember { QuranData.RECITERS.first() }
        val surahs = remember(playlist) {
            playlist.surahIds.mapNotNull { id -> QuranData.SURAHS.firstOrNull { it.id == id } }
        }
        val favorites by FavoritesStore.favoriteTracks.collectAsState()

        // Playback state observation from AudioEngine (read-only, drives live row state).
        val currentTrack by AudioEngine.currentTrack.collectAsState()
        val isPlaying by AudioEngine.isPlaying.collectAsState()

        val allTracks: List<TrackItem> = remember(surahs, reciter) {
            // Skip surahs this reciter has no audio for — never queue a known-404.
            // Display list (surahs) is untouched; only the queue is filtered.
            surahs.filter { reciter.isSurahAvailable(it.id) }.map { surah ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(surah.id),
                    durationMs = surah.ayahsCount * 15_000L
                )
            }
        }
        fun trackFor(surah: Surah): TrackItem? = allTracks.firstOrNull { it.surahId == surah.id }

        // Kept fraction — drives the hero segmented meter (monochrome, no hue).
        val keptCount = remember(surahs, allTracks, favorites) {
            surahs.count { surah ->
                val url = trackFor(surah)?.audioUrl
                url != null && favorites.any { it.audioUrl == url }
            }
        }
        val keptProgress: Float = remember(keptCount, surahs.size) {
            if (surahs.isEmpty()) 0f else keptCount.toFloat() / surahs.size.toFloat()
        }

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
                    contentPadding = PaddingValues(bottom = 120.dp) // room for MiniPlayer & dock
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
                                    text = "CURATED COLLECTION",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "${surahs.size} surahs",
                                    color = GhaisNoir.TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tonight's",
                                style = GhaisTypography.displayEditorial,
                                maxLines = 1
                            )
                            Text(
                                text = playlist.title,
                                style = GhaisTypography.displayEditorialBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Hero plate: grayscale art + identity + stats +
                            // kept meter + chrome/ghost actions.
                            PlaylistNoirHeroPlate(
                                playlist = playlist,
                                reciterName = reciter.nameEn,
                                surahCount = surahs.size,
                                keptCount = keptCount,
                                keptProgress = keptProgress,
                                playEnabled = allTracks.isNotEmpty(),
                                onPlayAll = {
                                    if (allTracks.isNotEmpty()) {
                                        AudioEngine.playQueue(allTracks, startIndex = 0)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                },
                                onShuffle = {
                                    val shuffled = allTracks.shuffled()
                                    if (shuffled.isNotEmpty()) {
                                        AudioEngine.playQueue(shuffled, startIndex = 0)
                                        rootNavigator.push(NowPlayingScreen())
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Surahs section header (monochrome, count as ghost action).
                            NoirSectionHeader(
                                label = "Surahs in this collection",
                                actionLabel = "${surahs.size} Surahs",
                                onAction = {}
                            )
                        }
                    }

                    // Surah rows
                    itemsIndexed(
                        items = surahs,
                        key = { _, s -> "pl_${playlist.id}_${s.id}" }
                    ) { index, surah ->
                        val track = trackFor(surah)
                        val isFavorite =
                            track?.let { t -> favorites.any { it.audioUrl == t.audioUrl } } ?: false
                        val isCurrentSurah =
                            currentTrack?.surahId == surah.id && currentTrack?.reciterSlug == reciter.slug
                        val isCurrentSurahPlaying = isCurrentSurah && isPlaying

                        PlaylistNoirSurahRow(
                            surah = surah,
                            isFavorite = isFavorite,
                            isCurrentTrack = isCurrentSurah,
                            isPlaying = isCurrentSurahPlaying,
                            onToggleFavorite = { track?.let { FavoritesStore.toggle(it) } },
                            onRowClick = { navigator.push(SurahDetailScreen(surah.id)) },
                            onPlayClick = {
                                val start = allTracks.indexOfFirst { it.surahId == surah.id }
                                    .takeIf { it >= 0 } ?: 0
                                if (allTracks.isNotEmpty()) {
                                    AudioEngine.playQueue(allTracks, startIndex = start)
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }
}

/**
 * Hero plate for the playlist (mirrors the reciter "resume plate" pattern):
 * grayscale cover art with chromium ring + darkening scrim, identity text,
 * stat chips, segmented kept meter, and chrome/ghost actions.
 *
 * Presentation only — all callbacks preserve the original screen logic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaylistNoirHeroPlate(
    playlist: MoodPlaylist,
    reciterName: String,
    surahCount: Int,
    keptCount: Int,
    keptProgress: Float,
    playEnabled: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    NoirHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Grayscale art plate: chromium ring + scrim so it reads engraved.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, GhaisNoir.SpecularTop, RoundedCornerShape(20.dp))
            ) {
                PlaylistCover(
                    art = playlist.art,
                    modifier = Modifier.fillMaxSize()
                )
                // Darkening scrim: keeps the plate recessed instead of glowing.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "COLLECTION",
                color = GhaisNoir.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = playlist.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = playlist.description,
                color = GhaisNoir.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$surahCount surahs • $reciterName",
                color = GhaisNoir.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stats as non-interactive NoirStatChip wells (zero hue).
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoirStatChip(text = "$surahCount Surahs")
                NoirStatChip(text = reciterName)
                NoirStatChip(text = "$keptCount kept")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented kept meter (engraved track, chrome fill).
            NoirSegmentedProgress(progress = keptProgress, trackHeight = 8.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$keptCount of $surahCount kept",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${(keptProgress * 100).toInt()}% kept",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary CTA: chromium play-all pill.
            ChromePillButton(
                text = "Play All",
                onClick = onPlayAll,
                modifier = Modifier.fillMaxWidth(),
                enabled = playEnabled,
                leadingIcon = Icons.Default.PlayArrow
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary action as ghost pill.
            GhostPillButton(
                text = "Shuffle",
                onClick = onShuffle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Single Surah row as a [NoirListRow]: clay icon-well, dual text, and a
 * monochrome trailing cluster (favorite affordance + chromium play disc).
 */
@Composable
private fun PlaylistNoirSurahRow(
    surah: Surah,
    isFavorite: Boolean,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onToggleFavorite: () -> Unit,
    onRowClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val durationText = remember(surah.ayahsCount) {
        formatSurahDuration(surah.ayahsCount)
    }
    val stateSuffix = when {
        isPlaying -> " • Playing"
        isCurrentTrack -> " • Queued"
        isFavorite -> " • Kept"
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
            onClick = onRowClick,
            trailing = {
                PlaylistRowTrailing(
                    isFavorite = isFavorite,
                    isCurrentTrack = isCurrentTrack,
                    isPlaying = isPlaying,
                    onToggleFavorite = onToggleFavorite,
                    onPlayClick = onPlayClick
                )
            }
        )
    }
}

/**
 * Monochrome trailing cluster: favorite affordance + chromium play disc.
 * Live state reads through chromium fill and the white equalizer — never hue.
 */
@Composable
private fun RowScope.PlaylistRowTrailing(
    isFavorite: Boolean,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onToggleFavorite: () -> Unit,
    onPlayClick: () -> Unit
) {
    // Favorite affordance — monochrome ramp only (kept = full white).
    IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
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
            .noirClickable(onPlayClick),
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
    val infiniteTransition = rememberInfiniteTransition(label = "PlaylistEq")

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
