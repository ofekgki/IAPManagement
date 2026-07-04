package com.example.iapmanagement.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * The demo intentionally does NOT use Android 12 dynamic color — it ships a fixed, professional theme
 * that matches the developer portal so the two products look like one platform.
 */

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EEF5),
    onPrimaryContainer = BrandPrimaryDark,
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = AccentTealDark,
    tertiary = SlateSecondary,
    onTertiary = Color.White,
    background = Canvas,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight,
    outlineVariant = BorderLight,
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorContainer,
    onErrorContainer = Color(0xFF991B1B),
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color(0xFF0B1B2E),
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = Color(0xFFD8E4F0),
    secondary = AccentTealLight,
    onSecondary = Color(0xFF06302B),
    secondaryContainer = AccentTealDark,
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFCBD5E1),
    onTertiary = Color(0xFF0F172A),
    background = CanvasDark,
    onBackground = TextPrimaryDark,
    surface = CardSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = BorderDark,
    error = StatusErrorDark,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
)

@Composable
fun IAPManagementTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
