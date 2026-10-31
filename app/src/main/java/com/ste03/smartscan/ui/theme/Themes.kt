package com.ste03.smartscan.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** A named colour theme: one accent colour applied app-wide for a uniform look. */
data class AppTheme(val name: String, val color: Color)

object Themes {
    val all = listOf(
        AppTheme("Ability", Color(0xFF1E88E5)),
        AppTheme("Accomplishment", Color(0xFF43A047)),
        AppTheme("Acumen", Color(0xFF00897B)),
        AppTheme("Aptitude", Color(0xFF3949AB)),
        AppTheme("Brilliance", Color(0xFFF9A825)),
        AppTheme("Flair", Color(0xFFE91E63)),
        AppTheme("Imagination", Color(0xFF8E24AA)),
        AppTheme("Ingenuity", Color(0xFF00ACC1)),
        AppTheme("Inspiration", Color(0xFFFF7043)),
        AppTheme("Intelligence", Color(0xFF1565C0)),
        AppTheme("Inventiveness", Color(0xFF7CB342)),
        AppTheme("Originality", Color(0xFF5E35B1)),
        AppTheme("Prodigy", Color(0xFFE53935)),
        AppTheme("Prowess", Color(0xFFF4511E)),
        AppTheme("Talent", Color(0xFF2E7D32)),
        AppTheme("Wisdom", Color(0xFF8D6E63))
    )

    fun byName(name: String): AppTheme = all.firstOrNull { it.name == name } ?: all.first()

    fun colorFor(name: String): Color = byName(name).color
}

/** Blend this colour toward white (fraction 0..1). */
fun Color.lighten(fraction: Float): Color = lerp(this, Color.White, fraction)

/** Blend this colour toward black (fraction 0..1). */
fun Color.darken(fraction: Float): Color = lerp(this, Color.Black, fraction)

/** Relative luminance (0..1) for choosing a contrasting on-colour. */
fun Color.luminance(): Float {
    fun channel(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else {
            val v = (c + 0.055f) / 1.055f
            v * v
        }
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}
