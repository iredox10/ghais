package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.DevotionalRepository
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.DEVOTIONAL_TRACK_SLUG
import com.ghais.domain.model.DevotionKind
import com.ghais.domain.model.DevotionalItem
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.IconWell
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.components.noir.noirClickable
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.devotional.DevotionListScreen
import com.ghais.ui.screens.devotional.DevotionPlayerScreen
import com.ghais.ui.screens.history.HistoryScreen
import com.ghais.ui.screens.library.FavoritesScreen
import com.ghais.ui.screens.library.LibraryScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.playlists.PlaylistDetailsScreen
import com.ghais.ui.screens.profile.ProfileScreen
import com.ghais.ui.screens.reciters.FollowedRecitersScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.search.SearchScreen
import com.ghais.ui.screens.stats.StatsScreen
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

/**
 * Phase 4 — Noir Home.
 *
 * Sticky-free editorial scroll on the Noir canvas (top glow zone -> absolute
 * black + ambient glow + film grain). Sections keep their original order and
 * navigation targets; only the surface language changed: every accent hue,
 * flat grey card and gradient tile is replaced by alpha-white glass.
 *
 * The former "Listen by routine" tile row is gone. Its Study / Work / Sleep
 * plates were hardcoded to three curated playlist ids, and the same intent
 * already lives in the mood screen, so the row is now [HomeDevotionRow] —
 * duas and ruqiyah, each opening its own catalogue list.
 *
 * Search lives in the top-bar well only, which pushes [SearchScreen] — the one
 * place that resolves ayah references and keeps query history. The scroll
 * itself opens at Continue listening.
 */
object HomeScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 0u,
                title = "Home",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        val tabNavigator = LocalTabNavigator.current

        NoirScreenRoot {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp)
            ) {
                // -------------------------------------------------------------
                // Top bar — actions only (headline removed)
                // -------------------------------------------------------------
                item {
                    HomeTopBar(
                        onProfileClick = { tabNavigator.current = ProfileScreen },
                        onSearchClick = { rootNavigator?.push(SearchScreen()) },
                        onStatsClick = { rootNavigator?.push(StatsScreen) }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // -------------------------------------------------------------
                // Continue listening — hero plate + rail
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(
                        title = "Continue listening",
                        onSeeAll = { rootNavigator?.push(HistoryScreen) }
                    )
                    HomeContinueListeningRow(onPlay = { entry ->
                        resumeHistoryEntry(
                            entry = entry,
                            openPlayer = { rootNavigator?.push(NowPlayingScreen()) },
                            openDevotion = { itemId ->
                                rootNavigator?.push(DevotionPlayerScreen(itemId = itemId))
                            }
                        )
                    })
                    Spacer(Modifier.height(18.dp))
                }

                // -------------------------------------------------------------
                // Playlists — monochrome clay-artwork plates
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(
                        title = "Collections",
                        onSeeAll = { tabNavigator.current = LibraryScreen }
                    )
                    HomePlaylistsRow(
                        onFavourites = { rootNavigator?.push(FavoritesScreen) },
                        onFocusWork = { rootNavigator?.push(PlaylistDetailsScreen("focus-work")) },
                        onNightSleep = { rootNavigator?.push(PlaylistDetailsScreen("sleep-mode")) }
                    )
                    Spacer(Modifier.height(18.dp))
                }

                // -------------------------------------------------------------
                // Qari you follow — grayscale wells + chromium ring
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(
                        title = "Qari you follow",
                        onSeeAll = { rootNavigator?.push(FollowedRecitersScreen) }
                    )
                    HomeFollowedRow(onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) })
                    Spacer(Modifier.height(18.dp))
                }

                // -------------------------------------------------------------
                // Devotionals — duas + ruqiyah bento tiles
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(title = "Devotionals", onSeeAll = null)
                    HomeDevotionRow(onKind = { kind -> rootNavigator?.push(DevotionListScreen(kind)) })
                    Spacer(Modifier.height(18.dp))
                }

                // -------------------------------------------------------------
                // Your stats — monochrome cells, ghost hairline separators
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(title = "Your stats", onSeeAll = null)
                    HomeStatsCard()
                }
            }
        }
    }
}

/**
 * Resumes a history entry into the engine queue, preserving the original
 * behaviour: if it is already the live track, just open the player (restarting
 * when the track had already finished).
 *
 * A devotional row (the [DEVOTIONAL_TRACK_SLUG] sentinel — a dua or a ruqiyah,
 * see `DevotionalItem.asTrackItem`) has NEITHER a reciter NOR a surah, so it
 * cannot go down the surah path at all: `getReciterBySlug` falls back to
 * Mishary and `entry.surahId == 0` matches no track, so the old code queued
 * Mishary's whole catalogue and started it at the surah nearest to 0 —
 * Al-Fatiha, for any devotional. It is re-queued by
 * [resumeDevotionalHistoryEntry] instead, and opens the devotional reader, which
 * is the surface that knows how to show and play that collection.
 */
private fun resumeHistoryEntry(
    entry: JumpBackInItem,
    openPlayer: () -> Unit,
    openDevotion: (String) -> Unit,
) {
    if (entry.reciterSlug == DEVOTIONAL_TRACK_SLUG) {
        resumeDevotionalHistoryEntry(entry, openDevotion)
        return
    }
    val reciter = QuranDataRepository.getReciterBySlug(entry.reciterSlug)
    // Never queue a surah this reciter never recorded (known-404 URL):
    // restrict the queue to available surahs only.
    val surahs = QuranDataRepository.getSurahs().filter { reciter.isSurahAvailable(it.id) }
    if (surahs.isEmpty()) return // "not recorded by this reciter" — do nothing.
    val allTracks = surahs.map { s ->
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
    val exactIndex = allTracks.indexOfFirst { it.surahId == entry.surahId }
    val startIndex = if (exactIndex >= 0) {
        exactIndex
    } else {
        // Resumed surah unavailable — start from the nearest available one
        // (absolute id distance, tie-break forward so listening keeps moving ahead).
        surahs.indices.minWithOrNull(
            compareBy(
                { kotlin.math.abs(surahs[it].id - entry.surahId) },
                { if (surahs[it].id >= entry.surahId) 0 else 1 },
                { surahs[it].id }
            )
        ) ?: 0
    }
    val redirected = exactIndex < 0
    val currentTrack = AudioEngine.currentTrack.value
    val isCurrent = currentTrack != null &&
        currentTrack.surahId == entry.surahId &&
        currentTrack.reciterSlug == reciter.slug

    when {
        isCurrent && AudioEngine.isPlaying.value -> openPlayer()
        isCurrent -> {
            AudioEngine.resume()
            openPlayer()
        }
        else -> {
            AudioEngine.playQueue(
                allTracks,
                startIndex = startIndex,
                startPositionMs = resumePositionMs(entry, redirected)
            )
            openPlayer()
        }
    }
}

/**
 * Where in a track a history row should resume from.
 *
 * A redirected start belongs to a different surah, so its saved position does
 * not apply — start from the beginning instead. Otherwise cap strictly below
 * duration (never start at/past the end) and restart entries already within 5s
 * of finishing. A row with no known length resumes from its saved position
 * uncapped, which is the same answer the store gives when it writes the row.
 */
private fun resumePositionMs(entry: JumpBackInItem, redirected: Boolean): Long {
    if (redirected) return 0L
    if (entry.durationMs > 0L) {
        val capped = entry.positionMs.coerceIn(0L, (entry.durationMs - 1L).coerceAtLeast(0L))
        return if (capped >= entry.durationMs - 5_000L) 0L else capped
    }
    return entry.positionMs.coerceAtLeast(0L)
}

/**
 * Resumes a devotional history row: re-queues the collection the row names and
 * opens its reader.
 *
 * ## Why the row is matched by TITLE
 *
 * `JumpBackInItem` carries no devotional id — its fields are title, subtitle,
 * progress, coverUrl, surahId, reciterSlug, positionMs, durationMs,
 * lastPlayedTimestampMs (`GhaisAssets.kt`, not this file) — and a devotional
 * track's only per-item field is its title (`asTrackItem` puts it in
 * `surahNameEn`). So the title IS the row's identity: it is what
 * `UserUsageRepository.historyIdentity` dedupes "Continue listening" on and
 * what the cloud doc id hashes, which makes a title match exactly as reliable
 * as the row itself. The alternative (guessing from the reciter or the surah
 * slot) is what produced the arbitrary-surah bug.
 *
 * A title this build's catalogue no longer carries resolves to nothing, and
 * then this does NOTHING: playing the wrong track is the bug being fixed, so an
 * unresolvable row stays inert. Adding an `itemId` to `JumpBackInItem` and
 * stamping it in `UserUsageRepository.historyRow` would close both this and the
 * 11 same-title catalogue collisions; that is a contract for the `GhaisAssets`
 * owner, not an edit here.
 */
private fun resumeDevotionalHistoryEntry(
    entry: JumpBackInItem,
    openDevotion: (String) -> Unit,
) {
    val title = entry.title.trim()
    val item = DevotionKind.entries
        .asSequence()
        .flatMap { kind -> DevotionalRepository.all(kind).asSequence() }
        .filter { it.title.trim().equals(title, ignoreCase = true) }
        // Several entries can share a title (three "Al-Ikhlas" duas and one
        // "Al-Ikhlas" ruqiyah): only an entry with audio can have produced the
        // row, so the audible reading is the one this row is. `maxByOrNull`
        // keeps the first of the winners, and still returns a match when every
        // one is silent.
        .maxByOrNull { if (DevotionalRepository.audioUrlFor(it) != null) 1 else 0 }
        ?: return
    // No audio: open the reader and leave the engine alone. Handing
    // `playQueue` a blank URL would only produce a load error, and the reader
    // degrades to read-only for exactly this fallback case.
    if (DevotionalRepository.audioUrlFor(item) == null) {
        openDevotion(item.id)
        return
    }
    val tracks = DevotionalRepository.playableTracks(item.kind)
    // Arithmetic, not content matching: same-titled twin entries share title
    // and text, so `indexOfFirst` can only ever find the first twin.
    val startIndex = DevotionalRepository.queueStartIndex(item.kind, item)
    if (startIndex < 0) {
        // The catalogue answered with an entry it cannot queue (an entry whose
        // rows were all filtered out). Read it rather than leave the tap dead.
        openDevotion(item.id)
        return
    }
    val currentTrack = AudioEngine.currentTrack.value
    val isCurrent = currentTrack != null && currentTrack.belongsToDevotion(item)
    when {
        isCurrent && AudioEngine.isPlaying.value -> openDevotion(item.id)
        isCurrent -> {
            AudioEngine.resume()
            openDevotion(item.id)
        }
        else -> {
            // `redirected = false`: the entry was found, so its saved position
            // applies. The queue is the whole collection, exactly as
            // `DevotionPlayerScreen` builds it, and the reader recognises the
            // engine as already on this entry and will not restart it.
            AudioEngine.playQueue(
                tracks = tracks,
                startIndex = startIndex,
                startPositionMs = resumePositionMs(entry, redirected = false)
            )
            openDevotion(item.id)
        }
    }
}

/**
 * Does this queued row belong to the devotional [item]?
 *
 * The three fields `DevotionalItem.asTrackItem()` actually encodes: the
 * sentinel slug, the title (`surahNameEn`) and the passage (`textUthmani`).
 * Matching on the passage as well as the title is what tells the recitable
 * "Al-Ikhlas" from its three same-titled silent twins, and it survives the
 * per-ayah expansion in `DevotionalRepository.playableTracks`, where several
 * consecutive rows share one entry. (`DevotionPlayerScreen` has the same
 * predicate, private to that file.)
 */
private fun TrackItem.belongsToDevotion(item: DevotionalItem): Boolean =
    reciterSlug == DEVOTIONAL_TRACK_SLUG &&
        surahNameEn == item.title.ifBlank { item.kind.label } &&
        textUthmani == item.arabic

private val DevotionTileHeight = 156.dp

/**
 * Identity for one devotional tile.
 *
 * [DevotionKind] carries the visible title ([DevotionKind.label]) and nothing
 * else, so the tile heading can never drift from the list screen it opens.
 */
private data class DevotionTileSpec(
    val kind: DevotionKind,
    val subtitle: String,
    val icon: ImageVector
)

/**
 * The devotional tile row — duas and ruqiyah, one glass plate each.
 *
 * Subtitles describe what the collection *is* rather than how many items it
 * holds: a baked-in count would need editing every time the catalogue grows,
 * and the catalogue is the thing most likely to change. "Supplications" and
 * "Quranic verses" also stay true if audio is ever attached to the duas or
 * the ruqiyah set is re-sourced, which a number would not.
 */
private val devotionTiles = listOf(
    DevotionTileSpec(
        kind = DevotionKind.DUA,
        subtitle = "Supplications",
        icon = Icons.Filled.AutoAwesome
    ),
    DevotionTileSpec(
        kind = DevotionKind.RUQIYAH,
        subtitle = "Quranic verses",
        icon = Icons.Filled.VerifiedUser
    )
)

@Composable
private fun HomeDevotionRow(onKind: (DevotionKind) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        devotionTiles.forEach { tile ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(DevotionTileHeight)
                    .background(GhaisNoir.cardFill(), GhaisShapes.cardNoir)
                    .border(1.dp, GhaisNoir.BorderCard, GhaisShapes.cardNoir)
                    .topSpecular(inset = 24.dp)
                    .noirClickable { onKind(tile.kind) }
                    .padding(14.dp)
            ) {
                IconWell(
                    icon = tile.icon,
                    size = 48.dp,
                    iconSize = 22.dp,
                    contentDescription = tile.kind.label,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = tile.kind.label,
                        color = GhaisNoir.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tile.subtitle,
                        color = GhaisNoir.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
