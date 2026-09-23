package com.ghais.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.navigator.tab.Tab
import com.ghais.ui.screens.home.HomeScreen
import com.ghais.ui.screens.library.LibraryScreen
import com.ghais.ui.screens.memorization.MemorizationScreen
import com.ghais.ui.screens.profile.ProfileScreen
import com.ghais.ui.screens.reciters.RecitersScreen

enum class AppTab(
    val title: String,
    val icon: ImageVector,
    val tab: Tab
) {
    Home("Home", Icons.Filled.Home, HomeScreen),
    Reciters("Reciters", Icons.Filled.RecordVoiceOver, RecitersScreen()),
    Memorization("Hifz", Icons.Filled.School, MemorizationScreen),
    Playlists("Collections", Icons.Filled.GridView, LibraryScreen),
    Profile("Profile", Icons.Filled.Person, ProfileScreen)
}
