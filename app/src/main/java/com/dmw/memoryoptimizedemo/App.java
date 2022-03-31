package com.dmw.memoryoptimizedemo;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by dumingwei on 2022/3/31.
 * <p>
 * Desc:
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}
