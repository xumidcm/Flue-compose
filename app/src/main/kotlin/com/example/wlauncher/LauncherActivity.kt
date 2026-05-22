package com.flue.launcher

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flue.launcher.ui.anim.appListLayerValues
import com.flue.launcher.ui.anim.faceLayerValues
import com.flue.launcher.ui.anim.scaleBlurAlpha
import com.flue.launcher.ui.drawer.HoneycombScreen
import com.flue.launcher.ui.drawer.ListDrawerScreen
import com.flue.launcher.ui.home.WatchFaceLayer
import com.flue.launcher.ui.navigation.GestureHost
import com.flue.launcher.ui.navigation.LayoutMode
import com.flue.launcher.ui.navigation.ScreenState
import com.flue.launcher.ui.notification.NotificationLayer
import com.flue.launcher.ui.smartstack.SmartStackLayer
import com.flue.launcher.ui.theme.WatchLauncherTheme
import com.flue.launcher.util.RecentsVisibility
import com.flue.launcher.viewmodel.LauncherViewModel
import com.flue.launcher.watchface.BUILT_IN_WATCHFACE_ID
import com.flue.launcher.watchface.BuiltInWatchFaceOptions
import com.flue.launcher.watchface.LunchWatchFaceHost
import androidx.compose.animation.core.Spring
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BASE_LAUNCH_MASK_DELAY_MS = 180L
private const val SIDE_SCREEN_TRANSITION_MS = 260
private const val SIDE_SCREEN_EXIT_OVERSHOOT = 1.08f
private const val NOTIFICATION_TRANSITION_MS = 240

class LauncherActivity : ComponentActivity() {

    private lateinit var vm: LauncherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecentsVisibility.apply(this)
        CrashHandler(applicationContext).install()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        onBackPressedDispatcher.addCallback(this) { vm.handleBackPress() }
        setContent {
            WatchLauncherTheme {
                val viewModel: LauncherViewModel = viewModel()
                vm = viewModel
                LauncherScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::vm.isInitialized && intent.isLauncherHomeIntent()) {
            vm.handleHomePress()
        }
    }

    override fun onResume() {
        super.onResume()
        RecentsVisibility.apply(this)
        if (::vm.isInitialized && vm.animationOverrideEnabled.value && vm.hasPendingExternalLaunchReturn()) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        if (::vm.isInitialized) {
            vm.onReturnToLauncher()
            vm.refreshWatchFaces()
            vm.refreshNotificationAccess()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::vm.isInitialized && vm.animationOverrideEnabled.value) {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

private fun Intent.isLauncherHomeIntent(): Boolean {
    return action == Intent.ACTION_MAIN &&
        hasCategory(Intent.CATEGORY_HOME)
}

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val screenState by vm.screenState.collectAsState()
    val layoutMode by vm.layoutMode.collectAsState()
    val sideScreenEnabled by vm.sideScreenEnabled.collectAsState()
    val sideScreenShortcuts by vm.sideScreenShortcuts.collectAsState()
    val sideScreenPreviewGroups by vm.sideScreenPreviewGroups.collectAsState()
    val blurEnabled by vm.blurEnabled.collectAsState()
    val edgeBlurEnabled by vm.edgeBlurEnabled.collectAsState()
    val apps by vm.apps.collectAsState()
    val appOpenOrigin by vm.appOpenOrigin.collectAsState()
    val splashIcon by vm.splashIcon.collectAsState()
    val splashDelay by vm.splashDelay.collectAsState()
    val currentLaunchIcon by vm.currentLaunchIcon.collectAsState()
    val honeycombCols by vm.honeycombCols.collectAsState()
    val honeycombTopBlur by vm.honeycombTopBlur.collectAsState()
    val honeycombBottomBlur by vm.honeycombBottomBlur.collectAsState()
    val honeycombTopFade by vm.honeycombTopFade.collectAsState()
    val honeycombBottomFade by vm.honeycombBottomFade.collectAsState()
    val honeycombFastScrollOptimization by vm.honeycombFastScrollOptimization.collectAsState()
    val showNotification by vm.showNotification.collectAsState()
    val notificationAccessGranted by vm.notificationAccessGranted.collectAsState()
    val notificationGroups by vm.notificationGroups.collectAsState()
    val revealedNotificationTarget by vm.revealedNotificationTarget.collectAsState()
    val launchSourceState by vm.launchSourceState.collectAsState()
    val selectedWatchFaceId by vm.selectedWatchFaceId.collectAsState()
    val selectedWatchFace by vm.selectedWatchFace.collectAsState()
    val watchFaceSelectionReady by vm.watchFaceSelectionReady.collectAsState()
    val watchFaceRefreshToken by vm.watchFaceRefreshToken.collectAsState()
    val builtInPhotoPath by vm.builtInPhotoPath.collectAsState()
    val builtInVideoPath by vm.builtInVideoPath.collectAsState()
    val builtInPhotoClockPosition by vm.builtInPhotoClockPosition.collectAsState()
    val builtInVideoClockPosition by vm.builtInVideoClockPosition.collectAsState()
    val builtInPhotoClockSize by vm.builtInPhotoClockSize.collectAsState()
    val builtInVideoClockSize by vm.builtInVideoClockSize.collectAsState()
    val builtInPhotoClockBold by vm.builtInPhotoClockBold.collectAsState()
    val builtInVideoClockBold by vm.builtInVideoClockBold.collectAsState()
    val builtInVideoFillScreen by vm.builtInVideoFillScreen.collectAsState()
    val builtInVideoClockColorMode by vm.builtInVideoClockColorMode.collectAsState()
    val layerBlurEnabled =
        blurEnabled && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || screenState != ScreenState.App)
    val reduceLegacyDrawerEffects = Build.VERSION.SDK_INT < Build.VERSION_CODES.S && screenState == ScreenState.App
    val notificationsEnabled = showNotification
    val notificationSceneActive = screenState == ScreenState.Notifications
    val openWatchFaceChooser: () -> Unit = remember(context) {
        {
            context.startActivity(
                Intent(context, SettingsActivity::class.java)
                    .putExtra(EXTRA_SETTINGS_DESTINATION, SETTINGS_DESTINATION_WATCH_FACES)
                    .putExtra(EXTRA_SETTINGS_RETURN_TO_FACE, true)
            )
            (context as? Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            Unit
        }
    }
    var prevState by remember { mutableStateOf(screenState) }
    val isReturningFromApp = prevState == ScreenState.App && screenState == ScreenState.Apps
    LaunchedEffect(screenState) { prevState = screenState }
    val launcherScope = rememberCoroutineScope()
    var retainedLaunchIcon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var retainedLaunchSplash by remember { mutableStateOf(false) }
    val notificationTransition = remember { Animatable(if (notificationSceneActive) 1f else 0f) }
    var notificationGestureActive by remember { mutableStateOf(false) }
    var notificationTargetProgress by remember { mutableFloatStateOf(if (notificationSceneActive) 1f else 0f) }

    val useOrigin = (screenState == ScreenState.App || isReturningFromApp) &&
        launchSourceState == ScreenState.Apps
    val fadeLaunch = launchSourceState == ScreenState.Stack || launchSourceState == ScreenState.Notifications

    LaunchedEffect(notificationSceneActive) {
        if (!notificationGestureActive) {
            notificationTargetProgress = if (notificationSceneActive) 1f else 0f
        }
    }

    LaunchedEffect(notificationTargetProgress, notificationGestureActive) {
        if (!notificationGestureActive) {
            notificationTransition.animateTo(
                notificationTargetProgress,
                tween(durationMillis = NOTIFICATION_TRANSITION_MS)
            )
        }
    }

    LaunchedEffect(sideScreenEnabled, screenState) {
        if (!sideScreenEnabled && (screenState == ScreenState.Stack || screenState == ScreenState.Notifications)) {
            vm.setState(ScreenState.Face)
        }
    }

    var showSplash by remember { mutableStateOf(false) }
    val showLaunchBackdrop = screenState == ScreenState.App && (fadeLaunch || currentLaunchIcon != null)
    val effectiveLaunchIcon = if (fadeLaunch) null else (currentLaunchIcon ?: retainedLaunchIcon)
    val effectiveLaunchSplash = !fadeLaunch && (
        (showSplash && currentLaunchIcon != null) ||
            (!showLaunchBackdrop && retainedLaunchSplash && retainedLaunchIcon != null)
        )
    LaunchedEffect(screenState, splashIcon, splashDelay, currentLaunchIcon, fadeLaunch) {
        if (!fadeLaunch && screenState == ScreenState.App && splashIcon && currentLaunchIcon != null) {
            showSplash = false
            delay(BASE_LAUNCH_MASK_DELAY_MS)
            showSplash = true
        } else {
            showSplash = false
        }
    }
    LaunchedEffect(showLaunchBackdrop, currentLaunchIcon, showSplash, fadeLaunch) {
        if (!fadeLaunch && showLaunchBackdrop && currentLaunchIcon != null) {
            retainedLaunchIcon = currentLaunchIcon
            retainedLaunchSplash = showSplash
        } else if (!showLaunchBackdrop) {
            delay(240)
            retainedLaunchIcon = null
            retainedLaunchSplash = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val sideSceneTravelPx = screenWidthPx * SIDE_SCREEN_EXIT_OVERSHOOT
        val notificationProgress = notificationTransition.value.coerceIn(0f, 1f)
        val keepNotificationLayer = notificationProgress > 0.001f || notificationSceneActive || notificationGestureActive
        val sidePageProgress by animateFloatAsState(
            targetValue = if (sideScreenEnabled && (screenState == ScreenState.Stack || screenState == ScreenState.Notifications)) {
                1f
            } else {
                0f
            },
            animationSpec = tween(durationMillis = SIDE_SCREEN_TRANSITION_MS),
            label = "side_page_progress"
        )
        val faceAnimState = when (screenState) {
            ScreenState.Stack, ScreenState.Notifications -> ScreenState.Face
            else -> screenState
        }
        val updateNotificationTransitionProgress: (Float) -> Unit = { progress ->
            notificationGestureActive = true
            launcherScope.launch {
                notificationTransition.snapTo(progress.coerceIn(0f, 1f))
            }
        }
        val settleNotificationTransition: (Float) -> Unit = { target ->
            notificationGestureActive = false
            notificationTargetProgress = target.coerceIn(0f, 1f)
        }
        val sideScreenVisible = sidePageProgress > 0.001f || screenState == ScreenState.Stack || screenState == ScreenState.Notifications
        val sideSceneOverlayActive = sideScreenVisible

        GestureHost(
            screenState = screenState,
            onStateChange = { vm.setState(it) },
            sideScreenEnabled = sideScreenEnabled,
            showNotification = notificationsEnabled,
            showControlCenter = false,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (screenState == ScreenState.Face && !sideSceneOverlayActive) 3f else 1f)
                    .graphicsLayer {
                        clip = true
                        translationX = sidePageProgress * sideSceneTravelPx
                    }
                    .scaleBlurAlpha(
                        targetValues = faceLayerValues(faceAnimState),
                        screenHeight = screenHeightPx,
                        blurEnabled = layerBlurEnabled
                    )
            ) {
                AnimatedContent(
                    targetState = if (watchFaceSelectionReady || selectedWatchFace.isBuiltin || selectedWatchFaceId == BUILT_IN_WATCHFACE_ID) {
                        selectedWatchFace.stableKey
                    } else {
                        "loading"
                    },
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220, delayMillis = 70)) togetherWith
                            fadeOut(animationSpec = tween(180))
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "watchface_switch"
                ) { targetKey ->
                    if (targetKey == "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    } else if (selectedWatchFace.isBuiltin) {
                        WatchFaceLayer(
                            watchFaceId = selectedWatchFace.id,
                            photoPath = builtInPhotoPath,
                            videoPath = builtInVideoPath,
                            isFaceVisible = screenState == ScreenState.Face,
                            photoOptions = BuiltInWatchFaceOptions(
                                clockPosition = builtInPhotoClockPosition,
                                clockSizeSp = builtInPhotoClockSize,
                                boldClock = builtInPhotoClockBold
                            ),
                            videoOptions = BuiltInWatchFaceOptions(
                                clockPosition = builtInVideoClockPosition,
                                clockSizeSp = builtInVideoClockSize,
                                boldClock = builtInVideoClockBold,
                                cropToFill = builtInVideoFillScreen,
                                clockColorMode = builtInVideoClockColorMode
                            ),
                            onLongPress = openWatchFaceChooser
                        )
                    } else {
                        LunchWatchFaceHost(
                            descriptor = selectedWatchFace,
                            isFaceVisible = screenState == ScreenState.Face,
                            refreshToken = watchFaceRefreshToken,
                            onLongPress = openWatchFaceChooser,
                            onLoadFailure = { descriptor, error ->
                                val rootCause = generateSequence(error) { it.cause }.last()
                                vm.fallbackToBuiltIn("${descriptor.displayName}: ${rootCause.message ?: rootCause.javaClass.simpleName}")
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (screenState == ScreenState.Apps) 3f else 0f)
                    .scaleBlurAlpha(
                        targetValues = appListLayerValues(screenState),
                        screenHeight = screenHeightPx,
                        blurEnabled = layerBlurEnabled,
                        origin = if (useOrigin) appOpenOrigin else null
                    )
            ) {
                when (layoutMode) {
                    LayoutMode.Honeycomb -> HoneycombScreen(
                        apps = apps,
                        blurEnabled = blurEnabled,
                        edgeBlurEnabled = edgeBlurEnabled,
                        suppressHeavyEffects = reduceLegacyDrawerEffects,
                        narrowCols = honeycombCols,
                        topBlurRadiusDp = honeycombTopBlur,
                        bottomBlurRadiusDp = honeycombBottomBlur,
                        topFadeRangeDp = honeycombTopFade,
                        bottomFadeRangeDp = honeycombBottomFade,
                        fastScrollOptimizationEnabled = honeycombFastScrollOptimization,
                        onAppClick = { appInfo, origin ->
                            val launchDelay = BASE_LAUNCH_MASK_DELAY_MS + if (splashIcon) splashDelay.toLong() else 0L
                            vm.openApp(appInfo, origin, launchDelay, ScreenState.Apps)
                        },
                        onReorder = { from, to -> vm.swapApps(from, to) },
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )
                    LayoutMode.List -> ListDrawerScreen(
                        apps = apps,
                        blurEnabled = blurEnabled,
                        edgeBlurEnabled = edgeBlurEnabled,
                        suppressHeavyEffects = reduceLegacyDrawerEffects,
                        topFadeRangeDp = honeycombTopFade,
                        bottomFadeRangeDp = honeycombBottomFade,
                        onAppClick = { appInfo, origin ->
                            val launchDelay = BASE_LAUNCH_MASK_DELAY_MS + if (splashIcon) splashDelay.toLong() else 0L
                            vm.openApp(appInfo, origin, launchDelay, ScreenState.Apps)
                        },
                        onReorder = { from, to -> vm.swapApps(from, to) },
                        onScrollToTop = { vm.setState(ScreenState.Face) }
                    )
                }
            }

            if (screenState == ScreenState.App) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent().changes.forEach { it.consume() }
                                }
                            }
                        }
                )
            }

            AnimatedVisibility(
                visible = showLaunchBackdrop,
                modifier = Modifier.zIndex(6f),
                enter = fadeIn(animationSpec = tween(140)),
                exit = fadeOut(animationSpec = tween(180)) +
                    slideOutVertically(
                        animationSpec = tween(220),
                        targetOffsetY = { fullHeight -> fullHeight / 6 }
                    )
            ) {
                LaunchBackdropContent(
                    showSplash = effectiveLaunchSplash,
                    icon = effectiveLaunchIcon
                )
            }

            if (sideScreenEnabled && sideScreenVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(4f)
                        .graphicsLayer {
                            translationX = (sidePageProgress - 1f) * sideSceneTravelPx
                            alpha = sidePageProgress.coerceIn(0f, 1f) *
                                (1f - 0.58f * notificationProgress)
                        }
                ) {
                    SmartStackLayer(
                        apps = apps,
                        sideScreenShortcuts = sideScreenShortcuts,
                        previewGroups = sideScreenPreviewGroups,
                        notificationsEnabled = notificationsEnabled,
                        notificationAccessGranted = notificationAccessGranted,
                        notificationsSceneActive = screenState == ScreenState.Notifications,
                        notificationTransitionProgress = notificationProgress,
                        revealedNotificationTarget = revealedNotificationTarget,
                        onRevealTargetChange = vm::setRevealedNotificationTarget,
                        onOpenNotifications = { vm.setState(ScreenState.Notifications) },
                        onNotificationTransitionProgressChange = updateNotificationTransitionProgress,
                        onNotificationTransitionRelease = { open ->
                            if (open) {
                                vm.setState(ScreenState.Notifications)
                                settleNotificationTransition(1f)
                            } else {
                                settleNotificationTransition(0f)
                            }
                        },
                        onLaunchApp = { appInfo, origin ->
                            val launchDelay = BASE_LAUNCH_MASK_DELAY_MS + if (splashIcon) splashDelay.toLong() else 0L
                            vm.openApp(appInfo, origin, launchDelay, ScreenState.Stack)
                        },
                        onSetShortcut = vm::setSideScreenShortcut,
                        onRemoveShortcut = vm::removeSideScreenShortcut,
                        onDismissGroup = vm::dismissNotificationGroup,
                        onDismissNotification = vm::dismissNotification,
                        onDismissToFace = { vm.setState(ScreenState.Face) }
                    )
                }
            }

            if (notificationsEnabled && keepNotificationLayer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(5f)
                        .graphicsLayer {
                            alpha = notificationProgress
                            translationY = (1f - notificationProgress) * screenHeightPx
                        }
                ) {
                    NotificationLayer(
                        isActive = notificationSceneActive || notificationGestureActive,
                        transitionProgress = notificationProgress,
                        notificationGroups = notificationGroups,
                        notificationAccessGranted = notificationAccessGranted,
                        revealedNotificationTarget = revealedNotificationTarget,
                        onRevealTargetChange = vm::setRevealedNotificationTarget,
                        onDismissToStack = {
                            vm.setState(if (sideScreenEnabled) ScreenState.Stack else ScreenState.Face)
                            settleNotificationTransition(0f)
                        },
                        onTransitionProgressChange = updateNotificationTransitionProgress,
                        onTransitionRelease = { dismiss ->
                            if (dismiss) {
                                vm.setState(if (sideScreenEnabled) ScreenState.Stack else ScreenState.Face)
                                settleNotificationTransition(0f)
                            } else {
                                settleNotificationTransition(1f)
                            }
                        },
                        onToggleGroup = vm::toggleNotificationGroup,
                        onDismissGroup = vm::dismissNotificationGroup,
                        onDismissNotification = vm::dismissNotification,
                        onOpenNotification = { key, origin ->
                            vm.openNotification(key, origin, ScreenState.Notifications, 0L)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LaunchBackdropContent(
    showSplash: Boolean,
    icon: androidx.compose.ui.graphics.ImageBitmap?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showSplash && icon != null,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.3f)
        ) {
            icon?.let { launchIcon ->
                Image(
                    bitmap = launchIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
