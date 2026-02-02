package com.jhonju.ps3netsrv.app;

import android.app.Application;
import android.content.Context;

public class PS3NetSrvApp extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        PS3NetSrvApp.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return PS3NetSrvApp.context;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(base));
    }
}
