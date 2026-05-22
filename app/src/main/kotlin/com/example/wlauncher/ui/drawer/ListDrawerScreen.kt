package com.flue.launcher.ui.drawer

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.ui.anim.platformBlur
import com.flue.launcher.ui.input.DrawerInputMode
import com.flue.launcher.ui.input.DrawerInputSource
import com.flue.launcher.ui.input.flueDrawerRotaryScrollable
import com.flue.launcher.ui.input.normalizeDrawerScrollDelta
import com.flue.launcher.ui.input.requestFocusAfterFirstFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

private const val LIST_MENU_TRIGGER_MS = 410L
private const val LIST_MENU_DRAG_START_DP = 20f
private const val LIST_AUTO_SCROLL_EDGE_DP = 84f
private const val LIST_AUTO_SCROLL_MAX_PX = 30f
private const val LIST_EDGE_ITEM_BLUR_DP = 4f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    blurEnabled: Boolean = true,
    edgeBlurEnabled: Boolean = false,
    suppressHeavyEffects: Boolean = false,
    iconSize: Dp = 48.dp,
    topFadeRangeDp: Int = 56,
    bottomFadeRangeDp: Int = 56,
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onLongClick: (AppInfo) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val effectiveEdgeBlur = edgeBlurEnabled && !suppressHeavyEffects

    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }
    val itemCenters = remember { mutableMapOf<Int, Float>() }
    val itemHeights = remember { mutableMapOf<Int, Float>() }
    val overscroll = remember { Animatable(0f) }
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragPointerY by remember { mutableFloatStateOf(Float.NaN) }
    var glidePressedIndex by remember { mutableStateOf<Int?>(null) }
    var initializedAtTop by remember { mutableStateOf(false) }
    var settlingApp by remember { mutableStateOf<AppInfo?>(null) }
    var settlingKey by remember { mutableStateOf<String?>(null) }
    val settlingCenterY = remember { Animatable(0f) }
    var focusReady by remember { mutableStateOf(false) }
    var wheelMomentumJob by remember { mutableStateOf<Job?>(null) }
    val visibleIndexes = remember(listState.layoutInfo.visibleItemsInfo) {
        listState.layoutInfo.visibleItemsInfo.map { it.index }
    }

    LaunchedEffect(visibleIndexes, dragFromIndex, dragCurrentIndex, apps.size) {
        val retain = buildSet {
            addAll(visibleIndexes)
            dragFromIndex?.let(::add)
            dragCurrentIndex?.let(::add)
        }
        itemCenters.keys.retainAll(retain)
        itemHeights.keys.retainAll(retain)
    }

    LaunchedEffect(focusReady) {
        if (focusReady) {
            focusRequester.requestFocusAfterFirstFrame()
        }
    }
    fun launchWheelScroll(delta: Float) {
        if (delta == 0f) return
        wheelMomentumJob?.cancel()
        wheelMomentumJob = scope.launch {
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
    LaunchedEffect(apps.size) {
        if (!initializedAtTop && apps.isNotEmpty()) {
            listState.scrollToItem(0)
            initializedAtTop = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .flueDrawerRotaryScrollable(focusRequester, DrawerInputMode.List) { rotaryDelta ->
                        wheelMomentumJob?.cancel()
                        scope.launch { listState.scrollBy(-rotaryDelta) }
                    }
                    .onGloballyPositioned {
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
                                if (source != NestedScrollSource.Drag || dragFromIndex != null || longPressedApp != null) return Offset.Zero
                                return consumeListOverscroll(
                                    availableY = available.y,
                                    listState = listState,
                                    overscroll = overscroll,
                                    scope = scope
                                )
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (source != NestedScrollSource.Drag || dragFromIndex != null || longPressedApp != null) return Offset.Zero
                                return consumeListOverscroll(
                                    availableY = available.y,
                                    listState = listState,
                                    overscroll = overscroll,
                                    scope = scope
                                )
                            }

                            override suspend fun onPreFling(available: Velocity): Velocity {
                                if (dragFromIndex != null || longPressedApp != null) return Velocity.Zero
                                val atTop = !listState.canScrollBackward
                                if ((overscroll.value > 80f || available.y > 1200f) && atTop) {
                                    scope.launch {
                                        overscroll.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 460f))
                                    }
                                    onScrollToTop()
                                    return available
                                }
                                if (overscroll.value != 0f) {
                                    overscroll.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 460f))
                                    return available
                                }
                                return Velocity.Zero
                            }
                        }
                    }
                )
                .platformBlur(16f, longPressedApp != null && blurEnabled && !suppressHeavyEffects)
                .pointerInput(apps) {
                    val maxDistancePx = with(density) { 72.dp.toPx() }
                    val menuDragStartPx = with(density) { LIST_MENU_DRAG_START_DP.dp.toPx() }
                    awaitEachGesture {
                        val releaseOverlayHeightPx = dragFromIndex?.let { itemHeights[it] }
                            ?: with(density) { (iconSize + 20.dp).toPx() }
                        val down = awaitPrimaryDown()
                        val startIndex = findNearestListIndex(
                            pointerY = down.position.y,
                            itemCenters = itemCenters,
                            maxDistance = maxDistancePx
                        ) ?: return@awaitEachGesture
                        val app = apps.getOrNull(startIndex) ?: return@awaitEachGesture
                        glidePressedIndex = startIndex
                        val longPress = awaitLongPressByTimeoutOrCancel(
                            pointerId = down.id,
                            downPosition = down.position,
                            timeoutMillis = LIST_MENU_TRIGGER_MS
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
                        var dragActive = false
                        var hasDragged = false

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

                                dragCurrentIndex = findNearestListIndex(
                                    pointerY = pointerY,
                                    itemCenters = itemCenters,
                                    maxDistance = Float.MAX_VALUE
                                ) ?: dragCurrentIndex
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                }
                            }
                        }

                        val from = dragFromIndex
                        val to = dragCurrentIndex
                        if (dragActive && from != null && to != null && from != to && hasDragged) {
                            val droppedApp = apps.getOrNull(from)
                            val releaseCenter = dragPointerY.coerceIn(
                                releaseOverlayHeightPx * 0.5f,
                                size.height.toFloat() - releaseOverlayHeightPx * 0.5f
                            )
                            val targetCenter = (itemCenters[to] ?: releaseCenter).coerceIn(
                                releaseOverlayHeightPx * 0.5f,
                                size.height.toFloat() - releaseOverlayHeightPx * 0.5f
                            )
                            dragFromIndex = null
                            dragCurrentIndex = null
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
            val screenCenterY = screenHeightPx / 2f
            val autoScrollEdgePx = with(density) { LIST_AUTO_SCROLL_EDGE_DP.dp.toPx() }
            val topEdgeBlurZonePx = with(density) { topFadeRangeDp.coerceIn(0, 220).dp.toPx() }
            val bottomEdgeBlurZonePx = with(density) { bottomFadeRangeDp.coerceIn(0, 220).dp.toPx() }
            val estimatedItemHeight = iconSize.coerceAtLeast(48.dp) + 20.dp
            val centeredPadding = 8.dp
            val dragRowShift = dragFromIndex?.let { itemHeights[it] } ?: with(density) { estimatedItemHeight.toPx() }
            val dragOverlayHeightPx = dragFromIndex?.let { itemHeights[it] } ?: with(density) { (iconSize + 20.dp).toPx() }
            val visibleInfoByIndex = remember(listState.layoutInfo.visibleItemsInfo) {
                listState.layoutInfo.visibleItemsInfo.associateBy { it.index }
            }
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
                    .graphicsLayer { translationY = overscroll.value }
                    .background(Color.Black),
                contentPadding = PaddingValues(
                    top = centeredPadding,
                    bottom = centeredPadding,
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(apps, key = { _, app -> app.componentKey }) { index, app ->
                    val itemInfo = visibleInfoByIndex[index]
                    val itemScale = computeItemScale(itemInfo, screenCenterY, screenHeightPx)
                    val itemBlur = computeVerticalEdgeBlur(
                        centerY = itemInfo?.let { it.offset + it.size / 2f } ?: screenCenterY,
                        screenHeight = screenHeightPx,
                        topBlurZonePx = topEdgeBlurZonePx,
                        bottomBlurZonePx = bottomEdgeBlurZonePx,
                        maxBlurDp = LIST_EDGE_ITEM_BLUR_DP
                    )
                    val useBlurredIcon = (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                        blurEnabled &&
                        effectiveEdgeBlur &&
                        itemBlur > 0.5f
                    )
                    val displayIcon = if (useBlurredIcon) app.cachedBlurredIcon else app.cachedIcon
                    val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val isDragged = dragFromIndex == index
                    val isSettling = settlingKey == app.componentKey
                    val isGlidePressed = glidePressedIndex == index
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
                            isPressed -> 0.12f
                            else -> 0f
                        },
                        animationSpec = tween(durationMillis = 170),
                        label = "list_press_overlay"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                val posY = coords.positionInRoot().y
                                itemCenters[index] = posY + coords.size.height / 2f
                                itemHeights[index] = coords.size.height.toFloat()
                            }
                            .graphicsLayer {
                                val targetScale = itemScale * pressedScale
                                translationY = if (isDragged) 0f else displayDisplacement
                                scaleX = targetScale
                                scaleY = targetScale
                                alpha = if (isDragged || isSettling) 0f else itemScale.coerceIn(0.3f, 1f)
                            }
                            .platformBlur(itemBlur, blurEnabled && effectiveEdgeBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            .background(Color.Black.copy(alpha = pressedOverlay), RoundedCornerShape(18.dp))
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (longPressedApp != null || dragFromIndex != null) return@combinedClickable
                                    val centerY = itemCenters[index] ?: screenCenterY
                                    onAppClick(app, Offset(0.15f, centerY / screenHeightPx))
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = displayIcon,
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = app.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            color = Color.White
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
                val draggedBlur = computeVerticalEdgeBlur(
                    centerY = overlayCenterY,
                    screenHeight = screenHeightPx,
                    topBlurZonePx = topEdgeBlurZonePx,
                    bottomBlurZonePx = bottomEdgeBlurZonePx,
                    maxBlurDp = LIST_EDGE_ITEM_BLUR_DP
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .platformBlur(draggedBlur, blurEnabled && effectiveEdgeBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        .graphicsLayer {
                            translationY = overlayCenterY - dragOverlayHeightPx / 2f
                            scaleX = 0.965f
                            scaleY = 0.965f
                            alpha = 0.98f
                            shadowElevation = 18.dp.toPx()
                        }
                        .background(Color.Black.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val overlayUseBlurredIcon = (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                            blurEnabled &&
                            effectiveEdgeBlur &&
                            draggedBlur > 0.5f
                        )
                    Image(
                        bitmap = if (overlayUseBlurredIcon) draggedApp.cachedBlurredIcon else draggedApp.cachedIcon,
                        contentDescription = draggedApp.label,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = draggedApp.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        color = Color.White
                    )
                }
            }
        }

        if (blurEnabled && effectiveEdgeBlur) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topFadeRangeDp.coerceAtLeast(0).dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.38f),
                                Color.Black.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
                    .platformBlur(LIST_EDGE_ITEM_BLUR_DP, true)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomFadeRangeDp.coerceAtLeast(0).dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.14f),
                                Color.Black.copy(alpha = 0.42f)
                            )
                        )
                    )
                    .platformBlur(LIST_EDGE_ITEM_BLUR_DP, true)
            )
        }
        if (topFadeRangeDp > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topFadeRangeDp.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)))
            )
        }
        if (bottomFadeRangeDp > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(bottomFadeRangeDp.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            )
        }
    }

    longPressedApp?.let { app ->
        AppShortcutOverlay(
            app = app,
            blurEnabled = blurEnabled,
            onDismiss = { longPressedApp = null }
        )
    }
}

private fun consumeListOverscroll(
    availableY: Float,
    listState: androidx.compose.foundation.lazy.LazyListState,
    overscroll: Animatable<Float, AnimationVector1D>,
    scope: kotlinx.coroutines.CoroutineScope
): Offset {
    val atTop = !listState.canScrollBackward
    val atBottom = !listState.canScrollForward
    val current = overscroll.value
    val next = when {
        availableY > 0f && atTop -> (current + availableY * 0.35f).coerceAtMost(180f)
        availableY < 0f && atBottom -> (current + availableY * 0.35f).coerceAtLeast(-180f)
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

private fun computeItemScale(
    itemInfo: androidx.compose.foundation.lazy.LazyListItemInfo?,
    screenCenterY: Float,
    screenHeight: Float
): Float {
    if (itemInfo == null) return 0.85f
    val itemCenterY = itemInfo.offset + itemInfo.size / 2f
    val dist = abs(itemCenterY - screenCenterY)
    val maxDist = screenHeight / 2f
    val t = (dist / maxDist).coerceIn(0f, 1f)
    return 1f - 0.2f * t
}

private fun computeVerticalEdgeBlur(
    centerY: Float,
    screenHeight: Float,
    topBlurZonePx: Float,
    bottomBlurZonePx: Float,
    maxBlurDp: Float
): Float {
    val topStrength = if (topBlurZonePx <= 0f) {
        0f
    } else {
        (1f - (centerY / topBlurZonePx)).coerceIn(0f, 1f)
    }
    val bottomDistance = screenHeight - centerY
    val bottomStrength = if (bottomBlurZonePx <= 0f) {
        0f
    } else {
        (1f - (bottomDistance / bottomBlurZonePx)).coerceIn(0f, 1f)
    }
    return maxOf(topStrength, bottomStrength) * maxBlurDp
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitLongPressByTimeoutOrCancel(
    pointerId: androidx.compose.ui.input.pointer.PointerId,
    downPosition: Offset,
    timeoutMillis: Long
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
            if ((change.position - downPosition).getDistance() > viewConfiguration.touchSlop) {
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
