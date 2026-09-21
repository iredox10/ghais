package com.ghais.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.data.repository.QuranDataRepository
import com.ghais.domain.model.TrackItem
import com.ghais.player.AudioEngine
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.history.HistoryScreen
import com.ghais.ui.screens.library.FavoritesScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.playlists.PlaylistDetailsScreen
import com.ghais.ui.screens.reciters.FollowedRecitersScreen
import com.ghais.ui.screens.reciters.ReciterProfileScreen
import com.ghais.ui.screens.search.SearchScreen
import com.ghais.ui.screens.profile.ProfileScreen

private val PureBlack = Color(0xFF000000)

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                item {
                    HomeTopBar(
                        onPremiumClick = { tabNavigator?.let { it.current = ProfileScreen } },
                        onStatsClick = { rootNavigator?.push(SearchScreen()) }
                    )
                }
                item { HomeSectionHeader(title = "Continue listening", onSeeAll = { rootNavigator?.push(HistoryScreen) }) }
                item {
                    HomeContinueListeningRow(onPlay = { entry ->
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
                        if (isCurrent && AudioEngine.isPlaying.value) {
                            rootNavigator?.push(NowPlayingScreen())
                        } else if (isCurrent) {
                            AudioEngine.resume()
                            rootNavigator?.push(NowPlayingScreen())
                        } else {
                            // Resume from where the user stopped (restart if finished).
                            val resumeAt = if (entry.durationMs > 0L && entry.positionMs >= entry.durationMs - 5_000L) {
                                0L
                            } else {
                                entry.positionMs.coerceAtLeast(0L)
                            }
                            AudioEngine.playQueue(allTracks, startIndex = startIndex, startPositionMs = resumeAt)
                            rootNavigator?.push(NowPlayingScreen())
                        }
                    })
                }
                item {
                    HomeSectionHeader(
                        title = "Playlists",
                        onSeeAll = { tabNavigator?.let { it.current = com.ghais.ui.screens.library.LibraryScreen } }
                    )
                }
                item {
                    HomePlaylistsRow(
                        onFavourites = { rootNavigator?.push(FavoritesScreen) },
                        onFocusWork = { rootNavigator?.push(PlaylistDetailsScreen("focus-work")) },
                        onNightSleep = { rootNavigator?.push(PlaylistDetailsScreen("sleep-mode")) }
                    )
                }
                item {
                    HomeSectionHeader(
                        title = "Qari you follow",
                        onSeeAll = { rootNavigator?.push(FollowedRecitersScreen) }
                    )
                }
                item {
                    HomeFollowedRow(onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) })
                }
                item {
                    HomeSectionHeader(
                        title = "Listen by routine",
                        onSeeAll = { tabNavigator.current = com.ghais.ui.screens.explore.ExploreScreen }
                    )
                }
                item {
                    HomeRoutineRow(onMode = { mode ->
                        val playlistId = when (mode) {
                            com.ghais.domain.model.RoutineModeType.STUDY -> "study-focus"
                            com.ghais.domain.model.RoutineModeType.WORK -> "focus-work"
                            com.ghais.domain.model.RoutineModeType.SLEEP -> "sleep-mode"
                        }
                        rootNavigator?.push(PlaylistDetailsScreen(playlistId))
                    })
                }
                item { HomeSectionHeader(title = "Your stats", onSeeAll = null) }
                item { HomeStatsCard() }
            }
        }
    }
}
