package com.flue.launcher

import android.app.Application
import android.content.Context
import com.dudu.wearlauncher.utils.ILog
import com.flue.launcher.data.repository.AppRepository
import com.flue.launcher.data.repository.WidgetRepository
import com.flue.launcher.ui.theme.ThemeMode
import com.flue.launcher.ui.theme.UiStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

class FlueApplication : Application() {
    val repositories: FlueRepositories by lazy { FlueRepositories(this) }

    override fun onCreate() {
        super.onCreate()
        ILog.configureDiagnostics(this)
    }

    companion object {
        fun repositories(context: Context): FlueRepositories =
            (context.applicationContext as? FlueApplication)?.repositories
                ?: FlueRepositories(context.applicationContext)
    }
}

class FlueRepositories(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val sharedState = SharedLauncherState()
    val appRepository: AppRepository by lazy { AppRepository(appContext) }
    val widgetRepository: WidgetRepository by lazy { WidgetRepository(appContext) }
}

class SharedLauncherState {
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val uiStyle = MutableStateFlow(UiStyle.APPLE_WATCH)
}
