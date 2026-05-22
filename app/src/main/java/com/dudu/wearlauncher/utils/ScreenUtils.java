package com.dudu.wearlauncher.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.WindowManager;

public class ScreenUtils {
    public static boolean isRoundScreen(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getConfiguration().isScreenRound();
        }
        return false;
    }
}
