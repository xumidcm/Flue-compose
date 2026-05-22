package com.flue.launcher.watchface

import android.graphics.drawable.Drawable

const val BUILT_IN_WATCHFACE_ID = "builtin"
const val BUILT_IN_PHOTO_WATCHFACE_ID = "builtin_photo"
const val BUILT_IN_VIDEO_WATCHFACE_ID = "builtin_video"
const val WATCHFACE_ACTION = "com.dudu.wearlauncher.WATCHFACE"
const val WATCHFACE_REFRESH_ACTION = "com.flue.launcher.action.WATCHFACE_REFRESH"

enum class LunchWatchFaceType {
    BUILTIN,
    EXTERNAL
}

data class LunchWatchFaceDescriptor(
    val id: String,
    val type: LunchWatchFaceType,
    val displayName: String,
    val summary: String = "",
    val packageName: String? = null,
    val watchFaceClassName: String? = null,
    val settingsEntryClassName: String? = null,
    val previewResId: Int = 0,
    val previewAssetPath: String? = null,
    val versionCode: Long = 0,
    val author: String? = null,
    val sourceApkPath: String? = null,
    val watchFaceName: String = id,
    val buildConfigClassName: String? = null,
    val supportsSettings: Boolean = false
) {
    val isBuiltin: Boolean get() = type == LunchWatchFaceType.BUILTIN
    val stableKey: String get() = packageName ?: id
}

data class LunchWatchFaceLoadResult(
    val descriptor: LunchWatchFaceDescriptor,
    val view: android.view.View,
    val bridge: com.dudu.wearlauncher.model.WatchFaceBridge?
)
