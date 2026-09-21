package com.ghais.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalGhaisColors = compositionLocalOf { GhaisColors }

private val DarkColorScheme = darkColorScheme(
    primary = GhaisColors.Primary,
    onPrimary = GhaisColors.OnPrimary,
    primaryContainer = GhaisColors.PrimaryContainer,
    onPrimaryContainer = GhaisColors.OnPrimaryContainer,
    inversePrimary = GhaisColors.InversePrimary,
    secondary = GhaisColors.Secondary,
    onSecondary = GhaisColors.OnSecondary,
    secondaryContainer = GhaisColors.SecondaryContainer,
    onSecondaryContainer = GhaisColors.OnSecondaryContainer,
    tertiary = GhaisColors.Tertiary,
    onTertiary = GhaisColors.OnTertiary,
    tertiaryContainer = GhaisColors.TertiaryContainer,
    onTertiaryContainer = GhaisColors.OnTertiaryContainer,
    background = GhaisColors.Background,
    onBackground = GhaisColors.TextPrimary,
    surface = GhaisColors.DarkSurface,
    onSurface = GhaisColors.TextPrimary,
    surfaceVariant = GhaisColors.ElevatedSurface,
    onSurfaceVariant = GhaisColors.TextSecondary,
    surfaceTint = GhaisColors.SurfaceTint,
    inverseSurface = GhaisColors.InverseSurface,
    inverseOnSurface = GhaisColors.InverseOnSurface,
    outline = GhaisColors.Outline,
    outlineVariant = GhaisColors.OutlineVariant,
    error = GhaisColors.Error,
    onError = GhaisColors.OnError,
    errorContainer = GhaisColors.ErrorContainer,
    onErrorContainer = GhaisColors.OnErrorContainer,
)

@Composable
fun GhaisTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGhaisColors provides GhaisColors,
        LocalSpacing provides GhaisSpacing
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            shapes = GhaisShapes.materialShapes,
            typography = GhaisTypography.materialTypography,
            content = content
        )
    }
}
