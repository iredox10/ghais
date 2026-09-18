package com.quranify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalQuranifyColors = compositionLocalOf { QuranifyColors }

private val DarkColorScheme = darkColorScheme(
    primary = QuranifyColors.Primary,
    onPrimary = QuranifyColors.OnPrimary,
    primaryContainer = QuranifyColors.PrimaryContainer,
    onPrimaryContainer = QuranifyColors.OnPrimaryContainer,
    inversePrimary = QuranifyColors.InversePrimary,
    secondary = QuranifyColors.Secondary,
    onSecondary = QuranifyColors.OnSecondary,
    secondaryContainer = QuranifyColors.SecondaryContainer,
    onSecondaryContainer = QuranifyColors.OnSecondaryContainer,
    tertiary = QuranifyColors.Tertiary,
    onTertiary = QuranifyColors.OnTertiary,
    tertiaryContainer = QuranifyColors.TertiaryContainer,
    onTertiaryContainer = QuranifyColors.OnTertiaryContainer,
    background = QuranifyColors.Background,
    onBackground = QuranifyColors.TextPrimary,
    surface = QuranifyColors.DarkSurface,
    onSurface = QuranifyColors.TextPrimary,
    surfaceVariant = QuranifyColors.ElevatedSurface,
    onSurfaceVariant = QuranifyColors.TextSecondary,
    surfaceTint = QuranifyColors.SurfaceTint,
    inverseSurface = QuranifyColors.InverseSurface,
    inverseOnSurface = QuranifyColors.InverseOnSurface,
    outline = QuranifyColors.Outline,
    outlineVariant = QuranifyColors.OutlineVariant,
    error = QuranifyColors.Error,
    onError = QuranifyColors.OnError,
    errorContainer = QuranifyColors.ErrorContainer,
    onErrorContainer = QuranifyColors.OnErrorContainer,
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
