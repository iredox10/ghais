package com.quranify.ui.screens.home

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
import com.quranify.data.repository.QuranDataRepository
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.library.FavoritesScreen
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.screens.playlists.PlaylistDetailsScreen
import com.quranify.ui.screens.reciters.RecitersScreen
import com.quranify.ui.screens.reciters.ReciterProfileScreen
import com.quranify.ui.screens.search.SearchScreen
import com.quranify.ui.screens.settings.SettingsScreen

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
                        onPremiumClick = { tabNavigator?.let { it.current = SettingsScreen } },
                        onStatsClick = { rootNavigator?.push(SearchScreen()) }
                    )
                }
                item { HomeSectionHeader(title = "Continue listening", onSeeAll = null) }
                item {
                    HomeContinueListeningRow(onPlay = { item ->
                        val reciter = QuranDataRepository.getReciterBySlug(item.reciterSlug)
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
                        val startIndex = allTracks.indexOfFirst { it.surahId == item.surahId }.coerceAtLeast(0)
                        val currentTrack = AudioEngine.currentTrack.value
                        val isCurrent = currentTrack != null &&
                            currentTrack.surahId == item.surahId &&
                            currentTrack.reciterSlug == reciter.slug
                        if (isCurrent && AudioEngine.isPlaying.value) {
                            rootNavigator?.push(NowPlayingScreen())
                        } else if (isCurrent) {
                            AudioEngine.resume()
                            rootNavigator?.push(NowPlayingScreen())
                        } else {
                            AudioEngine.playQueue(allTracks, startIndex = startIndex)
                            if (item.positionMs > 0L) {
                                AudioEngine.seekTo(item.positionMs)
                            }
                            rootNavigator?.push(NowPlayingScreen())
                        }
                    })
                }
                item {
                    HomeSectionHeader(
                        title = "Playlists",
                        onSeeAll = { tabNavigator?.let { it.current = com.quranify.ui.screens.library.LibraryScreen } }
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
                        title = "Newly added",
                        onSeeAll = { rootNavigator?.push(RecitersScreen()) }
                    )
                }
                item {
                    HomeNewlyAddedRow(onReciter = { slug -> rootNavigator?.push(ReciterProfileScreen(slug)) })
                }
                item {
                    HomeSectionHeader(
                        title = "Listen by routine",
                        onSeeAll = { tabNavigator.current = com.quranify.ui.screens.explore.ExploreScreen }
                    )
                }
                item {
                    HomeRoutineRow(onMode = { mode ->
                        val playlistId = when (mode) {
                            com.quranify.domain.model.RoutineModeType.STUDY -> "study-focus"
                            com.quranify.domain.model.RoutineModeType.WORK -> "focus-work"
                            com.quranify.domain.model.RoutineModeType.SLEEP -> "sleep-mode"
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
