package org.isayme.callstats.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9EFE1),
    onPrimaryContainer = Color(0xFF00251C),
    secondary = Color(0xFF8A5A1F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE0B8),
    onSecondaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF6F6F1),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFE4E8E2),
    onSurfaceVariant = Color(0xFF414944),
    outline = Color(0xFF737A75),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF68D8B6),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF0B4F3E),
    onPrimaryContainer = Color(0xFFC9EFE1),
    secondary = Color(0xFFE0B87A),
    onSecondary = Color(0xFF4A3000),
    secondaryContainer = Color(0xFF664400),
    onSecondaryContainer = Color(0xFFFFE0B8),
    background = Color(0xFF0D1210),
    onBackground = Color(0xFFE4EAE5),
    surface = Color(0xFF151C18),
    onSurface = Color(0xFFE4EAE5),
    surfaceVariant = Color(0xFF2A332E),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF8B948D),
    error = Color(0xFFFFB4AB),
)

@Composable
fun CallStatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
