package org.eu.nl.syu.way2fly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = IasiNavy,
    onPrimary = Color.White,
    secondary = IasiOrange,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IasiNavy,
    onPrimary = Color.White,
    secondary = IasiOrange,
    onSecondary = Color.White,
    background = IasiBackground,
    surface = Color.White,
    onSurface = IasiDarkGrey,
    secondaryContainer = IasiOrange.copy(alpha = 0.1f),
    onSecondaryContainer = IasiOrange
)

@Composable
fun Way2FlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic colors to keep Iasi branding consistent
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
