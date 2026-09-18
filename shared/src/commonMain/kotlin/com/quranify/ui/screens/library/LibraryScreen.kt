package com.quranify.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.screens.library.components.HifzGoalCard
import com.quranify.ui.screens.library.components.LibraryAppHeader
import com.quranify.ui.screens.library.components.LibraryFilterChips
import com.quranify.ui.screens.library.components.LibraryHadithFooter
import com.quranify.ui.screens.library.components.LibraryPlaylistsSection
import com.quranify.ui.screens.library.components.LibraryQuickGridSection
import com.quranify.ui.screens.library.components.LibraryTopBar
import com.quranify.ui.screens.library.components.RamadanKhatmCard
import com.quranify.ui.screens.reciters.ReciterProfileScreen
import com.quranify.ui.screens.search.SearchScreen
import com.quranify.ui.theme.QuranifyColors

object LibraryScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Library",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var selectedFilter by remember { mutableStateOf("All") }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Fixed/Sticky Top Bar matching Stitch specs
            item {
                LibraryAppHeader(
                    onSearchClick = { navigator?.push(SearchScreen()) },
                    onProfileClick = { /* Profile navigation */ }
                )
            }

            // 2. Section Header: "Your Library" with Live Pulsing Green Dot
            item {
                LibraryTopBar(
                    onSearchClick = { navigator?.push(SearchScreen()) },
                    onAddClick = { /* Add playlist or bookmark */ }
                )
            }

            // 3. Category Filter Chips ("All", "Playlists", "Downloaded", "Saved Verses", etc.)
            item {
                LibraryFilterChips(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it }
                )
            }

            // 4. Ramadan Khatm Tracker progress card (42% circular Canvas arc)
            if (selectedFilter == "All" || selectedFilter == "Hifz Goals") {
                item {
                    RamadanKhatmCard(
                        onResumeClick = { navigator?.push(KhatmaScreen) },
                        onClick = { navigator?.push(KhatmaScreen) }
                    )
                }
            }

            // 5. Quick Cards (Liked Verses & Duas, Offline Surahs)
            if (selectedFilter == "All" || selectedFilter == "Saved Verses" || selectedFilter == "Downloaded") {
                item {
                    LibraryQuickGridSection(
                        onLikedVersesClick = { navigator?.push(FavoritesScreen) },
                        onDownloadedClick = { selectedFilter = "Downloaded" }
                    )
                }
            }

            // 6. Hifz Goal Card (Surah Al-Mulk 67)
            if (selectedFilter == "All" || selectedFilter == "Hifz Goals") {
                item {
                    HifzGoalCard(
                        onReviewClick = { /* Review ayah */ }
                    )
                }
            }

            // 7. Custom Playlists & Recents Shelf
            item {
                LibraryPlaylistsSection(
                    selectedFilter = selectedFilter,
                    onPlaylistClick = { playlistId ->
                        navigator?.push(PlaylistDetailScreen(playlistId))
                    },
                    onReciterClick = { slug ->
                        navigator?.push(ReciterProfileScreen(slug))
                    }
                )
            }

            // 8. Inspirational Hadith Quote Footer
            item {
                LibraryHadithFooter()
            }
        }
    }
}
