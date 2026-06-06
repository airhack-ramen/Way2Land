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
    background = DarkIasiBackground,
    surface = DarkIasiSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E3A5F),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    secondaryContainer = IasiOrange.copy(alpha = 0.2f),
    onSecondaryContainer = IasiOrange,
    primaryContainer = IasiNavy.copy(alpha = 0.3f),
    onPrimaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IasiNavy,
    onPrimary = Color.White,
    secondary = IasiOrange,
    onSecondary = Color.White,
    background = IasiBackground,
    surface = Color.White,
    onSurface = IasiDarkGrey,
    surfaceVariant = Color(0xFFE8EDF1),
    onSurfaceVariant = IasiDarkGrey.copy(alpha = 0.7f),
    secondaryContainer = IasiOrange.copy(alpha = 0.1f),
    onSecondaryContainer = IasiOrange,
    primaryContainer = IasiNavy.copy(alpha = 0.1f),
    onPrimaryContainer = IasiNavy
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
