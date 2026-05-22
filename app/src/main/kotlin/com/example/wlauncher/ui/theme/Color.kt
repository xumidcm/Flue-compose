package com.flue.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// watchOS 风格配色 - 动态主题
object WatchColors {
    var isDark = true
        private set

    // 功能色
    val ActiveGreen
        get() = if (isDark) WatchColorsDark.ActiveGreen else WatchColorsLight.ActiveGreen
    val ActiveBlue
        get() = if (isDark) WatchColorsDark.ActiveBlue else WatchColorsLight.ActiveBlue
    val ActiveRed
        get() = if (isDark) WatchColorsDark.ActiveRed else WatchColorsLight.ActiveRed
    val ActiveCyan
        get() = if (isDark) WatchColorsDark.ActiveCyan else WatchColorsLight.ActiveCyan
    val ActiveOrange
        get() = if (isDark) WatchColorsDark.ActiveOrange else WatchColorsLight.ActiveOrange

    // 中性色
    val InactiveGray
        get() = if (isDark) WatchColorsDark.InactiveGray else WatchColorsLight.InactiveGray
    val SurfaceGlass
        get() = if (isDark) WatchColorsDark.SurfaceGlass else WatchColorsLight.SurfaceGlass
    val TextSecondary
        get() = if (isDark) WatchColorsDark.TextSecondary else WatchColorsLight.TextSecondary
    val TextTertiary
        get() = if (isDark) WatchColorsDark.TextTertiary else WatchColorsLight.TextTertiary

    // 黑白
    val Black = WatchColorsDark.Black
    val White = WatchColorsDark.White

    // 背景与文本（用于主题切换时的主背景色）
    val Background
        get() = if (isDark) WatchColorsDark.Black else WatchColorsDark.White
    val OnBackground
        get() = if (isDark) WatchColorsDark.White else WatchColorsDark.Black

    // 应用图标背景常用色
    val AppGreen
        get() = if (isDark) WatchColorsDark.AppGreen else WatchColorsLight.AppGreen
    val AppBlue
        get() = if (isDark) WatchColorsDark.AppBlue else WatchColorsLight.AppBlue
    val AppRed
        get() = if (isDark) WatchColorsDark.AppRed else WatchColorsLight.AppRed
    val AppOrange
        get() = if (isDark) WatchColorsDark.AppOrange else WatchColorsLight.AppOrange
    val AppPurple
        get() = if (isDark) WatchColorsDark.AppPurple else WatchColorsLight.AppPurple
    val AppYellow
        get() = if (isDark) WatchColorsDark.AppYellow else WatchColorsLight.AppYellow
    val AppGray
        get() = if (isDark) WatchColorsDark.AppGray else WatchColorsLight.AppGray

    fun setDarkMode(isDark: Boolean) {
        this.isDark = isDark
    }
}

// 深色主题配色
object WatchColorsDark {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // 功能色
    val ActiveGreen = Color(0xFF30D158)
    val ActiveBlue = Color(0xFF0A84FF)
    val ActiveRed = Color(0xFFFF453A)
    val ActiveCyan = Color(0xFF64D2FF)
    val ActiveOrange = Color(0xFFFF9F0A)

    // 中性色
    val InactiveGray = Color(0x26FFFFFF)
    val SurfaceGlass = Color(0x1FFFFFFF)
    val TextSecondary = Color(0xFFAAAAAA)
    val TextTertiary = Color(0xFF888888)

    // 应用图标背景常用色
    val AppGreen = Color(0xFF34C759)
    val AppBlue = Color(0xFF007AFF)
    val AppRed = Color(0xFFFF3B30)
    val AppOrange = Color(0xFFFF9500)
    val AppPurple = Color(0xFFAF52DE)
    val AppYellow = Color(0xFFFFCC00)
    val AppGray = Color(0xFF8E8E93)
}

// 浅色主题配色
object WatchColorsLight {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // 功能色
    val ActiveGreen = Color(0xFF34C759)
    val ActiveBlue = Color(0xFF007AFF)
    val ActiveRed = Color(0xFFFF3B30)
    val ActiveCyan = Color(0xFF5AC8FA)
    val ActiveOrange = Color(0xFFFF9500)

    // 中性色
    val InactiveGray = Color(0x26000000)
    val SurfaceGlass = Color(0x1F000000)
    val TextSecondary = Color(0xFF666666)
    val TextTertiary = Color(0xFF888888)

    // 应用图标背景常用色
    val AppGreen = Color(0xFF34C759)
    val AppBlue = Color(0xFF007AFF)
    val AppRed = Color(0xFFFF3B30)
    val AppOrange = Color(0xFFFF9500)
    val AppPurple = Color(0xFFAF52DE)
    val AppYellow = Color(0xFFFFCC00)
    val AppGray = Color(0xFF8E8E93)
}
