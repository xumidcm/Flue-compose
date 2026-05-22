package com.dudu.wearlauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.flue.launcher.BuildConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ILog {
    private static final String TAG = "FlueWatchFace";
    private static final String PREFS_NAME = "flue_diagnostics";
    private static final String KEY_DIAGNOSTIC_UNTIL = "diagnostic_logging_until";
    private static volatile long diagnosticLoggingUntil = 0L;

    public static void configureDiagnostics(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        diagnosticLoggingUntil = 0L;
        prefs.edit().remove(KEY_DIAGNOSTIC_UNTIL).apply();
    }

    public static boolean isDiagnosticLoggingEnabled() {
        return diagnosticLoggingUntil > System.currentTimeMillis();
    }

    public static void setDiagnosticLoggingEnabled(Context context, boolean enabled, long durationMs) {
        long until = enabled ? System.currentTimeMillis() + Math.max(0L, durationMs) : 0L;
        diagnosticLoggingUntil = until;
        if (context != null) {
            context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_DIAGNOSTIC_UNTIL, until)
                .apply();
        }
    }

    private static boolean shouldLog() {
        return BuildConfig.DEBUG || isDiagnosticLoggingEnabled();
    }

    public static void v(String msg) { if (shouldLog()) Log.v(TAG, String.valueOf(msg)); }
    public static void d(String msg) { if (shouldLog()) Log.d(TAG, String.valueOf(msg)); }
    public static void i(String msg) { if (shouldLog()) Log.i(TAG, String.valueOf(msg)); }
    public static void w(String msg) { if (shouldLog()) Log.w(TAG, String.valueOf(msg)); }
    public static void e(String msg) { if (shouldLog()) Log.e(TAG, String.valueOf(msg)); }

    public static void writeThrowableToFile(Throwable throwable, File file) {
        if (throwable == null || file == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String content = Log.getStackTraceString(throwable);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
        } catch (Exception error) {
            Log.e(TAG, "writeThrowableToFile failed", error);
        }
    }
}
