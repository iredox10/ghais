package com.quranify.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.quranify.player.AmbientMixer
import com.quranify.player.AmbientType
import com.quranify.player.AmbientVideoBridge
import com.quranify.player.AudioEngine
import com.quranify.player.videoKeys
import kotlinx.coroutines.delay

/**
 * Android actual: fullscreen muted background-video layer for Now Playing.
 *
 * Two stacked [PlayerView]s with fixed bindings (layer A <-> player A, layer B
 * <-> player B, both `use_controller=false` and tap-transparent). The manager
 * starts the back player BEFORE bumping [AmbientVideoBridge.swapEvent], so we
 * fade the back layer 0 -> 1 over ~1.2s and only then call
 * [AmbientVideoBridge.confirmSwap] (old front stops, back -> front). The faded-in
 * layer stays opaque across the role flip; the retired layer fades out behind
 * it, so there is no flicker.
 *
 * Empty [keys] renders an empty [Box] (the glow fallback underneath stays
 * visible) and stops the players.
 */
@Composable
actual fun AmbientVideoView(selectedType: AmbientType?, modifier: Modifier) {
    // Subscribed so mixer resets (clearAll / preset switches) recompose us even
    // if the caller holds a stale selectedType.
    AmbientMixer.channels.collectAsState()
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val frontIsA by AmbientVideoBridge.frontIsA.collectAsState()
    val backReadyKey by AmbientVideoBridge.backReadyKey.collectAsState()
    val swapEvent by AmbientVideoBridge.swapEvent.collectAsState()

    val keys = selectedType?.videoKeys().orEmpty()

    LaunchedEffect(keys, isPlaying) {
        if (keys.isEmpty()) {
            AmbientVideoBridge.stopVideos()
        } else if (isPlaying) {
            AmbientVideoBridge.playVideos(keys)
        } else {
            AmbientVideoBridge.pauseVideos()
        }
    }

    // Crossfade handshake: wait out the fade, hand roles over, record progress.
    // confirmSwap() is a no-op if the rotation was stopped mid-fade.
    var lastHandledSwap by remember { mutableStateOf(0L) }
    LaunchedEffect(swapEvent) {
        if (swapEvent == 0L || swapEvent == lastHandledSwap) return@LaunchedEffect
        delay(1300L)
        AmbientVideoBridge.confirmSwap()
        lastHandledSwap = swapEvent
    }

    // The front layer is always opaque; the back layer fades in only while a
    // swap is pending. After confirmSwap() the faded-in layer IS the front, so
    // its target stays 1 while the retired layer drops to 0 behind it.
    val layerATarget = if (frontIsA || backReadyKey != null) 1f else 0f
    val layerBTarget = if (!frontIsA || backReadyKey != null) 1f else 0f
    val alphaA by animateFloatAsState(
        targetValue = layerATarget,
        animationSpec = tween(durationMillis = 1200),
        label = "ambientLayerA",
    )
    val alphaB by animateFloatAsState(
        targetValue = layerBTarget,
        animationSpec = tween(durationMillis = 1200),
        label = "ambientLayerB",
    )

    DisposableEffect(Unit) {
        onDispose { AmbientVideoBridge.stopVideos() }
    }

    Box(modifier) {
        if (keys.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx).inflate(
                        com.quranify.shared.R.layout.ambient_video_texture_view,
                        null
                    ) as PlayerView).apply {
                        // surface_type/use_controller/resize_mode/show_buffering
                        // all come from the XML (texture_view is required for alpha fade).
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        // Tap-passthrough to the scrubber underneath.
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        player = AmbientVideoBridge.playerA()
                    }
                },
                update = { it.player = AmbientVideoBridge.playerA() },
                modifier = Modifier.matchParentSize().alpha(alphaA),
            )
            AndroidView(
                factory = { ctx ->
                    (android.view.LayoutInflater.from(ctx).inflate(
                        com.quranify.shared.R.layout.ambient_video_texture_view,
                        null
                    ) as PlayerView).apply {
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        // Tap-passthrough to the scrubber underneath.
                        isClickable = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        player = AmbientVideoBridge.playerB()
                    }
                },
                update = { it.player = AmbientVideoBridge.playerB() },
                modifier = Modifier.matchParentSize().alpha(alphaB),
            )
        }
    }
}
