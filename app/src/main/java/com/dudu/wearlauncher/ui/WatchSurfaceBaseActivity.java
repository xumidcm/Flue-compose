package com.dudu.wearlauncher.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;
import com.dudu.wearlauncher.model.WatchSurface;
import com.dudu.wearlauncher.utils.ILog;
import com.dudu.wearlauncher.utils.WatchSurfaceHelper;

public class WatchSurfaceBaseActivity extends Activity {
    WatchSurface wsf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String wfName = intent.getStringExtra("wfName");
        String wsfClassName = intent.getStringExtra("wsfClassName");
        wsf = WatchSurfaceHelper.getWatchSurface(this, wfName, wsfClassName);
        setContentView(wsf);
    }

    @Override
    protected void onDestroy() {
        if (wsf != null) {
            wsf.onDestroy();
        }
        WatchSurfaceHelper.requestRefreshWatchface(this);
        super.onDestroy();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(getFitDisplayContext(newBase));
    }

    public static Context getFitDisplayContext(Context old) {
        Context newContext = old;
        try {
            float density = (float) old.getResources().getDisplayMetrics().widthPixels / 360f;
            Configuration configuration = new Configuration(old.getResources().getConfiguration());
            configuration.smallestScreenWidthDp = 320;
            configuration.densityDpi = (int) (320f * density);
            newContext = old.createConfigurationContext(configuration);
        } catch (Exception e) {
            Toast.makeText(newContext, "调整缩放失败, 请联系开发者", Toast.LENGTH_SHORT).show();
            ILog.e("adjust dpi failed");
        }
        return newContext;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (wsf != null) {
            wsf.onActivityResult(requestCode, resultCode, data);
        }
    }
}
