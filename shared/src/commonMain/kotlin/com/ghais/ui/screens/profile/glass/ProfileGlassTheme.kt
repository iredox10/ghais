package com.ghais.ui.screens.profile.glass

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ProfileGlassTheme {
    val PageBg: Color = Color(0xFF000000)
    val GlassFill: Color = Color(0x0DFFFFFF)
    val GlassFillStrong: Color = Color(0x14FFFFFF)
    val GlassBorder: Color = Color(0x1AFFFFFF)
    val ChromeTop: Color = Color(0xFFF5F5F7)
    val ChromeBottom: Color = Color(0xFFC7C7CC)
    val AuraEmerald: Color = Color(0xFF4EDEA3)
    val AccentBlue: Color = Color(0xFF4C8DFF)
    val HeartRed: Color = Color(0xFFFF5A6E)
    val SuccessGreen: Color = Color(0xFF30D158)
    val TextPrimary: Color = Color.White
    val TextMuted: Color = Color(0xFF9A9AA0)
    val CardRadius = 24.dp
    val PillRadius = 50.dp

    fun cardGradient(): Brush = Brush.verticalGradient(
        listOf(GlassFillStrong, GlassFill)
    )

    fun chromeGradient(): Brush = Brush.verticalGradient(
        listOf(ChromeTop, ChromeBottom)
    )
}
