package com.flue.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WatchColorScheme = darkColorScheme(
    primary = WatchColors.ActiveCyan,
    secondary = WatchColors.ActiveGreen,
    tertiary = WatchColors.ActiveBlue,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun WatchLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WatchColorScheme,
        content = content
    )
}
