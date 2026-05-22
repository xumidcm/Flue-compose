package com.flue.launcher.ui.smartstack

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.data.model.WidgetInfo
import com.flue.launcher.data.model.iconForDisplay
import com.flue.launcher.FlueApplication
import com.flue.launcher.data.repository.WidgetRepository
import com.flue.launcher.ui.common.instantPressGesture
import com.flue.launcher.ui.common.rememberPressedState
import com.flue.launcher.ui.common.WatchBatteryPill
import com.flue.launcher.ui.drawer.AppBubble
import com.flue.launcher.ui.drawer.AppShortcutOverlay
import com.flue.launcher.ui.drawer.vibrateHaptic
import com.flue.launcher.ui.notification.NotificationEntryUi
import com.flue.launcher.ui.notification.NotificationGroupUi
import com.flue.launcher.ui.notification.NotificationRevealTarget
import com.flue.launcher.ui.notification.SwipeRevealDeleteContainer
import com.flue.launcher.ui.theme.LauncherTheme
import com.flue.launcher.ui.theme.WatchColors
import com.flue.launcher.viewmodel.SideScreenSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

private const val SIDE_SCREEN_CONTENT_WIDTH_RATIO = 0.884f
private const val SIDE_SCREEN_DISMISS_THRESHOLD = 86f
private const val SIDE_SCREEN_NOTIFICATION_DRAG_RANGE_RATIO = 0.78f
private const val SIDE_SCREEN_NOTIFICATION_DRAG_RANGE_MIN = 300f
private const val SIDE_SCREEN_NOTIFICATION_RELEASE_PROGRESS = 0.6f
private const val SIDE_SCREEN_NOTIFICATION_FLING_VELOCITY = 1350f
private const val STACKED_PREVIEW_TRANSLATION_DP = 28
private const val STACKED_PREVIEW_HORIZONTAL_INSET_DP = 4
private const val SIDE_SCREEN_APP_WIDGET_HOST_ID = 1024
private const val SIDE_SCREEN_SHORTCUT_MENU_TRIGGER_MS = 410L
private const val SIDE_SCREEN_OVERSCROLL_LIMIT = 140f
private const val SIDE_SCREEN_OVERSCROLL_RESISTANCE = 0.35f

private sealed interface SideScreenModalState {
    data object None : SideScreenModalState
    data class ShortcutPicker(val slotIndex: Int) : SideScreenModalState
    data object WidgetPicker : SideScreenModalState
    data class ShortcutActions(val slotIndex: Int, val app: AppInfo) : SideScreenModalState
    data class RemoveShortcut(val slotIndex: Int) : SideScreenModalState
    data class WidgetActions(val card: SideScreenWidgetCardItem) : SideScreenModalState
    data class RemoveWidget(val card: SideScreenWidgetCardItem) : SideScreenModalState
    data class SectionActions(val section: SideScreenSection) : SideScreenModalState
}

private data class SideScreenClockSnapshot(val time: String, val date: String)
private data class BatterySnapshot(val level: Int = 0, val charging: Boolean = false)
private data class StepSnapshot(val steps: Int? = null)
private data class ShortcutPickerItem(
    val componentKey: String,
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
    val source: AppInfo
)

private data class SideScreenWidgetCardItem(
    val widgetIndex: Int,
    val widget: WidgetInfo
)
private data class PendingWidgetSelection(
    val slotIndex: Int,
    val widget: WidgetInfo,
    val appWidgetId: Int
)

private fun SideScreenWidgetCardItem.stableKey(): String {
    return if (widget.widgetId != -1) {
        "widget_${widget.widgetId}"
    } else {
        "widget_${widget.widgetKey}_$widgetIndex"
    }
}

private fun <T> MutableList<T>.moveItem(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return
    val item = removeAt(fromIndex)
    add(toIndex, item)
}

private data class WidgetPickerApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val widgets: List<WidgetInfo>
)
private sealed interface PreviewRow {
    data class Group(val group: NotificationGroupUi, val entries: List<NotificationEntryUi>, val hiddenCount: Int) : PreviewRow
    data class Aggregate(val leadEntry: NotificationEntryUi, val hiddenCount: Int) : PreviewRow
}

private sealed interface PressHoldResult {
    data class LongPress(val change: PointerInputChange) : PressHoldResult
    data object Released : PressHoldResult
    data object Cancelled : PressHoldResult
}

@Composable
fun SmartStackLayer(
    apps: List<AppInfo>,
    sideScreenShortcuts: List<String?>,
    sideScreenWidgetSlots: List<String?>,
    previewGroups: List<NotificationGroupUi>,
    showWidgets: Boolean,
    sectionOrder: List<SideScreenSection>,
    leftSafeInsetPercent: Int,
    notificationsEnabled: Boolean,
    notificationAccessGranted: Boolean,
    notificationsSceneActive: Boolean,
    notificationTransitionProgress: Float,
    revealedNotificationTarget: NotificationRevealTarget?,
    onRevealTargetChange: (NotificationRevealTarget?) -> Unit,
    onOpenNotifications: () -> Unit,
    onNotificationTransitionProgressChange: (Float) -> Unit,
    onNotificationTransitionRelease: (Boolean) -> Unit,
    onOpenNotification: (String, Offset) -> Unit,
    onLaunchApp: (AppInfo, Offset) -> Unit,
    onSetShortcut: (Int, String?) -> Unit,
    onRemoveShortcut: (Int) -> Unit,
    onSwapShortcut: (Int, Int) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onSetWidget: (Int, String?) -> Unit,
    onSwapWidget: (Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onSetShowWidgets: (Boolean) -> Unit,
    onSetShowNotification: (Boolean) -> Unit,
    onMoveSection: (SideScreenSection, Int) -> Unit,
    onDismissGroup: (String) -> Unit,
    onDismissNotification: (String) -> Unit,
    onDismissToFace: () -> Unit,
    showSteps: Boolean,
    stackCardColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val clock = rememberClockSnapshot()
    val battery = rememberBatterySnapshot()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val stepPermissionGranted = rememberActivityRecognitionPermission(showSteps)
    val stepSnapshot = rememberStepSnapshot(showSteps, stepPermissionGranted)
    val slotCenters = remember { mutableStateMapOf<Int, Offset>() }
    val widgetCardHeightsPx = remember { mutableStateMapOf<Int, Int>() }
    val showNotificationPreview = notificationsEnabled
    val visibleShortcutCount = if (showNotificationPreview) 6 else 9
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val widgetRepository = remember(context) { FlueApplication.repositories(context).widgetRepository }
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember(context) { AppWidgetHost(context, SIDE_SCREEN_APP_WIDGET_HOST_ID) }
    var pendingWidgetSelection by remember { mutableStateOf<PendingWidgetSelection?>(null) }
    fun clearAllocatedWidget(slotIndex: Int) {
        widgetRepository.extractWidgetId(sideScreenWidgetSlots.getOrNull(slotIndex))?.let { widgetId ->
            runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
        }
    }

    fun persistBoundWidget(slotIndex: Int, widget: WidgetInfo, appWidgetId: Int) {
        clearAllocatedWidget(slotIndex)
        onSetWidget(slotIndex, widgetRepository.serializeSlotValue(widget.copy(widgetId = appWidgetId)))
    }

    fun discardPendingWidgetSelection() {
        pendingWidgetSelection?.let { selection ->
            runCatching { appWidgetHost.deleteAppWidgetId(selection.appWidgetId) }
        }
        pendingWidgetSelection = null
    }

    fun continueWidgetBinding(selection: PendingWidgetSelection, configureLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val providerInfo = appWidgetManager.getAppWidgetInfo(selection.appWidgetId)
            ?: widgetRepository.findProviderInfo(selection.widget.widgetKey)
        if (providerInfo?.configure != null) {
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, selection.appWidgetId)
            }
            configureLauncher.launch(configureIntent)
        } else {
            persistBoundWidget(selection.slotIndex, selection.widget, selection.appWidgetId)
            pendingWidgetSelection = null
        }
    }

    val configureWidgetLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val selection = pendingWidgetSelection ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            persistBoundWidget(selection.slotIndex, selection.widget, selection.appWidgetId)
        } else {
            runCatching { appWidgetHost.deleteAppWidgetId(selection.appWidgetId) }
        }
        pendingWidgetSelection = null
    }
    val bindWidgetLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val selection = pendingWidgetSelection ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            continueWidgetBinding(selection, configureWidgetLauncher)
        } else {
            runCatching { appWidgetHost.deleteAppWidgetId(selection.appWidgetId) }
            pendingWidgetSelection = null
        }
    }

    val shortcutItems = remember(apps, sideScreenShortcuts, visibleShortcutCount) {
        List(visibleShortcutCount) { i ->
            val appKey = sideScreenShortcuts.getOrNull(i)
            appKey?.let { key -> apps.firstOrNull { it.componentKey == key } }
        }
    }
    val widgetCards = remember(sideScreenWidgetSlots, widgetRepository) {
        sideScreenWidgetSlots.mapIndexedNotNull { index, raw ->
            widgetRepository.parseSlotValue(raw)?.let { SideScreenWidgetCardItem(index, it) }
        }
    }
    val visibleWidgetCards = remember(widgetCards, showWidgets) {
        if (showWidgets) widgetCards else emptyList()
    }
    val previewRows = remember(previewGroups, showNotificationPreview) {
        if (showNotificationPreview) buildPreviewRows(previewGroups, maxRows = 1) else emptyList()
    }
    val notificationDragRangePx = remember(configuration.screenHeightDp, density) {
        maxOf(
            with(density) { configuration.screenHeightDp.dp.toPx() } * SIDE_SCREEN_NOTIFICATION_DRAG_RANGE_RATIO,
            SIDE_SCREEN_NOTIFICATION_DRAG_RANGE_MIN
        )
    }
    val notificationReleaseThresholdPx = notificationDragRangePx * SIDE_SCREEN_NOTIFICATION_RELEASE_PROGRESS
    DisposableEffect(appWidgetHost, visibleWidgetCards.isNotEmpty()) {
        if (visibleWidgetCards.isNotEmpty()) {
            runCatching { appWidgetHost.startListening() }
        }
        onDispose {
            discardPendingWidgetSelection()
            if (visibleWidgetCards.isNotEmpty()) {
                runCatching { appWidgetHost.stopListening() }
            }
        }
    }
    var modalState by remember { mutableStateOf<SideScreenModalState>(SideScreenModalState.None) }
    var dragDx by remember { mutableFloatStateOf(0f) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    var dragVelocityY by remember { mutableFloatStateOf(0f) }
    var lastDragEventUptime by remember { mutableStateOf(0L) }
    var transitionInFlight by remember { mutableStateOf(false) }
    val sideListState = rememberLazyListState()
    val sideOverscroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val contentTranslationY = if (notificationsSceneActive || transitionInFlight) 0f else sideOverscroll.value

    fun releaseSideOverscroll() {
        scope.launch {
            if (sideOverscroll.value != 0f) {
                sideOverscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
            }
        }
    }

    fun applySideOverscroll(delta: Float) {
        val next = when {
            sideOverscroll.value > 0f && delta < 0f -> (sideOverscroll.value + delta).coerceAtLeast(0f)
            sideOverscroll.value < 0f && delta > 0f -> (sideOverscroll.value + delta).coerceAtMost(0f)
            delta > 0f -> (sideOverscroll.value + delta * SIDE_SCREEN_OVERSCROLL_RESISTANCE)
                .coerceIn(0f, SIDE_SCREEN_OVERSCROLL_LIMIT)
            delta < 0f -> (sideOverscroll.value + delta * SIDE_SCREEN_OVERSCROLL_RESISTANCE)
                .coerceIn(-SIDE_SCREEN_OVERSCROLL_LIMIT, 0f)
            else -> sideOverscroll.value
        }
        scope.launch { sideOverscroll.snapTo(next) }
    }

    val sideNestedScrollConnection = remember(sideListState, notificationsEnabled, notificationsSceneActive, transitionInFlight) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || notificationsSceneActive || transitionInFlight) {
                    return Offset.Zero
                }
                val atTop = sideListState.firstVisibleItemIndex == 0 && sideListState.firstVisibleItemScrollOffset == 0
                val lastVisible = sideListState.layoutInfo.visibleItemsInfo.lastOrNull()
                val atBottom = lastVisible != null &&
                    lastVisible.index >= sideListState.layoutInfo.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size <= sideListState.layoutInfo.viewportEndOffset
                when {
                    available.y > 0f && atTop -> {
                        applySideOverscroll(available.y)
                        return Offset(0f, available.y)
                    }
                    available.y < 0f && atBottom && !notificationsEnabled -> {
                        applySideOverscroll(available.y)
                        return Offset(0f, available.y)
                    }
                    sideOverscroll.value > 0f && available.y < 0f -> {
                        applySideOverscroll(available.y)
                        return Offset(0f, available.y)
                    }
                    sideOverscroll.value < 0f && available.y > 0f && !notificationsEnabled -> {
                        applySideOverscroll(available.y)
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sideOverscroll.value != 0f) {
                    sideOverscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(notificationsSceneActive) {
        sideOverscroll.snapTo(0f)
        dragDx = 0f
        dragDy = 0f
        dragVelocityY = 0f
        lastDragEventUptime = 0L
        transitionInFlight = false
    }
    LaunchedEffect(widgetCards) {
        val activeWidgetIndexes = widgetCards.map(SideScreenWidgetCardItem::widgetIndex).toSet()
        widgetCardHeightsPx.keys.toList().forEach { widgetIndex ->
            if (widgetIndex !in activeWidgetIndexes) {
                widgetCardHeightsPx.remove(widgetIndex)
            }
        }
    }
    val launcherStyle = LauncherTheme.style

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(40.dp))
            .background(launcherStyle.screenBackground)
            .pointerInput(notificationAccessGranted, notificationsEnabled, modalState, notificationsSceneActive, transitionInFlight) {
                if (modalState != SideScreenModalState.None || notificationsSceneActive || transitionInFlight) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        dragDx = 0f
                        dragDy = 0f
                        dragVelocityY = 0f
                        lastDragEventUptime = 0L
                    },
                    onDrag = { change, dragAmount ->
                        val listAtTop = sideListState.firstVisibleItemIndex == 0 &&
                            sideListState.firstVisibleItemScrollOffset == 0
                        val lastVisible = sideListState.layoutInfo.visibleItemsInfo.lastOrNull()
                        val listAtBottom = lastVisible != null &&
                            lastVisible.index >= sideListState.layoutInfo.totalItemsCount - 1 &&
                            lastVisible.offset + lastVisible.size <= sideListState.layoutInfo.viewportEndOffset
                        if (lastDragEventUptime != 0L) {
                            val deltaMs = (change.uptimeMillis - lastDragEventUptime).coerceAtLeast(1L)
                            val instantVelocityY = dragAmount.y / deltaMs * 1000f
                            dragVelocityY = if (dragVelocityY == 0f) {
                                instantVelocityY
                            } else {
                                dragVelocityY * 0.35f + instantVelocityY * 0.65f
                            }
                        }
                        lastDragEventUptime = change.uptimeMillis
                        dragDx += dragAmount.x
                        dragDy += dragAmount.y
                        if (abs(dragDx) > abs(dragDy)) {
                            if (dragDx < 0f) change.consume()
                        } else if (sideOverscroll.value != 0f) {
                            change.consume()
                            applySideOverscroll(dragAmount.y)
                        } else if (
                            !notificationsEnabled &&
                            (
                                visibleWidgetCards.isEmpty() ||
                                    (dragAmount.y > 0f && listAtTop) ||
                                    (dragAmount.y < 0f && listAtBottom)
                                )
                        ) {
                            change.consume()
                            applySideOverscroll(dragAmount.y)
                        } else if (notificationsEnabled && (dragDy < 0f || notificationTransitionProgress > 0f)) {
                            change.consume()
                            onNotificationTransitionProgressChange(
                                (-dragDy / notificationDragRangePx).coerceIn(0f, 1f)
                            )
                        } else if (
                            notificationsEnabled &&
                            dragDy > 0f &&
                            (visibleWidgetCards.isEmpty() ||
                                (sideListState.firstVisibleItemIndex == 0 && sideListState.firstVisibleItemScrollOffset == 0))
                        ) {
                            change.consume()
                            applySideOverscroll(dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        val verticalIntent = abs(dragDy) > abs(dragDx) ||
                            abs(dragVelocityY) > SIDE_SCREEN_NOTIFICATION_FLING_VELOCITY
                        val dismissToFace = dragDx < -SIDE_SCREEN_DISMISS_THRESHOLD && abs(dragDx) > abs(dragDy)
                        val openNotifications = notificationsEnabled &&
                            verticalIntent &&
                            (
                                dragDy < -notificationReleaseThresholdPx ||
                                    notificationTransitionProgress > SIDE_SCREEN_NOTIFICATION_RELEASE_PROGRESS ||
                                    dragVelocityY < -SIDE_SCREEN_NOTIFICATION_FLING_VELOCITY
                                )

                        when {
                            dismissToFace -> {
                                dragDx = 0f
                                dragDy = 0f
                                dragVelocityY = 0f
                                lastDragEventUptime = 0L
                                onNotificationTransitionRelease(false)
                                onRevealTargetChange(null)
                                releaseSideOverscroll()
                                onDismissToFace()
                            }
                            openNotifications -> {
                                dragDx = 0f
                                dragDy = 0f
                                dragVelocityY = 0f
                                lastDragEventUptime = 0L
                                onRevealTargetChange(null)
                                releaseSideOverscroll()
                                onNotificationTransitionRelease(true)
                            }
                            else -> {
                                dragDx = 0f
                                dragDy = 0f
                                dragVelocityY = 0f
                                lastDragEventUptime = 0L
                                releaseSideOverscroll()
                                onNotificationTransitionRelease(false)
                            }
                        }
                    },
                    onDragCancel = {
                        dragDx = 0f
                        dragDy = 0f
                        dragVelocityY = 0f
                        lastDragEventUptime = 0L
                        releaseSideOverscroll()
                        onNotificationTransitionRelease(false)
                    }
                )
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val leftSafeInset = maxWidth * (leftSafeInsetPercent.coerceIn(0, 50) / 100f)
        val availableWidth = maxWidth - leftSafeInset
        val contentWidth = availableWidth * SIDE_SCREEN_CONTENT_WIDTH_RATIO
        val quickHeight = if (showNotificationPreview) {
            (contentWidth * 0.58f).coerceAtLeast(152.dp)
        } else {
            (contentWidth * 0.88f).coerceAtLeast(226.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = contentTranslationY }
                .padding(start = leftSafeInset)
                .padding(top = 10.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                clock.time,
                color = launcherStyle.titleColor,
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                clock.date,
                color = WatchColors.TextSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            QuickPanel(
                width = contentWidth,
                height = quickHeight,
                items = shortcutItems,
                slotCenters = slotCenters,
                onAdd = { onRevealTargetChange(null); modalState = SideScreenModalState.ShortcutPicker(it) },
                onOpenActions = { slot, app ->
                    onRevealTargetChange(null)
                    modalState = SideScreenModalState.ShortcutActions(slot, app)
                },
                onCloseActions = {
                    if (modalState is SideScreenModalState.ShortcutActions) {
                        modalState = SideScreenModalState.None
                    }
                },
                onSwap = onSwapShortcut,
                onClickApp = { slot, app ->
                    val center = slotCenters[slot] ?: Offset(widthPx / 2f, heightPx / 2f)
                    onRevealTargetChange(null)
                    onLaunchApp(app, Offset(center.x / widthPx, center.y / heightPx))
                }
            )
            if (showNotificationPreview) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .width(contentWidth)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        !notificationAccessGranted -> PreviewInfoCard("开启通知访问", "副一屏上滑进入通知中心后完成授权", onOpenNotifications)
                        previewGroups.isEmpty() -> PreviewInfoCard("暂无通知", "副一屏上滑可打开通知中心", onOpenNotifications)
                        else -> previewRows.forEach { row ->
                            when (row) {
                                is PreviewRow.Aggregate -> StackedPreviewCard(
                                    row.leadEntry,
                                    row.hiddenCount,
                                    stackCardColor,
                                    onOpenNotifications
                                )
                                is PreviewRow.Group -> {
                                    val entry = row.entries.firstOrNull()
                                    if (entry != null) {
                                        SwipeRevealDeleteContainer(
                                            target = if (row.hiddenCount > 0) {
                                                NotificationRevealTarget.Group(row.group.packageName)
                                            } else {
                                                NotificationRevealTarget.Entry(entry.key)
                                            },
                                            revealedTarget = revealedNotificationTarget,
                                            onRevealTargetChange = onRevealTargetChange,
                                            enabled = if (row.hiddenCount > 0) {
                                                row.group.entries.any(NotificationEntryUi::isClearable)
                                            } else {
                                                entry.isClearable
                                            },
                                            onDelete = {
                                                if (row.hiddenCount > 0) {
                                                    onDismissGroup(row.group.packageName)
                                                } else {
                                                    onDismissNotification(entry.key)
                                                }
                                            },
                                            actionHeight = 72.dp
                                        ) {
                                            if (row.hiddenCount > 0) {
                                                StackedPreviewCard(entry, row.hiddenCount, stackCardColor, onOpenNotifications)
                                            } else {
                                                PreviewPill(
                                                    entry = entry,
                                                    onClick = onOpenNotifications
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!showWidgets || visibleWidgetCards.isEmpty()) {
                    Spacer(Modifier.height(64.dp))
                }
            }
            if (showWidgets && visibleWidgetCards.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    state = sideListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(sideNestedScrollConnection),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(bottom = 54.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item("widget_header") {
                        SideScreenSectionHeader(
                            title = "小组件",
                            width = contentWidth,
                            actionLabel = "添加",
                            onAction = {
                                onRevealTargetChange(null)
                                modalState = SideScreenModalState.WidgetPicker
                            },
                            onLongPress = {
                                onRevealTargetChange(null)
                                modalState = SideScreenModalState.SectionActions(SideScreenSection.Widgets)
                            }
                        )
                    }
                    items(
                        items = visibleWidgetCards,
                        key = { it.stableKey() },
                        contentType = { "side_widget" }
                    ) { card ->
                        Box(
                            Modifier
                                .width(contentWidth)
                                .animateItem(
                                    fadeInSpec = tween(180),
                                    placementSpec = spring(stiffness = 520f, dampingRatio = 0.84f),
                                    fadeOutSpec = tween(180)
                                )
                                .animateContentSize()
                        ) {
                            WidgetCard(
                                widget = card.widget,
                                widgetRepository = widgetRepository,
                                appWidgetHost = appWidgetHost,
                                onLongPress = {
                                    onRevealTargetChange(null)
                                    modalState = SideScreenModalState.WidgetActions(card)
                                },
                                onMeasuredHeight = { heightPx ->
                                    if (heightPx > 0) {
                                        widgetCardHeightsPx[card.widgetIndex] = heightPx
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        WatchBatteryAndStepsPill(
            level = battery.level,
            charging = battery.charging,
            steps = stepSnapshot.steps,
            showSteps = showSteps,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        )
    }

    when (val state = modalState) {
        is SideScreenModalState.ShortcutPicker -> ShortcutPickerOverlay(
            apps = apps,
            onSelect = {
                discardPendingWidgetSelection()
                onSetShortcut(state.slotIndex, it.componentKey)
            },
            onDismiss = { modalState = SideScreenModalState.None }
        )
        SideScreenModalState.WidgetPicker -> WidgetPickerOverlay(
            apps = apps,
            widgetRepository = widgetRepository,
            onSelectWidget = { widget ->
                val targetIndex = sideScreenWidgetSlots.size
                val providerInfo = widgetRepository.findProviderInfo(widget.widgetKey)
                if (providerInfo != null) {
                    discardPendingWidgetSelection()
                    clearAllocatedWidget(targetIndex)
                    val appWidgetId = runCatching { appWidgetHost.allocateAppWidgetId() }.getOrNull()
                    if (appWidgetId != null) {
                        val selection = PendingWidgetSelection(
                            slotIndex = targetIndex,
                            widget = widget,
                            appWidgetId = appWidgetId
                        )
                        pendingWidgetSelection = selection
                        val bindExtras = buildWidgetBindOptions(providerInfo)
                        val bound = runCatching {
                            appWidgetManager.bindAppWidgetIdIfAllowed(
                                appWidgetId,
                                providerInfo.provider,
                                bindExtras
                            )
                        }.getOrDefault(false)
                        if (bound) {
                            continueWidgetBinding(selection, configureWidgetLauncher)
                        } else if (activity != null) {
                            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, bindExtras)
                            }
                            bindWidgetLauncher.launch(bindIntent)
                        } else {
                            discardPendingWidgetSelection()
                        }
                    }
                }
            },
            onDismiss = { modalState = SideScreenModalState.None }
        )
        is SideScreenModalState.ShortcutActions -> AppShortcutOverlay(
            app = state.app,
            onExcludeApp = {
                onHideApp(state.app)
                onRemoveShortcut(state.slotIndex)
            },
            onRemoveShortcut = { onRemoveShortcut(state.slotIndex) },
            onDismiss = { modalState = SideScreenModalState.None }
        )
        is SideScreenModalState.RemoveShortcut -> {
            val app = shortcutItems.getOrNull(state.slotIndex)
            if (app != null) {
                RemoveOverlay(
                    app = app,
                    widget = null,
                    onRemove = { onRemoveShortcut(state.slotIndex) },
                    onDismiss = { modalState = SideScreenModalState.None }
                )
            } else {
                LaunchedEffect(state) { modalState = SideScreenModalState.None }
            }
        }
        is SideScreenModalState.WidgetActions -> {
            val selected = state.card
            val widgetCardIndex = widgetCards.indexOfFirst {
                it.widgetIndex == selected.widgetIndex &&
                    it.widget.widgetKey == selected.widget.widgetKey &&
                    it.widget.widgetId == selected.widget.widgetId
            }
            val currentCard = widgetCards.getOrNull(widgetCardIndex)
            if (currentCard != null) {
                WidgetActionOverlay(
                    widget = currentCard.widget,
                    canMoveUp = widgetCardIndex > 0,
                    canMoveDown = widgetCardIndex in 0 until widgetCards.lastIndex,
                    onMoveUp = {
                        val previousIndex = widgetCards.getOrNull(widgetCardIndex - 1)?.widgetIndex ?: return@WidgetActionOverlay
                        onSwapWidget(currentCard.widgetIndex, previousIndex)
                    },
                    onMoveDown = {
                        val nextIndex = widgetCards.getOrNull(widgetCardIndex + 1)?.widgetIndex ?: return@WidgetActionOverlay
                        onSwapWidget(currentCard.widgetIndex, nextIndex)
                    },
                    onRemove = {
                        clearAllocatedWidget(currentCard.widgetIndex)
                        onRemoveWidget(currentCard.widgetIndex)
                    },
                    onDismiss = { modalState = SideScreenModalState.None }
                )
            } else {
                LaunchedEffect(state) { modalState = SideScreenModalState.None }
            }
        }
        is SideScreenModalState.SectionActions -> {
            val sectionIndex = sectionOrder.indexOf(state.section)
            if (sectionIndex >= 0) {
                SectionActionOverlay(
                    title = sectionTitle(state.section),
                    canMoveUp = sectionIndex > 0,
                    canMoveDown = sectionIndex in 0 until sectionOrder.lastIndex,
                    onMoveUp = { onMoveSection(state.section, -1) },
                    onMoveDown = { onMoveSection(state.section, 1) },
                    onDisable = {
                        when (state.section) {
                            SideScreenSection.Widgets -> onSetShowWidgets(false)
                            SideScreenSection.Notifications -> onSetShowNotification(false)
                        }
                    },
                    onDismiss = { modalState = SideScreenModalState.None }
                )
            } else {
                LaunchedEffect(state) { modalState = SideScreenModalState.None }
            }
        }
        is SideScreenModalState.RemoveWidget -> {
            val selected = state.card
            val currentCard = widgetCards.firstOrNull {
                it.widgetIndex == selected.widgetIndex &&
                    it.widget.widgetKey == selected.widget.widgetKey &&
                    it.widget.widgetId == selected.widget.widgetId
            }
            if (currentCard != null) {
                RemoveOverlay(
                    app = null,
                    widget = currentCard.widget,
                    onRemove = {
                        clearAllocatedWidget(currentCard.widgetIndex)
                        onRemoveWidget(currentCard.widgetIndex)
                    },
                    onDismiss = { modalState = SideScreenModalState.None }
                )
            } else {
                LaunchedEffect(state) { modalState = SideScreenModalState.None }
            }
        }
        SideScreenModalState.None -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetPageLayer(
    apps: List<AppInfo>,
    sideScreenWidgetSlots: List<String?>,
    leftSafeInsetPercent: Int,
    onSetWidget: (Int, String?) -> Unit,
    onSwapWidget: (Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val density = LocalDensity.current
    val launcherStyle = LauncherTheme.style
    val widgetRepository = remember(context) { FlueApplication.repositories(context).widgetRepository }
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember(context) { AppWidgetHost(context, SIDE_SCREEN_APP_WIDGET_HOST_ID) }
    val listState = rememberLazyListState()
    val overscroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val widgetCards = remember(sideScreenWidgetSlots, widgetRepository) {
        sideScreenWidgetSlots.mapIndexedNotNull { index, raw ->
            widgetRepository.parseSlotValue(raw)?.let { SideScreenWidgetCardItem(index, it) }
        }
    }
    var editMode by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var pendingWidgetSelection by remember { mutableStateOf<PendingWidgetSelection?>(null) }
    val widgetBorderHotspotPx = with(density) { 18.dp.toPx() }

    fun clearAllocatedWidget(slotIndex: Int) {
        widgetRepository.extractWidgetId(sideScreenWidgetSlots.getOrNull(slotIndex))?.let { widgetId ->
            runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
        }
    }

    fun persistBoundWidget(slotIndex: Int, widget: WidgetInfo, appWidgetId: Int) {
        clearAllocatedWidget(slotIndex)
        onSetWidget(slotIndex, widgetRepository.serializeSlotValue(widget.copy(widgetId = appWidgetId)))
    }

    fun discardPendingWidgetSelection() {
        pendingWidgetSelection?.let { selection ->
            runCatching { appWidgetHost.deleteAppWidgetId(selection.appWidgetId) }
        }
        pendingWidgetSelection = null
    }

    fun continueWidgetBinding(
        selection: PendingWidgetSelection,
        configureLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        val providerInfo = appWidgetManager.getAppWidgetInfo(selection.appWidgetId)
            ?: widgetRepository.findProviderInfo(selection.widget.widgetKey)
        if (providerInfo?.configure != null) {
            val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, selection.appWidgetId)
            }
            configureLauncher.launch(configureIntent)
        } else {
            persistBoundWidget(selection.slotIndex, selection.widget, selection.appWidgetId)
            pendingWidgetSelection = null
        }
    }

    val configureWidgetLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val selection = pendingWidgetSelection ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            persistBoundWidget(selection.slotIndex, selection.widget, selection.appWidgetId)
        } else {
            runCatching { appWidgetHost.deleteAppWidgetId(selection.appWidgetId) }
        }
        pendingWidgetSelection = null
    }
    val bindWidgetLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val selection = pendingWidgetSelection ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            continueWidgetBinding(selection, configureWidgetLauncher)
        } else {
            runCatching { appWidgetHost.deleteAppWidgetId(selection.appWidgetId) }
            pendingWidgetSelection = null
        }
    }

    DisposableEffect(appWidgetHost, widgetCards.isNotEmpty()) {
        if (widgetCards.isNotEmpty()) {
            runCatching { appWidgetHost.startListening() }
        }
        onDispose {
            discardPendingWidgetSelection()
            if (widgetCards.isNotEmpty()) {
                runCatching { appWidgetHost.stopListening() }
            }
        }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                val atBottom = lastVisible != null &&
                    lastVisible.index >= listState.layoutInfo.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size <= listState.layoutInfo.viewportEndOffset
                when {
                    available.y > 0f && atTop -> {
                        scope.launch { overscroll.snapTo((overscroll.value + available.y * 0.35f).coerceAtMost(150f)) }
                        return Offset(0f, available.y)
                    }
                    available.y < 0f && atBottom -> {
                        scope.launch { overscroll.snapTo((overscroll.value + available.y * 0.35f).coerceAtLeast(-150f)) }
                        return Offset(0f, available.y)
                    }
                    overscroll.value > 0f && available.y < 0f -> {
                        scope.launch { overscroll.snapTo((overscroll.value + available.y).coerceAtLeast(0f)) }
                        return Offset(0f, available.y)
                    }
                    overscroll.value < 0f && available.y > 0f -> {
                        scope.launch { overscroll.snapTo((overscroll.value + available.y).coerceAtMost(0f)) }
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscroll.value != 0f) {
                    overscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(widgetCards) {
        if (widgetCards.isEmpty()) {
            editMode = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(40.dp))
            .background(launcherStyle.screenBackground)
    ) {
        val leftSafeInset = maxWidth * (leftSafeInsetPercent.coerceIn(0, 50) / 100f)
        val availableWidth = maxWidth - leftSafeInset
        val contentWidth = availableWidth * SIDE_SCREEN_CONTENT_WIDTH_RATIO
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { translationY = overscroll.value }
                .padding(start = leftSafeInset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item("widget_page_header") {
                Row(
                    modifier = Modifier.width(contentWidth),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "小组件",
                        color = launcherStyle.titleColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetPageChip(
                            label = if (editMode) "完成" else "编辑",
                            onClick = { editMode = !editMode },
                            enabled = widgetCards.isNotEmpty()
                        )
                        WidgetPageChip(
                            label = "添加",
                            onClick = { showPicker = true }
                        )
                    }
                }
            }
            if (widgetCards.isEmpty()) {
                item("empty_widget_page") {
                    EmptyWidgetCard(width = contentWidth)
                }
            } else {
                items(
                    items = widgetCards,
                    key = { it.stableKey() },
                    contentType = { "widget_page_card" }
                ) { card ->
                    val cardKey = card.stableKey()
                    val cardIndex = widgetCards.indexOfFirst { it.stableKey() == cardKey }
                    val shellShape = RoundedCornerShape(28.dp)
                    val editJiggle = rememberInfiniteTransition(label = "widget_edit_jiggle_$cardKey")
                    val jiggleRotation by editJiggle.animateFloat(
                        initialValue = -0.35f,
                        targetValue = 0.35f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 170
                                -0.35f at 0
                                0.35f at 85
                                -0.35f at 170
                            }
                        ),
                        label = "widget_edit_jiggle_rotation_$cardKey"
                    )
                    Box(
                        modifier = Modifier
                            .width(contentWidth)
                            .animateItem(
                                fadeInSpec = tween(180),
                                placementSpec = spring(stiffness = 520f, dampingRatio = 0.84f),
                                fadeOutSpec = tween(180)
                            )
                            .graphicsLayer {
                                rotationZ = if (editMode) jiggleRotation else 0f
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shellShape)
                                .background(Color.White.copy(alpha = if (editMode) 0.05f else 0.03f))
                                .pointerInput(editMode, cardKey) {
                                    if (editMode) return@pointerInput
                                    awaitEachGesture {
                                        val down = awaitPrimaryDown()
                                        val widthPx = size.width.toFloat()
                                        val heightPx = size.height.toFloat()
                                        val isBorderPress =
                                            down.position.x <= widgetBorderHotspotPx ||
                                                down.position.x >= widthPx - widgetBorderHotspotPx ||
                                                down.position.y <= widgetBorderHotspotPx ||
                                                down.position.y >= heightPx - widgetBorderHotspotPx
                                        if (!isBorderPress) return@awaitEachGesture
                                        when (val hold = awaitLongPressOrRelease(
                                            pointerId = down.id,
                                            downPosition = down.position,
                                            timeoutMillis = SIDE_SCREEN_SHORTCUT_MENU_TRIGGER_MS
                                        )) {
                                            is PressHoldResult.LongPress -> {
                                                vibrateHaptic(context)
                                                hold.change.consume()
                                                editMode = true
                                            }
                                            PressHoldResult.Cancelled,
                                            PressHoldResult.Released -> Unit
                                        }
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            WidgetCard(
                                widget = card.widget,
                                widgetRepository = widgetRepository,
                                appWidgetHost = appWidgetHost,
                                onLongPress = null,
                                onMeasuredHeight = {}
                            )
                        }
                        if (editMode) {
                            val blockerInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(shellShape)
                                    .background(Color.Black.copy(alpha = 0.08f))
                                    .clickable(
                                        interactionSource = blockerInteraction,
                                        indication = null,
                                        onClick = {}
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WidgetEditIconButton(
                                    enabled = cardIndex > 0,
                                    backgroundColor = Color(0xFF1976D2),
                                    onClick = {
                                        val previous = widgetCards.getOrNull(cardIndex - 1)
                                            ?: return@WidgetEditIconButton
                                        onSwapWidget(card.widgetIndex, previous.widgetIndex)
                                    }
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                WidgetEditIconButton(
                                    enabled = cardIndex in 0 until widgetCards.lastIndex,
                                    backgroundColor = Color(0xFF1976D2),
                                    onClick = {
                                        val next = widgetCards.getOrNull(cardIndex + 1)
                                            ?: return@WidgetEditIconButton
                                        onSwapWidget(card.widgetIndex, next.widgetIndex)
                                    }
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                WidgetEditIconButton(
                                    enabled = true,
                                    backgroundColor = Color(0xFFE53935),
                                    onClick = {
                                        clearAllocatedWidget(card.widgetIndex)
                                        onRemoveWidget(card.widgetIndex)
                                    }
                                ) {
                                    Text(
                                        text = "-",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        WidgetPickerOverlay(
            apps = apps,
            widgetRepository = widgetRepository,
            onSelectWidget = { widget ->
                val targetIndex = sideScreenWidgetSlots.size
                val providerInfo = widgetRepository.findProviderInfo(widget.widgetKey)
                if (providerInfo != null) {
                    discardPendingWidgetSelection()
                    clearAllocatedWidget(targetIndex)
                    val appWidgetId = runCatching { appWidgetHost.allocateAppWidgetId() }.getOrNull()
                    if (appWidgetId != null) {
                        val selection = PendingWidgetSelection(
                            slotIndex = targetIndex,
                            widget = widget,
                            appWidgetId = appWidgetId
                        )
                        pendingWidgetSelection = selection
                        val bindExtras = buildWidgetBindOptions(providerInfo)
                        val bound = runCatching {
                            appWidgetManager.bindAppWidgetIdIfAllowed(
                                appWidgetId,
                                providerInfo.provider,
                                bindExtras
                            )
                        }.getOrDefault(false)
                        if (bound) {
                            continueWidgetBinding(selection, configureWidgetLauncher)
                        } else if (activity != null) {
                            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, bindExtras)
                            }
                            bindWidgetLauncher.launch(bindIntent)
                        } else {
                            discardPendingWidgetSelection()
                        }
                    }
                }
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun WidgetPageChip(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val launcherStyle = LauncherTheme.style
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) launcherStyle.topBarChipColor else launcherStyle.cardColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) launcherStyle.topBarTextColor else WatchColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WidgetEditIconButton(
    enabled: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val alpha = if (enabled) 1f else 0.34f
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = backgroundColor.alpha * alpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable private fun QuickPanel(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    items: List<AppInfo?>,
    slotCenters: MutableMap<Int, Offset>,
    onAdd: (Int) -> Unit,
    onOpenActions: (Int, AppInfo) -> Unit,
    onCloseActions: () -> Unit,
    onSwap: (Int, Int) -> Unit,
    onClickApp: (Int, AppInfo) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { 10.dp.toPx() }
    val dropThresholdPx = with(density) { 68.dp.toPx() }
    val visibleSlots = remember(items.size) { items.indices.toSet() }
    val bubbleSize = 58.dp
    var draggingSlot by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragMoved by remember { mutableStateOf(false) }
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF353535))
            .padding(14.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            items.chunked(3).forEachIndexed { rowIndex, row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEachIndexed { colIndex, item ->
                        val slot = rowIndex * 3 + colIndex
                        Box(Modifier.onGloballyPositioned { c -> val p = c.positionInRoot(); slotCenters[slot] = Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f) }, contentAlignment = Alignment.Center) {
                            when (item) {
                                null -> {
                                    AddBubble(size = bubbleSize) { onAdd(slot) }
                                }
                                else -> {
                                    val dragModifier = Modifier.pointerInput(item.componentKey, slot, slotCenters) {
                                        awaitEachGesture {
                                            val down = awaitPrimaryDown()
                                            when (val hold = awaitLongPressOrRelease(
                                                pointerId = down.id,
                                                downPosition = down.position,
                                                timeoutMillis = SIDE_SCREEN_SHORTCUT_MENU_TRIGGER_MS
                                            )) {
                                                PressHoldResult.Cancelled -> Unit
                                                PressHoldResult.Released -> {
                                                    onClickApp(slot, item)
                                                }
                                                is PressHoldResult.LongPress -> {
                                                    vibrateHaptic(context)
                                                    hold.change.consume()
                                                    dragOffset = Offset.Zero
                                                    dragMoved = false
                                                    onOpenActions(slot, item)

                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                        if (!change.pressed) {
                                                            change.consume()
                                                            break
                                                        }
                                                        val delta = change.positionChange()
                                                        if (delta != Offset.Zero) {
                                                            dragOffset += delta
                                                            if (!dragMoved && dragOffset.getDistance() > dragThresholdPx) {
                                                                dragMoved = true
                                                                draggingSlot = slot
                                                                onCloseActions()
                                                            }
                                                            change.consume()
                                                        }
                                                    }

                                                    val release = slotCenters[slot]?.let { it + dragOffset }
                                                    val target = release?.let { pointer ->
                                                        slotCenters
                                                            .filterKeys { it != slot && it in visibleSlots }
                                                            .minByOrNull { (_, center) -> (center - pointer).getDistance() }
                                                            ?.takeIf { (_, center) -> (center - pointer).getDistance() <= dropThresholdPx }
                                                            ?.key
                                                    }
                                                    if (dragMoved && target != null) {
                                                        onSwap(slot, target)
                                                    }
                                                    draggingSlot = null
                                                    dragOffset = Offset.Zero
                                                    dragMoved = false
                                                }
                                            }
                                        }
                                    }
                                    val itemDragOffset = if (draggingSlot == slot) dragOffset else Offset.Zero
                                    AppBubble(
                                        item.cachedIcon,
                                        bubbleSize,
                                        onClick = {},
                                        onLongClick = null,
                                        forcePressed = draggingSlot == slot,
                                        gesturesEnabled = false,
                                        shape = CircleShape,
                                        modifier = dragModifier
                                            .zIndex(if (draggingSlot == slot) 6f else 0f)
                                            .graphicsLayer {
                                                translationX = itemDragOffset.x
                                                translationY = itemDragOffset.y
                                                shadowElevation = if (draggingSlot == slot) 12.dp.toPx() else 0f
                                            }
                                    )
                                }
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.size(bubbleSize)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetCard(
    widget: WidgetInfo,
    widgetRepository: WidgetRepository,
    appWidgetHost: AppWidgetHost,
    onLongPress: (() -> Unit)?,
    onMeasuredHeight: (Int) -> Unit
) {
    val context = LocalContext.current
    val providerInfo = remember(widgetRepository, widget.widgetKey) {
        widgetRepository.findProviderInfo(widget.widgetKey)
    }
    val minWidgetHeight = widget.minHeightDp.dp.coerceAtLeast(118.dp)
    val widgetAspectRatio = remember(providerInfo) {
        val minWidth = providerInfo?.minWidth?.takeIf { it > 0 } ?: 1
        val minHeight = providerInfo?.minHeight?.takeIf { it > 0 } ?: 1
        (minWidth.toFloat() / minHeight.toFloat()).coerceIn(0.5f, 2.5f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> onMeasuredHeight(coordinates.size.height) }
            .graphicsLayer {
                shape = RoundedCornerShape(24.dp)
                clip = true
            }
            .background(Color(0xFF1F2937))
    ) {
        if (providerInfo != null && widget.widgetId != -1) {
            val widgetBindTag = remember(widget.widgetId, providerInfo) {
                "${widget.widgetId}|${providerInfo.provider.flattenToString()}|${providerInfo.minWidth}x${providerInfo.minHeight}"
            }
            AndroidView(
                factory = { viewContext ->
                    appWidgetHost.createView(viewContext, widget.widgetId, providerInfo).apply {
                        setAppWidget(widget.widgetId, providerInfo)
                        tag = widgetBindTag
                        if (onLongPress != null) {
                            setOnLongClickListener {
                                vibrateHaptic(context)
                                onLongPress()
                                true
                            }
                        } else {
                            setOnLongClickListener(null)
                        }
                    }
                },
                update = { hostView ->
                    if (hostView.tag != widgetBindTag) {
                        @Suppress("DEPRECATION")
                        hostView.setAppWidget(widget.widgetId, providerInfo)
                        @Suppress("DEPRECATION")
                        hostView.updateAppWidgetSize(
                            buildWidgetBindOptions(providerInfo),
                            0,
                            0,
                            Int.MAX_VALUE,
                            Int.MAX_VALUE
                        )
                        hostView.tag = widgetBindTag
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(widgetAspectRatio)
                    .heightIn(min = minWidgetHeight)
                    .graphicsLayer {
                        shape = RoundedCornerShape(24.dp)
                        clip = true
                    }
            )
        } else {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (isPressed) 0.988f else 1f,
                spring(stiffness = 860f, dampingRatio = 0.78f),
                label = "widget_unavailable_scale"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(widgetAspectRatio)
                    .heightIn(min = minWidgetHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1F2937))
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            if (onLongPress != null) {
                                vibrateHaptic(context)
                                onLongPress()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "小组件不可用",
                    color = WatchColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WidgetActionOverlay(
    widget: WidgetInfo,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalShell(onDismiss) { dismiss ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(widget.label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            WidgetActionRow(
                text = "上移",
                enabled = canMoveUp,
                onClick = {
                    onMoveUp()
                    dismiss()
                }
            )
            Spacer(Modifier.height(8.dp))
            WidgetActionRow(
                text = "下移",
                enabled = canMoveDown,
                onClick = {
                    onMoveDown()
                    dismiss()
                }
            )
            Spacer(Modifier.height(8.dp))
            WidgetActionRow(
                text = "移除",
                enabled = true,
                textColor = Color(0xFFFF6B6B),
                onClick = {
                    onRemove()
                    dismiss()
                }
            )
        }
    }
}

@Composable
private fun SectionActionOverlay(
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalShell(onDismiss) { dismiss ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            WidgetActionRow(
                text = "上移",
                enabled = canMoveUp,
                onClick = {
                    onMoveUp()
                    dismiss()
                }
            )
            Spacer(Modifier.height(8.dp))
            WidgetActionRow(
                text = "下移",
                enabled = canMoveDown,
                onClick = {
                    onMoveDown()
                    dismiss()
                }
            )
            Spacer(Modifier.height(8.dp))
            WidgetActionRow(
                text = "关闭",
                enabled = true,
                textColor = Color(0xFFFF6B6B),
                onClick = {
                    onDisable()
                    dismiss()
                }
            )
        }
    }
}

@Composable
private fun WidgetActionRow(
    text: String,
    enabled: Boolean,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    val backgroundColor = if (enabled) Color(0xFF2C2C2E) else Color(0xFF232326)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else WatchColors.TextTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SideScreenSectionHeader(
    title: String,
    width: Dp,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
) {
    val launcherStyle = LauncherTheme.style
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .width(width)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = launcherStyle.titleColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(launcherStyle.topBarChipColor)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = launcherStyle.topBarTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun sectionTitle(section: SideScreenSection): String {
    return when (section) {
        SideScreenSection.Widgets -> "小组件"
        SideScreenSection.Notifications -> "通知"
    }
}

@Composable
private fun EmptyWidgetCard(width: Dp) {
    val launcherStyle = LauncherTheme.style
    Box(
        modifier = Modifier
            .width(width)
            .height(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(launcherStyle.cardColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "还没有小组件，点右上角添加",
            color = WatchColors.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable private fun AddBubble(size: Dp = 58.dp, onClick: () -> Unit) {
    val launcherStyle = LauncherTheme.style
    val pressed = rememberPressedState(); val isPressed by pressed
    val scale by animateFloatAsState(if (isPressed) 0.958f else 1f, spring(stiffness = 860f, dampingRatio = 0.72f), label = "side_add")
    Box(Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(launcherStyle.topBarChipColor).instantPressGesture(pressed, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Add, null, tint = launcherStyle.topBarTextColor, modifier = Modifier.size(24.dp))
    }
}

@Composable private fun PreviewInfoCard(title: String, subtitle: String, onClick: () -> Unit) {
    val fake = NotificationEntryUi("info", "", null, title, title, subtitle, 0L, null, false, false, false, false)
    PreviewPill(fake, onClick)
}

@Composable private fun PreviewPill(
    entry: NotificationEntryUi,
    onClick: () -> Unit,
    onMeasuredHeight: ((Int) -> Unit)? = null
) {
    val launcherStyle = LauncherTheme.style
    val cardColor = if (launcherStyle.cardColor.alpha < 0.35f) {
        if (launcherStyle.titleColor == Color.White) Color(0xFF353535) else Color(0xFFE8E8ED)
    } else {
        launcherStyle.cardColor
    }
    val pressed = rememberPressedState(); val isPressed by pressed
    val scale by animateFloatAsState(if (isPressed) 0.968f else 1f, spring(stiffness = 860f, dampingRatio = 0.72f), label = "preview_scale")
    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onMeasuredHeight?.invoke(coordinates.size.height)
            }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(28.dp))
            .background(cardColor)
            .instantPressGesture(pressed, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotificationIcon(entry.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title.ifBlank { entry.appLabel }, color = launcherStyle.titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(entry.text.ifBlank { entry.title.ifBlank { entry.appLabel } }, color = WatchColors.TextSecondary, fontSize = 13.sp, maxLines = 2)
            }
            if (entry.time > 0L) {
                Spacer(Modifier.width(10.dp))
                Text(formatClockTime(entry.time), color = launcherStyle.bodyColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable private fun StackedPreviewCard(
    entry: NotificationEntryUi,
    hiddenCount: Int,
    stackCardColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val stackStrength by animateFloatAsState(
        targetValue = if (hiddenCount > 0) 1f else 0f,
        animationSpec = spring(stiffness = 620f, dampingRatio = 0.82f),
        label = "preview_stack_strength"
    )
    var frontCardHeight by remember { mutableIntStateOf(72) }
    val density = LocalDensity.current
    val stackCardHeight: Dp = with(density) { frontCardHeight.toDp() }
    Box(Modifier.fillMaxWidth()) {
        repeat(minOf(hiddenCount, 1)) { index ->
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = with(density) { (STACKED_PREVIEW_TRANSLATION_DP * (index + 1)).dp.toPx() } * stackStrength
                        scaleX = 1f - (index + 1) * 0.012f * stackStrength
                        scaleY = 1f - (index + 1) * 0.012f * stackStrength
                        alpha = 0.44f + (0.18f / (index + 1))
                    }
                    .padding(horizontal = STACKED_PREVIEW_HORIZONTAL_INSET_DP.dp)
                    .fillMaxWidth()
                    .height(stackCardHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(notificationStackBackColor(stackCardColor, index))
            )
        }
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            PreviewPill(
                entry = entry,
                onClick = onClick,
                onMeasuredHeight = { heightPx ->
                    if (heightPx > 0 && frontCardHeight != heightPx) {
                        frontCardHeight = heightPx
                    }
                }
            )
            Text("+${hiddenCount}条新消息", color = WatchColors.TextTertiary, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
        }
    }
}

@Composable private fun NotificationIcon(icon: ImageBitmap?) {
    Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFD9D9D9)), contentAlignment = Alignment.Center) {
        if (icon != null) Image(icon, null, modifier = Modifier.fillMaxSize().clip(CircleShape), filterQuality = FilterQuality.Medium, contentScale = ContentScale.Crop)
        else Icon(Icons.Filled.Notifications, null, tint = Color(0xFF2B2B2B), modifier = Modifier.size(24.dp))
    }
}

private fun notificationStackBackColor(base: Color, index: Int): Color {
    return when {
        base != Color.Unspecified && base.alpha > 0f -> base.copy(alpha = (0.40f - index * 0.08f).coerceIn(0.18f, 0.42f))
        index == 0 -> Color(0xFF404040)
        else -> Color(0xFF2E2E2E)
    }
}

@Composable
private fun ShortcutPickerOverlay(
    apps: List<AppInfo>,
    onSelect: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val appPickerItems = remember(apps) {
        apps.map { app ->
            ShortcutPickerItem(
                componentKey = app.componentKey,
                label = app.label,
                packageName = app.packageName,
                icon = app.cachedIcon,
                source = app
            )
        }
    }
    val listState = rememberLazyListState()
    ModalShell(onDismiss) { dismiss ->
        Column(Modifier.fillMaxWidth(0.82f).heightIn(max = 420.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E1E1E)).padding(vertical = 12.dp)) {
            Text("添加快捷启动", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = appPickerItems,
                    key = { it.componentKey },
                    contentType = { "shortcut_picker_app" }
                ) { app ->
                    ShortcutPickerRow(
                        item = app,
                        onClick = {
                            onSelect(app.source)
                            dismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerOverlay(
    apps: List<AppInfo>,
    widgetRepository: WidgetRepository,
    onSelectWidget: (WidgetInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val widgetPickerItems = remember(widgetRepository) { widgetRepository.getAllWidgets() }
    val launcherAppsByPackage = remember(apps) {
        apps.filterNot { it.isBuiltInSettingsEntry }
            .groupBy { it.packageName }
            .mapValues { it.value.minByOrNull { app -> app.label.length } ?: it.value.first() }
    }
    val pickerApps = remember(widgetPickerItems, launcherAppsByPackage) {
        widgetPickerItems
            .groupBy { it.packageName }
            .map { (packageName, widgets) ->
                val launcherApp = launcherAppsByPackage[packageName]
                WidgetPickerApp(
                    packageName = packageName,
                    label = launcherApp?.label ?: packageName.substringAfterLast('.'),
                    icon = launcherApp?.iconForDisplay(useTwoTone = false, blurred = false),
                    widgets = widgets.sortedBy { it.label.lowercase(Locale.getDefault()) }
                )
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    val selectedApp = selectedPackage?.let { selected ->
        pickerApps.firstOrNull { it.packageName == selected }
    }
    val listState = rememberLazyListState()
    ModalShell(onDismiss) { dismiss ->
        Column(Modifier.fillMaxWidth(0.82f).heightIn(max = 420.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1E1E1E)).padding(vertical = 12.dp)) {
            Text(
                selectedApp?.label ?: "添加小组件",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (widgetPickerItems.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("未找到可用的小组件", color = WatchColors.TextTertiary, fontSize = 14.sp)
                        }
                    }
                } else if (selectedApp == null) {
                    items(
                        items = pickerApps,
                        key = { it.packageName },
                        contentType = { "widget_app_picker" }
                    ) { app ->
                        WidgetPickerAppRow(
                            app = app,
                            onClick = {
                                selectedPackage = app.packageName
                            }
                        )
                    }
                } else {
                    item("back_to_widget_apps") {
                        WidgetPickerBackRow(onClick = { selectedPackage = null })
                    }
                    items(
                        items = selectedApp.widgets,
                        key = { it.widgetKey },
                        contentType = { "widget_picker" }
                    ) { widget ->
                        WidgetPickerRow(
                            widget = widget,
                            onClick = {
                                onSelectWidget(widget)
                                dismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerBackRow(onClick: () -> Unit) {
    val pressed = rememberPressedState()
    val isPressed by pressed
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) Color(0xFF303030) else Color.Transparent)
            .instantPressGesture(pressed, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("返回应用列表", color = WatchColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WidgetPickerAppRow(
    app: WidgetPickerApp,
    onClick: () -> Unit
) {
    val pressed = rememberPressedState()
    val isPressed by pressed
    val scale by animateFloatAsState(
        if (isPressed) 0.985f else 1f,
        spring(stiffness = 880f, dampingRatio = 0.8f),
        label = "widget_app_picker_row"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (isPressed) Color(0xFF303030) else Color.Transparent)
            .instantPressGesture(pressed, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(app.label.take(1), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("${app.widgets.size} 个小组件", color = WatchColors.TextTertiary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun WidgetPickerRow(
    widget: WidgetInfo,
    onClick: () -> Unit
) {
    val pressed = rememberPressedState()
    val isPressed by pressed
    val scale by animateFloatAsState(
        if (isPressed) 0.985f else 1f,
        spring(stiffness = 880f, dampingRatio = 0.8f),
        label = "widget_picker_row"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (isPressed) Color(0xFF303030) else Color.Transparent)
            .instantPressGesture(pressed, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF007AFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(widget.label.take(1), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(widget.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(widget.packageName, color = WatchColors.TextTertiary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TypeTab(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val pressed = rememberPressedState()
    val isPressed by pressed
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF007AFF) else Color.Transparent)
            .instantPressGesture(pressed, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else WatchColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ShortcutPickerRow(
    item: ShortcutPickerItem,
    onClick: () -> Unit
) {
    val pressed = rememberPressedState()
    val isPressed by pressed
    val scale by animateFloatAsState(
        if (isPressed) 0.985f else 1f,
        spring(stiffness = 880f, dampingRatio = 0.8f),
        label = "picker_row"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (isPressed) Color(0xFF303030) else Color.Transparent)
            .instantPressGesture(pressed, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            item.icon,
            null,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(item.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(item.packageName, color = WatchColors.TextTertiary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable private fun RemoveOverlay(app: AppInfo?, widget: WidgetInfo?, onRemove: () -> Unit, onDismiss: () -> Unit) {
    ModalShell(onDismiss) { dismiss ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.74f).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2C2C2E))) {
                Box(Modifier.fillMaxWidth().clickable { onRemove(); dismiss() }.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("移除", color = Color(0xFFFF453A), fontSize = 15.sp, fontWeight = FontWeight.W500)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (app != null) {
                Image(app.cachedIcon, null, modifier = Modifier.size(88.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.height(6.dp))
                Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W600)
            } else if (widget != null) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(widget.label.take(1), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(widget.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable private fun ModalShell(onDismissRequest: () -> Unit, content: @Composable (dismiss: () -> Unit) -> Unit) {
    val dismissInteraction = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    fun dismiss() { visible = false }
    LaunchedEffect(Unit) { visible = true }
    BackHandler { dismiss() }
    LaunchedEffect(visible) { if (!visible) { delay(220); onDismissRequest() } }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, spring(stiffness = 720f, dampingRatio = 0.85f), label = "modal_alpha")
    val scale by animateFloatAsState(if (visible) 1f else 0.84f, spring(stiffness = 700f, dampingRatio = 0.8f), label = "modal_scale")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = dismissInteraction) { dismiss() }
        )
        Box(
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content(::dismiss)
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitLongPressOrRelease(
    pointerId: PointerId,
    downPosition: Offset,
    timeoutMillis: Long
): PressHoldResult {
    val result = withTimeoutOrNull(timeoutMillis) {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId }
                ?: return@withTimeoutOrNull PressHoldResult.Cancelled
            if (!change.pressed) {
                return@withTimeoutOrNull PressHoldResult.Released
            }
            if ((change.position - downPosition).getDistance() > viewConfiguration.touchSlop) {
                return@withTimeoutOrNull PressHoldResult.Cancelled
            }
        }
        @Suppress("UNREACHABLE_CODE")
        PressHoldResult.Cancelled
    }
    if (result != null) return result
    val current = currentEvent.changes.firstOrNull { it.id == pointerId && it.pressed }
    return current?.let(PressHoldResult::LongPress) ?: PressHoldResult.Cancelled
}

private suspend fun AwaitPointerEventScope.awaitPrimaryDown(): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.pressed }
        if (change != null) return change
    }
}

@Composable private fun rememberClockSnapshot(): SideScreenClockSnapshot {
    var snapshot by remember { mutableStateOf(SideScreenClockSnapshot("--:--", "--")) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date(); val locale = Locale.getDefault()
            snapshot = SideScreenClockSnapshot(SimpleDateFormat("HH:mm", locale).format(now), if (locale.language.startsWith("zh")) SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(now) else SimpleDateFormat("MMM d, EEEE", locale).format(now))
            delay(1000)
        }
    }
    return snapshot
}

@Composable private fun rememberBatterySnapshot(): BatterySnapshot {
    val context = LocalContext.current
    var level by remember(context) { mutableIntStateOf(0) }
    var charging by remember(context) { mutableStateOf(false) }
    DisposableEffect(context) {
        fun readLevel(): Int = (context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0).coerceIn(0, 100)
        fun readCharging(intent: Intent?): Boolean {
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }
        level = readLevel()
        val stickyIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        charging = readCharging(stickyIntent)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                level = readLevel()
                charging = readCharging(intent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return BatterySnapshot(level = level, charging = charging)
}

@Composable
private fun WatchBatteryAndStepsPill(
    level: Int,
    charging: Boolean,
    steps: Int?,
    showSteps: Boolean,
    modifier: Modifier = Modifier
) {
    val launcherStyle = LauncherTheme.style
    val formattedSteps = remember(steps) {
        steps?.let {
            NumberFormat.getIntegerInstance(Locale.getDefault()).format(it)
        } ?: "--"
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WatchBatteryPill(
            level = level,
            charging = charging,
            sizeScale = 1.3f
        )
        if (showSteps) {
            Spacer(Modifier.width(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = launcherStyle.bodyColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = formattedSteps,
                    color = launcherStyle.bodyColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun rememberActivityRecognitionPermission(showSteps: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    val context = LocalContext.current
    var granted by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { allowed ->
        granted = allowed
    }
    LaunchedEffect(showSteps, context) {
        val latest = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        granted = latest
        if (showSteps && !latest) {
            launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }
    return granted
}

@Composable
private fun rememberStepSnapshot(
    showSteps: Boolean,
    permissionGranted: Boolean
): StepSnapshot {
    val context = LocalContext.current
    var steps by remember(showSteps, permissionGranted) { mutableStateOf<Int?>(null) }
    DisposableEffect(context, showSteps, permissionGranted) {
        if (!showSteps || !permissionGranted) {
            steps = null
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val stepCounter = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensorManager == null || stepCounter == null) {
            steps = null
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val value = event?.values?.firstOrNull() ?: return
                steps = value.toInt().coerceAtLeast(0)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = runCatching {
            sensorManager.registerListener(listener, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
        }.getOrDefault(false)
        if (!registered) {
            steps = null
        }
        onDispose {
            runCatching { sensorManager.unregisterListener(listener) }
        }
    }
    return StepSnapshot(steps = steps)
}
private fun buildPreviewRows(groups: List<NotificationGroupUi>, maxRows: Int): List<PreviewRow> {
    val safeMaxRows = maxRows.coerceAtLeast(0)
    if (safeMaxRows == 0) return emptyList()

    val totalNotificationCount = groups.sumOf { it.entries.size }
    val distinctGroupCount = groups.size

    if (distinctGroupCount > 1 && totalNotificationCount > 1) {
        val leadEntry = groups.firstOrNull()?.entries?.firstOrNull() ?: return emptyList()
        return listOf(
            PreviewRow.Aggregate(
                leadEntry = leadEntry,
                hiddenCount = (totalNotificationCount - 1).coerceAtLeast(0)
            )
        )
    }

    return groups.take(safeMaxRows).map { group ->
        if (group.entries.size > 1) {
            PreviewRow.Group(group, listOf(group.entries.first()), group.entries.size - 1)
        } else {
            PreviewRow.Group(group, group.entries, 0)
        }
    }
}

private fun buildWidgetBindOptions(providerInfo: AppWidgetProviderInfo): Bundle {
    return Bundle().apply {
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            providerInfo.minWidth.coerceAtLeast(120)
        )
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
            providerInfo.minHeight.coerceAtLeast(120)
        )
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            providerInfo.minResizeWidth.takeIf { it > 0 } ?: providerInfo.minWidth.coerceAtLeast(120)
        )
        putInt(
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            providerInfo.minResizeHeight.takeIf { it > 0 } ?: providerInfo.minHeight.coerceAtLeast(120)
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun formatClockTime(timestamp: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetBubble(
    widget: WidgetInfo,
    size: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.958f else 1f,
        spring(stiffness = 860f, dampingRatio = 0.72f),
        label = "widget_bubble"
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color(0xFF007AFF))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = widget.label.take(2),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
