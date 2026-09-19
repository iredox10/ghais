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
import com.quranify.domain.model.TrackItem
import com.quranify.player.AudioEngine
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.screens.curated.AllCuratedPlaylistsScreen
import com.quranify.ui.screens.library.FavoritesScreen
import com.quranify.ui.screens.mood.RoutineModeScreen
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
                item { HomeFeaturedHero(onClick = { rootNavigator?.push(ReciterProfileScreen("mishary")) }) }
                item { HomeSectionHeader(title = "Continue listening", onSeeAll = null) }
                item {
                    HomeContinueListeningRow(onPlay = { title ->
                        AudioEngine.playTrack(
                            TrackItem(
                                reciterSlug = "alafasy",
                                reciterName = "Mishary Rashid Alafasy",
                                surahId = 67,
                                surahNameEn = title,
                                surahNameAr = "الملك",
                                ayahNo = 1,
                                audioUrl = "https://everyayah.com/data/Alafasy_64kbps/067001.mp3",
                                durationMs = 0L
                            )
                        )
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
                        onFocusWork = { rootNavigator?.push(AllCuratedPlaylistsScreen()) },
                        onNightSleep = { rootNavigator?.push(RoutineModeScreen(com.quranify.domain.model.RoutineModeType.SLEEP)) }
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
                item { HomeSectionHeader(title = "Listen by routine", onSeeAll = null) }
                item {
                    HomeRoutineRow(onMode = { mode -> rootNavigator?.push(RoutineModeScreen(mode)) })
                }
                item { HomeSectionHeader(title = "Your stats", onSeeAll = null) }
                item { HomeStatsCard() }
            }
        }
    }
}
