package com.quranify.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.navigator.tab.Tab
import com.quranify.ui.screens.explore.ExploreScreen
import com.quranify.ui.screens.home.HomeScreen
import com.quranify.ui.screens.library.LibraryScreen
import com.quranify.ui.screens.mushaf.MushafScreen

enum class AppTab(
    val title: String,
    val icon: ImageVector,
    val tab: Tab
) {
    Home("Home", Icons.Filled.Home, HomeScreen),
    Explore("Explore", Icons.AutoMirrored.Filled.MenuBook, ExploreScreen),
    Library("Library", Icons.Filled.LibraryMusic, LibraryScreen),
    Mushaf("Mushaf", Icons.Filled.AutoStories, MushafScreen)
}
