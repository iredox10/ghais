package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghais.data.repository.ChartsRepository
import com.ghais.data.repository.DailyMix
import com.ghais.data.repository.EditorialPlaylist
import com.ghais.data.repository.EditorialRepository
import com.ghais.data.repository.FollowStore
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RelatedQaris
import com.ghais.data.repository.UserUsageRepository
import com.ghais.data.repository.prettifySlug
import com.ghais.data.repository.toFullSurahTrackItem
import com.ghais.domain.model.Reciter
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.screens.reciters.ReciterReelCard
import com.ghais.ui.screens.reciters.rememberCloudPhoto
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes
import kotlin.time.Clock

/**
 * Personalized Home shelves (Spotify-discovery batch 2).
 *
 * Four shelves inserted into [HomeScreen] directly after "Qari you follow":
 * Daily Mix hero, Top Qaris this week, New from qaris you follow, Discover.
 *
 * Every shelf is self-contained (header included) and renders NOTHING when
 * its data source is empty or unreachable — including its own trailing
 * spacing, so a hidden shelf leaves zero gap. Nothing here throws when the
 * repos are empty: a fresh install shows only the Daily Mix fallback (the
 * [DailyMix] seed reciter guarantees a non-empty mix whenever the catalog
 * itself loads).
 */

private const val THIRTY_DAYS_MS = 30L * 86_400_000L
private const val MILLIS_PER_DAY = 86_400_000L

// ---------------------------------------------------------------------------
// (a) Your Daily Mix — one hero card + play button
// ---------------------------------------------------------------------------

/**
 * Deterministic daily hero from the user's most-played qaris.
 *
 * Rebuild key is `remember(daySeed, topQaris)`: the same epoch day + the
 * same top-qari order always yields the same mix. Tap queues the mix via
 * [AudioEngine.playQueue] and pushes NowPlaying through [onOpenPlayer]
 * (mirrors the Continue-listening / Library editorial playback pattern).
 * Hidden when the mix is empty (catalog unavailable).
 */
@Composable
fun HomeDailyMixShelf(onOpenPlayer: () -> Unit) {
    val history by UserUsageRepository.history.collectAsState()
    val daySeed = remember { Clock.System.now().toEpochMilliseconds() / MILLIS_PER_DAY }
    val topQaris = remember(history) {
        try {
            UserUsageRepository.topQariSlugs(5)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val mix = remember(daySeed, topQaris) {
        try {
            val played = history.mapNotNull { item ->
                val slug = item.reciterSlug.trim()
                if (slug.isEmpty() || item.surahId !in 1..114) null else slug to item.surahId
            }.toSet()
            DailyMix.build(
                topQariSlugs = topQaris,
                playedSurahIds = played,
                daySeed = daySeed,
                count = 20
            )
        } catch (_: Exception) {
            emptyList()
        }
    }
    if (mix.isEmpty()) return

    val qariNames = remember(mix) {
        mix.map { it.reciterName.trim() }.filter { it.isNotEmpty() }.distinct().take(3)
    }
    val monogram = qariNames.firstOrNull()?.firstOrNull { it.isLetter() }?.uppercase() ?: "M"

    HomeSectionHeader(title = "Your Daily Mix", onSeeAll = null)
    // Feature card with an inline tracklist: the top-3 rows preview the mix
    // and each one starts playback from its own index.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GhaisNoir.tintFill(), GhaisShapes.cardNoir)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular(inset = 28.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Overlapping cover collage from the mix's first qaris.
            Box(modifier = Modifier.size(76.dp)) {
                val collageSlugs = remember(mix) { mix.map { it.reciterSlug }.distinct().take(3) }
                collageSlugs.forEachIndexed { index, slug ->
                    val collagePhoto = rememberCloudPhoto(slug)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .align(Alignment.Center)
                            .offset(x = ((index - 1) * 14).dp)
                    ) {
                        NoirArtworkWell(
                            coverUrl = collagePhoto.orEmpty(),
                            monogram = monogram,
                            shape = CircleShape,
                            size = 52.dp,
                            monogramSize = 18.sp,
                            ring = index == 1,
                            grayscale = false,
                            scrimAlpha = 0f
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY MIX",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Made for you",
                    color = GhaisNoir.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${mix.size} tracks • refreshed daily",
                    color = GhaisNoir.TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        mix.take(3).forEachIndexed { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GhaisShapes.row)
                    .noirClickable {
                        try {
                            AudioEngine.playQueue(mix, startIndex = index)
                            onOpenPlayer()
                        } catch (_: Exception) {
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (index < 9) "0${index + 1}" else "${index + 1}",
                    color = if (index == 0) GhaisNoir.Lavender else GhaisNoir.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.surahNameEn.trim().ifEmpty { "Surah ${track.surahId}" },
                        color = GhaisNoir.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.reciterName.trim().ifEmpty { "Unknown qari" },
                        color = GhaisNoir.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = GhaisNoir.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GhaisShapes.pill)
                .background(GhaisNoir.lavenderFill())
                .noirClickable {
                    try {
                        AudioEngine.playQueue(mix, startIndex = 0)
                        onOpenPlayer()
                    } catch (_: Exception) {
                    }
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = GhaisNoir.LavenderInk,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Play mix",
                color = GhaisNoir.LavenderInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(Modifier.height(18.dp))
}

// ---------------------------------------------------------------------------
// (b) Top Qaris this week — cloud chart wells
// ---------------------------------------------------------------------------

/**
 * Horizontal wells from `snapshot.topQaris7d`, each slug resolved through
 * the nullable [QuranDataRepository.getBrowseReciterBySlug] (unresolvable
 * slugs are skipped, never borrowed). Refreshes once per screen entry via
 * `LaunchedEffect(Unit)`. Hidden when the snapshot is null/unfetched or no
 * slug resolves (offline, empty charts).
 */
@Composable
fun HomeTopQarisShelf(onReciter: (String) -> Unit) {
    LaunchedEffect(Unit) { ChartsRepository.refresh() }
    val snapshot by ChartsRepository.snapshot.collectAsState()
    val reciters = remember(snapshot) {
        try {
            snapshot?.topQaris7d.orEmpty()
                .mapNotNull { QuranDataRepository.getBrowseReciterBySlug(it) }
                .distinctBy { it.slug }
        } catch (_: Exception) {
            emptyList()
        }
    }
    if (reciters.isEmpty()) return

    HomeSectionHeader(title = "Top Qaris this week", onSeeAll = null)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(reciters, key = { _, it -> it.slug }) { index, reciter ->
            val photo = rememberCloudPhoto(reciter.slug)
            val rank = index + 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .noirClickable { onReciter(reciter.slug) }
            ) {
                // Chart-rank pill — AirPods-ref chip language on the rail.
                Box(
                    modifier = Modifier
                        .clip(GhaisShapes.pill)
                        .background(
                            brush = if (rank == 1) GhaisNoir.lavenderFill()
                            else Brush.verticalGradient(
                                listOf(GhaisNoir.Fill2, GhaisNoir.Fill2)
                            ),
                            shape = GhaisShapes.pill
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#$rank",
                        color = if (rank == 1) GhaisNoir.LavenderInk else GhaisNoir.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                NoirArtworkWell(
                    coverUrl = photo.orEmpty(),
                    monogram = reciter.nameEn.firstOrNull()?.uppercase() ?: "Q",
                    shape = CircleShape,
                    size = 88.dp,
                    monogramSize = 30.sp,
                    ring = rank == 1,
                    grayscale = false,
                    scrimAlpha = 0f
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = reciter.nameEn,
                    color = GhaisNoir.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    Spacer(Modifier.height(18.dp))
}

// ---------------------------------------------------------------------------
// (c) New from qaris you follow — recent editorial filtered to follows
// ---------------------------------------------------------------------------

/**
 * Editorial playlists published in the last 30 days whose [EditorialPlaylist.qariSlugs]
 * intersect the follow set. Tile taps play the editorial directly ([EditorialPlaylist.toTrackItems]
 * into [AudioEngine.playQueue]) — `PlaylistDetailsScreen` only knows mood ids and would show
 * the wrong content for an editorial id. Hidden when nothing is followed,
 * nothing is new, or the editorial pull never succeeded.
 */
@Composable
fun HomeNewFromFollowedShelf(onPlay: (EditorialPlaylist) -> Unit) {
    val playlists by EditorialRepository.playlists.collectAsState()
    val followed by FollowStore.followedSlugs.collectAsState()
    val fresh = remember(playlists, followed) {
        try {
            if (followed.isEmpty()) {
                emptyList()
            } else {
                val sinceMs = Clock.System.now().toEpochMilliseconds() - THIRTY_DAYS_MS
                EditorialRepository.newSince(sinceMs)
                    .filter { playlist -> playlist.qariSlugs.any { it in followed } }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    if (fresh.isEmpty()) return

    HomeSectionHeader(title = "New from qaris you follow", onSeeAll = null)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(fresh, key = { it.id }) { playlist ->
            NoirEditorialTile(
                playlist = playlist,
                onClick = { onPlay(playlist) }
            )
        }
    }
    Spacer(Modifier.height(18.dp))
}

/** Glass playlist plate mirroring the Collections tile recipe. */
@Composable
private fun NoirEditorialTile(
    playlist: EditorialPlaylist,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 164.dp, height = 196.dp)
            .background(GhaisNoir.cardFill(), GhaisShapes.cardNoir)
            .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
            .topSpecular(inset = 28.dp)
            .noirClickable(onClick)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp),
            contentAlignment = Alignment.Center
        ) {
            NoirArtworkWell(
                coverUrl = playlist.coverUrl.orEmpty(),
                monogram = playlist.title.firstOrNull { it.isLetter() }?.uppercase() ?: "N",
                shape = RoundedCornerShape(22.dp),
                size = 76.dp,
                monogramSize = 26.sp,
                ring = false
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = playlist.title,
                color = GhaisNoir.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = playlist.description.trim().takeIf { it.isNotEmpty() }
                    ?: "${playlist.items.size} tracks",
                color = GhaisNoir.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// (d) Discover — related qaris from the top listen
// ---------------------------------------------------------------------------

/**
 * "Because you listen to X" rail: [RelatedQaris] over the browse catalog
 * seeded by the user's most-played qari (name resolved, falling back to the
 * literal "your top qari"), rendered with the profile reel-card recipe
 * ([ReciterReelCard]). Hidden when there is no history, the seed does not
 * resolve, or no related qaris come back.
 */
@Composable
fun HomeDiscoverShelf(
    onReciter: (String) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val history by UserUsageRepository.history.collectAsState()
    val followed by FollowStore.followedSlugs.collectAsState()
    val followerCounts by FollowStore.followerCounts.collectAsState()
    val topQaris = remember(history) {
        try {
            UserUsageRepository.topQariSlugs(5)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val seed = remember(topQaris) {
        try {
            topQaris.firstOrNull()?.let { QuranDataRepository.getBrowseReciterBySlug(it) }
        } catch (_: Exception) {
            null
        }
    }
    val related = remember(seed) {
        try {
            if (seed == null) emptyList()
            else RelatedQaris.relatedTo(
                seed = seed,
                candidates = QuranDataRepository.getBrowseReciters(),
                limit = 8
            )
        } catch (_: Exception) {
            emptyList()
        }
    }
    if (seed == null || related.isEmpty()) return
    val seedName = seed.nameEn.trim().takeIf { it.isNotEmpty() }
        ?: seed.slug.prettifySlug().takeIf { it.isNotBlank() }
        ?: "your top qari"

    HomeSectionHeader(title = "Because you listen to $seedName", onSeeAll = null)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(related, key = { it.catalogKey() }) { reciter ->
            val photo = rememberCloudPhoto(reciter.slug)
            ReciterReelCard(
                reciter = reciter,
                photoUrl = photo,
                onClick = { onReciter(reciter.slug) },
                onPlayClick = { playReciterQueue(reciter, onOpenPlayer) },
                isFollowing = reciter.slug in followed,
                onFollowClick = { FollowStore.toggle(reciter.slug) },
                followerCount = followerCounts[reciter.slug]
            )
        }
    }
    Spacer(Modifier.height(18.dp))
}

/**
 * Queues a reciter's available full-surah catalog from the top and opens the
 * player (same playable-track shape [DailyMix] builds). Null-safe: does
 * nothing when the reciter has no available surahs.
 */
private fun playReciterQueue(reciter: Reciter, openPlayer: () -> Unit) {
    try {
        val tracks = QuranDataRepository.getSurahsForReciter(reciter)
            .filter { reciter.isSurahAvailable(it.id) }
            .map { it.toFullSurahTrackItem(reciter) }
        if (tracks.isEmpty()) return
        AudioEngine.playQueue(tracks, startIndex = 0)
        openPlayer()
    } catch (_: Exception) {
    }
}
