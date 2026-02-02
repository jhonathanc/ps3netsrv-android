package com.jhonju.ps3netsrv.app;

import android.content.SharedPreferences;
import android.os.Environment;

import com.jhonju.ps3netsrv.R;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SettingsService {
    private static final String settings = "settings";
    private static final SharedPreferences spPort = PS3NetSrvApp.getAppContext().getSharedPreferences("PORT",0);
    private static final SharedPreferences spFolder = PS3NetSrvApp.getAppContext().getSharedPreferences("FOLDER",0);
    private static final SharedPreferences spFolders = PS3NetSrvApp.getAppContext().getSharedPreferences("FOLDERS",0);
    private static final SharedPreferences spIps = PS3NetSrvApp.getAppContext().getSharedPreferences("IPS",0);
    private static final SharedPreferences spListType = PS3NetSrvApp.getAppContext().getSharedPreferences("LIST_TYPE",0);
    private static final SharedPreferences spMaxConnections = PS3NetSrvApp.getAppContext().getSharedPreferences("MAX_CONNECTIONS",0);
    private static final SharedPreferences spReadOnly = PS3NetSrvApp.getAppContext().getSharedPreferences("READ_ONLY",0);

    public static int getPort() { return spPort.getInt(settings, PS3NetSrvApp.getAppContext().getResources().getInteger(R.integer.defaultPort)); }

    public static Set<String> getIps() { return spIps.getStringSet(settings, new HashSet<String>()); }

    public static Set<String> getFolders() {
        HashSet<String> folders = new HashSet<String>();
        folders.add(spFolder.getString(settings, getDefaultFolder())); //if user has updated to this new version, reuse the settings.
        return spFolders.getStringSet(settings, folders);
    }

    public static int getListType() { return spListType.getInt(settings, 0); }

    public static int getMaxConnections() { return spMaxConnections.getInt(settings, 0); }

    public static boolean isReadOnly() { return spReadOnly.getBoolean(settings, false); }

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

    public static void setFolders(Set<String> folders) {
        SharedPreferences.Editor editor = spFolders.edit();
        editor.putStringSet(settings, folders);
        editor.apply();
    }

    public static void setIps(Set<String> ips) {
        SharedPreferences.Editor editor = spIps.edit();
        editor.putStringSet(settings, ips);
        editor.apply();
    }

    public static void setListType(int listType) {
        SharedPreferences.Editor editor = spListType.edit();
        editor.putInt(settings, listType);
        editor.apply();
    }

    public static void setMaxConnections(int maxConnections) {
        SharedPreferences.Editor editor = spMaxConnections.edit();
        editor.putInt(settings, maxConnections);
        editor.apply();
    }
}
