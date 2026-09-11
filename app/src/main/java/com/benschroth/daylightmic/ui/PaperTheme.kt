package com.benschroth.daylightmic.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Paper = Color(0xFFF3EEE4)
private val Ink = Color(0xFF111111)
private val Ash = Color(0xFF6B6B6B)
private val Mist = Color(0xFFE2DDD3)

/** Grayscale theme for the Daylight display. Hue is never used to carry meaning. */
@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = Paper,
            secondary = Ash,
            onSecondary = Paper,
            background = Paper,
            onBackground = Ink,
            surface = Paper,
            onSurface = Ink,
            surfaceVariant = Mist,
            onSurfaceVariant = Ink,
            outline = Ink,
            error = Ink,
            onError = Paper,
            primaryContainer = Mist,
            onPrimaryContainer = Ink,
            secondaryContainer = Mist,
            onSecondaryContainer = Ink,
        ),
        content = content,
    )
}
