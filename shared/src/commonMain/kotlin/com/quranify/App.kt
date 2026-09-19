package com.quranify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransition
import com.quranify.player.AudioEngine
import com.quranify.ui.components.MiniPlayer
import com.quranify.ui.navigation.LocalRootNavigator
import com.quranify.ui.navigation.MainScreen
import com.quranify.ui.screens.player.NowPlayingScreen
import com.quranify.ui.theme.QuranifyTheme
import kotlinx.coroutines.delay

@Composable
fun App() {
    QuranifyTheme {
        Navigator(MainScreen) { navigator ->
            val currentTrack by AudioEngine.currentTrack.collectAsState()
            val isNowPlaying = navigator.lastItem is NowPlayingScreen
            val isPushedScreen = navigator.lastItem !is MainScreen && !isNowPlaying

            // Keep ScreenTransition layered above the overlay MiniPlayer while NowPlaying is active or animating out
            var isNowPlayingTransitioning by remember { mutableStateOf(false) }
            LaunchedEffect(navigator.lastItem) {
                if (navigator.lastItem is NowPlayingScreen) {
                    isNowPlayingTransitioning = true
                } else if (isNowPlayingTransitioning) {
                    delay(380L)
                    isNowPlayingTransitioning = false
                }
            }

            CompositionLocalProvider(LocalRootNavigator provides navigator) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenTransition(
                        navigator = navigator,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isNowPlaying || isNowPlayingTransitioning) 2f else 0f),
                        transition = {
                            val isTargetNowPlaying = targetState is NowPlayingScreen
                            val isInitialNowPlaying = initialState is NowPlayingScreen

                            when {
                                isTargetNowPlaying -> {
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                                    ) togetherWith fadeOut(animationSpec = tween(200))
                                }
                                isInitialNowPlaying -> {
                                    fadeIn(animationSpec = tween(200)) togetherWith slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                                    )
                                }
                                else -> {
                                    val (initialOffset, targetOffset) = when (navigator.lastEvent) {
                                        StackEvent.Pop -> ({ size: Int -> -size } to { size: Int -> size })
                                        else -> ({ size: Int -> size } to { size: Int -> -size })
                                    }
                                    slideInHorizontally(
                                        initialOffsetX = initialOffset,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) togetherWith slideOutHorizontally(
                                        targetOffsetX = targetOffset,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    )

                    // Overlay MiniPlayer on any pushed screen (Reciter Profile, Surah Detail, etc.)
                    AnimatedVisibility(
                        visible = currentTrack != null && isPushedScreen,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
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
