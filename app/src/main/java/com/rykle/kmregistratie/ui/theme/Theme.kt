package com.rykle.kmregistratie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Altijd donker, net als de landingspagina van baronpower.nl.
private val BaronColorScheme = darkColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E4A2A),
    onPrimaryContainer = GreenPale,
    secondary = Blue2,
    onSecondary = Deep,
    secondaryContainer = Color(0xFF1C3358),
    onSecondaryContainer = TextLight,
    tertiary = Green2,
    onTertiary = Deep,
    background = Deep,
    onBackground = TextLight,
    surface = Deep,
    onSurface = TextLight,
    surfaceVariant = Navy,
    onSurfaceVariant = Mute,
    surfaceContainerLowest = Deep,
    surfaceContainerLow = Deep2,
    surfaceContainer = Deep2,
    surfaceContainerHigh = Color(0xFF15203B),
    surfaceContainerHighest = Color(0xFF182441),
    outline = Color(0xFF3A4766),
    outlineVariant = Color(0xFF222C44),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFF3B1219),
    onErrorContainer = Color(0xFFFFC9CB),
)

@Composable
fun KmRegistratieTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BaronColorScheme, content = content)
}
