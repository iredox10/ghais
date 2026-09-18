package com.quranify.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.quranify.player.AudioEngine
import com.quranify.ui.components.MiniPlayer
import com.quranify.ui.screens.home.HomeScreen
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.theme.QuranifyColors

val LocalRootNavigator = compositionLocalOf<Navigator?> { null }

object MainScreen : Screen {
    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        CompositionLocalProvider(LocalRootNavigator provides rootNavigator) {
            TabNavigator(HomeScreen) {
                val currentTrack by AudioEngine.currentTrack.collectAsState()
            
            Scaffold(
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = currentTrack != null,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            MiniPlayer(
                                onOpenNowPlaying = {
                                    rootNavigator.push(NowPlayingScreen())
                                }
                            )
                        }
                        QuranifyBottomNavBar()
                    }
                },
                containerColor = QuranifyColors.Background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    CurrentTab()
                }
            }
        }
    }
}
}
