package com.quranify.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quranify.player.AmbientType

/**
 * Fullscreen background video for the selected ambient sound.
 * Renders nothing when [selectedType] is null or has no videos (glow fallback stays).
 * The actual player handles transport + crossfade internally.
 */
@Composable
expect fun AmbientVideoView(selectedType: AmbientType?, modifier: Modifier = Modifier)
