package com.flue.launcher.data.model

import androidx.compose.ui.graphics.ImageBitmap

const val BUILTIN_SETTINGS_ENTRY_PACKAGE = "com.flue.launcher.internal"
const val BUILTIN_SETTINGS_ENTRY_ACTIVITY = "builtin.SettingsEntry"
const val BUILTIN_SETTINGS_ENTRY_COMPONENT = "$BUILTIN_SETTINGS_ENTRY_PACKAGE/$BUILTIN_SETTINGS_ENTRY_ACTIVITY"
const val APP_LIST_SHORTCUT_KEY_PREFIX = "shortcut:"
const val APP_LIST_FOLDER_KEY_PREFIX = "folder:"

enum class AppListItemType {
    APP,
    SHORTCUT,
    FOLDER
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val cachedIcon: ImageBitmap,
    val cachedBlurredIcon: ImageBitmap,
    val cachedTwoToneIcon: ImageBitmap? = null,
    val cachedTwoToneBlurredIcon: ImageBitmap? = null,
    val cachedPlainIcon: ImageBitmap? = null,
    val cachedPlainBlurredIcon: ImageBitmap? = null,
    val cachedPlainTwoToneIcon: ImageBitmap? = null,
    val cachedPlainTwoToneBlurredIcon: ImageBitmap? = null,
    val isBuiltInSettingsEntry: Boolean = false,
    val itemType: AppListItemType = AppListItemType.APP,
    val shortcutIntentUri: String? = null,
    val folderItemKeys: List<String> = emptyList()
) {
    val componentKey: String
        get() = when {
            isBuiltInSettingsEntry -> BUILTIN_SETTINGS_ENTRY_COMPONENT
            itemType == AppListItemType.SHORTCUT -> "$APP_LIST_SHORTCUT_KEY_PREFIX$activityName"
            itemType == AppListItemType.FOLDER -> "$APP_LIST_FOLDER_KEY_PREFIX$activityName"
            else -> "$packageName/$activityName"
        }

    val isAppListShortcut: Boolean
        get() = itemType == AppListItemType.SHORTCUT

    val isFolder: Boolean
        get() = itemType == AppListItemType.FOLDER
}

fun AppInfo.iconForDisplay(useTwoTone: Boolean, blurred: Boolean = false): ImageBitmap {
    if (useTwoTone) {
        val twoTone = if (blurred) cachedTwoToneBlurredIcon ?: cachedTwoToneIcon else cachedTwoToneIcon
        if (twoTone != null) return twoTone
    }
    return if (blurred) cachedBlurredIcon else cachedIcon
}

fun AppInfo.plainIconForDisplay(useTwoTone: Boolean, blurred: Boolean = false): ImageBitmap {
    if (useTwoTone) {
        val twoTone = if (blurred) {
            cachedPlainTwoToneBlurredIcon ?: cachedPlainTwoToneIcon
        } else {
            cachedPlainTwoToneIcon
        }
        if (twoTone != null) return twoTone
    }
    return if (blurred) {
        cachedPlainBlurredIcon ?: cachedBlurredIcon
    } else {
        cachedPlainIcon ?: cachedIcon
    }
}
