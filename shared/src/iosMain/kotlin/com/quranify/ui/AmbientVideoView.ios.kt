package com.quranify.ui.screens.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quranify.player.AmbientType

@Composable
actual fun AmbientVideoView(selectedType: AmbientType?, modifier: Modifier = Modifier) {
    Box(modifier)
}
