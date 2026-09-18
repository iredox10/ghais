package com.quranify.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import com.quranify.ui.theme.QuranifyColors

@Composable
fun QuranifyBottomNavBar(modifier: Modifier = Modifier) {
    val tabNavigator = LocalTabNavigator.current

    Column(modifier = modifier) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = QuranifyColors.Divider
        )
        NavigationBar(
            containerColor = QuranifyColors.Surface,
            contentColor = QuranifyColors.TextTertiary,
            tonalElevation = 0.dp
        ) {
            AppTab.entries.forEach { appTab ->
                val isSelected = tabNavigator.current == appTab.tab

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { tabNavigator.current = appTab.tab },
                    icon = {
                        Icon(
                            imageVector = appTab.icon,
                            contentDescription = appTab.title
                        )
                    },
                    label = {
                        Text(text = appTab.title)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuranifyColors.Primary,
                        unselectedIconColor = QuranifyColors.TextTertiary,
                        selectedTextColor = QuranifyColors.Primary,
                        unselectedTextColor = QuranifyColors.TextTertiary,
                        indicatorColor = QuranifyColors.Surface
                    )
                )
            }
        }
    }
}
