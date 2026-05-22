package com.dudu.wearlauncher.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WatchFaceBridge {
    private final Object watchface;

    public WatchFaceBridge(Object watchface) {
        this.watchface = watchface;
    }

    public void onWatchfaceVisibilityChanged(boolean isVisible) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            tryInvoke("onWatchfaceVisibilityChanged", new Class[]{boolean.class}, new Object[]{isVisible});
        } catch (NoSuchMethodException missing) {
            tryInvoke(isVisible ? "onWatchfaceVisible" : "onWatchfaceInvisible", new Class[0], new Object[0]);
        }
    }

    public void onScreenStateChanged(boolean isScreenOn) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            tryInvoke("onScreenStateChanged", new Class[]{boolean.class}, new Object[]{isScreenOn});
        } catch (NoSuchMethodException missing) {
            tryInvoke(isScreenOn ? "onScreenOn" : "onScreenOff", new Class[0], new Object[0]);
        }
    }

    public void updateBattery(int battery, int status) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        tryInvoke("updateBattery", new Class[]{int.class, int.class}, new Object[]{battery, status});
    }

    public void updateTime() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        tryInvoke("updateTime", new Class[0], new Object[0]);
    }

    private Object tryInvoke(String methodName, Class<?>[] parameterTypes, Object[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = watchface.getClass().getMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(watchface, args);
    }
}
