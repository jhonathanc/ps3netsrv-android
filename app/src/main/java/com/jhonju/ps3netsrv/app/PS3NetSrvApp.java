package com.jhonju.ps3netsrv.app;

import android.content.Context;

import androidx.multidex.MultiDexApplication;

import java.lang.ref.WeakReference;

public class PS3NetSrvApp extends MultiDexApplication {

    private static WeakReference<Context> contextRef;

    @Override
    public void onCreate() {
        super.onCreate();
        contextRef = new WeakReference<>(getApplicationContext());
    }

    public static Context getAppContext() {
        return contextRef != null ? contextRef.get() : null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(base));
    }
}
