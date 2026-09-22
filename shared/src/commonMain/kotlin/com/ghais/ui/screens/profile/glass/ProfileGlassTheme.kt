package com.ghais.ui.screens.profile.glass

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ghais.ui.theme.GhaisNoir

/**
 * Phase 3 — legacy profile glass token shim, now aliased to the Noir system.
 * Kept for source compatibility; new code should use [GhaisNoir] directly.
 * Note: monochrome — no emerald accent, no red danger.
 */
object ProfileGlassTheme {
    val PageBg: Color = GhaisNoir.NoirBlack
    val CardFillWhite: Color = GhaisNoir.Fill3
    val CardFillDeep: Color = GhaisNoir.FillDeep
    val CardBorder: Color = GhaisNoir.BorderCard
    val Hairline: Color = GhaisNoir.BorderGhost
    val TextPrimary: Color = GhaisNoir.TextPrimary
    val TextMuted: Color = GhaisNoir.TextSecondary
    val ChromeTop: Color = GhaisNoir.ChromeTop
    val ChromeBottom: Color = GhaisNoir.ChromeBottom
    val InkDark: Color = GhaisNoir.OnChrome
    val Accent: Color = GhaisNoir.TextPrimary
    val Danger: Color = GhaisNoir.TextTertiary

    fun cardGradient(): Brush = GhaisNoir.cardFill()

    fun chromeGradient(): Brush = GhaisNoir.chromeFill()
}

