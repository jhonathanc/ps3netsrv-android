package com.jhonju.ps3netsrv.app;

import android.content.SharedPreferences;
import android.os.Environment;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.jhonju.ps3netsrv.R;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SettingsService {
  private static final String settings = "settings";
  private static final SharedPreferences spPort = PS3NetSrvApp.getAppContext().getSharedPreferences("PORT", 0);
  private static final SharedPreferences spFolder = PS3NetSrvApp.getAppContext().getSharedPreferences("FOLDER", 0);
  private static final SharedPreferences spFolders = PS3NetSrvApp.getAppContext().getSharedPreferences("FOLDERS", 0);
  private static final SharedPreferences spIps = PS3NetSrvApp.getAppContext().getSharedPreferences("IPS", 0);
  private static final SharedPreferences spListType = PS3NetSrvApp.getAppContext().getSharedPreferences("LIST_TYPE", 0);
  private static final SharedPreferences spMaxConnections = PS3NetSrvApp.getAppContext()
      .getSharedPreferences("MAX_CONNECTIONS", 0);
  private static final SharedPreferences spReadOnly = PS3NetSrvApp.getAppContext().getSharedPreferences("READ_ONLY", 0);

  public static int getPort() {
    return spPort.getInt(settings, PS3NetSrvApp.getAppContext().getResources().getInteger(R.integer.defaultPort));
  }

  public static Set<String> getIps() {
    return spIps.getStringSet(settings, new HashSet<String>());
  }

  public static int getListType() {
    return spListType.getInt(settings, 0);
  }

  public static int getMaxConnections() {
    return spMaxConnections.getInt(settings, 0);
  }

  public static boolean isReadOnly() {
    return spReadOnly.getBoolean(settings, false);
  }

  public static void setReadOnly(boolean readOnly) {
    SharedPreferences.Editor editor = spReadOnly.edit();
    editor.putBoolean(settings, readOnly);
    editor.apply();
  }

  private static String getDefaultFolder() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      File baseDirFile = PS3NetSrvApp.getAppContext().getExternalFilesDir(null);
      if (baseDirFile == null) {
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

  public static List<String> getFolders() {
    if (!spFolders.contains(settings)) {
      // Migration logic: Check if old SET exists
      if (spFolder.contains(settings)) {
        // Very old single folder setting
        List<String> folders = new ArrayList<>();
        folders.add(spFolder.getString(settings, getDefaultFolder()));
        setFolders(folders);
        return folders;
      }
      // Check if Set<String> exists in the same preference key (might throw
      // ClassCastException if we change type directly)
      // To be safe, we use a new key name for the List, or handle the error.
      // Given SharedPreferences limitations, it's SAFER to use a NEW KEY for the
      // ordered list.
      // Let's call it "FOLDERS_ORDERED".
    }

    String jsonLink = spFolders.getString("FOLDERS_JSON", null);
    List<String> folders = new ArrayList<>();

    if (jsonLink == null) {
      // Try to migrate from the old StringSet if it exists
      Set<String> oldSet = spFolders.getStringSet(settings, null);
      if (oldSet != null) {
        folders.addAll(oldSet);
        setFolders(folders); // Save as JSON
        // Optional: remove old key? spFolders.edit().remove(settings).apply();
      } else {
        // Default
        folders.add(getDefaultFolder());
      }
    } else {
      try {
        JSONArray jsonArray = new JSONArray(jsonLink);
        for (int i = 0; i < jsonArray.length(); i++) {
          folders.add(jsonArray.getString(i));
        }
      } catch (JSONException e) {
        e.printStackTrace();
        folders.add(getDefaultFolder());
      }
    }
    return folders;
  }

  public static void setFolders(List<String> folders) {
    SharedPreferences.Editor editor = spFolders.edit();
    JSONArray jsonArray = new JSONArray();
    for (String folder : folders) {
      jsonArray.put(folder);
    }
    editor.putString("FOLDERS_JSON", jsonArray.toString());
    // Also save to the old key for backward compatibility or safety if needed?
    // No, let's switch to the new one.
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
