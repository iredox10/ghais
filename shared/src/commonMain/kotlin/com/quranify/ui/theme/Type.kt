package com.quranify.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object QuranifyTypography {
    val displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = QuranifyColors.TextPrimary
    )
    val titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = QuranifyColors.TextPrimary
    )
    val titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = QuranifyColors.TextPrimary
    )
    val titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = QuranifyColors.TextPrimary
    )
    val bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = QuranifyColors.TextPrimary
    )
    val bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = QuranifyColors.TextSecondary
    )
    val bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = QuranifyColors.TextSecondary
    )
    val labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = QuranifyColors.TextPrimary
    )
    val labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = QuranifyColors.TextSecondary
    )
    
    val arabicTitle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        color = QuranifyColors.TextPrimary
    )
    val arabicBody = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        color = QuranifyColors.TextPrimary
    )
    val arabicSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        color = QuranifyColors.TextPrimary
    )

    val materialTypography = Typography(
        displayLarge = displayLarge,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelSmall = labelSmall
    )
}
