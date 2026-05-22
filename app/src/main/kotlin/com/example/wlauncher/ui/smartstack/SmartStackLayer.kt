package com.flue.launcher.ui.smartstack

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.ui.common.instantPressGesture
import com.flue.launcher.ui.common.rememberPressedState
import com.flue.launcher.ui.drawer.AppBubble
import com.flue.launcher.ui.notification.NotificationEntryUi
import com.flue.launcher.ui.notification.NotificationGroupUi
import com.flue.launcher.ui.notification.NotificationRevealTarget
import com.flue.launcher.ui.notification.SwipeRevealDeleteContainer
import com.flue.launcher.ui.theme.WatchColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
private sealed interface SideScreenModalState {
    data object None : SideScreenModalState
    data class Picker(val slotIndex: Int) : SideScreenModalState
    data class Remove(val slotIndex: Int) : SideScreenModalState
}

private data class SideScreenClockSnapshot(val time: String, val date: String)
private data class BatterySnapshot(val level: Int = 0)
private data class ShortcutPickerItem(
    val componentKey: String,
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
    val source: AppInfo
)
private sealed interface PreviewRow {
    data class Group(val group: NotificationGroupUi, val entries: List<NotificationEntryUi>, val hiddenCount: Int) : PreviewRow
    data class Aggregate(val leadEntry: NotificationEntryUi, val hiddenCount: Int) : PreviewRow
}

@Composable
fun SmartStackLayer(
    apps: List<AppInfo>,
    sideScreenShortcuts: List<String?>,
    previewGroups: List<NotificationGroupUi>,
    notificationsEnabled: Boolean,
    notificationAccessGranted: Boolean,
    notificationsSceneActive: Boolean,
    notificationTransitionProgress: Float,
    revealedNotificationTarget: NotificationRevealTarget?,
    onRevealTargetChange: (NotificationRevealTarget?) -> Unit,
    onOpenNotifications: () -> Unit,
    onNotificationTransitionProgressChange: (Float) -> Unit,
    onNotificationTransitionRelease: (Boolean) -> Unit,
    onLaunchApp: (AppInfo, Offset) -> Unit,
    onSetShortcut: (Int, String?) -> Unit,
    onRemoveShortcut: (Int) -> Unit,
    onDismissGroup: (String) -> Unit,
    onDismissNotification: (String) -> Unit,
    onDismissToFace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val clock = rememberClockSnapshot()
    val battery = rememberBatterySnapshot()
    val slotCenters = remember { mutableStateMapOf<Int, Offset>() }
    val visibleShortcutCount = if (notificationsEnabled) 6 else 9
    val notificationDragRangePx = remember(configuration.screenHeightDp, density) {
        maxOf(
            with(density) { configuration.screenHeightDp.dp.toPx() } * SIDE_SCREEN_NOTIFICATION_DRAG_RANGE_RATIO,
            SIDE_SCREEN_NOTIFICATION_DRAG_RANGE_MIN
        )
    }
    val notificationReleaseThresholdPx = notificationDragRangePx * SIDE_SCREEN_NOTIFICATION_RELEASE_PROGRESS
    val shortcuts = remember(apps, sideScreenShortcuts, visibleShortcutCount) {
        sideScreenShortcuts
            .take(visibleShortcutCount)
            .map { key -> key?.let { k -> apps.firstOrNull { it.componentKey == k } } }
    }
    var modalState by remember { mutableStateOf<SideScreenModalState>(SideScreenModalState.None) }
    var dragDx by remember { mutableFloatStateOf(0f) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    var dragVelocityY by remember { mutableFloatStateOf(0f) }
    var lastDragEventUptime by remember { mutableStateOf(0L) }
    var overscrollTarget by remember { mutableFloatStateOf(0f) }
    var transitionInFlight by remember { mutableStateOf(false) }
    val overscrollY by animateFloatAsState(overscrollTarget, spring(stiffness = 440f, dampingRatio = 0.78f), label = "side_overscroll")
    val contentTranslationY = if (notificationsSceneActive || transitionInFlight) 0f else overscrollY

    LaunchedEffect(notificationsSceneActive) {
        overscrollTarget = 0f
        dragDx = 0f
        dragDy = 0f
        dragVelocityY = 0f
        lastDragEventUptime = 0L
        transitionInFlight = false
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(40.dp))
            .background(WatchColors.Black)
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
                        } else {
                            change.consume()
                            val notificationPullActive = notificationsEnabled &&
                                (dragDy < 0f || notificationTransitionProgress > 0f)
                            if (notificationPullActive) {
                                overscrollTarget = 0f
                                onNotificationTransitionProgressChange(
                                    (-dragDy / notificationDragRangePx).coerceIn(0f, 1f)
                                )
                            } else {
                                overscrollTarget = (overscrollTarget + dragAmount.y * 0.35f).coerceIn(-180f, 120f)
                            }
                        }
                    },
                    onDragEnd = {
                        val verticalIntent = abs(dragDy) > abs(dragDx) || abs(dragVelocityY) > SIDE_SCREEN_NOTIFICATION_FLING_VELOCITY
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
                                overscrollTarget = 0f
                                dragDx = 0f
                                dragDy = 0f
                                dragVelocityY = 0f
                                lastDragEventUptime = 0L
                                onNotificationTransitionRelease(false)
                                onRevealTargetChange(null)
                                onDismissToFace()
                            }
                            openNotifications -> {
                                overscrollTarget = 0f
                                dragDx = 0f
                                dragDy = 0f
                                dragVelocityY = 0f
                                lastDragEventUptime = 0L
                                onRevealTargetChange(null)
                                onNotificationTransitionRelease(true)
                            }
                            else -> {
                                overscrollTarget = 0f
                                dragDx = 0f
                                dragDy = 0f
                                dragVelocityY = 0f
                                lastDragEventUptime = 0L
                                onNotificationTransitionRelease(false)
                            }
                        }
                    },
                    onDragCancel = {
                        overscrollTarget = 0f
                        dragDx = 0f
                        dragDy = 0f
                        dragVelocityY = 0f
                        lastDragEventUptime = 0L
                        onNotificationTransitionRelease(false)
                    }
                )
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val contentWidth = maxWidth * SIDE_SCREEN_CONTENT_WIDTH_RATIO
        val quickHeight = if (notificationsEnabled) {
            (contentWidth * 0.58f).coerceAtLeast(152.dp)
        } else {
            (contentWidth * 0.88f).coerceAtLeast(226.dp)
        }
        val previewCapacity = remember(contentWidth, maxHeight, notificationsEnabled) {
            if (!notificationsEnabled) return@remember 0
            val headerPx = with(density) { 112.dp.toPx() }
            val footerPx = with(density) { 38.dp.toPx() }
            val quickPx = with(density) { quickHeight.toPx() }
            val rowPx = with(density) { 76.dp.toPx() + 10.dp.toPx() }
            floor(((heightPx - headerPx - footerPx - quickPx).coerceAtLeast(rowPx)) / rowPx).toInt().coerceAtLeast(1)
        }
        val previewRows = remember(previewGroups, previewCapacity) { buildPreviewRows(previewGroups, previewCapacity) }

        Column(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationY = contentTranslationY
            }.padding(top = 10.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(clock.time, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(clock.date, color = WatchColors.TextSecondary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            QuickPanel(contentWidth, quickHeight, shortcuts, slotCenters,
                onAdd = { onRevealTargetChange(null); modalState = SideScreenModalState.Picker(it) },
                onLongPress = { onRevealTargetChange(null); modalState = SideScreenModalState.Remove(it) },
                onClick = { slot, app ->
                    val center = slotCenters[slot] ?: Offset(widthPx / 2f, heightPx / 2f)
                    onRevealTargetChange(null)
                    onLaunchApp(app, Offset(center.x / widthPx, center.y / heightPx))
                }
            )
            if (notificationsEnabled) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .width(contentWidth)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        !notificationAccessGranted -> PreviewInfoCard("开启通知访问", "副一屏上滑进入通知中心后完成授权", onOpenNotifications)
                        previewRows.isEmpty() -> PreviewInfoCard("暂无通知", "副一屏上滑可打开通知中心", onOpenNotifications)
                        else -> previewRows.forEach { preview ->
                            when (preview) {
                                is PreviewRow.Aggregate -> {
                                    StackedPreviewCard(preview.leadEntry, preview.hiddenCount, onOpenNotifications)
                                }
                                is PreviewRow.Group -> {
                                    if (preview.hiddenCount > 0) {
                                        SwipeRevealDeleteContainer(
                                            target = NotificationRevealTarget.Group(preview.group.packageName),
                                            revealedTarget = revealedNotificationTarget,
                                            onRevealTargetChange = onRevealTargetChange,
                                            enabled = preview.group.entries.any(NotificationEntryUi::isClearable),
                                            onDelete = { onDismissGroup(preview.group.packageName) },
                                            actionHeight = 72.dp
                                        ) { StackedPreviewCard(preview.entries.first(), preview.hiddenCount, onOpenNotifications) }
                                    } else {
                                        preview.entries.forEach { entry ->
                                            SwipeRevealDeleteContainer(
                                                target = NotificationRevealTarget.Entry(entry.key),
                                                revealedTarget = revealedNotificationTarget,
                                                onRevealTargetChange = onRevealTargetChange,
                                                enabled = entry.isClearable,
                                                onDelete = { onDismissNotification(entry.key) }
                                            ) { PreviewPill(entry, onOpenNotifications) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            BatteryPill(battery.level, Modifier.padding(bottom = 4.dp))
        }
    }

    when (val state = modalState) {
        is SideScreenModalState.Picker -> PickerOverlay(apps, onSelect = { onSetShortcut(state.slotIndex, it.componentKey) }, onDismiss = { modalState = SideScreenModalState.None })
        is SideScreenModalState.Remove -> shortcuts.getOrNull(state.slotIndex)?.let { app ->
            RemoveOverlay(app, onRemove = { onRemoveShortcut(state.slotIndex) }, onDismiss = { modalState = SideScreenModalState.None })
        } ?: LaunchedEffect(state) { modalState = SideScreenModalState.None }
        SideScreenModalState.None -> Unit
    }
}

@Composable private fun QuickPanel(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    apps: List<AppInfo?>,
    slotCenters: MutableMap<Int, Offset>,
    onAdd: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onClick: (Int, AppInfo) -> Unit
) {
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(28.dp)).background(Color(0xFF353535)).padding(14.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            apps.chunked(3).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEachIndexed { colIndex, app ->
                        val slot = rowIndex * 3 + colIndex
                        Box(Modifier.onGloballyPositioned { c -> val p = c.positionInRoot(); slotCenters[slot] = Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f) }, contentAlignment = Alignment.Center) {
                            if (app == null) {
                                AddBubble { onAdd(slot) }
                            } else {
                                AppBubble(
                                    app.cachedIcon,
                                    58.dp,
                                    onClick = {
                                        onClick(slot, app)
                                    },
                                    onLongClick = { onLongPress(slot) }
                                )
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.size(58.dp)) }
                }
            }
        }
    }
}

@Composable private fun AddBubble(onClick: () -> Unit) {
    val pressed = rememberPressedState(); val isPressed by pressed
    val scale by animateFloatAsState(if (isPressed) 0.958f else 1f, spring(stiffness = 860f, dampingRatio = 0.72f), label = "side_add")
    Box(Modifier.size(58.dp).graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).instantPressGesture(pressed, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Add, null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(24.dp))
    }
}

@Composable private fun PreviewInfoCard(title: String, subtitle: String, onClick: () -> Unit) {
    val fake = NotificationEntryUi("info", "", null, title, title, subtitle, 0L, null, false, false, false, false)
    PreviewPill(fake, onClick)
}

@Composable private fun PreviewPill(entry: NotificationEntryUi, onClick: () -> Unit) {
    val pressed = rememberPressedState(); val isPressed by pressed
    val scale by animateFloatAsState(if (isPressed) 0.968f else 1f, spring(stiffness = 860f, dampingRatio = 0.72f), label = "preview_scale")
    Box(Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(28.dp)).background(Color(0xFF353535)).instantPressGesture(pressed, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NotificationIcon(entry.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title.ifBlank { entry.appLabel }, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(entry.text.ifBlank { entry.title.ifBlank { entry.appLabel } }, color = WatchColors.TextSecondary, fontSize = 13.sp, maxLines = 2)
            }
            if (entry.time > 0L) {
                Spacer(Modifier.width(10.dp))
                Text(formatClockTime(entry.time), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable private fun StackedPreviewCard(entry: NotificationEntryUi, hiddenCount: Int, onClick: () -> Unit) {
    val stackStrength by animateFloatAsState(
        targetValue = if (hiddenCount > 0) 1f else 0f,
        animationSpec = spring(stiffness = 620f, dampingRatio = 0.82f),
        label = "preview_stack_strength"
    )
    Box(Modifier.fillMaxWidth()) {
        repeat(minOf(hiddenCount, 1)) { index ->
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (index + 1) * 40f * stackStrength
                        scaleX = 1f - (index + 1) * 0.012f * stackStrength
                        scaleY = 1f - (index + 1) * 0.012f * stackStrength
                        alpha = 0.44f + (0.18f / (index + 1))
                    }
                    .padding(start = 4.dp, end = 4.dp)
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (index == 0) Color(0xFF404040) else Color(0xFF2E2E2E))
            )
        }
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            PreviewPill(entry, onClick)
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

@Composable private fun PickerOverlay(apps: List<AppInfo>, onSelect: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    val pickerItems = remember(apps) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = pickerItems,
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

@Composable private fun RemoveOverlay(app: AppInfo, onRemove: () -> Unit, onDismiss: () -> Unit) {
    ModalShell(onDismiss) { dismiss ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.74f).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF2C2C2E))) {
                Box(Modifier.fillMaxWidth().clickable { onRemove(); dismiss() }.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("移除", color = Color(0xFFFF453A), fontSize = 15.sp, fontWeight = FontWeight.W500)
                }
            }
            Spacer(Modifier.height(16.dp))
            Image(app.cachedIcon, null, modifier = Modifier.size(88.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(6.dp))
            Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W600)
        }
    }
}

@Composable private fun ModalShell(onDismissRequest: () -> Unit, content: @Composable (dismiss: () -> Unit) -> Unit) {
    val dismissInteraction = remember { MutableInteractionSource() }
    val blockInteraction = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    fun dismiss() { visible = false }
    LaunchedEffect(Unit) { visible = true }
    BackHandler { dismiss() }
    LaunchedEffect(visible) { if (!visible) { delay(220); onDismissRequest() } }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, spring(stiffness = 720f, dampingRatio = 0.85f), label = "modal_alpha")
    val scale by animateFloatAsState(if (visible) 1f else 0.84f, spring(stiffness = 700f, dampingRatio = 0.8f), label = "modal_scale")
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(Color.Black.copy(alpha = 0.72f)).clickable(indication = null, interactionSource = dismissInteraction) { dismiss() }, contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clickable(indication = null, interactionSource = blockInteraction) {}) { content(::dismiss) }
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
    DisposableEffect(context) {
        fun readLevel(): Int = (context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0).coerceIn(0, 100)
        level = readLevel()
        val receiver = object : BroadcastReceiver() { override fun onReceive(context: android.content.Context?, intent: Intent?) { level = readLevel() } }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return BatterySnapshot(level)
}

@Composable private fun BatteryPill(level: Int, modifier: Modifier = Modifier) {
    val tint = when { level <= 10 -> WatchColors.ActiveRed; level <= 20 -> WatchColors.ActiveOrange; else -> WatchColors.ActiveGreen }
    Box(modifier.width(56.dp).height(24.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF141414)).padding(2.dp)) {
        Box(Modifier.fillMaxWidth((level / 100f).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.24f)))
        Text("${level}%", color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
    }
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

private fun formatClockTime(timestamp: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
