package com.quranify.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.navigator.tab.Tab
import com.quranify.ui.screens.home.HomeScreen
import com.quranify.ui.screens.explore.ExploreScreen
import com.quranify.ui.screens.library.LibraryScreen
import com.quranify.ui.screens.settings.SettingsScreen

enum class AppTab(
    val title: String,
    val icon: ImageVector,
    val tab: Tab
) {
    Home("Home", Icons.Filled.Home, HomeScreen),
    Explore("Explore", Icons.Filled.Search, ExploreScreen),
    Library("Library", Icons.Filled.Book, LibraryScreen),
    Settings("Settings", Icons.Filled.Settings, SettingsScreen)
}
