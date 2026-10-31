package com.ste03.smartscan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkText = Color(0xFF1B1C1E)

@Composable
fun SmartScanTheme(
    accent: Color,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val onPrimary = if (accent.luminance() > 0.5f) DarkText else Color.White

    val light = lightColorScheme(
        primary = accent,
        onPrimary = onPrimary,
        primaryContainer = accent.lighten(0.85f),
        onPrimaryContainer = accent.darken(0.5f),
        secondary = accent,
        onSecondary = onPrimary,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightBackground,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline
    )

    val dark = darkColorScheme(
        primary = accent,
        onPrimary = onPrimary,
        primaryContainer = accent.darken(0.72f),
        onPrimaryContainer = accent.lighten(0.78f),
        secondary = accent,
        onSecondary = onPrimary,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurface,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline
    )

    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        content = content
    )
}
