package com.quranify.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import com.quranify.ui.theme.QuranifyColors

@Composable
fun QuranifyBottomNavBar(modifier: Modifier = Modifier) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBar(
        modifier = modifier,
        containerColor = QuranifyColors.Surface,
        contentColor = QuranifyColors.TextSecondary
    ) {
        AppTab.values().forEach { appTab ->
            val isSelected = tabNavigator.current == appTab.tab
            
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextSecondary
            )
            
            val textColor by animateColorAsState(
                targetValue = if (isSelected) QuranifyColors.Primary else QuranifyColors.TextSecondary
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { tabNavigator.current = appTab.tab },
                icon = {
                    Icon(
                        imageVector = appTab.icon,
                        contentDescription = appTab.title,
                        tint = iconColor
                    )
                },
                label = {
                    Text(
                        text = appTab.title,
                        color = textColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = QuranifyColors.Primary,
                    unselectedIconColor = QuranifyColors.TextSecondary,
                    selectedTextColor = QuranifyColors.Primary,
                    unselectedTextColor = QuranifyColors.TextSecondary,
                    indicatorColor = QuranifyColors.Surface
                )
            )
        }
    }
}
