package com.flue.launcher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.flue.launcher.ui.home.BuiltInWatchFacePreview
import com.flue.launcher.ui.theme.LauncherTheme
import com.flue.launcher.ui.theme.UiStyle
import com.flue.launcher.ui.theme.WatchColors
import com.flue.launcher.watchface.BuiltInWatchFaceOptions
import com.flue.launcher.watchface.LunchWatchFaceDescriptor
import com.flue.launcher.watchface.LunchWatchFaceScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WatchFaceSettingCard(
    descriptor: LunchWatchFaceDescriptor,
    selected: Boolean,
    scale: Float,
    uiStyle: UiStyle = UiStyle.APPLE_WATCH,
    builtInPhotoPath: String? = null,
    builtInVideoPath: String? = null,
    photoOptions: BuiltInWatchFaceOptions = BuiltInWatchFaceOptions(),
    videoOptions: BuiltInWatchFaceOptions = BuiltInWatchFaceOptions(),
    onSelect: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val cardPressed by cardInteraction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (cardPressed) 0.965f else 1f,
        animationSpec = spring(stiffness = 780f, dampingRatio = 0.72f),
        label = "watchface_card_press_scale"
    )
    val launcherStyle = LauncherTheme.style

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale * pressScale
                scaleY = scale * pressScale
                alpha = scale.coerceIn(0.3f, 1f)
            }
            .clip(launcherStyle.compactShape)
            .background(if (selected) launcherStyle.selectedCardColor else launcherStyle.cardColor)
            .clickable(interactionSource = cardInteraction, indication = null, onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (descriptor.isBuiltin) {
            BuiltInWatchFacePreview(
                watchFaceId = descriptor.id,
                photoPath = builtInPhotoPath,
                videoPath = builtInVideoPath,
                uiStyle = uiStyle,
                photoOptions = photoOptions,
                videoOptions = videoOptions,
                showClock = false,
                playVideo = false,
                modifier = Modifier
                    .size(52.dp)
                    .clip(launcherStyle.compactShape)
            )
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            val previewBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = descriptor.stableKey) {
                value = withContext(Dispatchers.Default) {
                    LunchWatchFaceScanner.loadPreviewDrawable(context, descriptor)
                        ?.toBitmap(120, 120)
                        ?.asImageBitmap()
                }
            }
            val bitmap = previewBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(launcherStyle.compactShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(launcherStyle.compactShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(modifier = Modifier.padding(end = 56.dp)) {
                Text(descriptor.displayName, fontSize = 14.sp, color = launcherStyle.titleColor)
                Spacer(modifier = Modifier.height(2.dp))
                if (descriptor.summary.isNotBlank()) {
                    Text(
                        descriptor.summary,
                        fontSize = 11.sp,
                        color = launcherStyle.secondaryTextColor
                    )
                }
                val meta = buildString {
                    if (!descriptor.isBuiltin && descriptor.packageName != null) {
                        append(descriptor.packageName)
                    }
                    if (descriptor.author != null) {
                        if (isNotEmpty()) append("  ·  ")
                        append(descriptor.author)
                    }
                    if (descriptor.versionCode > 0) {
                        if (isNotEmpty()) append("  ·  ")
                        append("v")
                        append(descriptor.versionCode)
                    }
                }
                if (meta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(meta, fontSize = 11.sp, color = launcherStyle.secondaryTextColor)
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(launcherStyle.topBarChipColor)
                            .clickable(onClick = onDelete)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                    }
                } else if (onOpenSettings != null && descriptor.supportsSettings) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(launcherStyle.topBarChipColor)
                            .clickable(onClick = onOpenSettings)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = launcherStyle.accentColor)
                    }
                }
                if (selected) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = launcherStyle.accentColor)
                }
            }
        }
    }
}
