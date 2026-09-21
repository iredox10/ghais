package com.ghais.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Quranify Design System Color Tokens.
 *
 * Fully audited and aligned with the Stitch Design Specifications:
 * - Liquid Emerald Frosted Audio
 * - Obsidian & Emerald Spiritual Elegance
 *
 * Key Design Tokens:
 * - Background: Obsidian `#111415`, Dark Surface `#1E2325`, Elevated Surface `#262C2E`
 * - Primary Accent: Vivid Liquid Emerald `#4EDEA3`
 * - Secondary Accent / Highlights: Mint Glow `#6EE7B7`, Amber `#F2B880`
 * - Text Hierarchy: Pure White `#FFFFFF`, High-contrast `#E1E3E4`, Muted Subtitle `#8E989C`
 * - Borders: Vivid Liquid Emerald 12% alpha (`#4EDEA3` @ 0.12f) / Pure White 8% alpha (`#FFFFFF` @ 0.08f)
 */
object QuranifyColors {
    // -------------------------------------------------------------------------
    // Trending Purple & Obsidian Monochrome Design Tokens
    // -------------------------------------------------------------------------
    val PitchBlack = Color(0xFF0B0C0E)            // Absolute deep obsidian black
    val PureBlack = Color(0xFF000000)             // Pure AMOLED black
    val ObsidianCard = Color(0xFF121418)          // Obsidian card background

    val TrendingPurple = Color(0xFFA855F7)        // Trending Purple accent
    val ElectricViolet = Color(0xFF8B5CF6)        // Electric Violet gradient start
    val NeonLilac = Color(0xFFC084FC)             // Neon Lilac gradient highlight
    val RoyalViolet = Color(0xFF6D28D9)           // Deep rich violet
    val PurpleGlow = Color(0x33A855F7)            // 20% alpha purple aura
    val PurpleGlass = Color(0x26A855F7)           // 15% alpha purple frosted glass
    val PurpleBorder = Color(0x4DA855F7)          // 30% alpha purple border highlight

    // -------------------------------------------------------------------------
    // Canvas & Core Surfaces (Stitch Specification)
    // -------------------------------------------------------------------------
    val Obsidian = Color(0xFF111415)              // Base canvas / obsidian slate background
    val DarkSurface = Color(0xFF1E2325)           // Dark surface container / cards
    val ElevatedSurface = Color(0xFF262C2E)       // Elevated glass / sheet surface

    val Background = Obsidian                     // #111415
    val Surface = DarkSurface                     // #1E2325
    val SurfaceElevated = ElevatedSurface         // #262C2E
    val SurfaceLow = Color(0xFF191C1D)            // Low-elevation surface
    val SurfaceContainer = DarkSurface            // #1E2325
    val SurfaceHigh = ElevatedSurface             // #262C2E
    val SurfaceHighest = Color(0xFF323536)        // Tonal highlight surface
    val SurfaceBright = Color(0xFF373A3B)
    val SurfaceDim = Obsidian                     // #111415
    val SurfaceTint = Color(0xFF4EDEA3)

    // -------------------------------------------------------------------------
    // Primary Accent: Vivid Liquid Emerald (#4EDEA3)
    // -------------------------------------------------------------------------
    val VividLiquidEmerald = Color(0xFF4EDEA3)
    val Primary = VividLiquidEmerald              // #4EDEA3
    val PrimaryContainer = Color(0xFF10B981)      // Deep emerald container
    val OnPrimary = Color(0xFF003824)
    val OnPrimaryContainer = Color(0xFF00422B)
    val InversePrimary = Color(0xFF006C49)
    val PrimaryFixed = Color(0xFF6FFBBE)
    val PrimaryFixedDim = Color(0xFF4EDEA3)

    // -------------------------------------------------------------------------
    // Secondary Accent & Highlights: Mint Glow (#6EE7B7), Amber (#F2B880)
    // -------------------------------------------------------------------------
    val MintGlow = Color(0xFF6EE7B7)              // Luminous mint secondary highlight
    val Amber = Color(0xFFF2B880)                 // Sacred champagne amber gold

    // Secondary maps to Amber for sacred bookmarks, badges, tajweed indicators
    val Secondary = Amber                         // #F2B880
    val SecondaryContainer = Color(0xFFEE9800)
    val OnSecondary = Color(0xFF472A00)
    val OnSecondaryContainer = Color(0xFF5B3800)
    val SecondaryFixed = Color(0xFFFFDDB8)
    val SecondaryFixedDim = Color(0xFFFFB95F)

    // Tertiary maps to Mint Glow for secondary bioluminescent illumination
    val Tertiary = MintGlow                       // #6EE7B7
    val TertiaryContainer = Color(0xFF3EB686)
    val OnTertiary = Color(0xFF003825)
    val OnTertiaryContainer = Color(0xFF00422C)
    val TertiaryFixed = Color(0xFF85F8C4)
    val TertiaryFixedDim = Color(0xFF68DBA9)

    val HighlightMint = MintGlow                  // #6EE7B7
    val HighlightAmber = Amber                    // #F2B880

    // -------------------------------------------------------------------------
    // Text Hierarchy (Stitch Specification)
    // -------------------------------------------------------------------------
    val PureWhite = Color(0xFFFFFFFF)             // Pure White
    val HighContrast = Color(0xFFE1E3E4)          // High-contrast primary reading text
    val MutedSubtitle = Color(0xFF8E989C)         // Muted subtitle & metadata text

    val TextPureWhite = PureWhite                 // #FFFFFF
    val TextHighContrast = HighContrast           // #E1E3E4
    val TextMutedSubtitle = MutedSubtitle         // #8E989C

    val TextPrimary = HighContrast                // #E1E3E4
    val TextSecondary = MutedSubtitle             // #8E989C
    val TextTertiary = Color(0xFF86948A)          // Low-emphasis / disabled
    val TextWhite = PureWhite                     // #FFFFFF
    val TextMuted = MutedSubtitle                 // #8E989C
    val TextSecondaryVariant = Color(0xFFBBCABF)  // Retained for backward compatibility

    // -------------------------------------------------------------------------
    // Borders & Outlines (Stitch Specification)
    // -------------------------------------------------------------------------
    val BorderEmerald = Color(0xFF4EDEA3).copy(alpha = 0.12f) // #4EDEA3 @ 0.12f alpha
    val BorderWhite = Color(0xFFFFFFFF).copy(alpha = 0.08f)   // White @ 0.08f alpha

    val Border = BorderWhite                      // Default ghost micro-stroke
    val BorderDefault = BorderWhite
    val BorderActive = BorderEmerald
    val BorderHighlight = BorderEmerald

    val Outline = Color(0xFF86948A)               // Standard structural outline
    val OutlineVariant = Color(0xFF3C4A42)        // Subtle container outline

    // -------------------------------------------------------------------------
    // Glassmorphic Surface Overlays
    // -------------------------------------------------------------------------
    val GlassBase = Color(0x8C121A16)             // rgba(18, 26, 22, 0.55) - Base Card Glass
    val GlassElevated = Color(0xA61C2822)         // rgba(28, 40, 34, 0.65) - Elevated Glass
    val GlassHighlight = Color(0x1F34D399)        // rgba(52, 211, 153, 0.12) - Active/Highlight Surface
    val GlassFloating = Color(0xBF1E2C24)         // rgba(30, 44, 36, 0.75) - Floating Island Glass
    val GlassMiniPlayer = Color(0xD9111618)       // rgba(17, 22, 24, 0.85) - Mini Player Frost
    val GlassBorder = BorderWhite
    val GlassBorderActive = BorderEmerald

    // -------------------------------------------------------------------------
    // Semantic Containers (Card, Sheets)
    // -------------------------------------------------------------------------
    val Card = DarkSurface                        // #1E2325
    val CardElevated = ElevatedSurface            // #262C2E

    // -------------------------------------------------------------------------
    // Functional Feedback & Utility Colors
    // -------------------------------------------------------------------------
    val Error = Color(0xFFFFB4AB)                 // Stitch M3 error
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
    val Success = Color(0xFF4CAF50)

    val Divider = Color(0xFFFFFFFF).copy(alpha = 0.08f) // Hairline rule matching 0.08f white
    val Overlay = Color(0x80000000)

    val InverseSurface = HighContrast             // #E1E3E4
    val InverseOnSurface = Color(0xFF2E3132)

    // -------------------------------------------------------------------------
    // Audio Player & Mood Gradients
    // -------------------------------------------------------------------------
    val PlayerGradientStart = DarkSurface         // #1E2325
    val PlayerGradientEnd = Obsidian              // #111415

    val MoodStudy = Color(0xFF3D6B8E)             // Blue for study
    val MoodWork = Color(0xFF4A8C6F)              // Green for work
    val MoodSleep = Color(0xFF6B5B8D)             // Purple for sleep
}
