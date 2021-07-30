package com.jhonju.ps3netsrv;

import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

public class SettingsService {
    private static final String settings = "settings";
    private static SharedPreferences spPort = PS3NetSrvApp.getAppContext().getSharedPreferences("PORT",0);
    private static SharedPreferences spFolder = PS3NetSrvApp.getAppContext().getSharedPreferences("FOLDER",0);

    public static int getPort() {
        return spPort.getInt(settings, PS3NetSrvApp.getAppContext().getResources().getInteger(R.integer.defaultPort));
    }

    public static String getFolder() {
        return spFolder.getString(settings, getDefaultFolder());
    }

    private static String getDefaultFolder() {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)) {
            File baseDirFile = PS3NetSrvApp.getAppContext().getExternalFilesDir(null);
            if(baseDirFile == null) {
                return PS3NetSrvApp.getAppContext().getFilesDir().getAbsolutePath();
            } else {
                return baseDirFile.getAbsolutePath();
            }
        } else {
            return PS3NetSrvApp.getAppContext().getFilesDir().getAbsolutePath();
        }
    }

    public static void setPort(int port) {
        SharedPreferences.Editor editor = spPort.edit();
        editor.putInt(settings, port);
        editor.apply();
    }

    public static void setFolder(String folder) {
        SharedPreferences.Editor editor = spFolder.edit();
        editor.putString(settings, folder);
        editor.apply();
    }
}
