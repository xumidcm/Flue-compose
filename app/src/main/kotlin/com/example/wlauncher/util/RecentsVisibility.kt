package com.flue.launcher.util

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import com.flue.launcher.viewmodel.LauncherViewModel
import com.flue.launcher.viewmodel.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object RecentsVisibility {
    @JvmStatic
    fun readPreference(context: Context): Boolean = runBlocking {
        context.applicationContext.dataStore.data.first()[LauncherViewModel.KEY_HIDE_FROM_RECENTS] ?: true
    }

    @JvmStatic
    fun apply(activity: Activity) {
        apply(activity, readPreference(activity.applicationContext))
    }

    @JvmStatic
    fun apply(activity: Activity, enabled: Boolean) {
        val activityManager = activity.getSystemService(ActivityManager::class.java) ?: return
        activityManager.appTasks.forEach { task ->
            runCatching { task.setExcludeFromRecents(enabled) }
        }
    }
}
