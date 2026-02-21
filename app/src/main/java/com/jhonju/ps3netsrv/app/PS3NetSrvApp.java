package com.jhonju.ps3netsrv.app;

import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;

public class PS3NetSrvApp extends Application {

    private static WeakReference<Context> contextRef;

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
