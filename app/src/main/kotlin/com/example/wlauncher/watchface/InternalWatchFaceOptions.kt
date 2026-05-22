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

data class BuiltInWatchFaceOptions(
    val clockPosition: WatchClockPosition = WatchClockPosition.CENTER,
    val clockSizeSp: Int = 64,
    val boldClock: Boolean = false,
    val cropToFill: Boolean = true,
    val clockColorMode: WatchClockColorMode = WatchClockColorMode.AUTO
)
