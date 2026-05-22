package com.dudu.wearlauncher.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.dudu.wearlauncher.utils.ILog;
import java.io.File;
import java.lang.reflect.Method;

public abstract class WatchFace extends FrameLayout {
    public static String watchFaceFolder = "";
    public static String watchFaceSuffix = ".wf";
    public static String watchFaceClassName = ".WatchFaceImpl";

    private Context hostContext;
    private final String path;
    private Resources resources;

    public WatchFace(Context context) {
        this(context, null, 0, context.getApplicationInfo() != null ? context.getApplicationInfo().sourceDir : null);
    }

    public WatchFace(Context context, AttributeSet attrs) {
        this(context, attrs, 0, context.getApplicationInfo() != null ? context.getApplicationInfo().sourceDir : null);
    }

    public WatchFace(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, context.getApplicationInfo() != null ? context.getApplicationInfo().sourceDir : null);
    }

    public WatchFace(Context context, String path) {
        this(context, null, 0, path);
    }

    public WatchFace(Context context, AttributeSet attrs, String path) {
        this(context, attrs, 0, path);
    }

    public WatchFace(Context context, AttributeSet attrs, int defStyleAttr, String path) {
        super(context, attrs, defStyleAttr);
        this.hostContext = context;
        this.path = path;
        ensureWatchFaceFolder(context);
        this.resources = initResources(context);
        replaceContextResources(context);
        initView();
    }

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

    @Override
    public Resources getResources() {
        return resources != null ? resources : super.getResources();
    }

    public void onWatchfaceVisibilityChanged(boolean isVisible) {
        if (isVisible) {
            onWatchfaceVisible();
        } else {
            onWatchfaceInvisible();
        }
    }

    public void onScreenStateChanged(boolean isScreenOn) {
        if (isScreenOn) {
            onScreenOn();
        } else {
            onScreenOff();
        }
    }

    public abstract void initView();

    public void onWatchfaceInvisible() {}

    public void onWatchfaceVisible() {}

    public void onScreenOff() {}

    public void onScreenOn() {}

    public void updateBattery(int level, int batteryStatus) {}

    public void updateStep(int steps) {}

    public abstract void updateTime();

    private static void ensureWatchFaceFolder(Context context) {
        if (watchFaceFolder == null || watchFaceFolder.isEmpty()) {
            File external = context.getExternalFilesDir("watchface");
            File target = external != null ? external : new File(context.getFilesDir(), "watchface");
            if (!target.exists()) {
                target.mkdirs();
            }
            watchFaceFolder = target.getAbsolutePath();
        }
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
