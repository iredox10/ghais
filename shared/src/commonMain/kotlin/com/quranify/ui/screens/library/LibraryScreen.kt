package com.quranify.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.quranify.ui.screens.library.components.LibraryFilterChips
import com.quranify.ui.screens.library.components.LibraryPlaylistsSection
import com.quranify.ui.screens.library.components.LibraryQuickGridSection
import com.quranify.ui.screens.library.components.LibraryTopBar
import com.quranify.ui.screens.library.components.RamadanKhatmCard
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
        var selectedFilter by remember { mutableStateOf("All") }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(QuranifyColors.Background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                LibraryTopBar()
            }
            item {
                LibraryFilterChips(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it }
                )
            }
            item {
                RamadanKhatmCard()
            }
            item {
                LibraryQuickGridSection()
            }
            item {
                LibraryPlaylistsSection()
            }
        }
    }
}
