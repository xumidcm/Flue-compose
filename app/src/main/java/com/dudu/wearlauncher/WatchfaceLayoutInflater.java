package com.dudu.wearlauncher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import androidx.core.view.LayoutInflaterCompat;

public class WatchfaceLayoutInflater {
    public static LayoutInflater from(Context context, ClassLoader classLoader) {
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(context);
        LayoutInflaterCompat.setFactory2(inflater, new PluginInflaterFactory(inflater.getFactory2(), classLoader));
        return inflater;
    }
}
