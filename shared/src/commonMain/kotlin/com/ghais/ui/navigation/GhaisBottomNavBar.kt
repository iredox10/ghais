package com.ghais.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.ghais.player.AudioEngine
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

// Phase 2 — Noir floating island dock tokens (translucent black glass).
private val DockGlassTop = Color(0x8C141416) // ~55% black — content ghosts through
private val DockGlassBottom = Color(0x59101012) // ~35% black — lighter at the base

/**
 * Noir floating island dock: 5 circular tab buttons + a raised chrome
 * play/pause FAB riding center (reference floating-dock language).
 * Selected tab: white wash disc + rim; inactive: tertiary grey glyphs.
 */
@Composable
fun GhaisBottomNavBar(modifier: Modifier = Modifier) {
    val tabNavigator = LocalTabNavigator.current
    val tabs = AppTab.entries
    // Home + Reciters left of the FAB, remaining three right of it.
    val tabsBeforeFab = 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(76.dp),
            shape = GhaisShapes.navDock,
            color = Color.Transparent,
            border = BorderStroke(1.dp, GhaisNoir.BorderCard),
            shadowElevation = 16.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(DockGlassTop, DockGlassBottom)
                        ),
                        GhaisShapes.navDock
                    )
                    .clip(GhaisShapes.navDock)
                    .topSpecular(inset = 60.dp)
            ) {
                // Diagonal glass sheen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GhaisNoir.sheen())
                )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.take(tabsBeforeFab).forEach { appTab ->
                    NoirDockButton(appTab = appTab)
                }
                NoirCenterFab()
                tabs.drop(tabsBeforeFab).forEach { appTab ->
                    NoirDockButton(appTab = appTab)
                }
            }
            }
        }
    }
}

/** Circular tab button: white wash disc + rim when selected, grey glyph idle. */
@Composable
private fun NoirDockButton(appTab: AppTab) {
    val tabNavigator = LocalTabNavigator.current
    val currentTab = runCatching { tabNavigator.current }.getOrNull()
    val isSelected = currentTab == appTab.tab

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 550f),
        label = "dockPress"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dockBg"
    )
    val tint by animateColorAsState(
        targetValue = if (isSelected) GhaisNoir.TextPrimary else GhaisNoir.TextTertiary,
        animationSpec = tween(durationMillis = 200),
        label = "dockTint"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = bgAlpha))
            .then(
                if (isSelected) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                else Modifier
            )
            .clickable(interactionSource = interaction, indication = null) {
                tabNavigator.current = appTab.tab
            },
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            Icon(
                imageVector = appTab.icon,
                contentDescription = appTab.title,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Raised chrome play/pause FAB riding the dock center. */
@Composable
private fun NoirCenterFab() {
    val isPlaying by AudioEngine.isPlaying.collectAsState()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 550f),
        label = "fabPress"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(56.dp)
            .clip(CircleShape)
            .background(GhaisNoir.chromeFill())
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { AudioEngine.togglePlayPause() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = GhaisNoir.OnChrome,
            modifier = Modifier.size(26.dp)
        )
    }
}
