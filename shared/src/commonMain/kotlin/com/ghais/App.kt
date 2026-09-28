package com.ghais

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.ghais.data.auth.AuthRepository
import com.ghais.data.repository.OnboardingStore
import com.ghais.data.sync.SyncTriggers
import com.ghais.player.AudioEngine
import com.ghais.ui.components.MiniPlayer
import com.ghais.ui.components.isFullScreenPlayer
import com.ghais.ui.navigation.LocalRootNavigator
import com.ghais.ui.navigation.MainScreen
import com.ghais.ui.screens.auth.AuthScreen
import com.ghais.ui.screens.onboarding.OnboardingScreen
import com.ghais.ui.screens.player.NowPlayingScreen
import com.ghais.ui.screens.splash.SplashScreen
import com.ghais.ui.theme.GhaisTheme
import kotlinx.coroutines.delay

@Composable
fun App() {
    GhaisTheme {
        val session by AuthRepository.session.collectAsState()
        val checked by AuthRepository.authChecked.collectAsState()
        val cachedSession by AuthRepository.cachedSession.collectAsState()
        val doneForUser by OnboardingStore.isDoneForCurrentUser.collectAsState()
        var forceOnboarding by remember { mutableStateOf(false) }
        val signedInUserId = session?.userId ?: cachedSession?.userId
        LaunchedEffect(Unit) { AuthRepository.refreshSession() }
        LaunchedEffect(Unit) { SyncTriggers.start(this) }
        // OnboardingScreen exposes plain Screen.Content() with no onFinish/onComplete
        // callback (completion lands in OnboardingStore internally); clearing the
        // replay latch is therefore observed via doneForUser.
        LaunchedEffect(doneForUser) { if (doneForUser) forceOnboarding = false }
        // Onboarding belongs to brand-new accounts only. Every other
        // established session (returning login, session restored at launch,
        // cached offline session) records the per-user flag as done, so the
        // gate below and any later owner rebind can never drop that user back
        // into onboarding. Keyed off the session user id rather than the store
        // namespace because OnboardingStore.setOwner runs on SyncTriggers'
        // own collector and may still be bound to "local" on this frame —
        // doneForUser doubles as a key so that the first write landing in the
        // wrong namespace is retried once setOwner reports the real value.
        LaunchedEffect(signedInUserId, forceOnboarding, doneForUser) {
            if (signedInUserId == null || forceOnboarding || doneForUser) return@LaunchedEffect
            OnboardingStore.markDoneForCurrentUser()
        }
        // Offline gate: no internet → refreshSession fails → session null even
        // for logged-in users. cachedSession is the last-known user from
        // persistent storage; when set, enter main content in offline mode
        // instead of bouncing to the login gate. MainScreen takes no offline
        // flag (plain object), so entry is normal. SyncEngine stays idle via
        // its NetworkMonitor gate in SyncTriggers; when connectivity returns
        // and refresh succeeds, the session flow drives a seamless transition.
        val offlineMode = checked && session == null && cachedSession != null
        if (!checked) {
            SplashScreen.Content()
        } else if (session == null && !offlineMode) {
            // Full-screen gate above everything; MiniPlayer stays under the gate.
            // Auth is mandatory — no guest mode. Logged-out never sees onboarding;
            // returning here (e.g. sign-out) also clears any pending replay.
            if (forceOnboarding) forceOnboarding = false
            AuthScreen(
                onAuthenticated = { isNewAccount ->
                    if (isNewAccount) {
                        OnboardingStore.restartForNewUser()
                        forceOnboarding = true
                    }
                },
            ).Content()
        } else if (forceOnboarding) {
            OnboardingScreen.Content()
        } else {
            Navigator(MainScreen) { navigator ->
            val currentTrack by AudioEngine.currentTrack.collectAsState()
            // Any full-screen player, not just the Quran one: the devotional
            // reader animates its own exit exactly like NowPlayingScreen, so it
            // needs the same layering (below) and the same transition branch.
            val isFullPlayerOpen = isFullScreenPlayer(navigator.lastItem)
            // Deliberately NOT widened with isFullPlayerOpen: the devotional
            // reader is laid out as a pushed screen and reserves
            // MINI_PLAYER_CLEARANCE for this bar (DevotionPlayerScreen.kt:90-100),
            // so it keeps being treated as one. Only the Quran player is opaque
            // enough to hide it.
            val isPushedScreen = navigator.lastItem !is MainScreen &&
                navigator.lastItem !is NowPlayingScreen

            // Keep ScreenTransition layered above the overlay MiniPlayer while any full-screen player is active or animating out
            var isFullPlayerTransitioning by remember { mutableStateOf(false) }
            LaunchedEffect(navigator.lastItem) {
                if (isFullScreenPlayer(navigator.lastItem)) {
                    isFullPlayerTransitioning = true
                } else if (isFullPlayerTransitioning) {
                    delay(380L)
                    isFullPlayerTransitioning = false
                }
            }

            CompositionLocalProvider(LocalRootNavigator provides navigator) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenTransition(
                        navigator = navigator,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isFullPlayerOpen || isFullPlayerTransitioning) 2f else 0f),
                        transition = {
                            val isTargetPlayer = isFullScreenPlayer(targetState)
                            val isInitialPlayer = isFullScreenPlayer(initialState)

                            when {
                                isTargetPlayer -> {
                                    slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(350, easing = FastOutSlowInEasing)
                                    ) togetherWith fadeOut(animationSpec = tween(200))
                                }
                                isInitialPlayer -> {
                                    // The player animates its own exit (see
                                    // NowPlayingScreen.animateOutAndDismiss and
                                    // DevotionPlayerScreen.animateOutAndDismiss —
                                    // the devotional reader copied that invariant)
                                    // and the screen underneath is opaque, so the
                                    // stack must not animate on top of it: the
                                    // incoming screen is drawn from frame one
                                    // (EnterTransition.None) and the player is
                                    // simply removed when its own tween lands.
                                    // Fading/sliding both sides here is what
                                    // produced the blank frame between the full
                                    // player and the mini player.
                                    EnterTransition.None togetherWith ExitTransition.None
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
}
