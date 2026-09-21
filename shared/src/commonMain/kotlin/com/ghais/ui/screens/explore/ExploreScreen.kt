package com.ghais.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.screens.explore.components.ExploreBentoCard
import com.ghais.ui.screens.explore.components.PureBlack
import com.ghais.ui.screens.explore.components.isWideCard
import com.ghais.ui.screens.explore.glass.ProtonCategoryRow
import com.ghais.ui.screens.explore.glass.ProtonEmptyState
import com.ghais.ui.screens.explore.glass.ProtonExploreHeader
import com.ghais.ui.screens.explore.glass.ProtonExploreSearch
import com.ghais.ui.screens.explore.glass.ProtonFeaturedBanner
import com.ghais.ui.screens.playlists.MoodPlaylists
import com.ghais.ui.screens.playlists.PlaylistDetailsScreen

/**
 * ExploreScreen:
 *
 * Designed according to:
 * - iOS Human Interface Guidelines (Apple-style UI): SF-scale Large Title, subheadline, generous corner radii,
 *   continuous curves, 48dp touch targets, Apple-style frosted search bar and category filter pills.
 * - Dark Minimalist UI: Pure black background (#000000), dark obsidian layers (#0D0F12 / #141418), soft off-white text.
 * - Glassmorphism: Translucent frosted glass containers, specular hairline borders, top specular reflection.
 * - Modular Cards with Chunky / Oversized Avatars (76dp - 84dp): Halo glowing borders, instant full-surah queue play.
 * - Migrated Focus Mode: Deep Focus & Study, Deep Work & Flow, and Restful Sleep routines prominently featured.
 */
object ExploreScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Explore",
                icon = null
            )
        }

    private val Categories = listOf(
        "All",
        "Focus",
        "Work",
        "Sleep",
        "Peace",
        "Healing",
        "Night",
        "Barakah",
        "Reflection"
    )

    @Composable
    override fun Content() {
        val rootNavigator = LocalRootNavigator.current ?: LocalNavigator.current?.parent ?: LocalNavigator.current
        var query by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("All") }

        val filteredCards = remember(query, selectedCategory) {
            MoodPlaylists.filter { playlist ->
                val matchesQuery = query.isBlank() ||
                    playlist.title.contains(query, ignoreCase = true) ||
                    playlist.description.contains(query, ignoreCase = true)

                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Focus" -> playlist.id in listOf("study-focus", "focus-work")
                    "Work" -> playlist.id in listOf("focus-work", "study-focus")
                    "Sleep" -> playlist.id in listOf("sleep-mode", "tahajjud")
                    "Peace" -> playlist.id in listOf("heart-soothing", "most-beautiful")
                    "Healing" -> playlist.id in listOf("duaa-ruqia", "heart-soothing")
                    "Night" -> playlist.id in listOf("tahajjud", "sleep-mode")
                    "Barakah" -> playlist.id in listOf("sunrise-barakah", "favourites")
                    "Reflection" -> playlist.id in listOf("emotional", "most-beautiful")
                    else -> true
                }

                matchesQuery && matchesCategory
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            ProtonExploreHeader(
                titleWhite = "Find Your",
                titleGrey = "Focus",
                subtitle = "Curated collections, deep focus routines & sacred themes"
            )

            ProtonExploreSearch(
                query = query,
                onQuery = { query = it }
            )

            ProtonCategoryRow(
                categories = Categories,
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )

            if (query.isBlank() && selectedCategory == "All" && filteredCards.isNotEmpty()) {
                ProtonFeaturedBanner(
                    title = "Deep Focus & Study",
                    subtitle = "Steady recitation to carry your day",
                    buttonText = "Open",
                    onOpen = { rootNavigator?.push(PlaylistDetailsScreen("study-focus")) }
                )
            }

            // Bento Grid of Modular Cards with Chunky / Oversized Avatars
            if (filteredCards.isEmpty()) {
                ProtonEmptyState(
                    title = "No collections found",
                    body = "Try adjusting your search query or category filter."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = filteredCards,
                        key = { it.id },
                        span = { playlist ->
                            if (playlist.isWideCard()) GridItemSpan(2) else GridItemSpan(1)
                        }
                    ) { playlist ->
                        ExploreBentoCard(
                            playlist = playlist,
                            onClick = { rootNavigator?.push(PlaylistDetailsScreen(playlist.id)) }
                        )
                    }
                }
            }
        }
    }
}
