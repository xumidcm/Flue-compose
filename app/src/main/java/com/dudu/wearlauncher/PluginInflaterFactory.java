package com.dudu.wearlauncher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class PluginInflaterFactory implements LayoutInflater.Factory2 {
    private static final String[] CLASS_PREFIX_LIST = new String[] {
        "android.widget.",
        "android.view.",
        "android.webkit."
    };

    private final LayoutInflater.Factory2 delegate;
    private final ClassLoader classLoader;

    public PluginInflaterFactory(LayoutInflater.Factory2 delegate, ClassLoader classLoader) {
        this.delegate = delegate;
        this.classLoader = classLoader;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        View delegated = delegate != null ? delegate.onCreateView(parent, name, context, attrs) : null;
        if (delegated != null) {
            return delegated;
        }
        return createPluginView(name, context, attrs);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View delegated = delegate != null ? delegate.onCreateView(name, context, attrs) : null;
        if (delegated != null) {
            return delegated;
        }
        return createPluginView(name, context, attrs);
    }

    private View createPluginView(String name, Context context, AttributeSet attrs) {
        try {
            if (name.contains(".")) {
                Class<?> clazz = classLoader.loadClass(name);
                java.lang.reflect.Constructor<?> constructor = clazz.getConstructor(Context.class, AttributeSet.class);
                constructor.setAccessible(true);
                return (View) constructor.newInstance(context, attrs);
            }
            LayoutInflater inflater = LayoutInflater.from(context);
            for (String prefix : CLASS_PREFIX_LIST) {
                try {
                    return inflater.createView(name, prefix, attrs);
                } catch (ClassNotFoundException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
