package com.jhonju.ps3netsrv;

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

}
