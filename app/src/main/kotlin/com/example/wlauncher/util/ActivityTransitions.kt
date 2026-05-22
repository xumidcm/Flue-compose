package com.flue.launcher.util

import android.app.Activity
import android.os.Build

fun Activity.applyFadeOpenTransition() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_OPEN,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

fun Activity.applyFadeCloseTransition() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_CLOSE,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
