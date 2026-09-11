package dev.bennyb.daysay.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tokens for the Daylight display. Everything is ink on paper; hue never carries meaning.
 * Serif for the one big word on each screen, sans for everything spoken to the user,
 * monospace for what the user spoke: the transcript reads as typed output.
 */
object Paper {
    val paper = Color(0xFFF3EEE4)
    val vellum = Color(0xFFEBE6DC)
    val mist = Color(0xFFDDD8CE)
    val ash = Color(0xFF8C8C8C)
    val graphite = Color(0xFF4A4A4A)
    val ink = Color(0xFF141414)
}

val DisplayStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 40.sp,
    lineHeight = 46.sp,
    letterSpacing = (-0.5).sp,
)

val EyebrowStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.6.sp,
)

val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 24.sp,
)

private val PaperTypography = Typography(
    headlineLarge = DisplayStyle,
    headlineMedium = DisplayStyle.copy(fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = EyebrowStyle,
)

@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Paper.ink,
            onPrimary = Paper.paper,
            secondary = Paper.graphite,
            onSecondary = Paper.paper,
            background = Paper.paper,
            onBackground = Paper.ink,
            surface = Paper.paper,
            onSurface = Paper.ink,
            surfaceVariant = Paper.vellum,
            onSurfaceVariant = Paper.graphite,
            outline = Paper.ink,
            outlineVariant = Paper.mist,
            error = Paper.ink,
            onError = Paper.paper,
            primaryContainer = Paper.vellum,
            onPrimaryContainer = Paper.ink,
            secondaryContainer = Paper.mist,
            onSecondaryContainer = Paper.ink,
            surfaceContainer = Paper.vellum,
            surfaceContainerHigh = Paper.vellum,
            surfaceContainerHighest = Paper.mist,
        ),
        typography = PaperTypography,
        content = content,
    )
}
