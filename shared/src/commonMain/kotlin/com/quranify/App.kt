package com.quranify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.quranify.player.AudioEngine
import com.quranify.ui.components.MiniPlayer
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.navigation.MainScreen
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.theme.QuranifyTheme

@Composable
fun App() {
    QuranifyTheme {
        Navigator(MainScreen) { navigator ->
            val currentTrack by AudioEngine.currentTrack.collectAsState()
            val isPushedScreen = navigator.lastItem !is MainScreen && navigator.lastItem !is NowPlayingScreen

            CompositionLocalProvider(LocalRootNavigator provides navigator) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SlideTransition(navigator)

                    // Overlay MiniPlayer on any pushed screen (Reciter Profile, Surah Detail, etc.)
                    AnimatedVisibility(
                        visible = currentTrack != null && isPushedScreen,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp)
                    ) {
                        MiniPlayer(
                            onOpenNowPlaying = {
                                navigator.push(NowPlayingScreen())
                            }
                        )
                    }
                }
            }
        }
    }
}
