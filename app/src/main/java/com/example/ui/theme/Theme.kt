package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    secondary = CosmicSecondary,
    tertiary = CosmicTertiary,
    background = CosmicBgDark,
    surface = CosmicSurfaceDark,
    surfaceVariant = CosmicSurfaceVariantDark,
    onPrimary = CosmicBgDark,
    onSecondary = CosmicBgDark,
    onTertiary = CosmicBgDark,
    onBackground = CosmicTextPrimary,
    onSurface = CosmicTextPrimary,
    onSurfaceVariant = CosmicTextSecondary,
    outline = CosmicOutline,
    outlineVariant = CosmicOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CrispPrimary,
    secondary = CrispSecondary,
    tertiary = CrispTertiary,
    background = CrispBgLight,
    surface = CrispSurfaceLight,
    surfaceVariant = CrispSurfaceVariantLight,
    onPrimary = CrispSurfaceLight,
    onSecondary = CrispSurfaceLight,
    onTertiary = CrispSurfaceLight,
    onBackground = CrispTextPrimary,
    onSurface = CrispTextPrimary,
    onSurfaceVariant = CrispTextSecondary,
    outline = CrispOutline,
    outlineVariant = CrispOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
