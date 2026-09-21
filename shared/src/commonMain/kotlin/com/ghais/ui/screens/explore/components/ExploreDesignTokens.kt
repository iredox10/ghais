package com.ghais.ui.screens.explore.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * iOS HIG Dark Minimalist and Glassmorphic Design Tokens for Explore screens and components.
 */
val PureBlack = Color(0xFF000000)
val DarkObsidian = Color(0xFF0D0F12)
val OffWhiteText = Color(0xFFF5F5F7)
val SystemGrey = Color(0xFF8E8E93)
val SystemGrey2 = Color(0xFF636366)
val SystemGrey3 = Color(0xFF48484A)

val HairlineSpecularBorder = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.06f)
    )
)

val HairlineSubtleBorder = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.04f)
    )
)
