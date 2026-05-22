package com.flue.launcher.watchface

enum class WatchClockPosition {
    CENTER,
    TOP_CENTER,
    BOTTOM_CENTER,
    LEFT_CENTER,
    RIGHT_CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

enum class WatchClockColorMode {
    AUTO,
    WHITE,
    BLACK
}

enum class WatchFaceClockStyle {
    DIGITAL,
    MD3_ANALOG,
    MD3E_CLOCK,
    APPLE_WATCH
}

enum class WatchFaceMd3eShape {
    COOKIE,
    SOFT_STAR,
    CIRCLE
}

data class BuiltInWatchFaceOptions(
    val clockPosition: WatchClockPosition = WatchClockPosition.CENTER,
    val clockSizeSp: Int = 64,
    val boldClock: Boolean = false,
    val cropToFill: Boolean = true,
    val clockColorMode: WatchClockColorMode = WatchClockColorMode.AUTO,
    val clockStyle: WatchFaceClockStyle = WatchFaceClockStyle.DIGITAL,
    val showSeconds: Boolean = false,
    val customText: String = "",
    val fontPath: String? = null,
    val md3eShape: WatchFaceMd3eShape = WatchFaceMd3eShape.COOKIE,
    val useThemeTextColor: Boolean = true,
    val textColorArgb: Int = 0xFFFFFFFF.toInt(),
    val md3eAutoColors: Boolean = true,
    val md3eTextColorArgb: Int = 0xFF202938.toInt(),
    val md3eFaceColorArgb: Int = 0xFFEAF1FF.toInt(),
    val md3eHourHandColorArgb: Int = 0xFF334155.toInt(),
    val md3eMinuteHandColorArgb: Int = 0xFF5F84B6.toInt(),
    val md3eSecondHandColorArgb: Int = 0xFF806EA4.toInt()
)
