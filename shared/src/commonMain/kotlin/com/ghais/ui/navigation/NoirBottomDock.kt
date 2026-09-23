package com.ghais.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.ghais.ui.components.noir.topSpecular
import com.ghais.ui.theme.GhaisNoir
import com.ghais.ui.theme.GhaisShapes

// Noir floating pill dock tokens (translucent black glass, zero hue).
// Copied from GhaisBottomNavBar.kt so both docks share one glass recipe.
private val DockGlassTop = Color(0x8C141416) // ~55% black — content ghosts through
private val DockGlassBottom = Color(0x59101012) // ~35% black — lighter at the base

/**
 * Noir floating pill dock: all 6 [AppTab] entries in a single row, no center FAB.
 *
 * Container language mirrors GhaisBottomNavBar (floating pill, fillMaxWidth max
 * 520dp, 76dp, navDock shape, glass gradient + BorderCard + topSpecular + sheen).
 * Bottom margin stays 12dp with no extra navigationBarsPadding(): MainScreen hosts
 * the dock inside Scaffold innerPadding, which already consumes the navigation-bar
 * inset — adding it again would float the dock ~24dp above the gesture bar.
 *
 * Item visuals live in [DockTabItem] (NoirDockItem.kt, same package, no import):
 * selected icon on a white rounded square, tiny label underneath, Noir tokens only.
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
                        Brush.verticalGradient(
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
                        .padding(horizontal = 6.dp, vertical = 4.dp),
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
