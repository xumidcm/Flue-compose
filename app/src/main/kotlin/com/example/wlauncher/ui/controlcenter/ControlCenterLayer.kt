package com.flue.launcher.ui.controlcenter

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings as SettingsIcon
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.mediarouter.app.SystemOutputSwitcherDialogController
import com.flue.launcher.FlueApplication
import com.flue.launcher.service.WLauncherNotificationListener
import com.flue.launcher.ui.common.WatchBatteryPill
import com.flue.launcher.ui.common.rememberPressedState
import com.flue.launcher.ui.theme.LauncherTheme
import com.flue.launcher.ui.theme.WatchColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val CONTROL_CENTER_OVERSCROLL_LIMIT = 96f
private const val CONTROL_CENTER_OVERSCROLL_RESISTANCE = 0.35f
private const val CONTROL_CENTER_MIN_PRESS_MS = 140L
private const val MEDIA_PROGRESS_COLLAPSED_HEIGHT_DP = 4f
private const val MEDIA_PROGRESS_EXPANDED_HEIGHT_DP = 8f
private const val MEDIA_PROGRESS_HIT_HEIGHT_DP = 24f
private const val MEDIA_PROGRESS_SEEK_SYNC_HOLD_MS = 1200L
private const val MUSIC_TEXT_MARQUEE_INITIAL_DELAY_MS = 650L
private const val MUSIC_TEXT_MARQUEE_SPEED_DP_PER_SECOND = 34f
private const val MUSIC_TEXT_FADE_EDGE_DP = 18f
private const val MUSIC_TEXT_FADE_TRANSITION_MS = 80
private const val MUSIC_TEXT_MARQUEE_SIDE_PADDING_DP = 8f

private data class ControlCenterMediaState(
    val controller: MediaController? = null,
    val title: String = "未在播放",
    val artist: String = "打开音乐应用后会显示控制",
    val artwork: ImageBitmap? = null,
    val appIcon: ImageBitmap? = null,
    val customActions: List<ControlCenterMediaAction> = emptyList(),
    val artworkColor: Color = Color(0xFF2F3B45),
    val playing: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val appPackage: String? = null
)

private data class ControlCenterMediaAction(
    val action: String,
    val title: String,
    val extras: Bundle? = null,
    val icon: ImageBitmap? = null
)

private data class ConnectivitySnapshot(
    val wifiActive: Boolean = false,
    val cellularActive: Boolean = false
)

private data class BatteryStatusSnapshot(
    val level: Int = 0,
    val charging: Boolean = false
)

@Composable
fun ControlCenterLayer(
    leftSafeInsetPercent: Int,
    showMusicControls: Boolean,
    notificationAccessGranted: Boolean,
    musicTextSwitchAnimation: String = MusicTextSwitchAnimations.DEFAULT_ID,
    onDismissToFace: () -> Unit,
    showPowerButton: Boolean = false,
    onPowerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcherStyle = LauncherTheme.style
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    var mediaVolume by remember(context) {
        mutableFloatStateOf(readMusicVolume(audioManager))
    }
    var pendingRequestedMediaVolume by remember(context) { mutableStateOf<Float?>(null) }
    var brightness by remember(context) {
        mutableFloatStateOf(readBrightness(context))
    }
    var batteryStatus by remember(context) { mutableStateOf(readBatteryStatus(context)) }
    var timeText by remember { mutableStateOf("--:--") }
    var connectivity by remember(context) { mutableStateOf(readConnectivitySnapshot(context)) }
    var bluetoothEnabled by remember(context) { mutableStateOf(isBluetoothLikelyEnabled(context)) }
    var dndEnabled by remember(context) { mutableStateOf(isDndEnabled(context)) }
    val mediaState = rememberMediaState(context)
    val verticalScrollState = rememberScrollState()
    val toggleScrollState = rememberScrollState()
    val verticalOverscroll = remember { Animatable(0f) }
    val toggleOverscroll = remember { Animatable(0f) }
    val overscrollScope = androidx.compose.runtime.rememberCoroutineScope()
    var bottomDismissArmed by remember { mutableStateOf(false) }
    var fullScreenDismissTriggered by remember { mutableStateOf(false) }
    var controlCenterGestureLocked by remember { mutableStateOf(false) }
    val lockControlCenterGesture: (Boolean) -> Unit = remember {
        { locked ->
            controlCenterGestureLocked = locked
            if (locked) {
                bottomDismissArmed = false
                fullScreenDismissTriggered = false
            }
        }
    }
    val verticalElasticScroll = remember(verticalScrollState, controlCenterGestureLocked) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source != NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero
                if (controlCenterGestureLocked) return androidx.compose.ui.geometry.Offset(0f, available.y)
                val atTop = verticalScrollState.value <= 0
                val atBottom = verticalScrollState.maxValue > 0 &&
                    verticalScrollState.value >= verticalScrollState.maxValue
                if (available.y > 0f && atTop) {
                    overscrollScope.launch {
                        verticalOverscroll.snapTo(
                            (verticalOverscroll.value + available.y * CONTROL_CENTER_OVERSCROLL_RESISTANCE)
                                .coerceAtMost(CONTROL_CENTER_OVERSCROLL_LIMIT)
                        )
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                if (available.y < 0f && atBottom) {
                    if (bottomDismissArmed) {
                        onDismissToFace()
                    } else {
                        overscrollScope.launch {
                            verticalOverscroll.snapTo(
                                (verticalOverscroll.value + available.y * CONTROL_CENTER_OVERSCROLL_RESISTANCE)
                                    .coerceAtLeast(-CONTROL_CENTER_OVERSCROLL_LIMIT)
                            )
                        }
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                if (verticalOverscroll.value > 0f && available.y < 0f) {
                    overscrollScope.launch {
                        verticalOverscroll.snapTo((verticalOverscroll.value + available.y).coerceAtLeast(0f))
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                if (verticalOverscroll.value < 0f && available.y > 0f) {
                    overscrollScope.launch {
                        verticalOverscroll.snapTo((verticalOverscroll.value + available.y).coerceAtMost(0f))
                    }
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (controlCenterGestureLocked) return available
                if (verticalOverscroll.value != 0f) {
                    verticalOverscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    val trackDismissPointer = Modifier.pointerInput(verticalScrollState, controlCenterGestureLocked) {
        awaitPointerEventScope {
            var pressed = false
            while (true) {
                val event = awaitPointerEvent()
                if (controlCenterGestureLocked) {
                    bottomDismissArmed = false
                    pressed = event.changes.any { it.pressed }
                    continue
                }
                val isPressed = event.changes.any { it.pressed }
                if (!pressed && isPressed) {
                    bottomDismissArmed = verticalScrollState.maxValue > 0 &&
                        verticalScrollState.value >= verticalScrollState.maxValue
                } else if (pressed && !isPressed) {
                    bottomDismissArmed = false
                }
                pressed = isPressed
            }
        }
    }
    val toggleElasticScroll = remember(toggleScrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source != NestedScrollSource.UserInput) return androidx.compose.ui.geometry.Offset.Zero
                val atStart = toggleScrollState.value <= 0
                val atEnd = toggleScrollState.value >= toggleScrollState.maxValue
                if (available.x > 0f && atStart) {
                    overscrollScope.launch {
                        toggleOverscroll.snapTo(
                            (toggleOverscroll.value + available.x * CONTROL_CENTER_OVERSCROLL_RESISTANCE)
                                .coerceAtMost(CONTROL_CENTER_OVERSCROLL_LIMIT)
                        )
                    }
                    return androidx.compose.ui.geometry.Offset(available.x, 0f)
                }
                if (available.x < 0f && atEnd) {
                    overscrollScope.launch {
                        toggleOverscroll.snapTo(
                            (toggleOverscroll.value + available.x * CONTROL_CENTER_OVERSCROLL_RESISTANCE)
                                .coerceAtLeast(-CONTROL_CENTER_OVERSCROLL_LIMIT)
                        )
                    }
                    return androidx.compose.ui.geometry.Offset(available.x, 0f)
                }
                if (toggleOverscroll.value > 0f && available.x < 0f) {
                    overscrollScope.launch {
                        toggleOverscroll.snapTo((toggleOverscroll.value + available.x).coerceAtLeast(0f))
                    }
                    return androidx.compose.ui.geometry.Offset(available.x, 0f)
                }
                if (toggleOverscroll.value < 0f && available.x > 0f) {
                    overscrollScope.launch {
                        toggleOverscroll.snapTo((toggleOverscroll.value + available.x).coerceAtMost(0f))
                    }
                    return androidx.compose.ui.geometry.Offset(available.x, 0f)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (toggleOverscroll.value != 0f) {
                    toggleOverscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    val internetPanelLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
        connectivity = readConnectivitySnapshot(context)
    }
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(StartActivityForResult()) {
        bluetoothEnabled = isBluetoothLikelyEnabled(context)
    }
    var pendingBluetoothEnable by remember(context) { mutableStateOf(false) }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted && pendingBluetoothEnable) {
            requestBluetoothEnable(bluetoothEnableLauncher)
        }
        pendingBluetoothEnable = false
        bluetoothEnabled = isBluetoothLikelyEnabled(context)
    }

    LaunchedEffect(context) {
        while (true) {
            batteryStatus = readBatteryStatus(context)
            timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val polledMediaVolume = readMusicVolume(audioManager)
            val requestedMediaVolume = pendingRequestedMediaVolume
            when {
                requestedMediaVolume == null -> mediaVolume = polledMediaVolume
                abs(polledMediaVolume - quantizeMusicVolume(audioManager, requestedMediaVolume)) > 0.001f -> {
                    pendingRequestedMediaVolume = null
                    mediaVolume = polledMediaVolume
                }
            }
            brightness = readBrightness(context)
            connectivity = readConnectivitySnapshot(context)
            bluetoothEnabled = isBluetoothLikelyEnabled(context)
            dndEnabled = isDndEnabled(context)
            delay(250)
        }
    }
    val greetingText = remember(timeText) { controlCenterGreeting() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(40.dp))
            .background(launcherStyle.screenBackground)
    ) {
        val leftSafeInset = maxWidth * (leftSafeInsetPercent.coerceIn(0, 50) / 100f)
        val verticalPadding = 16.dp
        val outerSpacing = if (showMusicControls) 14.dp else 18.dp
        val controlsSpacing = if (showMusicControls) 14.dp else 20.dp
        val footerTopGap = 4.dp
        val toggleRowTopPadding = 6.dp
        val toggleSize = 58.dp
        val toggleIconSize = 26.dp
        val toggleLabelGap = 7.dp
        val sliderHeight = 58.dp
        val sliderHorizontalPadding = 14.dp
        val topStatusHorizontalInset = 20.dp
        val contentHorizontalPadding = 18.dp
        val toggleRowSpacing = 14.dp
        val toggleRowRequiredWidth = toggleSize * 4f + toggleRowSpacing * 3f
        val availableToggleRowWidth = (maxWidth - leftSafeInset - contentHorizontalPadding * 2f).coerceAtLeast(0.dp)
        val toggleRowFits = availableToggleRowWidth >= toggleRowRequiredWidth
        val estimatedControlsHeight = toggleSize +
            controlsSpacing +
            sliderHeight +
            controlsSpacing +
            sliderHeight +
            if (showMusicControls) controlsSpacing + 156.dp else 0.dp
        val estimatedContentHeight = verticalPadding * 2f +
            32.dp +
            outerSpacing +
            estimatedControlsHeight +
            20.dp +
            footerTopGap
        val verticalScrollEnabled = estimatedContentHeight > maxHeight
        LaunchedEffect(verticalScrollEnabled, toggleRowFits) {
            verticalOverscroll.snapTo(0f)
            toggleOverscroll.snapTo(0f)
            fullScreenDismissTriggered = false
            bottomDismissArmed = false
        }
        val verticalElasticModifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = verticalOverscroll.value }
            .padding(start = leftSafeInset)
        val contentModifier = if (verticalScrollEnabled) {
            verticalElasticModifier
                .then(trackDismissPointer)
                .nestedScroll(verticalElasticScroll)
                .verticalScroll(verticalScrollState)
        } else {
            verticalElasticModifier.pointerInput(controlCenterGestureLocked) {
                detectDragGestures(
                    onDragStart = {
                        fullScreenDismissTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        if (controlCenterGestureLocked) {
                            change.consume()
                            return@detectDragGestures
                        }
                        if (abs(dragAmount.y) >= abs(dragAmount.x)) {
                            change.consume()
                            overscrollScope.launch {
                                val next = when {
                                    verticalOverscroll.value > 0f && dragAmount.y < 0f ->
                                        (verticalOverscroll.value + dragAmount.y).coerceAtLeast(0f)
                                    dragAmount.y > 0f ->
                                        (verticalOverscroll.value + dragAmount.y * CONTROL_CENTER_OVERSCROLL_RESISTANCE)
                                            .coerceAtMost(CONTROL_CENTER_OVERSCROLL_LIMIT)
                                    dragAmount.y < 0f -> if (!fullScreenDismissTriggered) {
                                        fullScreenDismissTriggered = true
                                        onDismissToFace()
                                        verticalOverscroll.value
                                    } else {
                                        verticalOverscroll.value
                                    }
                                    else -> verticalOverscroll.value
                                }
                                verticalOverscroll.snapTo(next)
                            }
                        }
                    },
                    onDragEnd = {
                        fullScreenDismissTriggered = false
                        overscrollScope.launch {
                            if (verticalOverscroll.value != 0f) {
                                verticalOverscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                            }
                        }
                    },
                    onDragCancel = {
                        fullScreenDismissTriggered = false
                        overscrollScope.launch {
                            if (verticalOverscroll.value != 0f) {
                                verticalOverscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                            }
                        }
                    }
                )
            }
        }
        Column(
            modifier = Modifier
                .then(contentModifier)
                .padding(horizontal = contentHorizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(outerSpacing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = topStatusHorizontalInset)
            ) {
                WatchBatteryPill(
                    level = batteryStatus.level,
                    charging = batteryStatus.charging,
                    sizeScale = 1.15f,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (showPowerButton) {
                        SystemSettingsButton(
                            icon = Icons.Filled.PowerSettingsNew,
                            contentDescription = "电源选项",
                            size = 28.dp,
                            iconSize = 17.dp,
                            onGestureLockChange = lockControlCenterGesture,
                            onClick = onPowerClick
                        )
                    }
                    SystemSettingsButton(
                        icon = Icons.Outlined.SettingsIcon,
                        contentDescription = "系统设置",
                        size = 28.dp,
                        iconSize = 17.dp,
                        onGestureLockChange = lockControlCenterGesture,
                        onClick = { openSettings(context, Settings.ACTION_SETTINGS) }
                    )
                    Text(
                        timeText,
                        color = launcherStyle.titleColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(controlsSpacing)) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = toggleRowTopPadding)
                ) {
                    val toggleViewportWidth = maxWidth
                    if (toggleRowFits) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ControlToggle(
                                icon = Icons.Filled.Wifi,
                                label = "WiFi",
                                active = connectivity.wifiActive,
                                size = toggleSize,
                                iconSize = toggleIconSize,
                                labelGap = toggleLabelGap,
                                onGestureLockChange = lockControlCenterGesture,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        openInternetConnectivityPanel(context, internetPanelLauncher)
                                    } else if (!toggleWifiLegacy(context)) {
                                        openSettings(context, Settings.ACTION_WIFI_SETTINGS)
                                    }
                                    connectivity = readConnectivitySnapshot(context)
                                },
                                onLongClick = { openSettings(context, Settings.ACTION_WIFI_SETTINGS) }
                            )
                            ControlToggle(
                                icon = Icons.Filled.Bluetooth,
                                label = "蓝牙",
                                active = bluetoothEnabled,
                                size = toggleSize,
                                iconSize = toggleIconSize,
                                labelGap = toggleLabelGap,
                                onGestureLockChange = lockControlCenterGesture,
                                onClick = {
                                    requestBluetoothToggle(
                                        context = context,
                                        enableLauncher = bluetoothEnableLauncher,
                                        permissionLauncher = bluetoothPermissionLauncher,
                                        onPermissionPending = { pendingBluetoothEnable = true }
                                    )
                                    bluetoothEnabled = isBluetoothLikelyEnabled(context)
                                },
                                onLongClick = { openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) }
                            )
                            ControlToggle(
                                icon = Icons.Filled.NetworkCell,
                                label = "流量",
                                active = connectivity.cellularActive,
                                size = toggleSize,
                                iconSize = toggleIconSize,
                                labelGap = toggleLabelGap,
                                onGestureLockChange = lockControlCenterGesture,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        openInternetConnectivityPanel(context, internetPanelLauncher)
                                    } else {
                                        openSettings(context, Settings.ACTION_WIRELESS_SETTINGS)
                                    }
                                    connectivity = readConnectivitySnapshot(context)
                                },
                                onLongClick = { openSettings(context, Settings.ACTION_WIRELESS_SETTINGS) }
                            )
                            ControlToggle(
                                icon = Icons.Filled.DoNotDisturbOn,
                                label = "勿扰",
                                active = dndEnabled,
                                size = toggleSize,
                                iconSize = toggleIconSize,
                                labelGap = toggleLabelGap,
                                onGestureLockChange = lockControlCenterGesture,
                                onClick = {
                                    dndEnabled = !dndEnabled
                                    toggleDnd(context)
                                    dndEnabled = isDndEnabled(context)
                                },
                                onLongClick = { openDndSettings(context) }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(toggleElasticScroll)
                                .horizontalScroll(toggleScrollState)
                        ) {
                            Row(
                                modifier = Modifier
                                    .widthIn(min = toggleViewportWidth)
                                    .graphicsLayer { translationX = toggleOverscroll.value },
                                horizontalArrangement = Arrangement.spacedBy(toggleRowSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ControlToggle(
                                    icon = Icons.Filled.Wifi,
                                    label = "WiFi",
                                    active = connectivity.wifiActive,
                                    size = toggleSize,
                                    iconSize = toggleIconSize,
                                    labelGap = toggleLabelGap,
                                    onGestureLockChange = lockControlCenterGesture,
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            openInternetConnectivityPanel(context, internetPanelLauncher)
                                        } else if (!toggleWifiLegacy(context)) {
                                            openSettings(context, Settings.ACTION_WIFI_SETTINGS)
                                        }
                                        connectivity = readConnectivitySnapshot(context)
                                    },
                                    onLongClick = { openSettings(context, Settings.ACTION_WIFI_SETTINGS) }
                                )
                                ControlToggle(
                                    icon = Icons.Filled.Bluetooth,
                                    label = "蓝牙",
                                    active = bluetoothEnabled,
                                    size = toggleSize,
                                    iconSize = toggleIconSize,
                                    labelGap = toggleLabelGap,
                                    onGestureLockChange = lockControlCenterGesture,
                                    onClick = {
                                        requestBluetoothToggle(
                                            context = context,
                                            enableLauncher = bluetoothEnableLauncher,
                                            permissionLauncher = bluetoothPermissionLauncher,
                                            onPermissionPending = { pendingBluetoothEnable = true }
                                        )
                                        bluetoothEnabled = isBluetoothLikelyEnabled(context)
                                    },
                                    onLongClick = { openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) }
                                )
                                ControlToggle(
                                    icon = Icons.Filled.NetworkCell,
                                    label = "流量",
                                    active = connectivity.cellularActive,
                                    size = toggleSize,
                                    iconSize = toggleIconSize,
                                    labelGap = toggleLabelGap,
                                    onGestureLockChange = lockControlCenterGesture,
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            openInternetConnectivityPanel(context, internetPanelLauncher)
                                        } else {
                                            openSettings(context, Settings.ACTION_WIRELESS_SETTINGS)
                                        }
                                        connectivity = readConnectivitySnapshot(context)
                                    },
                                    onLongClick = { openSettings(context, Settings.ACTION_WIRELESS_SETTINGS) }
                                )
                                ControlToggle(
                                    icon = Icons.Filled.DoNotDisturbOn,
                                    label = "勿扰",
                                    active = dndEnabled,
                                    size = toggleSize,
                                    iconSize = toggleIconSize,
                                    labelGap = toggleLabelGap,
                                    onGestureLockChange = lockControlCenterGesture,
                                    onClick = {
                                        dndEnabled = !dndEnabled
                                        toggleDnd(context)
                                        dndEnabled = isDndEnabled(context)
                                    },
                                    onLongClick = { openDndSettings(context) }
                                )
                            }
                        }
                    }
                }

                ControlSliderCard(
                    icon = Icons.Filled.VolumeUp,
                    label = "音量",
                    value = mediaVolume,
                    height = sliderHeight,
                    horizontalPadding = sliderHorizontalPadding,
                    onValueChange = { value ->
                        mediaVolume = value
                        pendingRequestedMediaVolume = value
                        setMusicVolume(audioManager, value)
                    }
                )
                ControlSliderCard(
                    icon = Icons.Filled.Brightness6,
                    label = "亮度",
                    value = brightness,
                    height = sliderHeight,
                    horizontalPadding = sliderHorizontalPadding,
                    onValueChange = { value ->
                        brightness = value
                        setBrightness(context, value)
                    }
                )

                if (showMusicControls) {
                    MediaControlCard(
                        mediaState = mediaState,
                        notificationAccessGranted = notificationAccessGranted,
                        musicTextSwitchAnimation = musicTextSwitchAnimation,
                        onGestureLockChange = lockControlCenterGesture,
                        onOpenApp = { openMusicApp(context, mediaState.appPackage) },
                        onOpenNotificationAccess = { openNotificationAccessSettings(context) },
                        onOpenOutputSwitcherOrApp = {
                            if (!openMediaOutputSwitcher(context)) {
                                openMusicApp(context, mediaState.appPackage)
                            }
                        }
                    )
                }
            }
            if (verticalScrollEnabled) {
                Spacer(Modifier.height(4.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = greetingText,
                color = WatchColors.TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = footerTopGap)
            )
        }
    }
}

@Composable
private fun SystemSettingsButton(
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onGestureLockChange: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val launcherStyle = LauncherTheme.style
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "system_settings_scale")
    val iconTint = if (pressed) {
        launcherStyle.titleColor.copy(alpha = 0.56f)
    } else {
        launcherStyle.titleColor
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .controlCenterPressGesture(
                pressedState = pressedState,
                onGestureLockChange = onGestureLockChange,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ControlToggle(
    icon: ImageVector,
    label: String,
    active: Boolean,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    labelGap: androidx.compose.ui.unit.Dp,
    onGestureLockChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val darkMode = WatchColors.isDark
    val targetScale by animateFloatAsState(
        when {
            pressed -> 0.88f
            active -> 1f
            else -> 0.96f
        },
        label = "control_toggle_scale"
    )
    val activeButtonColor = if (darkMode) Color.White else Color.Black
    val inactiveButtonColor = if (darkMode) Color(0xFF2C2C2E) else Color.White
    val activePressedButtonColor = if (darkMode) Color(0xFFE4E4E4) else Color(0xFF1C1C1E)
    val inactivePressedButtonColor = if (darkMode) Color(0xFF424242) else Color(0xFFE4E4E4)
    val buttonColor = when {
        pressed -> if (active) activePressedButtonColor else inactivePressedButtonColor
        active -> activeButtonColor
        else -> inactiveButtonColor
    }
    val contentColor = if (active) {
        if (darkMode) Color.Black else Color.White
    } else {
        if (darkMode) Color.White else Color.Black
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = targetScale
                    scaleY = targetScale
                }
                .clip(CircleShape)
                .background(buttonColor)
                .controlCenterPressGesture(
                    pressedState = pressedState,
                    onGestureLockChange = onGestureLockChange,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(Modifier.height(labelGap))
        Text(label, color = WatchColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ControlSliderCard(
    icon: ImageVector,
    label: String,
    value: Float,
    height: androidx.compose.ui.unit.Dp,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onValueChange: (Float) -> Unit
) {
    val launcherStyle = LauncherTheme.style
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(28.dp))
            .background(launcherStyle.cardColor)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = launcherStyle.bodyColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = launcherStyle.bodyColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MediaControlCard(
    mediaState: ControlCenterMediaState,
    notificationAccessGranted: Boolean,
    musicTextSwitchAnimation: String,
    onGestureLockChange: (Boolean) -> Unit,
    onOpenApp: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenOutputSwitcherOrApp: () -> Unit
) {
    val bg = if (notificationAccessGranted) mediaState.artworkColor else Color(0xFF2F3B45)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bg.copy(alpha = 0.88f))
            .clickable(onClick = if (notificationAccessGranted) onOpenApp else onOpenNotificationAccess)
            .padding(12.dp)
    ) {
        if (!notificationAccessGranted) {
            MediaNotificationAccessPrompt()
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val artworkGap = 12.dp
            val preferredArtworkSize = 132.dp
            val minArtworkSize = 96.dp
            val preferredControlsWidth = 36.dp * 3f + 8.dp * 2f
            val artworkSize = if (maxWidth >= preferredArtworkSize + artworkGap + preferredControlsWidth) {
                preferredArtworkSize
            } else {
                (maxWidth - artworkGap - preferredControlsWidth).coerceIn(minArtworkSize, preferredArtworkSize)
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaState.artwork != null) {
                        Image(
                            bitmap = mediaState.artwork,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.width(artworkGap))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val titleEndPadding = if (mediaState.appIcon != null) 34.dp else 0.dp
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = titleEndPadding)
                            ) {
                                AnimatedMusicText(
                                    text = mediaState.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    animationStyleId = musicTextSwitchAnimation
                                )
                                AnimatedMusicText(
                                    text = mediaState.artist,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    animationStyleId = musicTextSwitchAnimation
                                )
                            }
                            if (mediaState.appIcon != null) {
                                MediaAppIconButton(
                                    icon = mediaState.appIcon,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(30.dp),
                                    onGestureLockChange = onGestureLockChange,
                                    onClick = onOpenOutputSwitcherOrApp
                                )
                            }
                        }
                    }
                    MediaProgressSection(
                        mediaState = mediaState,
                        onGestureLockChange = onGestureLockChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MediaControlsRow(
                        mediaState = mediaState,
                        onGestureLockChange = onGestureLockChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
}

@Composable
private fun MediaNotificationAccessPrompt() {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "需要通知使用权",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "授权后显示音乐信息和控制",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "点击授权",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MediaAppIconButton(
    icon: ImageBitmap,
    modifier: Modifier = Modifier,
    onGestureLockChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "media_app_icon_scale")
    val iconAlpha by animateFloatAsState(if (pressed) 0.72f else 1f, label = "media_app_icon_alpha")
    val iconShape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(iconShape)
            .controlCenterPressGesture(
                pressedState = pressedState,
                onGestureLockChange = onGestureLockChange,
                onClick = onClick
            )
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(iconShape)
                .graphicsLayer { alpha = iconAlpha },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun MediaControlsRow(
    mediaState: ControlCenterMediaState,
    onGestureLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val customActions = mediaState.customActions.take(2)
        val preferredButtonSize = 36.dp
        val minButtonSize = 24.dp
        val customButtonScale = 1.12f
        val preferredSpacing = 8.dp
        val minSpacing = 4.dp
        val customCountFloat = customActions.size.toFloat()
        val buttonCount = 3 + customActions.size
        val gapCountFloat = (buttonCount - 1).coerceAtLeast(1).toFloat()
        val buttonWidthUnits = 3f + customCountFloat * customButtonScale
        val preferredWidth = preferredButtonSize * buttonWidthUnits + preferredSpacing * gapCountFloat
        val minSpacingWidth = minSpacing * gapCountFloat
        val buttonSize = if (maxWidth >= preferredWidth) {
            preferredButtonSize
        } else {
            ((maxWidth - minSpacingWidth) / buttonWidthUnits).coerceIn(minButtonSize, preferredButtonSize)
        }
        val customButtonSize = (buttonSize * customButtonScale).coerceAtLeast(buttonSize)
        val occupiedButtonWidth = buttonSize * 3f + customButtonSize * customCountFloat
        val buttonSpacing = ((maxWidth - occupiedButtonWidth) / gapCountFloat)
            .coerceIn(minSpacing, preferredSpacing)
        val iconSize = (buttonSize * 0.56f).coerceIn(16.dp, 20.dp)
        val customIconSize = (customButtonSize * 0.60f).coerceIn(iconSize, 23.dp)

        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            customActions.getOrNull(0)?.let { customAction ->
                MediaCustomActionButton(
                    action = customAction,
                    size = customButtonSize,
                    iconSize = customIconSize,
                    onGestureLockChange = onGestureLockChange
                ) {
                    mediaState.controller?.transportControls?.sendCustomAction(
                        customAction.action,
                        customAction.extras
                    )
                }
            }
            MediaButton(
                Icons.Filled.SkipPrevious,
                size = buttonSize,
                iconSize = iconSize,
                onGestureLockChange = onGestureLockChange
            ) {
                mediaState.controller?.transportControls?.skipToPrevious()
            }
            MediaButton(
                icon = if (mediaState.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                size = buttonSize,
                iconSize = iconSize,
                onGestureLockChange = onGestureLockChange
            ) {
                val controls = mediaState.controller?.transportControls
                if (mediaState.playing) controls?.pause() else controls?.play()
            }
            MediaButton(
                Icons.Filled.SkipNext,
                size = buttonSize,
                iconSize = iconSize,
                onGestureLockChange = onGestureLockChange
            ) {
                mediaState.controller?.transportControls?.skipToNext()
            }
            customActions.getOrNull(1)?.let { customAction ->
                MediaCustomActionButton(
                    action = customAction,
                    size = customButtonSize,
                    iconSize = customIconSize,
                    onGestureLockChange = onGestureLockChange
                ) {
                    mediaState.controller?.transportControls?.sendCustomAction(
                        customAction.action,
                        customAction.extras
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaCustomActionButton(
    action: ControlCenterMediaAction,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onGestureLockChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "media_custom_button_scale")
    val contentAlpha by animateFloatAsState(if (pressed) 0.64f else 1f, label = "media_custom_button_alpha")
    Box(
        modifier = Modifier
            .requiredSize(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .controlCenterPressGesture(
                pressedState = pressedState,
                onGestureLockChange = onGestureLockChange,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (action.icon != null) {
            Image(
                bitmap = action.icon,
                contentDescription = action.title,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer { alpha = contentAlpha },
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = action.title.take(2).ifBlank { "..." },
                color = Color.White.copy(alpha = contentAlpha),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnimatedMusicText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    animationStyleId: String
) {
    val density = LocalDensity.current
    val preset = remember(animationStyleId) { MusicTextSwitchAnimations.byId(animationStyleId) }
    var displayedText by remember { mutableStateOf(text) }
    var phase by remember { mutableStateOf(MusicTextSwitchPhase.Idle) }
    val progress = remember { Animatable(1f) }
    val distancePx = with(density) { 18.dp.toPx() }

    LaunchedEffect(text, animationStyleId) {
        val currentPreset = MusicTextSwitchAnimations.byId(animationStyleId)
        if (displayedText == text) {
            phase = MusicTextSwitchPhase.Idle
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        phase = MusicTextSwitchPhase.Out
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            tween(
                durationMillis = currentPreset.outMotion.durationMs,
                easing = currentPreset.outMotion.easing
            )
        )
        displayedText = text
        phase = MusicTextSwitchPhase.In
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            tween(
                durationMillis = currentPreset.inMotion.durationMs,
                easing = currentPreset.inMotion.easing
            )
        )
        phase = MusicTextSwitchPhase.Idle
        progress.snapTo(1f)
    }

    val transform = preset.transform(phase, progress.value, distancePx)
    Box(
        modifier = Modifier
            .clipToBounds()
            .graphicsLayer {
                alpha = transform.alpha
                translationX = transform.translationX
                translationY = transform.translationY
                scaleX = transform.scale
                scaleY = transform.scale
                rotationX = transform.rotationX
                rotationY = transform.rotationY
                rotationZ = transform.rotationZ
                cameraDistance = 12f * density.density
            }
    ) {
        OneShotMarqueeText(
            text = displayedText,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun OneShotMarqueeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight
) {
    val density = LocalDensity.current
    val sidePadding = MUSIC_TEXT_MARQUEE_SIDE_PADDING_DP.dp
    val sidePaddingPx = with(density) { sidePadding.toPx() }
    val fadeEdgePx = with(density) { MUSIC_TEXT_FADE_EDGE_DP.dp.toPx() }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var rawTextWidthPx by remember(text) { mutableIntStateOf(0) }
    var contentWidthPx by remember { mutableIntStateOf(0) }
    val hasOverflow = containerWidthPx > 0 &&
        rawTextWidthPx > 0 &&
        rawTextWidthPx + sidePaddingPx > containerWidthPx.toFloat()
    val contentLayoutReady = !hasOverflow ||
        contentWidthPx.toFloat() >= rawTextWidthPx + sidePaddingPx - 1f
    val marqueeScrollState = rememberScrollState()
    val scrollTargetPx = if (hasOverflow && contentLayoutReady) {
        (contentWidthPx - containerWidthPx).coerceAtLeast(0)
    } else {
        0
    }

    LaunchedEffect(text, hasOverflow, scrollTargetPx, density.density) {
        marqueeScrollState.scrollTo(0)
        if (hasOverflow && scrollTargetPx > 0) {
            delay(MUSIC_TEXT_MARQUEE_INITIAL_DELAY_MS)
            val durationMs = ((scrollTargetPx / density.density) / MUSIC_TEXT_MARQUEE_SPEED_DP_PER_SECOND * 1000f)
                .roundToInt()
                .coerceAtLeast(900)
            marqueeScrollState.animateScrollTo(
                value = scrollTargetPx,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
        }
    }

    val scrollOffsetPx = marqueeScrollState.value.toFloat()
    val remainingScrollPx = (scrollTargetPx - marqueeScrollState.value).coerceAtLeast(0).toFloat()
    val leftFadeTarget = if (hasOverflow && fadeEdgePx > 0f) {
        if (scrollOffsetPx > 0.5f) 1f else 0f
    } else {
        0f
    }
    val rightFadeTarget = if (hasOverflow && fadeEdgePx > 0f) {
        (remainingScrollPx / fadeEdgePx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val leftFadeAlpha by animateFloatAsState(
        targetValue = leftFadeTarget,
        animationSpec = tween(durationMillis = MUSIC_TEXT_FADE_TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "music_text_left_edge_fade"
    )
    val rightFadeAlpha by animateFloatAsState(
        targetValue = rightFadeTarget,
        animationSpec = tween(durationMillis = MUSIC_TEXT_FADE_TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "music_text_right_edge_fade"
    )
    val fadeModifier = if (hasOverflow || leftFadeAlpha > 0.001f || rightFadeAlpha > 0.001f) {
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val fadeWidthPx = MUSIC_TEXT_FADE_EDGE_DP.dp.toPx().coerceAtMost(size.width / 2f)
                val leftStop = if (size.width == 0f) 0f else (fadeWidthPx / size.width).coerceIn(0f, 1f)
                val rightStart = if (size.width == 0f) 1f else ((size.width - fadeWidthPx) / size.width).coerceIn(0f, 1f)
                val fadeBrush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 1f - leftFadeAlpha.coerceIn(0f, 1f)),
                        leftStop to Color.Black,
                        rightStart to Color.Black,
                        1f to Color.Black.copy(alpha = 1f - rightFadeAlpha.coerceIn(0f, 1f))
                    )
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                }
            }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width.coerceAtLeast(0) }
            .then(fadeModifier)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(marqueeScrollState, enabled = false)
                .onSizeChanged { contentWidthPx = it.width.coerceAtLeast(0) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                onTextLayout = { rawTextWidthPx = it.size.width.coerceAtLeast(0) }
            )
            if (hasOverflow) {
                Spacer(Modifier.width(sidePadding))
            }
        }
    }
}

@Composable
private fun MediaProgressSection(
    mediaState: ControlCenterMediaState,
    onGestureLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMediaState by rememberUpdatedState(mediaState)
    val currentGestureLockChange by rememberUpdatedState(onGestureLockChange)
    var widthPx by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf(false) }
    var skipNextProgressSync by remember { mutableStateOf(false) }
    var seekSyncHoldUntilMs by remember { mutableStateOf(0L) }
    var activeMediaIdentity by remember {
        mutableStateOf(mediaState.appPackage to mediaState.title)
    }
    var localProgress by remember {
        mutableFloatStateOf(mediaState.progress.coerceIn(0f, 1f))
    }
    LaunchedEffect(mediaState.appPackage, mediaState.title, dragging) {
        val mediaIdentity = mediaState.appPackage to mediaState.title
        if (!dragging && activeMediaIdentity != mediaIdentity) {
            activeMediaIdentity = mediaIdentity
            skipNextProgressSync = false
            seekSyncHoldUntilMs = 0L
            localProgress = mediaState.progress.coerceIn(0f, 1f)
        }
    }
    LaunchedEffect(mediaState.progress, dragging) {
        if (!dragging) {
            val now = SystemClock.uptimeMillis()
            if (now < seekSyncHoldUntilMs) {
                return@LaunchedEffect
            }
            if (skipNextProgressSync) {
                skipNextProgressSync = false
                seekSyncHoldUntilMs = now + MEDIA_PROGRESS_SEEK_SYNC_HOLD_MS
                return@LaunchedEffect
            }
            localProgress = mediaState.progress.coerceIn(0f, 1f)
        }
    }
    val trackHeight by animateFloatAsState(
        if (dragging) MEDIA_PROGRESS_EXPANDED_HEIGHT_DP else MEDIA_PROGRESS_COLLAPSED_HEIGHT_DP,
        label = "media_progress_height"
    )
    fun seekToProgress(progress: Float) {
        val duration = currentMediaState.durationMs
        if (duration <= 0L) return
        currentMediaState.controller?.transportControls?.seekTo((progress.coerceIn(0f, 1f) * duration).toLong())
    }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MEDIA_PROGRESS_HIT_HEIGHT_DP.dp)
                .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
                .pointerInput(Unit) {
                    val dragHitHeightPx = MEDIA_PROGRESS_HIT_HEIGHT_DP.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val centerY = size.height / 2f
                        val withinTrack = abs(down.position.y - centerY) <= dragHitHeightPx / 2f
                        if (!withinTrack || currentMediaState.durationMs <= 0L) {
                            return@awaitEachGesture
                        }
                        down.consume()
                        val startX = down.position.x
                        val startProgress = localProgress.coerceIn(0f, 1f)
                        fun updateProgressFromDelta(deltaX: Float) {
                            localProgress = (startProgress + deltaX / widthPx).coerceIn(0f, 1f)
                        }
                        try {
                            currentGestureLockChange(true)
                            dragging = true
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                    break
                                }
                                updateProgressFromDelta(change.position.x - startX)
                                change.consume()
                            }
                            skipNextProgressSync = true
                            seekToProgress(localProgress)
                        } finally {
                            dragging = false
                            currentGestureLockChange(false)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(localProgress.coerceIn(0f, 1f))
                    .height(trackHeight.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMediaTime((localProgress.coerceIn(0f, 1f) * mediaState.durationMs).toLong()),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp
            )
            Text(
                text = formatMediaTime(mediaState.durationMs),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MediaButton(
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    onGestureLockChange: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    val pressedState = rememberPressedState()
    val pressed by pressedState
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "media_button_scale")
    val backgroundAlpha by animateFloatAsState(if (pressed) 0.28f else 0.18f, label = "media_button_alpha")
    Box(
        modifier = Modifier
            .requiredSize(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = backgroundAlpha))
            .controlCenterPressGesture(
                pressedState = pressedState,
                onGestureLockChange = onGestureLockChange,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

private fun Modifier.controlCenterPressGesture(
    pressedState: MutableState<Boolean>,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onGestureLockChange: (Boolean) -> Unit = {},
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(onClick, onLongClick, onGestureLockChange, enabled) {
        detectTapGestures(
            onPress = {
                val startTime = SystemClock.elapsedRealtime()
                onGestureLockChange(true)
                pressedState.value = true
                try {
                    tryAwaitRelease()
                    val remaining = CONTROL_CENTER_MIN_PRESS_MS - (SystemClock.elapsedRealtime() - startTime)
                    if (remaining > 0L) delay(remaining)
                } finally {
                    pressedState.value = false
                    onGestureLockChange(false)
                }
            },
            onTap = {
                onClick()
            },
            onLongPress = {
                onLongClick?.invoke()
            }
        )
    }
}

private fun controlCenterGreeting(now: Calendar = Calendar.getInstance()): String {
    val hour = now.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "早好～"
        in 11..16 -> "午好～"
        else -> "晚好～"
    }
}

@Composable
private fun rememberMediaState(context: Context): ControlCenterMediaState {
    var state by remember(context) { mutableStateOf(ControlCenterMediaState()) }
    LaunchedEffect(context) {
        while (true) {
            state = readMediaState(context)
            delay(500)
        }
    }
    return state
}

private fun readMediaState(context: Context): ControlCenterMediaState {
    val manager = context.getSystemService(MediaSessionManager::class.java) ?: return ControlCenterMediaState()
    val component = ComponentName(context, WLauncherNotificationListener::class.java)
    val controller = runCatching {
        manager.getActiveSessions(component).firstOrNull()
    }.getOrNull() ?: return ControlCenterMediaState()
    val metadata = controller.metadata
    val playbackState = controller.playbackState
    val appIconBitmap = loadMediaAppIconBitmap(context, controller.packageName)
    val artworkBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val customActions = playbackState?.customActions.orEmpty().mapNotNull { action ->
        val title = action.name?.toString().orEmpty()
        val actionIcon = loadCustomActionIconBitmap(context, controller.packageName, action.icon)
        if (action.action.isBlank() && title.isBlank()) return@mapNotNull null
        ControlCenterMediaAction(
            action = action.action,
            title = title.ifBlank { action.action },
            extras = action.extras,
            icon = actionIcon?.asImageBitmap()
        )
    }.take(2)
    val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0L } ?: 0L
    val rawPosition = playbackState?.position?.coerceAtLeast(0L) ?: 0L
    val position = if (
        playbackState?.state == PlaybackState.STATE_PLAYING &&
        duration > 0L &&
        playbackState.lastPositionUpdateTime > 0L
    ) {
        val elapsed = SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
        (rawPosition + elapsed).coerceIn(0L, duration)
    } else {
        rawPosition.coerceAtMost(duration.takeIf { it > 0L } ?: rawPosition)
    }
    val progress = if (duration > 0L) position.toFloat() / duration else 0f
    return ControlCenterMediaState(
        controller = controller,
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "未在播放",
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: controller.packageName.substringAfterLast('.'),
        artwork = (artworkBitmap ?: appIconBitmap)?.asImageBitmap(),
        appIcon = appIconBitmap?.asImageBitmap(),
        customActions = customActions,
        artworkColor = (artworkBitmap ?: appIconBitmap)?.let(::averageBitmapColor) ?: Color(0xFF2F3B45),
        playing = playbackState?.state == PlaybackState.STATE_PLAYING,
        progress = progress,
        positionMs = position,
        durationMs = duration,
        appPackage = controller.packageName
    )
}

private fun loadMediaAppIconBitmap(context: Context, packageName: String): Bitmap? {
    val repositoryIcon = runCatching {
        FlueApplication.repositories(context).appRepository.loadDisplayIconForPackage(
            packageName = packageName,
            iconSize = 128,
            preferPlainIcon = true
        )
    }.getOrNull()?.asAndroidBitmap()
    if (repositoryIcon != null) return repositoryIcon
    return runCatching {
        context.packageManager.getApplicationIcon(packageName).toBitmap(width = 128, height = 128)
    }.getOrNull()
}

private fun loadCustomActionIconBitmap(
    context: Context,
    packageName: String,
    iconResId: Int
): Bitmap? {
    if (iconResId == 0) return null
    return runCatching {
        val packContext = context.createPackageContext(
            packageName,
            Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
        )
        packContext.resources.getDrawable(iconResId, packContext.theme)
            .toBitmap(width = 96, height = 96)
    }.getOrNull()
}

private fun openMusicApp(context: Context, packageName: String?) {
    if (packageName.isNullOrBlank()) return
    runCatching {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openMediaOutputSwitcher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    return runCatching {
        SystemOutputSwitcherDialogController.showDialog(context)
    }.getOrDefault(false)
}

private fun openNotificationAccessSettings(context: Context) {
    openSettings(context, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}

private fun formatMediaTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.US, minutes, seconds)
}

private fun averageBitmapColor(bitmap: Bitmap): Color {
    val sampleWidth = 18
    val sampleHeight = 18
    val scaled = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0L
    for (x in 0 until sampleWidth) {
        for (y in 0 until sampleHeight) {
            val pixel = scaled.getPixel(x, y)
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            count++
        }
    }
    if (scaled != bitmap) scaled.recycle()
    val r = ((red / count) * 0.72f).roundToInt().coerceIn(0, 255)
    val g = ((green / count) * 0.72f).roundToInt().coerceIn(0, 255)
    val b = ((blue / count) * 0.72f).roundToInt().coerceIn(0, 255)
    return Color(android.graphics.Color.rgb(r, g, b))
}

private fun readBatteryStatus(context: Context): BatteryStatusSnapshot {
    val batteryManager = context.getSystemService(BatteryManager::class.java)
    val level = batteryManager
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?.coerceIn(0, 100)
        ?: 0
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL ||
        plugged != 0
    return BatteryStatusSnapshot(level = level, charging = charging)
}

private fun readMusicVolume(audioManager: AudioManager?): Float {
    if (audioManager == null) return 0f
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun quantizeMusicVolume(audioManager: AudioManager?, value: Float): Float {
    if (audioManager == null) return value.coerceIn(0f, 1f)
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return ((value.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)).toFloat() / max
}

private fun setMusicVolume(audioManager: AudioManager?, value: Float) {
    if (audioManager == null) return
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (value.coerceIn(0f, 1f) * max).roundToInt(), 0)
}

private fun readBrightness(context: Context): Float {
    val value = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrDefault(128)
    return value.coerceIn(0, 255) / 255f
}

private fun setBrightness(context: Context, value: Float) {
    if (!Settings.System.canWrite(context)) {
        Toast.makeText(context, "需要修改系统设置权限", Toast.LENGTH_SHORT).show()
        openSettings(
            context,
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        return
    }
    Settings.System.putInt(
        context.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS,
        (value.coerceIn(0f, 1f) * 255).roundToInt().coerceIn(1, 255)
    )
}

private fun readConnectivitySnapshot(context: Context): ConnectivitySnapshot {
    return ConnectivitySnapshot(
        wifiActive = isWifiEnabled(context),
        cellularActive = isCellularDataEnabled(context)
    )
}

private fun isBluetoothLikelyEnabled(context: Context): Boolean {
    if (!hasBluetoothAccessPermission(context)) {
        return false
    }
    return runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
}

private fun requestBluetoothToggle(
    context: Context,
    enableLauncher: ActivityResultLauncher<Intent>,
    permissionLauncher: ActivityResultLauncher<String>,
    onPermissionPending: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothAccessPermission(context)) {
        onPermissionPending()
        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        return
    }
    val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull() ?: return
    val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
    if (!enabled) {
        requestBluetoothEnable(enableLauncher)
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        @Suppress("DEPRECATION")
        runCatching { adapter.disable() }
    } else {
        openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
    }
}

private fun requestBluetoothEnable(enableLauncher: ActivityResultLauncher<Intent>) {
    runCatching {
        enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }
}

private fun isDndEnabled(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    return manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
}

private fun toggleDnd(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (!manager.isNotificationPolicyAccessGranted) {
        return
    }
    val next = if (isDndEnabled(context)) {
        NotificationManager.INTERRUPTION_FILTER_ALL
    } else {
        NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }
    manager.setInterruptionFilter(next)
}

private fun openDndSettings(context: Context) {
    val actions = listOf(
        "android.settings.ZEN_MODE_SETTINGS",
        "android.settings.ZEN_MODE_PRIORITY_SETTINGS",
        Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
    )
    val action = actions.firstOrNull { Intent(it).resolveActivity(context.packageManager) != null }
        ?: Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
    openSettings(context, action)
}

private fun openSettings(context: Context, action: String, data: Uri? = null) {
    runCatching {
        context.startActivity(
            Intent(action).apply {
                data?.let { setData(it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

private fun hasBluetoothAccessPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_CONNECT
    } else {
        Manifest.permission.BLUETOOTH
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun openInternetConnectivityPanel(
    context: Context,
    launcher: ActivityResultLauncher<Intent>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        val resolved = intent.resolveActivity(context.packageManager)
        if (resolved != null) {
            runCatching { launcher.launch(intent) }.onSuccess { return }
        }
    }
    openSettings(context, Settings.ACTION_WIRELESS_SETTINGS)
}

private fun toggleWifiLegacy(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return false
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CHANGE_WIFI_STATE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java) ?: return false
    return runCatching {
        @Suppress("DEPRECATION")
        wifiManager.setWifiEnabled(!isWifiEnabled(context))
    }.getOrDefault(false)
}

private fun isWifiEnabled(context: Context): Boolean {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java) ?: return false
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (wifiManager.wifiState) {
                WifiManager.WIFI_STATE_ENABLED,
                WifiManager.WIFI_STATE_ENABLING -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled
        }
    }.getOrDefault(false)
}

private fun isCellularDataEnabled(context: Context): Boolean {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
        return false
    }
    val telephonyManager = context.getSystemService(TelephonyManager::class.java) ?: return false
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telephonyManager.isDataEnabled
        } else {
            val manager = context.getSystemService(ConnectivityManager::class.java)
                ?: return@runCatching false
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
    }.getOrDefault(false)
}
