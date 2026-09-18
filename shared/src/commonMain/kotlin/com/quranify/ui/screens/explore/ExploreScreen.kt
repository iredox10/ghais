package com.quranify.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.data.seed.QuranData
import com.quranify.ui.screens.explore.components.CuratedThemesGrid
import com.quranify.ui.screens.explore.components.ExploreDirectorySection
import com.quranify.ui.screens.explore.components.ExploreFilterPills
import com.quranify.ui.screens.explore.components.ExploreSearchBar
import com.quranify.ui.screens.explore.components.ExploreSegmentedSwitcher
import com.quranify.ui.screens.explore.components.ExploreTopBar
import com.quranify.ui.theme.QuranifyColors

object ExploreScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 1u,
                title = "Explore",
                icon = null
            )
        }

    @Composable
    override fun Content() {
        var searchQuery by remember { mutableStateOf("") }
        var selectedPill by remember { mutableStateOf("All") }
        var selectedTabIndex by remember { mutableStateOf(0) }

        val filteredSurahs = remember(searchQuery) {
            if (searchQuery.isBlank()) {
                QuranData.SURAHS
            } else {
                QuranData.SURAHS.filter {
                    it.nameEn.contains(searchQuery, ignoreCase = true) ||
                            it.nameAr.contains(searchQuery, ignoreCase = true) ||
                            it.meaning.contains(searchQuery, ignoreCase = true) ||
                            it.transliteration.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ExploreTopBar()
            }
            item {
                ExploreSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
            item {
                ExploreFilterPills(
                    selectedPill = selectedPill,
                    onSelectPill = { selectedPill = it }
                )
            }
            item {
                ExploreSegmentedSwitcher(
                    selectedTab = selectedTabIndex,
                    onSelectTab = { selectedTabIndex = it }
                )
            }
            item {
                CuratedThemesGrid()
            }
            item {
                ExploreDirectorySection(
                    surahs = filteredSurahs
                )
            }
        }
    }
}
