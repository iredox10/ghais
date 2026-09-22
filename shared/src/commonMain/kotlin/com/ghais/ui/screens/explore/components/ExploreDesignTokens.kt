package com.ghais.ui.screens.explore.components

import androidx.compose.ui.graphics.Brush
import com.ghais.ui.theme.GhaisNoir

/**
 * Noir Glass tokens for Explore headers and components — strict monochrome.
 *
 * Legacy iOS-grey names are preserved as type-stable aliases so existing
 * callers ([ExploreScreen][com.ghais.ui.screens.explore.ExploreScreen],
 * ExploreThemesSection) keep compiling untouched. Every value now resolves
 * to [GhaisNoir]: zero hue, alpha-white surfaces, monochrome text ladder,
 * ghost/specular hairlines.
 */
val PureBlack = GhaisNoir.NoirBlack
val DarkObsidian = GhaisNoir.CanvasTop
val OffWhiteText = GhaisNoir.TextPrimary
val SystemGrey = GhaisNoir.TextSecondary
val SystemGrey2 = GhaisNoir.TextTertiary
val SystemGrey3 = GhaisNoir.TextDisabled

/** Bright top-to-ghost vertical hairline for elevated glass edges. */
val HairlineSpecularBorder: Brush = Brush.verticalGradient(
    listOf(
        GhaisNoir.SpecularTop,
        GhaisNoir.BorderGhost
    )
)

/** Faint ghost hairline for resting / unselected edges. */
val HairlineSubtleBorder: Brush = Brush.verticalGradient(
    listOf(
        GhaisNoir.BorderCard,
        GhaisNoir.BorderGhost
    )
)
