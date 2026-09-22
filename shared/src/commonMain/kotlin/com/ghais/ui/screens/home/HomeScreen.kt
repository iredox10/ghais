package com.ghais.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.seed.JumpBackInItem
import com.ghais.domain.model.RoutineModeType
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.NoirScreenRoot
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.explore.ExploreScreen
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

/**
 * Phase 4 — Noir Home.
 *
 * Sticky-free editorial scroll on the Noir canvas (top glow zone -> absolute
 * black + ambient glow + film grain). Sections keep their original order and
 * navigation targets; only the surface language changed: every accent hue,
 * flat grey card and gradient tile is replaced by alpha-white glass.
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
                // Editorial header — actions, headline, today's listening chips
                // -------------------------------------------------------------
                item {
                    HomeTopBar(
                        onProfileClick = { tabNavigator.current = ProfileScreen },
                        onSearchClick = { rootNavigator?.push(SearchScreen()) },
                        onStatsClick = { rootNavigator?.push(StatsScreen) }
                    )
                    Spacer(Modifier.height(18.dp))
                    NoirHomeGreeting()
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
                        resumeHistoryEntry(entry) { rootNavigator?.push(NowPlayingScreen()) }
                    })
                    Spacer(Modifier.height(18.dp))
                }

                // -------------------------------------------------------------
                // Playlists — monochrome clay-artwork plates
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(
                        title = "Playlists",
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
                // Listen by routine — monochrome bento tiles
                // -------------------------------------------------------------
                item {
                    HomeSectionHeader(
                        title = "Listen by routine",
                        onSeeAll = { tabNavigator.current = ExploreScreen }
                    )
                    HomeRoutineRow(onMode = { mode ->
                        val playlistId = when (mode) {
                            RoutineModeType.STUDY -> "study-focus"
                            RoutineModeType.WORK -> "focus-work"
                            RoutineModeType.SLEEP -> "sleep-mode"
                        }
                        rootNavigator?.push(PlaylistDetailsScreen(playlistId))
                    })
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
 */
private fun resumeHistoryEntry(entry: JumpBackInItem, openPlayer: () -> Unit) {
    val reciter = QuranDataRepository.getReciterBySlug(entry.reciterSlug)
    val surahs = QuranDataRepository.getSurahs()
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
    val startIndex = allTracks.indexOfFirst { it.surahId == entry.surahId }.coerceAtLeast(0)
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
            val resumeAt = if (entry.durationMs > 0L && entry.positionMs >= entry.durationMs - 5_000L) {
                0L
            } else {
                entry.positionMs.coerceAtLeast(0L)
            }
            AudioEngine.playQueue(allTracks, startIndex = startIndex, startPositionMs = resumeAt)
            openPlayer()
        }
    }
}
