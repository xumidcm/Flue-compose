package com.flue.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlueAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
        _connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (activeService === this) {
            activeService = null
            _connected.value = false
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (activeService === this) {
            activeService = null
            _connected.value = false
        }
        super.onDestroy()
    }

    companion object {
        private var activeService: FlueAccessibilityService? = null
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        fun lockScreen(): Boolean {
            val service = activeService ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun openPowerDialog(): Boolean {
            val service = activeService ?: return false
            return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        }
    }
}
