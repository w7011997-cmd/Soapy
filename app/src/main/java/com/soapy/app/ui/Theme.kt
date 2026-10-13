package com.soapy.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark purple palette, matching the other apps. */
object SoapyColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF110F1D)
    val SurfaceHigh = Color(0xFF1E1C2A)
    val Card = Color(0xFF231B44)
    val Primary = Color(0xFFA66CFF)
    val PrimaryDeep = Color(0xFF8D36FF)
    val PrimaryContainer = Color(0xFF3B1D67)
    val Lavender = Color(0xFFC7B2FF)
    val TextPrimary = Color(0xFFEDEAF7)
    val TextMuted = Color(0xFF8F8E9E)
    val Outline = Color(0xFF3A3552)
    val Success = Color(0xFF5DD6A2)
    val Warning = Color(0xFFFFB84D)
    val Danger = Color(0xFFFF6B7A)
}

private val SoapyScheme = darkColorScheme(
    primary = SoapyColors.Primary,
    onPrimary = Color(0xFF16002F),
    primaryContainer = SoapyColors.PrimaryContainer,
    onPrimaryContainer = SoapyColors.Lavender,
    secondary = SoapyColors.PrimaryDeep,
    onSecondary = Color.White,
    background = SoapyColors.Background,
    onBackground = SoapyColors.TextPrimary,
    surface = SoapyColors.Surface,
    onSurface = SoapyColors.TextPrimary,
    surfaceVariant = SoapyColors.SurfaceHigh,
    onSurfaceVariant = SoapyColors.TextMuted,
    outline = SoapyColors.Outline,
    error = SoapyColors.Danger
)

@Composable
fun SoapyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SoapyScheme, content = content)
}
