package com.flue.launcher.ui.drawer

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.data.model.iconForDisplay
import com.flue.launcher.ui.anim.platformBlur
import com.flue.launcher.ui.input.DrawerInputMode
import com.flue.launcher.ui.input.DrawerInputSource
import com.flue.launcher.ui.input.flueDrawerRotaryScrollable
import com.flue.launcher.ui.input.normalizeDrawerScrollDelta
import com.flue.launcher.ui.input.requestFocusAfterFirstFrame
import com.flue.launcher.ui.theme.LauncherTheme
import com.flue.launcher.ui.theme.UiStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
private const val LIST_MENU_TRIGGER_MS = 620L
private const val LIST_MENU_DRAG_START_DP = 20f
private const val LIST_FOLDER_HOVER_MS = 700L
private const val LIST_FOLDER_HOVER_MAX_SPEED_DP_PER_MS = 0.55f
private const val LIST_AUTO_SCROLL_EDGE_DP = 84f
private const val LIST_AUTO_SCROLL_MAX_PX = 30f
private const val LIST_EDGE_ITEM_BLUR_DP = 4f
private const val FAST_FLOW_SLOW_TOP_FRACTION = 0.30f
private const val FAST_FLOW_SLOW_BOTTOM_FRACTION = 0.70f
private const val FAST_FLOW_SLOW_SCALE = 0.56f
private const val FAST_FLOW_FAST_SCALE = 1.85f
private const val FAST_FLOW_MAX_OFFSET_PX = 260f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    fisheyeEnabled: Boolean = true,
    fisheyeRangeRows: Int = 4,
    fisheyeStrengthPercent: Int = 100,
    edgeSpacingCompressionEnabled: Boolean = true,
    fastFlowAnimationEnabled: Boolean = false,
    twoToneIconsEnabled: Boolean = false,
    iconShadowEnabled: Boolean = true,
    themeColor: Color = Color(0xFF7BE8FF),
    uiStyle: UiStyle = UiStyle.APPLE_WATCH,
    darkMode: Boolean = true,
    iconSize: Dp = 48.dp,
    topFadeRangeDp: Int = 56,
    bottomFadeRangeDp: Int = 56,
    topBlurRadiusDp: Float = LIST_EDGE_ITEM_BLUR_DP,
    bottomBlurRadiusDp: Float = LIST_EDGE_ITEM_BLUR_DP,
    rotaryHapticsEnabled: Boolean = true,
    active: Boolean = true,
    leftSafeInsetPercent: Int = 0,
    appListScalePercent: Int = 100,
    entryProgress: Float = 1f,
    folderOpen: Boolean = false,
    useWatchFaceColors: Boolean = true,
    rowBorderEnabled: Boolean = false,
    allowFolderCreation: Boolean = true,
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onCreateFolder: (Int, Int) -> Unit = { _, _ -> },
    onLongClick: (AppInfo) -> Unit = {},
    onExcludeApp: (AppInfo) -> Unit = {},
    onRemoveShortcut: (AppInfo) -> Unit = {},
    onRenameFolder: (AppInfo, String) -> Unit = { _, _ -> },
    onDissolveFolder: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val effectiveEdgeBlur = edgeBlurEnabled && !suppressHeavyEffects

    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }
    val itemCenters = remember { mutableMapOf<Int, Float>() }
    val itemHeights = remember { mutableMapOf<Int, Float>() }
    val itemLaunchCenters = remember { mutableMapOf<Int, Offset>() }
    val overscroll = remember { Animatable(0f) }
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentIndex by remember { mutableStateOf<Int?>(null) }
    var folderHoverIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragPointerY by remember { mutableFloatStateOf(Float.NaN) }
    var glidePressedIndex by remember { mutableStateOf<Int?>(null) }
    var settlingApp by remember { mutableStateOf<AppInfo?>(null) }
    var settlingKey by remember { mutableStateOf<String?>(null) }
    val settlingCenterY = remember { Animatable(0f) }
    var focusReady by remember { mutableStateOf(false) }
    var wheelMomentumJob by remember { mutableStateOf<Job?>(null) }
    var fastScrollActive by remember { mutableStateOf(false) }
    var fastScrollResetJob by remember { mutableStateOf<Job?>(null) }
    var containerTopInRoot by remember { mutableFloatStateOf(0f) }
    var returnTriggered by remember { mutableStateOf(false) }
    val visibleIndexes by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }
        }
    }
    val overscrollLimitPx = with(density) { configuration.screenHeightDp.dp.toPx() / 2f }
    val topReturnThresholdPx = with(density) { 72.dp.toPx() }
    val launcherStyle = LauncherTheme.style
    val listPaletteStyle = if (uiStyle == UiStyle.MATERIAL_3) UiStyle.APPLE_WATCH else uiStyle
    val appPalette = appListPalette(themeColor, darkMode, listPaletteStyle, useWatchFaceColors)
    val rowShape = RoundedCornerShape(18.dp)
    val entryVisuals = appListEntryVisuals(entryProgress)
    val appListScale = appListScalePercent.coerceIn(50, 200) / 100f
    val scaledIconSize = (iconSize * appListScale).coerceIn(24.dp, 128.dp)
    val scaledRowVerticalPadding = (10.dp * appListScale).coerceIn(4.dp, 24.dp)
    val scaledRowHorizontalPadding = (16.dp * appListScale).coerceIn(10.dp, 32.dp)
    val scaledIconTextGap = (14.dp * appListScale).coerceIn(8.dp, 28.dp)
    val scaledTextSizeSp = (16f * appListScale).coerceIn(10f, 32f).sp
    val fisheyeMinScale = fisheyeMinScale(fisheyeStrengthPercent)

    LaunchedEffect(visibleIndexes, dragFromIndex, dragCurrentIndex, apps.size) {
        val retain = buildSet {
            addAll(visibleIndexes)
            dragFromIndex?.let(::add)
            dragCurrentIndex?.let(::add)
        }
        itemCenters.keys.retainAll(retain)
        itemHeights.keys.retainAll(retain)
        itemLaunchCenters.keys.retainAll(retain)
    }

    LaunchedEffect(focusReady) {
        if (focusReady) {
            focusRequester.requestFocusAfterFirstFrame()
        }
    }

    LaunchedEffect(active) {
        if (!active) {
            if (!returnTriggered) {
                overscroll.snapTo(0f)
            }
        } else {
            returnTriggered = false
            focusRequester.requestFocusAfterFirstFrame()
        }
    }

    fun markFastScrollActive(durationMs: Long = 180L) {
        fastScrollActive = true
        fastScrollResetJob?.cancel()
        fastScrollResetJob = scope.launch {
            delay(durationMs)
            fastScrollActive = false
        }
    }

    fun launchWheelScroll(delta: Float) {
        if (delta == 0f) return
        wheelMomentumJob?.cancel()
        wheelMomentumJob = scope.launch {
            markFastScrollActive(220L)
            val baseDelta = delta.coerceIn(-96f, 96f)
            listState.scrollBy(baseDelta)
            var tail = baseDelta * 0.35f
            repeat(3) {
                withFrameNanos { }
                if (abs(tail) < 1f) return@launch
                listState.scrollBy(tail)
                tail *= 0.45f
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .flueDrawerRotaryScrollable(
                        focusRequester,
                        DrawerInputMode.List,
                        onRotaryScroll = { if (rotaryHapticsEnabled) vibrateHaptic(context) }
                    ) { rotaryDelta ->
                        wheelMomentumJob?.cancel()
                        scope.launch {
                            markFastScrollActive(180L)
                            listState.scrollBy(-rotaryDelta)
                        }
                    }
                    .onGloballyPositioned {
                        containerTopInRoot = it.positionInRoot().y
                        if (!focusReady) focusReady = true
                    }
                    .pointerInput(listState) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val delta = normalizeDrawerScrollDelta(
                                    verticalScrollPixels = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f,
                                    source = DrawerInputSource.MouseWheel,
                                    mode = DrawerInputMode.List
                                )
                                if (delta != 0f) {
                                    launchWheelScroll(delta)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                .nestedScroll(
                    remember(listState, dragFromIndex, longPressedApp) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (source != NestedScrollSource.UserInput || dragFromIndex != null || longPressedApp != null) return Offset.Zero
                                return consumeListOverscroll(
                                    availableY = available.y,
                                    listState = listState,
                                    overscroll = overscroll,
                                    scope = scope,
                                    overscrollLimitPx = overscrollLimitPx
                                )
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (source != NestedScrollSource.UserInput || dragFromIndex != null || longPressedApp != null) return Offset.Zero
                                return consumeListOverscroll(
                                    availableY = available.y,
                                    listState = listState,
                                    overscroll = overscroll,
                                    scope = scope,
                                    overscrollLimitPx = overscrollLimitPx
                                )
                            }

                            override suspend fun onPreFling(available: Velocity): Velocity {
                                if (dragFromIndex != null || longPressedApp != null) return Velocity.Zero
                                val atTop = !listState.canScrollBackward
                                if (overscroll.value >= topReturnThresholdPx && atTop) {
                                    if (!returnTriggered) {
                                        returnTriggered = true
                                        onScrollToTop()
                                    }
                                    overscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                                    returnTriggered = false
                                    return available
                                }
                                if (overscroll.value != 0f) {
                                    overscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                                    return available
                                }
                                return Velocity.Zero
                            }
                        }
                    }
                )
                .pointerInput(onScrollToTop, dragFromIndex, longPressedApp) {
                    awaitEachGesture {
                        awaitPrimaryDown()
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                        }
                        if (dragFromIndex == null && longPressedApp == null && overscroll.value != 0f) {
                            val shouldReturnToFace = overscroll.value >= topReturnThresholdPx
                            scope.launch {
                                if (shouldReturnToFace) {
                                    returnTriggered = true
                                    onScrollToTop()
                                }
                                overscroll.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = 420f))
                                returnTriggered = false
                            }
                        }
                    }
                }
                .platformBlur(16f, (longPressedApp != null || folderOpen) && blurEnabled && !suppressHeavyEffects)
                .pointerInput(apps) {
                    val maxDistancePx = with(density) { 72.dp.toPx() }
                    val menuDragStartPx = with(density) { LIST_MENU_DRAG_START_DP.dp.toPx() }
                    val folderHoverMaxSpeedPxPerMs = with(density) {
                        LIST_FOLDER_HOVER_MAX_SPEED_DP_PER_MS.dp.toPx()
                    }
                    awaitEachGesture {
                        val down = awaitPrimaryDown()
                        val startIndex = findNearestListIndex(
                            pointerY = down.position.y,
                            itemCenters = itemCenters,
                            maxDistance = maxDistancePx
                        ) ?: return@awaitEachGesture
                        val app = apps.getOrNull(startIndex) ?: return@awaitEachGesture
                        val releaseOverlayHeightPx = itemHeights[startIndex]
                            ?: with(density) { (scaledIconSize + scaledRowVerticalPadding * 2f).toPx() }
                        glidePressedIndex = startIndex
                        val longPress = awaitLongPressByTimeoutOrCancel(
                            pointerId = down.id,
                            downPosition = down.position,
                            timeoutMillis = LIST_MENU_TRIGGER_MS,
                            moveTolerancePx = menuDragStartPx
                        )
                        if (longPress == null) {
                            dragPointerY = Float.NaN
                            glidePressedIndex = null
                            return@awaitEachGesture
                        }

                        onLongClick(app)
                        longPressedApp = app
                        glidePressedIndex = null
                        vibrateHaptic(context)

                        val dragAnchorY = longPress.position.y
                        var previousPointerY = dragAnchorY
                        var previousMoveUptime = longPress.uptimeMillis
                        var dragActive = false
                        var hasDragged = false
                        var folderHoverStartedAt = 0L
                        var folderDropIndex: Int? = null

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!dragActive) {
                                    change.consume()
                                }
                                break
                            }

                            val pointerY = change.position.y
                            val elapsedMs = (change.uptimeMillis - previousMoveUptime).coerceAtLeast(1L)
                            val pointerSpeed = abs(pointerY - previousPointerY) / elapsedMs.toFloat()
                            val deltaFromAnchor = abs(pointerY - dragAnchorY)
                            if (!dragActive && deltaFromAnchor > menuDragStartPx) {
                                longPressedApp = null
                                dragActive = true
                                dragFromIndex = startIndex
                                dragCurrentIndex = startIndex
                                dragPointerY = pointerY
                                glidePressedIndex = null
                                vibrateHaptic(context)
                            }

                            val activeFromIndex = dragFromIndex
                            if (dragActive && activeFromIndex != null) {
                                dragPointerY = pointerY
                                val anchorCenter = itemCenters[activeFromIndex] ?: pointerY
                                dragOffsetY = pointerY - anchorCenter
                                if (abs(dragOffsetY) > menuDragStartPx * 0.35f) {
                                    hasDragged = true
                                }

                                val reorderTarget = findNearestListIndex(
                                    pointerY = pointerY,
                                    itemCenters = itemCenters,
                                    maxDistance = Float.MAX_VALUE
                                )
                                val folderCandidate = if (allowFolderCreation) {
                                    findNearestListIndex(
                                        pointerY = pointerY,
                                        itemCenters = itemCenters,
                                        maxDistance = minOf(
                                            releaseOverlayHeightPx * 0.34f,
                                            with(density) { scaledIconSize.toPx() } * 0.46f
                                        )
                                    )?.takeIf {
                                        it != activeFromIndex && pointerSpeed <= folderHoverMaxSpeedPxPerMs
                                    }
                                } else {
                                    null
                                }
                                if (folderCandidate != null) {
                                    if (folderHoverIndex != folderCandidate) {
                                        folderHoverIndex = folderCandidate
                                        folderHoverStartedAt = change.uptimeMillis
                                        folderDropIndex = null
                                    } else if (change.uptimeMillis - folderHoverStartedAt >= LIST_FOLDER_HOVER_MS) {
                                        folderDropIndex = folderCandidate
                                    }
                                    dragCurrentIndex = reorderTarget ?: dragCurrentIndex
                                } else {
                                    folderHoverIndex = null
                                    folderHoverStartedAt = 0L
                                    folderDropIndex = null
                                    dragCurrentIndex = reorderTarget ?: dragCurrentIndex
                                }
                                previousPointerY = pointerY
                                previousMoveUptime = change.uptimeMillis
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                }
                            }
                        }

                        val from = dragFromIndex
                        val to = dragCurrentIndex
                        val folderTarget = folderDropIndex
                        if (dragActive && from != null && folderTarget != null && from != folderTarget && hasDragged) {
                            dragFromIndex = null
                            dragCurrentIndex = null
                            folderHoverIndex = null
                            dragOffsetY = 0f
                            dragPointerY = Float.NaN
                            glidePressedIndex = null
                            settlingApp = null
                            settlingKey = null
                            onCreateFolder(from, folderTarget)
                        } else if (dragActive && from != null && to != null && from != to && hasDragged) {
                            val droppedApp = apps.getOrNull(from)
                            val releaseCenter = dragPointerY.coerceIn(
                                releaseOverlayHeightPx * 0.5f,
                                size.height.toFloat() - releaseOverlayHeightPx * 0.5f
                            )
                            val targetCenter = findListItemCenter(
                                itemCenters = itemCenters,
                                index = to,
                                fallback = releaseCenter
                            ).coerceIn(
                                releaseOverlayHeightPx * 0.5f,
                                size.height.toFloat() - releaseOverlayHeightPx * 0.5f
                            )
                            dragFromIndex = null
                            dragCurrentIndex = null
                            folderHoverIndex = null
                            dragOffsetY = 0f
                            dragPointerY = Float.NaN
                            glidePressedIndex = null
                            if (droppedApp != null) {
                                settlingApp = droppedApp
                                settlingKey = droppedApp.componentKey
                                scope.launch {
                                    settlingCenterY.snapTo(releaseCenter)
                                    settlingCenterY.animateTo(targetCenter, tween(durationMillis = 170))
                                    onReorder(from, to)
                                    delay(48)
                                    settlingApp = null
                                    settlingKey = null
                                    settlingCenterY.snapTo(0f)
                                }
                            } else {
                                onReorder(from, to)
                            }
                        } else {
                            dragFromIndex = null
                            dragCurrentIndex = null
                            folderHoverIndex = null
                            dragOffsetY = 0f
                            dragPointerY = Float.NaN
                            glidePressedIndex = null
                            settlingApp = null
                            settlingKey = null
                        }
                    }
                }
        ) {
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val containerMaxWidth = maxWidth
            val containerMaxHeight = maxHeight
            val screenCenterY = screenHeightPx / 2f
            val entryOffsetY = (1f - entryProgress.coerceIn(0f, 1f)) * screenHeightPx * 0.08f
            val listViewportCenterY by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    if (info.totalItemsCount > 0) {
                        (info.viewportStartOffset + info.viewportEndOffset) / 2f
                    } else {
                        screenCenterY
                    }
                }
            }
            val fastFlowSlowTopY = screenHeightPx * FAST_FLOW_SLOW_TOP_FRACTION
            val fastFlowSlowBottomY = screenHeightPx * FAST_FLOW_SLOW_BOTTOM_FRACTION
            val autoScrollEdgePx = with(density) { LIST_AUTO_SCROLL_EDGE_DP.dp.toPx() }
            val estimatedItemHeight = scaledIconSize.coerceAtLeast(24.dp) + scaledRowVerticalPadding * 2f
            val centeredPadding = ((maxHeight - estimatedItemHeight) / 2f).coerceAtLeast(8.dp)
            val leftSafeInset = maxWidth * (leftSafeInsetPercent.coerceIn(0, 50) / 100f)
            val launchOriginX = with(density) {
                (leftSafeInset + 12.dp + scaledRowHorizontalPadding + scaledIconSize * 0.5f).toPx()
            }.coerceIn(0f, screenWidthPx) / screenWidthPx
            val visibleItems by remember {
                derivedStateOf {
                    listState.layoutInfo.visibleItemsInfo
                }
            }
            val fisheyeVisibleItems = listState.layoutInfo.visibleItemsInfo
            val fisheyeScrollTick = listState.firstVisibleItemIndex * 100_000 +
                listState.firstVisibleItemScrollOffset
            val dragRowShift = dragFromIndex?.let {
                itemHeights[it] ?: visibleItems.firstOrNull { item -> item.index == it }?.size?.toFloat()
            } ?: with(density) { estimatedItemHeight.toPx() }
            val dragOverlayHeightPx = dragFromIndex?.let {
                itemHeights[it] ?: visibleItems.firstOrNull { item -> item.index == it }?.size?.toFloat()
            } ?: with(density) { (scaledIconSize + scaledRowVerticalPadding * 2f).toPx() }
            val reduceListVisualLoad = (fastScrollActive || listState.isScrollInProgress) &&
                dragFromIndex == null &&
                longPressedApp == null
            AppListEntryBackground(
                maxWidth = containerMaxWidth,
                maxHeight = containerMaxHeight,
                visuals = entryVisuals,
                color = appPalette.background
            )
            LaunchedEffect(dragFromIndex, screenHeightPx) {
                var previousFrameNanos = 0L
                while (dragFromIndex != null) {
                    var pendingScrollDelta = 0f
                    withFrameNanos { frameTimeNanos ->
                        val frameDeltaSeconds = if (previousFrameNanos == 0L) {
                            1f / 60f
                        } else {
                            ((frameTimeNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(1f / 144f, 0.05f)
                        }
                        previousFrameNanos = frameTimeNanos
                        val pointerY = dragPointerY
                        val activeFromIndex = dragFromIndex
                        if (!pointerY.isNaN() && activeFromIndex != null) {
                            val autoScrollVelocity = when {
                                pointerY < autoScrollEdgePx -> {
                                    -LIST_AUTO_SCROLL_MAX_PX * 60f * ((autoScrollEdgePx - pointerY) / autoScrollEdgePx)
                                }
                                pointerY > screenHeightPx - autoScrollEdgePx -> {
                                    LIST_AUTO_SCROLL_MAX_PX * 60f * ((pointerY - (screenHeightPx - autoScrollEdgePx)) / autoScrollEdgePx)
                                }
                                else -> 0f
                            }
                            if (autoScrollVelocity != 0f) {
                                pendingScrollDelta = autoScrollVelocity * frameDeltaSeconds
                            }
                            val anchorCenter = itemCenters[activeFromIndex] ?: pointerY
                            dragOffsetY = pointerY - anchorCenter
                            dragCurrentIndex = findNearestListIndex(
                                pointerY = pointerY,
                                itemCenters = itemCenters,
                                maxDistance = Float.MAX_VALUE
                            ) ?: dragCurrentIndex
                        }
                    }
                    if (pendingScrollDelta != 0f) {
                        listState.scrollBy(pendingScrollDelta)
                    }
                }
            }

            LazyColumn(
                state = listState,
                userScrollEnabled = dragFromIndex == null && longPressedApp == null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = overscroll.value + entryOffsetY },
                contentPadding = PaddingValues(
                    top = centeredPadding,
                    bottom = centeredPadding,
                    start = 12.dp + leftSafeInset,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.componentKey },
                    contentType = { _, _ -> "app_row" }
                ) { index, app ->
                    val itemInfo = fisheyeVisibleItems.firstOrNull { it.index == index }
                    val itemScale = computeItemScale(
                        itemInfo = itemInfo,
                        screenCenterY = listViewportCenterY,
                        screenHeight = screenHeightPx,
                        rowHeight = with(density) { estimatedItemHeight.toPx() },
                        rangeRows = fisheyeRangeRows,
                        minScale = fisheyeMinScale,
                        fisheyeEnabled = fisheyeEnabled,
                        scrollTick = fisheyeScrollTick
                    )
                    val rowCenterY = itemInfo?.let { it.offset + it.size / 2f } ?: listViewportCenterY
                    val flowOffset = computeFastFlowOffset(
                        rowCenterY = rowCenterY,
                        screenCenterY = listViewportCenterY,
                        slowTopY = fastFlowSlowTopY,
                        slowBottomY = fastFlowSlowBottomY,
                        enabled = fastFlowAnimationEnabled &&
                            !reduceListVisualLoad &&
                            dragFromIndex == null &&
                            longPressedApp == null
                    )
                    val displayIcon = remember(
                        app.componentKey,
                        app.cachedIcon,
                        app.cachedTwoToneIcon,
                        twoToneIconsEnabled
                    ) {
                        app.iconForDisplay(
                            useTwoTone = twoToneIconsEnabled,
                            blurred = false
                        )
                    }
                    val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val isDragged = dragFromIndex == index
                    val isSettling = settlingKey == app.componentKey
                    val isGlidePressed = glidePressedIndex == index
                    val isFolderHoverTarget = folderHoverIndex == index
                    val showMenuBackground = longPressedApp?.componentKey == app.componentKey &&
                        dragFromIndex == null
                    val showRowBackground = rowBorderEnabled || showMenuBackground || isFolderHoverTarget
                    val displacedTarget = listDisplacementForIndex(
                        index = index,
                        dragFromIndex = dragFromIndex,
                        dragCurrentIndex = dragCurrentIndex,
                        dragRowShift = dragRowShift
                    )
                    val animatedDisplacement by animateFloatAsState(
                        targetValue = displacedTarget,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                        label = "list_drag_displacement"
                    )
                    val displayDisplacement = if (isDragged) dragOffsetY else animatedDisplacement
                    val pressedScale by animateFloatAsState(
                        targetValue = when {
                            isDragged -> 0.965f
                            isGlidePressed -> 0.992f
                            isPressed -> 0.97f
                            else -> 1f
                        },
                        animationSpec = tween(durationMillis = 170),
                        label = "list_press_scale"
                    )
                    val pressedOverlay by animateFloatAsState(
                        targetValue = when {
                            isDragged -> 0.10f
                            isGlidePressed -> 0.05f
                            isFolderHoverTarget -> 0.20f
                            isPressed -> 0.12f
                            else -> 0f
                        },
                        animationSpec = tween(durationMillis = 170),
                        label = "list_press_overlay"
                    )
                    val pressedOverlayColor = if (isFolderHoverTarget) {
                        Color.Black
                    } else {
                        appPalette.itemPressedOverlay
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val posY = coords.positionInRoot().y - containerTopInRoot
                                itemCenters[index] = posY + coords.size.height / 2f
                                itemHeights[index] = coords.size.height.toFloat()
                            }
                            .graphicsLayer {
                                val targetScale = itemScale * pressedScale
                                translationY = if (isDragged) 0f else displayDisplacement + flowOffset
                                scaleX = targetScale
                                scaleY = targetScale
                                val rowAlpha = if (isDragged || isSettling) {
                                    0f
                                } else if (fisheyeEnabled) {
                                    itemScale.coerceIn(0.3f, 1f)
                                } else {
                                    1f
                                }
                                alpha = rowAlpha * entryVisuals.iconProgress
                            }
                            .then(
                                if (showRowBackground) {
                                    Modifier.background(
                                        appPalette.item.copy(alpha = entryVisuals.surfaceProgress),
                                        rowShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .background(pressedOverlayColor.copy(alpha = pressedOverlay), rowShape)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (longPressedApp != null || dragFromIndex != null) return@combinedClickable
                                    val centerY = (itemCenters[index] ?: rowCenterY)
                                        .let { it + displayDisplacement + flowOffset }
                                        .coerceIn(0f, screenHeightPx)
                                    onAppClick(
                                        app,
                                        Offset(
                                            launchOriginX.coerceIn(0f, 1f),
                                            (centerY / screenHeightPx).coerceIn(0f, 1f)
                                        )
                                    )
                                }
                            )
                            .padding(horizontal = scaledRowHorizontalPadding, vertical = scaledRowVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = displayIcon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(scaledIconSize)
                                .onGloballyPositioned { coords ->
                                    itemLaunchCenters[index] = coords.boundsInRoot().center
                                }
                                .clip(RoundedCornerShape((14.dp * appListScale).coerceIn(10.dp, 24.dp))),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(scaledIconTextGap))
                        Text(
                            text = app.label,
                            fontSize = scaledTextSizeSp,
                            fontWeight = FontWeight.W500,
                            color = appPalette.text
                        )
                    }
                }
            }

            val draggedIndex = dragFromIndex
            val draggedApp = draggedIndex?.let { apps.getOrNull(it) } ?: settlingApp
            val overlayCenterY = when {
                draggedIndex != null && !dragPointerY.isNaN() -> dragPointerY.coerceIn(
                    dragOverlayHeightPx * 0.5f,
                    screenHeightPx - dragOverlayHeightPx * 0.5f
                )
                settlingApp != null -> settlingCenterY.value
                else -> Float.NaN
            }
            if (draggedApp != null && !overlayCenterY.isNaN()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = overlayCenterY - dragOverlayHeightPx / 2f
                            scaleX = 0.965f
                            scaleY = 0.965f
                            alpha = 0.98f
                            shadowElevation = if (iconShadowEnabled) 18.dp.toPx() else 0f
                        }
                        .then(
                            if (rowBorderEnabled) {
                                Modifier.background(appPalette.item, rowShape)
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = (28.dp * appListScale).coerceIn(14.dp, 40.dp), vertical = scaledRowVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = draggedApp.iconForDisplay(
                            useTwoTone = twoToneIconsEnabled,
                            blurred = false
                        ),
                        contentDescription = draggedApp.label,
                        modifier = Modifier
                            .size(scaledIconSize)
                            .clip(RoundedCornerShape((14.dp * appListScale).coerceIn(10.dp, 24.dp))),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(scaledIconTextGap))
                    Text(
                        text = draggedApp.label,
                        fontSize = scaledTextSizeSp,
                        fontWeight = FontWeight.W500,
                        color = appPalette.text
                    )
                }
            }
        }

        if ((active || entryProgress > 0.001f) && topFadeRangeDp > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topFadeRangeDp.coerceAtLeast(0).dp)
                    .graphicsLayer { alpha = entryVisuals.edgeProgress }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                appPalette.fadeEdge.copy(alpha = 0.78f),
                                appPalette.fadeEdge.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
                    .platformBlur(topBlurRadiusDp, blurEnabled && effectiveEdgeBlur)
            )
        }
        if ((active || entryProgress > 0.001f) && bottomFadeRangeDp > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomFadeRangeDp.coerceAtLeast(0).dp)
                    .graphicsLayer { alpha = entryVisuals.edgeProgress }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                appPalette.fadeEdge.copy(alpha = 0.28f),
                                appPalette.fadeEdge.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .platformBlur(bottomBlurRadiusDp, blurEnabled && effectiveEdgeBlur)
            )
        }
    }

    longPressedApp?.let { app ->
        AppShortcutOverlay(
            app = app,
            blurEnabled = blurEnabled,
            onExcludeApp = { onExcludeApp(app) },
            onRemoveShortcut = if (app.isAppListShortcut) { { onRemoveShortcut(app) } } else null,
            onRenameFolder = if (app.isFolder) { name -> onRenameFolder(app, name) } else null,
            onDissolveFolder = if (app.isFolder) { { onDissolveFolder(app) } } else null,
            onDismiss = { longPressedApp = null }
        )
    }
}

private fun consumeListOverscroll(
    availableY: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    overscroll: Animatable<Float, AnimationVector1D>,
    scope: kotlinx.coroutines.CoroutineScope,
    overscrollLimitPx: Float
): Offset {
    val atTop = !listState.canScrollBackward
    val atBottom = !listState.canScrollForward
    val current = overscroll.value
    val next = when {
        availableY > 0f && atTop -> (current + availableY).coerceAtMost(overscrollLimitPx)
        availableY < 0f && atBottom -> (current + availableY).coerceAtLeast(-overscrollLimitPx)
        current > 0f && availableY < 0f -> (current + availableY).coerceAtLeast(0f)
        current < 0f && availableY > 0f -> (current + availableY).coerceAtMost(0f)
        else -> current
    }
    if (next == current) return Offset.Zero
    scope.launch { overscroll.snapTo(next) }
    return Offset(0f, availableY)
}

private fun listDisplacementForIndex(
    index: Int,
    dragFromIndex: Int?,
    dragCurrentIndex: Int?,
    dragRowShift: Float
): Float {
    if (dragFromIndex == null || dragCurrentIndex == null || dragFromIndex == dragCurrentIndex) return 0f
    return when {
        dragCurrentIndex > dragFromIndex && index in (dragFromIndex + 1)..dragCurrentIndex -> -dragRowShift
        dragCurrentIndex < dragFromIndex && index in dragCurrentIndex until dragFromIndex -> dragRowShift
        else -> 0f
    }
}

private fun findNearestListIndex(
    pointerY: Float,
    itemCenters: Map<Int, Float>,
    maxDistance: Float
): Int? {
    var bestIndex: Int? = null
    var bestDistance = Float.MAX_VALUE
    itemCenters.forEach { (index, centerY) ->
        val distance = abs(centerY - pointerY)
        if (distance < bestDistance && distance <= maxDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

private fun findListItemCenter(
    itemCenters: Map<Int, Float>,
    index: Int,
    fallback: Float
): Float {
    return itemCenters[index] ?: fallback
}

@Suppress("UNUSED_PARAMETER")
private fun computeItemScale(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenCenterY: Float,
    screenHeight: Float,
    rowHeight: Float,
    rangeRows: Int,
    minScale: Float,
    fisheyeEnabled: Boolean,
    scrollTick: Int = 0
): Float {
    if (!fisheyeEnabled) return 1f
    if (itemInfo == null) return 1f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val dist = abs(itemCenterY - screenCenterY)
    val maxDist = (rowHeight * rangeRows.coerceIn(1, 8)).coerceIn(rowHeight, screenHeight)
    val t = (dist / maxDist).coerceIn(0f, 1f)
    return 1f - (1f - minScale) * t
}

private fun computeFastFlowOffset(
    rowCenterY: Float,
    screenCenterY: Float,
    slowTopY: Float,
    slowBottomY: Float,
    enabled: Boolean
): Float {
    if (!enabled) return 0f
    val transformedY = when {
        rowCenterY < slowTopY -> {
            screenCenterY + (slowTopY - screenCenterY) * FAST_FLOW_SLOW_SCALE +
                (rowCenterY - slowTopY) * FAST_FLOW_FAST_SCALE
        }
        rowCenterY > slowBottomY -> {
            screenCenterY + (slowBottomY - screenCenterY) * FAST_FLOW_SLOW_SCALE +
                (rowCenterY - slowBottomY) * FAST_FLOW_FAST_SCALE
        }
        else -> screenCenterY + (rowCenterY - screenCenterY) * FAST_FLOW_SLOW_SCALE
    }
    return (transformedY - rowCenterY).coerceIn(-FAST_FLOW_MAX_OFFSET_PX, FAST_FLOW_MAX_OFFSET_PX)
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitLongPressByTimeoutOrCancel(
    pointerId: androidx.compose.ui.input.pointer.PointerId,
    downPosition: Offset,
    timeoutMillis: Long,
    moveTolerancePx: Float = viewConfiguration.touchSlop
): androidx.compose.ui.input.pointer.PointerInputChange? {
    val cancelled = withTimeoutOrNull<Boolean>(timeoutMillis) {
        var cancelledByGesture = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId }
            if (change == null) {
                cancelledByGesture = true
                break
            }
            if (!change.pressed) {
                cancelledByGesture = true
                break
            }
            if ((change.position - downPosition).getDistance() > moveTolerancePx) {
                cancelledByGesture = true
                break
            }
        }
        cancelledByGesture
    } ?: false
    if (cancelled) return null
    val current = currentEvent.changes.firstOrNull { it.id == pointerId }
    return current?.takeIf { it.pressed }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPrimaryDown():
    androidx.compose.ui.input.pointer.PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.pressed }
        if (change != null) return change
    }
}
