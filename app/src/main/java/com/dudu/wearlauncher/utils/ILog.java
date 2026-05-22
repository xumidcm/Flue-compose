package com.dudu.wearlauncher.utils;

import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ILog {
    private static final String TAG = "FlueWatchFace";

    public static void v(String msg) { Log.v(TAG, String.valueOf(msg)); }
    public static void d(String msg) { Log.d(TAG, String.valueOf(msg)); }
    public static void i(String msg) { Log.i(TAG, String.valueOf(msg)); }
    public static void w(String msg) { Log.w(TAG, String.valueOf(msg)); }
    public static void e(String msg) { Log.e(TAG, String.valueOf(msg)); }

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
