package com.ghais.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ghais.player.AudioEngine
import com.ghais.ui.components.MiniPlayer
import com.ghais.ui.screens.home.HomeScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.theme.QuranifyColors

val LocalRootNavigator = compositionLocalOf<Navigator?> { null }

object MainScreen : Screen {
    @Composable
    override fun Content() {
        val rootNavigator = LocalNavigator.currentOrThrow
        CompositionLocalProvider(LocalRootNavigator provides rootNavigator) {
            TabNavigator(HomeScreen) {
                val currentTrack by AudioEngine.currentTrack.collectAsState()
            
            Scaffold(
                containerColor = QuranifyColors.Background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    CurrentTab()
                    // Floating overlay: transparent glass dock + mini-player hover
                    // over the scrolling content (no bottomBar so content ghosts through).
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    ) {
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
                }
            }
        }
    }
}
}
