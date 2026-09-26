package com.ghais.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ghais.shared.generated.resources.Res
import ghais.shared.generated.resources.QCF_Hafs
import ghais.shared.generated.resources.NotoNaskhArabic_Regular
import org.jetbrains.compose.resources.Font

/**
 * Ghais Typography Tokens.
 *
 * Aligned with Stitch Specifications (Plus Jakarta Sans & Traditional Naskh Arabic):
 * - display-lg: 40sp / 48sp lineHeight, -0.03em letterSpacing
 * - display-lg-mobile: 32sp / 40sp lineHeight, -0.02em letterSpacing
 * - headline-lg: 28sp / 36sp lineHeight, -0.02em letterSpacing
 * - headline-md: 22sp / 28sp lineHeight, -0.015em letterSpacing
 * - headline-sm: 18sp / 24sp lineHeight, -0.01em letterSpacing
 * - body-lg: 16sp / 24sp lineHeight
 * - body-md: 14sp / 20sp lineHeight
 * - body-sm: 12sp / 16sp lineHeight
 * - label-lg: 14sp / 20sp lineHeight, 0.01em letterSpacing
 * - label-md: 12sp / 16sp lineHeight, 0.02em letterSpacing
 * - label-sm: 11sp / 14sp lineHeight, 0.03em letterSpacing
 * - Arabic Ayah & Surah text: QURAN_LINE_HEIGHT_RATIO line-height ratio
 *   to prevent tashkeel clipping
 */
object GhaisTypography {
    // -------------------------------------------------------------------------
    // Display styles (Plus Jakarta Sans)
    // -------------------------------------------------------------------------
    val displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.03).em,
        color = GhaisColors.TextPureWhite
    )
    val displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.025).em,
        color = GhaisColors.TextPureWhite
    )
    val displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em,
        color = GhaisColors.TextPureWhite
    )

    // -------------------------------------------------------------------------
    // Headline styles
    // -------------------------------------------------------------------------
    val headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
        color = GhaisColors.TextPrimary
    )
    val headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.015).em,
        color = GhaisColors.TextPrimary
    )
    val headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
        color = GhaisColors.TextPrimary
    )

    // -------------------------------------------------------------------------
    // Title styles
    // -------------------------------------------------------------------------
    val titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = GhaisColors.TextPrimary
    )
    val titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = GhaisColors.TextPrimary
    )
    val titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = GhaisColors.TextPrimary
    )

    // -------------------------------------------------------------------------
    // Body styles
    // -------------------------------------------------------------------------
    val bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.005).em,
        color = GhaisColors.TextPrimary
    )
    val bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = GhaisColors.TextSecondary
    )
    val bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = GhaisColors.TextSecondary
    )

    // -------------------------------------------------------------------------
    // Label styles
    // -------------------------------------------------------------------------
    val labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
        color = GhaisColors.TextPrimary
    )
    val labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02.em,
        color = GhaisColors.TextSecondary
    )
    val labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.03.em,
        color = GhaisColors.TextSecondary
    )

    // -------------------------------------------------------------------------
    // Noir editorial display (Phase 1) — light-weight sticker-typography
    // headlines with tight tracking ("Keep Your Life Safe" style).
    // Scripture (Arabic) styles below are deliberately untouched.
    // -------------------------------------------------------------------------
    val displayEditorial = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.03).em,
        color = GhaisNoir.TextPrimary
    )
    val displayEditorialSmall = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.025).em,
        color = GhaisNoir.TextPrimary
    )
    val displayEditorialBold = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.03).em,
        color = GhaisNoir.TextPrimary
    )
    // Inline icon-chip support: headline text + IconWell chips in a FlowRow.
    val editorialBody = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.005).em,
        color = GhaisNoir.TextSecondary
    )

    // -------------------------------------------------------------------------
    // Arabic Scripture typography (QURAN_LINE_HEIGHT_RATIO line-height ratio
    // for tashkeel diacritics)
    // QCF Hafs (KFGQPC HAFS Uthmanic Script v22, official bundle) — see
    // composeResources/font. Resource fonts load in composition, so the
    // family is a composable getter; pass it as Text's fontFamily alongside
    // the arabic* styles.
    // -------------------------------------------------------------------------

    /**
     * Single line-height ratio for every Quranic text surface.
     *
     * QCF Hafs' designed line box is 1.758 em (hhea ascender 2400 / descender
     * -1200 at upem 2048), so 2.0 clears the tashkeel with headroom and is the
     * ratio used app-wide. Hoisted here so the mushaf surfaces stop drifting
     * onto their own 2.1.
     */
    const val QURAN_LINE_HEIGHT_RATIO = 2.0f

    val quranFont: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.QCF_Hafs))

    /**
     * Numeral face for the ayah rosette drawn by
     * [com.ghais.ui.util.ayahMarkContent].
     *
     * The rosette cannot use [quranFont] for its digits: QCF Hafs' `rlig`
     * feature turns any run of U+0660..U+0669 into its ayah ornament, so the
     * digits would fuse with the ring we draw ourselves. Noto Naskh has no
     * such substitution and keeps U+0660..U+0669 as plain, legible digits.
     */
    val rosetteDigitFont: FontFamily
        @Composable
        get() = FontFamily(Font(Res.font.NotoNaskhArabic_Regular))

    /**
     * Quranic text style: QCF Hafs with tracking explicitly zeroed.
     *
     * `MaterialTheme` hands out a `LocalTextStyle` carrying bodyLarge's
     * `letterSpacing = (-0.005).em`, and any `Text` that does not override
     * tracking inherits it. Arabic is a cursive script: non-zero tracking
     * inserts gaps between joined letters, which breaks the cursive joins
     * (CSS Text L3 §7.2.1 requires a UA to drop spacing it cannot apply
     * safely; see also Google bug 442051322 for measurement loss/truncation
     * on Arabic with tracking). Pass this as `style =` on Quranic `Text` so
     * tracking is zero at the source. Inline `fontSize`/`lineHeight`/`color`/
     * `fontWeight` still win over it.
     */
    val quranScript: TextStyle
        @Composable
        get() = TextStyle(fontFamily = quranFont, letterSpacing = 0.sp)

    val arabicTitle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 56.sp,
        color = GhaisColors.TextPrimary
    )
    val arabicBody = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 43.sp,
        color = GhaisColors.TextPrimary
    )
    val arabicSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 32.sp,
        color = GhaisColors.TextPrimary
    )

    // -------------------------------------------------------------------------
    // Material 3 Typography Mapping
    // -------------------------------------------------------------------------
    val materialTypography = Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall
    )
}
