package com.quranify

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.quranify.ui.navigation.MainScreen
import com.quranify.ui.theme.QuranifyTheme

@Composable
fun App() {
    QuranifyTheme {
        Navigator(MainScreen) { navigator ->
            SlideTransition(navigator)
        }
    }
}
