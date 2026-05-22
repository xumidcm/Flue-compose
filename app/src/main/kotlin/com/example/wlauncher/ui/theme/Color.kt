package com.flue.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// watchOS 风格配色
object WatchColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // 控制中心按钮色
    val ActiveGreen = Color(0xFF30D158)    // 蜂窝网络、蓝牙
    val ActiveBlue = Color(0xFF0A84FF)     // WiFi
    val ActiveRed = Color(0xFFFF453A)      // 静音
    val ActiveCyan = Color(0xFF64D2FF)     // 强调色
    val ActiveOrange = Color(0xFFFF9F0A)   // 电量低

    // 非激活状态
    val InactiveGray = Color(0x26FFFFFF)   // 15% white
    val SurfaceGlass = Color(0x1FFFFFFF)   // 12% white, 毛玻璃卡片
    val TextSecondary = Color(0xFFAAAAAA)
    val TextTertiary = Color(0xFF888888)

    // 应用图标背景色
    val AppGreen = Color(0xFF34C759)
    val AppBlue = Color(0xFF007AFF)
    val AppRed = Color(0xFFFF3B30)
    val AppOrange = Color(0xFFFF9500)
    val AppPurple = Color(0xFFAF52DE)
    val AppYellow = Color(0xFFFFCC00)
    val AppGray = Color(0xFF8E8E93)
}
