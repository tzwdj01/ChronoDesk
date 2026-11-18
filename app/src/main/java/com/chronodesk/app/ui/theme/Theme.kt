package com.chronodesk.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TechCyan,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = TechCyan,
    secondary = TechEmerald,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

@Composable
fun ChronoDeskTheme(
    isNightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isNightMode) {
        DarkColorScheme.copy(
            primary = NightAccent,
            background = NightBackground,
            surface = NightBackground,
            onBackground = NightTextPrimary,
            onSurface = NightTextPrimary
        )
    } else {
        DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PureBlack.toArgb()
            window.navigationBarColor = PureBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
