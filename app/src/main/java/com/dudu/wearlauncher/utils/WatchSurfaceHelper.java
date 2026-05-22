package com.dudu.wearlauncher.utils;

import android.content.Context;
import android.content.Intent;
import com.dudu.wearlauncher.model.WatchSurface;
import com.flue.launcher.watchface.LunchWatchFaceDescriptor;
import com.flue.launcher.watchface.LunchWatchFaceRegistry;
import com.flue.launcher.watchface.LunchWatchFaceRuntime;
import com.flue.launcher.watchface.LunchWatchFaceDescriptorKt;

public class WatchSurfaceHelper {
    public static void startWsfActivity(Context context, String wfName, Class<?> clazz) {
        startWsfActivity(context, wfName, clazz.getName());
    }

    public static void startWsfActivity(Context context, String wfName, String className) {
        Intent intent = new Intent(context, com.dudu.wearlauncher.ui.WatchSurfaceBaseActivity.class);
        intent.putExtra("wfName", wfName);
        intent.putExtra("wsfClassName", className);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static WatchSurface getWatchSurface(Context context, String wfName, String wsfClassName) {
        LunchWatchFaceDescriptor descriptor = LunchWatchFaceRegistry.resolve(wfName);
        if (descriptor == null) {
            descriptor = LunchWatchFaceRegistry.getCurrentSelected();
        }
        if (descriptor == null) {
            throw new IllegalStateException("No watchface registered for settings surface");
        }
        return (WatchSurface) LunchWatchFaceRuntime.INSTANCE.instantiateWatchSurface(context, descriptor, wsfClassName);
    }

    public static void requestRefreshWatchface(Context context) {
        context.sendBroadcast(new Intent(LunchWatchFaceDescriptorKt.WATCHFACE_REFRESH_ACTION));
    }
}
