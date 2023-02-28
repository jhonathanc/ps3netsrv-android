package com.jhonju.ps3netsrv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.jhonju.ps3netsrv.server.PS3NetSrvTask;

public class PS3NetService extends Service {
    private static PS3NetSrvTask server;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            server = new PS3NetSrvTask(SettingsService.getPort(), SettingsService.getFolder());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            server.run();
        } catch (Exception e) {
            server.shutdown();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	server.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
