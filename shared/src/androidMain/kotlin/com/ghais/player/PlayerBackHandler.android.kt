package com.ghais.player

import androidx.compose.runtime.Composable

@Composable
actual fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
