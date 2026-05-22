package com.flue.launcher.watchface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dudu.wearlauncher.model.WatchFaceBridge

@Composable
fun LunchWatchFaceHost(
    descriptor: LunchWatchFaceDescriptor,
    isFaceVisible: Boolean,
    refreshToken: Int,
    onLoadFailure: (LunchWatchFaceDescriptor, Throwable) -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LunchWatchFaceHostView(context).apply {
                setFailureHandler(onLoadFailure)
                setLongPressHandler(onLongPress)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { host ->
            host.setFailureHandler(onLoadFailure)
            host.setLongPressHandler(onLongPress)
            host.bind(descriptor, isFaceVisible, refreshToken)
        }
    )
}

private class LunchWatchFaceHostView(context: Context) : FrameLayout(context) {
    private var descriptor: LunchWatchFaceDescriptor? = null
    private var refreshToken: Int = -1
    private var faceVisible: Boolean = false
    private var screenOn: Boolean = currentScreenOn(context)
    private var bridge: WatchFaceBridge? = null
    private var failureHandler: ((LunchWatchFaceDescriptor, Throwable) -> Unit)? = null
    private var longPressHandler: (() -> Unit)? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            dispatchBattery(intent)
        }
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            dispatchTime()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOn = when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> false
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> true
                else -> currentScreenOn(context)
            }
            dispatchScreenState()
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            descriptor?.let { load(it) }
        }
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isLongClickable = true
        setOnLongClickListener {
            longPressHandler?.invoke()
            longPressHandler != null
        }
    }

    fun setFailureHandler(handler: (LunchWatchFaceDescriptor, Throwable) -> Unit) {
        failureHandler = handler
    }

    fun setLongPressHandler(handler: (() -> Unit)?) {
        longPressHandler = handler
    }

    fun bind(descriptor: LunchWatchFaceDescriptor, isFaceVisible: Boolean, refreshToken: Int) {
        faceVisible = isFaceVisible
        val descriptorChanged = this.descriptor?.stableKey != descriptor.stableKey
        val refreshChanged = this.refreshToken != refreshToken
        this.refreshToken = refreshToken
        if (descriptorChanged || refreshChanged || childCount == 0) {
            load(descriptor)
        } else {
            dispatchScreenState()
            dispatchVisibility()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ContextCompat.registerReceiver(context, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, timeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, refreshReceiver, IntentFilter(WATCHFACE_REFRESH_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { dispatchBattery(it) }
        dispatchTime()
        dispatchScreenState()
        dispatchVisibility()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching { context.unregisterReceiver(batteryReceiver) }
        runCatching { context.unregisterReceiver(timeReceiver) }
        runCatching { context.unregisterReceiver(screenReceiver) }
        runCatching { context.unregisterReceiver(refreshReceiver) }
        clearLoadedView()
    }

    private fun load(descriptor: LunchWatchFaceDescriptor) {
        this.descriptor = descriptor
        clearLoadedView()
        runCatching {
            val result = LunchWatchFaceRuntime.loadExternalWatchFace(context, descriptor)
            bridge = result.bridge
            addView(result.view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            dispatchScreenState()
            dispatchVisibility()
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { dispatchBattery(it) }
            dispatchTime()
        }.onFailure { error ->
            clearLoadedView()
            showFailure(error)
            failureHandler?.invoke(descriptor, error)
        }
    }

    private fun clearLoadedView() {
        removeAllViews()
        bridge = null
    }

    private fun showFailure(error: Throwable) {
        val rootCause = generateSequence(error) { it.cause }.last()
        val view = TextView(context).apply {
            text = "表盘加载失败：${rootCause.message ?: rootCause.javaClass.simpleName}"
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
        }
        addView(view, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    private fun dispatchVisibility() {
        runCatching { bridge?.onWatchfaceVisibilityChanged(faceVisible) }
    }

    private fun dispatchScreenState() {
        runCatching { bridge?.onScreenStateChanged(screenOn) }
    }

    private fun dispatchBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        if (level < 0 || scale <= 0) return
        val battery = level * 100 / scale
        runCatching { bridge?.updateBattery(battery, status) }
    }

    private fun dispatchTime() {
        runCatching { bridge?.updateTime() }
    }

    private fun currentScreenOn(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isInteractive ?: true
    }
}
