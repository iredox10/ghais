package com.ghais.ui.screens.profile.glass

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ProfileGlassTheme {
    val PageBg: Color = Color(0xFF000000)
    val CardFillWhite: Color = Color(0x14FFFFFF)
    val CardFillDeep: Color = Color(0x08FFFFFF)
    val CardBorder: Color = Color(0x1AFFFFFF)
    val Hairline: Color = Color(0x0FFFFFFF)
    val TextPrimary: Color = Color.White
    val TextMuted: Color = Color(0xFF9A9AA0)
    val ChromeTop: Color = Color(0xFFF5F5F7)
    val ChromeBottom: Color = Color(0xFFC7C7CC)
    val InkDark: Color = Color(0xFF0B0C0E)
    val Accent: Color = Color(0xFF4EDEA3)
    val Danger: Color = Color(0xFFFF5A6E)

    fun cardGradient(): Brush = Brush.verticalGradient(
        listOf(CardFillWhite, CardFillDeep)
    )

    fun chromeGradient(): Brush = Brush.verticalGradient(
        listOf(ChromeTop, ChromeBottom)
    )
}
