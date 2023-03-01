package com.jhonju.ps3netsrv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jhonju.ps3netsrv.server.PS3NetSrvTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetService extends Service {
    private static ExecutorService executorService;
    private static PS3NetSrvTask server;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            startTask();
        } catch (Exception e) {
            stopTask();
            throw new RuntimeException(e);
        }
    }

    private void stopTask() {
        try {
            if (server != null) server.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (!executorService.isShutdown()) executorService.shutdownNow();
        }
    }

    private void startTask() throws Exception {
        server = new PS3NetSrvTask(SettingsService.getPort(), SettingsService.getFolder());
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(server);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = getPackageName();
            String channelName = "PS3NetSrv Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("PS3NetSrv is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        } else {
            startForeground(1, new Notification());
        }
    }


    @Override
    public void onDestroy() {
        stopTask();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
