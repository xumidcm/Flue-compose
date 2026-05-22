package com.flue.launcher.ui.notification

import androidx.compose.ui.graphics.ImageBitmap

data class NotificationEntryUi(
    val key: String,
    val packageName: String,
    val groupKey: String?,
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: ImageBitmap?,
    val isClearable: Boolean,
    val contentIntentAvailable: Boolean,
    val isOngoing: Boolean,
    val isForegroundService: Boolean
)

data class NotificationGroupUi(
    val packageName: String,
    val appLabel: String,
    val headerTitle: String,
    val icon: ImageBitmap?,
    val latestTime: Long,
    val latestSummary: String,
    val entries: List<NotificationEntryUi>,
    val visiblePreviewEntries: List<NotificationEntryUi>,
    val hiddenPreviewCount: Int,
    val expanded: Boolean
)

sealed interface NotificationRevealTarget {
    data class Group(val packageName: String) : NotificationRevealTarget
    data class Entry(val key: String) : NotificationRevealTarget
}
