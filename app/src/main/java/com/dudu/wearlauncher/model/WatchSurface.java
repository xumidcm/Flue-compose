package com.dudu.wearlauncher.model;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.dudu.wearlauncher.WatchfaceLayoutInflater;
import com.dudu.wearlauncher.utils.ILog;
import java.lang.reflect.Method;

public abstract class WatchSurface extends FrameLayout {
    private Context hostContext;
    private final String path;
    private Resources resources;

    public WatchSurface(Context context, String path) {
        this(context, null, 0, path);
    }

    public WatchSurface(Context context, AttributeSet attrs, String path) {
        this(context, attrs, 0, path);
    }

    public WatchSurface(Context context, AttributeSet attrs, int defStyleAttr, String path) {
        super(context, attrs, defStyleAttr);
        this.hostContext = context;
        this.path = path;
        this.resources = initResources(context);
        replaceContextResources(context);
        onCreate();
    }

    public abstract void onCreate();

    public void onDestroy() {}

    public void onActivityResult(int requestCode, int resultCode, Intent data) {}

    public Context getHostContext() {
        return hostContext;
    }

    public Resources initResources(Context context) {
        if (path == null || path.isEmpty()) {
            return context.getResources();
        }
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, path);
            Resources base = context.getResources();
            return new Resources(assetManager, base.getDisplayMetrics(), base.getConfiguration());
        } catch (Exception error) {
            error.printStackTrace();
            return context.getResources();
        }
    }

    public void replaceContextResources(Context context) {
        this.hostContext = context;
        try {
            java.lang.reflect.Field field = findField(context.getClass(), "mResources");
            if (field != null) {
                field.setAccessible(true);
                field.set(context, getResources());
            }
        } catch (Exception ignore) {
            ILog.d("replace resources skipped");
        }
    }

    public void setContentView(int layoutResId, ClassLoader classLoader) {
        LayoutInflater inflater = WatchfaceLayoutInflater.from(hostContext, classLoader);
        inflater.inflate(getResources().getLayout(layoutResId), this);
    }

    @Override
    public Resources getResources() {
        return resources != null ? resources : super.getResources();
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
