package com.flue.launcher.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object WatchTypography {
    val clockLarge = TextStyle(
        fontSize = 56.sp,
        fontWeight = FontWeight.W300,
        letterSpacing = 1.sp,
        color = WatchColors.White
    )
    val clockMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.W500,
        color = WatchColors.White
    )
    val title = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W600,
        color = WatchColors.White
    )
    val body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = WatchColors.White
    )
    val caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = WatchColors.TextSecondary
    )
}
