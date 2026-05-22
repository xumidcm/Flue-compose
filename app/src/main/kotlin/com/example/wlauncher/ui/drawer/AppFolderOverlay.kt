package com.flue.launcher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flue.launcher.data.model.AppInfo
import com.flue.launcher.data.model.iconForDisplay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val FOLDER_ITEM_MENU_TRIGGER_MS = 620L
private const val FOLDER_ITEM_DRAG_OUT_MS = 2_000L
private const val FOLDER_GRID_COLUMNS = 3

@Composable
fun AppFolderOverlay(
    folder: AppInfo,
    items: List<AppInfo>,
    listMode: Boolean,
    blurEnabled: Boolean,
    twoToneIconsEnabled: Boolean,
    onAppClick: (AppInfo, Offset) -> Unit,
    onReorderItems: (List<AppInfo>) -> Unit,
    onMoveItemOut: (AppInfo) -> Unit,
    onRenameFolder: (String) -> Unit,
    onExcludeApp: (AppInfo) -> Unit,
    onRemoveShortcut: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val folderOverscroll = remember { androidx.compose.animation.core.Animatable(0f) }
    val visibleItems = remember { mutableStateListOf<AppInfo>() }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var showing by remember(folder.componentKey) { mutableStateOf(false) }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameDialogVisible by remember { mutableStateOf(false) }
    var renameText by remember(folder.componentKey) { mutableStateOf(folder.label) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
    var draggedOutsidePanel by remember { mutableStateOf(false) }
    var hoverKey by remember { mutableStateOf<String?>(null) }
    var moveOutJob by remember { mutableStateOf<Job?>(null) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }
    val dismissInteraction = remember { MutableInteractionSource() }
    val blockInteraction = remember { MutableInteractionSource() }
    val dragThresholdPx = with(density) { 22.dp.toPx() }
    val dragIconHalfSizePx = with(density) { 29.dp.toPx() }
    val folderOverscrollLimitPx = with(density) { 72.dp.toPx() }

    @Suppress("UNUSED_VARIABLE")
    val sameStyleForEveryLayout = listMode

    fun cancelMoveOutJob() {
        moveOutJob?.cancel()
        moveOutJob = null
    }

    fun requestDismiss() {
        if (dismissJob != null) return
        cancelMoveOutJob()
        menuApp = null
        renameDialogVisible = false
        showing = false
        dismissJob = scope.launch {
            delay(190)
            onDismiss()
        }
    }

    fun finishMoveOut(app: AppInfo) {
        cancelMoveOutJob()
        draggedKey = null
        draggedOutsidePanel = false
        hoverKey = null
        onMoveItemOut(app)
        requestDismiss()
    }

    fun persistOrderIfChanged(originalKeys: List<String>) {
        val currentKeys = visibleItems.map { it.componentKey }
        if (currentKeys != originalKeys) {
            onReorderItems(visibleItems.toList())
        }
    }

    fun moveVisibleItem(from: Int, to: Int) {
        if (from !in visibleItems.indices || to !in visibleItems.indices || from == to) return
        val item = visibleItems.removeAt(from)
        visibleItems.add(to.coerceIn(0, visibleItems.size), item)
    }

    LaunchedEffect(folder.componentKey, items.map { it.componentKey }.joinToString("|")) {
        visibleItems.clear()
        visibleItems.addAll(items)
        itemBounds.clear()
        hoverKey = null
    }
    LaunchedEffect(folder.componentKey) {
        dismissJob?.cancel()
        dismissJob = null
        showing = true
    }
    LaunchedEffect(draggedKey) {
        if (draggedKey != null && folderOverscroll.value != 0f) {
            folderOverscroll.snapTo(0f)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            cancelMoveOutJob()
            dismissJob?.cancel()
        }
    }
    BackHandler(enabled = true) { requestDismiss() }

    fun gridAtTop(): Boolean {
        return gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
    }

    fun gridAtBottom(): Boolean {
        val info = gridState.layoutInfo
        val last = info.visibleItemsInfo.maxByOrNull { it.index } ?: return true
        return last.index >= info.totalItemsCount - 1 &&
            last.offset.y + last.size.height <= info.viewportEndOffset
    }

    fun consumeFolderOverscroll(availableY: Float): Offset {
        if (draggedKey != null || availableY == 0f) return Offset.Zero
        val current = folderOverscroll.value
        val next = when {
            availableY > 0f && current < 0f ->
                (current + availableY * 0.45f).coerceAtMost(0f)
            availableY < 0f && current > 0f ->
                (current + availableY * 0.45f).coerceAtLeast(0f)
            availableY > 0f && gridAtTop() ->
                (current + availableY * 0.45f).coerceAtMost(folderOverscrollLimitPx)
            availableY < 0f && gridAtBottom() ->
                (current + availableY * 0.45f).coerceAtLeast(-folderOverscrollLimitPx)
            else -> return Offset.Zero
        }
        scope.launch { folderOverscroll.snapTo(next) }
        return Offset(0f, availableY)
    }

    val folderNestedScrollConnection = remember(gridState, draggedKey, folderOverscrollLimitPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return consumeFolderOverscroll(available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                return consumeFolderOverscroll(available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (folderOverscroll.value != 0f) {
                    folderOverscroll.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 380f))
                }
                return Velocity.Zero
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (showing) 1f else 0f,
        animationSpec = tween(180),
        label = "folder_overlay_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (showing) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "folder_overlay_scale"
    )

    if (renameDialogVisible) {
        AlertDialog(
            onDismissRequest = { renameDialogVisible = false },
            title = { Text("重命名文件夹") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameFolder(renameText)
                        renameDialogVisible = false
                    }
                ) {
                    Text("完成")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogVisible = false }) {
                    Text("取消")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black.copy(alpha = if (blurEnabled) 0.58f else 0.46f))
            .clickable(indication = null, interactionSource = dismissInteraction) { requestDismiss() },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val panelMaxHeight = (maxHeight * 0.76f).coerceAtLeast(210.dp)
            val gridMaxHeight = (panelMaxHeight - 76.dp).coerceAtLeast(150.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = panelMaxHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .onGloballyPositioned { panelBounds = it.boundsInRoot() }
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xE62A2A2C))
                    .clickable(indication = null, interactionSource = blockInteraction) { }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .align(Alignment.Center)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = folder.label,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W700,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            renameText = folder.label
                            renameDialogVisible = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(FOLDER_GRID_COLUMNS),
                    state = gridState,
                    contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .heightIn(min = 150.dp, max = gridMaxHeight)
                        .nestedScroll(folderNestedScrollConnection)
                        .graphicsLayer { translationY = folderOverscroll.value }
                ) {
                    itemsIndexed(
                        items = visibleItems,
                        key = { _, app -> app.componentKey },
                        contentType = { _, _ -> "folder_app" }
                    ) { index, app ->
                        val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val isDragged = draggedKey == app.componentKey
                        val isFolderHoverTarget = hoverKey == app.componentKey
                        val pressedScale by animateFloatAsState(
                            targetValue = when {
                                isDragged -> 0.92f
                                isFolderHoverTarget -> 0.96f
                                isPressed -> 0.95f
                                else -> 1f
                            },
                            animationSpec = tween(150),
                            label = "folder_item_press"
                        )
                        val pressedOverlay by animateFloatAsState(
                            targetValue = when {
                                isDragged -> 0.18f
                                isFolderHoverTarget -> 0.26f
                                isPressed -> 0.12f
                                else -> 0f
                            },
                            animationSpec = tween(150),
                            label = "folder_item_overlay"
                        )
                        Column(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = tween(140),
                                    placementSpec = spring(stiffness = 520f, dampingRatio = 0.84f),
                                    fadeOutSpec = tween(120)
                                )
                                .onGloballyPositioned { coords ->
                                    itemBounds[app.componentKey] = coords.boundsInRoot()
                                }
                                .graphicsLayer {
                                    scaleX = pressedScale
                                    scaleY = pressedScale
                                    this.alpha = if (isDragged) 0f else 1f
                                }
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = pressedOverlay))
                                .pointerInput(app.componentKey, panelBounds, visibleItems.size) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val originalKeys = visibleItems.map { it.componentKey }
                                        var longPressCancelled = false
                                        withTimeoutOrNull(FOLDER_ITEM_MENU_TRIGGER_MS) {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                    ?: run {
                                                        longPressCancelled = true
                                                        return@withTimeoutOrNull
                                                    }
                                                if (!change.pressed) {
                                                    longPressCancelled = true
                                                    return@withTimeoutOrNull
                                                }
                                                if ((change.position - down.position).getDistance() > dragThresholdPx) {
                                                    longPressCancelled = true
                                                    return@withTimeoutOrNull
                                                }
                                            }
                                        }
                                        if (longPressCancelled) return@awaitEachGesture

                                        menuApp = app
                                        vibrateHaptic(context)
                                        var dragActive = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) break
                                            val movedDistance = (change.position - down.position).getDistance()
                                            if (!dragActive && movedDistance > dragThresholdPx) {
                                                menuApp = null
                                                dragActive = true
                                                draggedKey = app.componentKey
                                                draggedOutsidePanel = false
                                                vibrateHaptic(context)
                                            }
                                            if (dragActive) {
                                                val bounds = itemBounds[app.componentKey]
                                                val rootPointer = if (bounds != null) {
                                                    Offset(bounds.left + change.position.x, bounds.top + change.position.y)
                                                } else {
                                                    change.position
                                                }
                                                dragPointerRoot = rootPointer
                                                val insidePanel = panelBounds.contains(rootPointer)
                                                draggedOutsidePanel = !insidePanel
                                                if (insidePanel) {
                                                    cancelMoveOutJob()
                                                    val from = visibleItems.indexOfFirst { it.componentKey == app.componentKey }
                                                    val to = findFolderGridIndexAt(
                                                        pointer = rootPointer,
                                                        items = visibleItems,
                                                        itemBounds = itemBounds,
                                                        excludedKey = app.componentKey
                                                    )
                                                    hoverKey = to?.let { visibleItems.getOrNull(it)?.componentKey }
                                                    if (from >= 0 && to != null && to != from) {
                                                        moveVisibleItem(from, to)
                                                    }
                                                } else {
                                                    hoverKey = null
                                                    if (moveOutJob == null) {
                                                        moveOutJob = scope.launch {
                                                            delay(FOLDER_ITEM_DRAG_OUT_MS)
                                                            if (draggedKey == app.componentKey && draggedOutsidePanel) {
                                                                finishMoveOut(app)
                                                            }
                                                        }
                                                    }
                                                }
                                                change.consume()
                                            }
                                        }

                                        if (dragActive) {
                                            val releasedOutside = draggedOutsidePanel
                                            draggedKey = null
                                            draggedOutsidePanel = false
                                            hoverKey = null
                                            if (releasedOutside) {
                                                finishMoveOut(app)
                                            } else {
                                                cancelMoveOutJob()
                                                persistOrderIfChanged(originalKeys)
                                            }
                                        }
                                    }
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (draggedKey == null && menuApp == null) {
                                        onAppClick(app, Offset(0.5f, 0.5f))
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = app.iconForDisplay(twoToneIconsEnabled),
                                contentDescription = app.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = app.label,
                                color = Color.White.copy(alpha = 0.86f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        val draggedApp = draggedKey?.let { key -> visibleItems.firstOrNull { it.componentKey == key } }
        if (draggedApp != null) {
            Image(
                bitmap = draggedApp.iconForDisplay(twoToneIconsEnabled),
                contentDescription = draggedApp.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        translationX = dragPointerRoot.x - dragIconHalfSizePx
                        translationY = dragPointerRoot.y - dragIconHalfSizePx
                        this.alpha = if (draggedOutsidePanel) 0.58f else 0.94f
                        scaleX = if (draggedOutsidePanel) 0.94f else 1.04f
                        scaleY = if (draggedOutsidePanel) 0.94f else 1.04f
                        shadowElevation = 18.dp.toPx()
                    }
                    .clip(RoundedCornerShape(18.dp))
            )
        }

        menuApp?.let { app ->
            AppShortcutOverlay(
                app = app,
                blurEnabled = blurEnabled,
                onExcludeApp = { onExcludeApp(app) },
                onRemoveShortcut = if (app.isAppListShortcut) { { onRemoveShortcut(app) } } else null,
                onDismiss = { menuApp = null }
            )
        }
    }
}

private fun findFolderGridIndexAt(
    pointer: Offset,
    items: List<AppInfo>,
    itemBounds: Map<String, Rect>,
    excludedKey: String
): Int? {
    return items.indexOfFirst { app ->
        app.componentKey != excludedKey && itemBounds[app.componentKey]?.contains(pointer) == true
    }.takeIf { it >= 0 }
}
