package com.quranify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalQuranifyColors = compositionLocalOf { QuranifyColors }

private val DarkColorScheme = darkColorScheme(
    primary = QuranifyColors.Primary,
    onPrimary = QuranifyColors.Background,
    primaryContainer = QuranifyColors.PrimaryVariant,
    onPrimaryContainer = QuranifyColors.TextPrimary,
    secondary = QuranifyColors.Secondary,
    onSecondary = QuranifyColors.TextPrimary,
    secondaryContainer = QuranifyColors.SecondaryVariant,
    onSecondaryContainer = QuranifyColors.TextPrimary,
    background = QuranifyColors.Background,
    onBackground = QuranifyColors.TextPrimary,
    surface = QuranifyColors.Surface,
    onSurface = QuranifyColors.TextPrimary,
    surfaceVariant = QuranifyColors.Card,
    onSurfaceVariant = QuranifyColors.TextSecondary,
    error = QuranifyColors.Error,
    onError = QuranifyColors.TextPrimary,
)

@Composable
fun QuranifyTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalQuranifyColors provides QuranifyColors,
        LocalSpacing provides QuranifySpacing
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            shapes = QuranifyShapes.materialShapes,
            typography = QuranifyTypography.materialTypography,
            content = content
        )
    }
}
