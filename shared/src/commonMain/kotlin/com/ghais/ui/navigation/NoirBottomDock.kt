package com.ghais.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

// Noir floating pill dock tokens: near-opaque black bar with circular keys.
private val DockBarFill = Color(0xF2141416) // ~95% black — solid pill, content hidden

/**
 * Noir floating pill dock: all [AppTab] entries as circular keys in a solid
 * black pill, no center FAB, no labels.
 *
 * Bottom margin stays 12dp with no extra navigationBarsPadding(): MainScreen hosts
 * the dock inside Scaffold innerPadding, which already consumes the navigation-bar
 * inset — adding it again would float the dock ~24dp above the gesture bar.
 *
 * Item visuals live in [DockTabItem] (NoirDockItem.kt, same package, no import):
 * selected key is a light filled circle with a near-black glyph, the rest are
 * quiet light glyphs on transparent circles.
 */
@Composable
fun NoirBottomDock(modifier: Modifier = Modifier) {
    val tabNavigator = LocalTabNavigator.current
    val currentTab = runCatching { tabNavigator.current }.getOrNull()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(78.dp),
            shape = GhaisShapes.navDock,
            color = DockBarFill,
            border = BorderStroke(1.dp, GhaisNoir.BorderCard),
            shadowElevation = 16.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(GhaisShapes.navDock)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTab.entries.forEach { appTab ->
                        DockTabItem(
                            tab = appTab,
                            selected = currentTab == appTab.tab,
                            onClick = { tabNavigator.current = appTab.tab },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
