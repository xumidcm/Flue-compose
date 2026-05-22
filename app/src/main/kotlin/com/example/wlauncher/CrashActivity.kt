package com.flue.launcher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flue.launcher.util.RecentsVisibility
import java.io.File
import java.util.Locale

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecentsVisibility.apply(this)
        val crashInfo = intent.getStringExtra("crash_info") ?: readCache("crash_info.txt", "Unknown error")
        val crashBrief = intent.getStringExtra("crash_brief") ?: readCache("crash_brief.txt", "Unknown")
        val appInfo = intent.getStringExtra("crash_app_info") ?: readCache("crash_app_info.txt", "Unknown")
        val fullCrashLog = buildString {
            appendLine(appInfo)
            appendLine()
            appendLine(crashBrief)
            appendLine()
            append(crashInfo)
        }.trim()
        setContent {
            val isZh = remember { Locale.getDefault().language.startsWith("zh") }
            CrashScreen(
                isZh = isZh,
                crashInfo = crashInfo,
                appInfo = appInfo,
                onCopyLog = {
                    copyText(
                        fullCrashLog,
                        if (isZh) "已复制完整崩溃日志" else "Copied full crash log"
                    )
                },
                onRestart = { restart() },
                onOpenSettings = { openSettings() }
            )
        }
    }

    private fun readCache(name: String, fallback: String): String {
        return try {
            File(cacheDir, name).takeIf { it.exists() }?.readText() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun copyText(text: String, toastText: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("crash", text))
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
    }

    private fun restart() {
        startActivity(packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

@Composable
fun CrashScreen(
    isZh: Boolean,
    crashInfo: String,
    appInfo: String,
    onCopyLog: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showFull by remember { mutableStateOf(false) }
    val previewCrash = remember(crashInfo) { crashInfo.lineSequence().take(5).joinToString("\n") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = if (isZh) "哎呀，崩溃了" else "App Crashed",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isZh) {
                "请复制详细错误并提供复现步骤给开发者。"
            } else {
                "Shows first five error lines by default. Tap to expand full log."
            },
            fontSize = 14.sp,
            color = Color(0xFFBFBFBF),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF171717))
                .padding(14.dp)
        ) {
            Text(
                text = "$appInfo\n\n${if (showFull) crashInfo else previewCrash}",
                fontSize = 12.sp,
                color = Color(0xFFE8E8E8),
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (showFull) {
                if (isZh) "收起日志" else "Collapse"
            } else {
                if (isZh) "展开完整日志" else "Show full log"
            },
            fontSize = 13.sp,
            color = Color(0xFF74B8FF),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showFull = !showFull }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))
        CrashButton(
            text = if (isZh) "复制完整崩溃日志" else "Copy full crash log",
            bg = Color(0xFF2D7DFF),
            fg = Color.White,
            onClick = onCopyLog
        )
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(
            text = if (isZh) "重启应用" else "Restart app",
            bg = Color(0xFF1F1F1F),
            fg = Color(0xFF74B8FF),
            onClick = onRestart
        )
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(
            text = if (isZh) "应用详情" else "App settings",
            bg = Color(0xFF1F1F1F),
            fg = Color(0xFF74B8FF),
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun CrashButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.W600)
    }
}
