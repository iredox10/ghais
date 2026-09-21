package com.ghais.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * GHAIS NOIR — Full-monochrome "Carbon Glass" token system.
 *
 * Reference-faithful dark glassmorphism language (white chrome only, zero emerald):
 * - True-black canvas with a top glow zone, never flat grey.
 * - Surfaces are alpha-white ramps (elevated = fill gradient, never solid).
 * - Depth comes from layered hairlines: ghost border + brighter top specular.
 * - Inputs are engraved (dark inset); buttons are extruded chrome.
 *
 * Phase 1 foundation only. Legacy emerald GhaisColors remain untouched so
 * existing screens keep compiling; Phases 2-4 migrate screens onto these tokens.
 */
object GhaisNoir {
    // ------------------------------------------------------------------
    // Canvas
    // ------------------------------------------------------------------
    val NoirBlack = Color(0xFF050506)              // Absolute canvas black
    val CanvasTop = Color(0xFF0C0C0E)              // Top glow-zone lift
    val Canvas = NoirBlack

    // ------------------------------------------------------------------
    // Surface ramp — alpha whites, never flat greys
    // ------------------------------------------------------------------
    val Fill1 = Color(0x0AFFFFFF)                  // 4%  — ghost wash
    val Fill2 = Color(0x0FFFFFFF)                  // 6%  — resting card base
    val Fill3 = Color(0x14FFFFFF)                  // 8%  — card gradient top
    val Fill4 = Color(0x1FFFFFFF)                  // 12% — active / selected fill
    val FillDeep = Color(0x08FFFFFF)               // 3%  — card gradient bottom

    // ------------------------------------------------------------------
    // Hairlines & specular highlight
    // ------------------------------------------------------------------
    val BorderGhost = Color(0x0FFFFFFF)             // 6%  — faint separators
    val BorderCard = Color(0x1AFFFFFF)              // 10% — card outline
    val SpecularTop = Color(0x38FFFFFF)             // 22% — bright TOP-ONLY hairline
    val SheenDiagonal = Color.White.copy(alpha = 0.06f)

    // ------------------------------------------------------------------
    // Engraved (inset) surfaces — search bars, tracks, recessed wells
    // ------------------------------------------------------------------
    val InsetFill = Color(0x73000000)               // black @ 45%
    val InsetBorder = Color(0x0DFFFFFF)             // ~5% rim

    // ------------------------------------------------------------------
    // Chrome — primary CTA gradient pills, FAB, segmented fills
    // ------------------------------------------------------------------
    val ChromeTop = Color(0xFFF5F5F7)
    val ChromeBottom = Color(0xFFC7C7CC)
    val OnChrome = Color(0xFF0B0C0E)                // near-black label on chrome
    val ChromeInnerHighlight = Color.White.copy(alpha = 0.55f)

    // ------------------------------------------------------------------
    // Monochrome text ramp (contrast ladder replaces accent color)
    // ------------------------------------------------------------------
    val TextPrimary = Color(0xFFFFFFFF)             // 100%
    val TextSecondary = Color(0x9EFFFFFF)           // 62%
    val TextTertiary = Color(0x61FFFFFF)            // 38%
    val TextDisabled = Color(0x3DFFFFFF)            // 24%

    // ------------------------------------------------------------------
    // Ambient atmosphere
    // ------------------------------------------------------------------
    val AmbientGlow = Color.White.copy(alpha = 0.06f)   // top-center radial light
    val GrainAlpha = 0.03f                              // film-grain overlay opacity
    val Scrim = Color(0x99000000)                        // sheet / dialog scrim

    // ------------------------------------------------------------------
    // Brushes — the signature glass recipes
    // ------------------------------------------------------------------

    /** Elevated card: bright top -> deep bottom. Pair with BorderCard + top hairline. */
    fun cardFill(): Brush = Brush.verticalGradient(listOf(Fill3, FillDeep))

    /** Resting card variant for dense lists. */
    fun cardFillSoft(): Brush = Brush.verticalGradient(listOf(Fill2, FillDeep))

    /** Active / selected card: stronger wash. */
    fun cardFillActive(): Brush = Brush.verticalGradient(
        listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF))
    )

    /** Chrome CTA: light top -> silver bottom. */
    fun chromeFill(): Brush = Brush.verticalGradient(listOf(ChromeTop, ChromeBottom))

    /** Clay icon-well: top-lit dome -> shaded base (claymorphic emboss). */
    fun wellFill(): Brush = Brush.verticalGradient(
        listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF))
    )

    /** Diagonal glass sheen swept across hero cards / dock. */
    fun sheen(): Brush = Brush.linearGradient(
        listOf(
            Color.Transparent,
            SheenDiagonal,
            Color.Transparent
        )
    )

    /** Canvas: glow zone melting into absolute black. */
    fun canvasFill(): Brush = Brush.verticalGradient(listOf(CanvasTop, NoirBlack))

    /** Engraved track (progress, search). Darker at top = carved in. */
    fun insetFill(): Brush = Brush.verticalGradient(
        listOf(Color(0x80000000), Color(0x59000000))
    )
}
