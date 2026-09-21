package com.ghais.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

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
 * - Arabic Ayah & Surah text: 1.8x line-height ratio to prevent tashkeel clipping
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
    // Arabic Scripture typography (1.8x line-height ratio for tashkeel diacritics)
    // -------------------------------------------------------------------------
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
