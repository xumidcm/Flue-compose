package com.flue.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// UI 风格
enum class UiStyle {
    APPLE_WATCH,
    MATERIAL_3
}

// 深色 Material3 颜色方案
private val DarkColorScheme = darkColorScheme(
    primary = WatchColorsDark.ActiveCyan,
    secondary = WatchColorsDark.ActiveGreen,
    tertiary = WatchColorsDark.ActiveBlue,
    background = WatchColorsDark.Black,
    surface = WatchColorsDark.Black,
    onPrimary = WatchColorsDark.White,
    onSecondary = WatchColorsDark.White,
    onTertiary = WatchColorsDark.White,
    onBackground = WatchColorsDark.White,
    onSurface = WatchColorsDark.White,
)

// 浅色 Material3 颜色方案
private val LightColorScheme = lightColorScheme(
    primary = WatchColorsLight.ActiveCyan,
    secondary = WatchColorsLight.ActiveGreen,
    tertiary = WatchColorsLight.ActiveBlue,
    background = WatchColorsDark.White,
    surface = WatchColorsDark.White,
    onPrimary = WatchColorsDark.Black,
    onSecondary = WatchColorsDark.Black,
    onTertiary = WatchColorsDark.Black,
    onBackground = WatchColorsDark.Black,
    onSurface = WatchColorsDark.Black,
)

// 主题模式
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

val LocalGlobalUiScale = compositionLocalOf { 1f }

@Composable
fun WatchLauncherTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    uiStyle: UiStyle = UiStyle.APPLE_WATCH,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    WatchColors.setDarkMode(isDark)

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme) {
        ProvideLauncherStyle(
            uiStyle = uiStyle,
            darkMode = isDark,
            content = content
        )
    }
}

@Composable
fun ProvideGlobalUiScale(
    scalePercent: Int,
    content: @Composable () -> Unit
) {
    val baseDensity = LocalDensity.current
    val scale = scalePercent.coerceIn(50, 150) / 100f
    val scaledDensity = remember(baseDensity, scale) {
        Density(
            density = baseDensity.density * scale,
            fontScale = baseDensity.fontScale
        )
    }
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalGlobalUiScale provides scale,
        content = content
    )
}
