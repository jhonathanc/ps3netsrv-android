package com.jhonju.ps3netsrv.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.utils.Utils;
import com.jhonju.ps3netsrv.server.PS3NetSrvTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetService extends Service {
    private static ExecutorService executorService;
    private static PS3NetSrvTask task;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        task = new PS3NetSrvTask(SettingsService.getPort(), SettingsService.getFolder(), new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                sendBroadcast(Utils.getErrorIntent(e.getMessage()));
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startTask();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sendBroadcast(Utils.getErrorIntent(e.getMessage()));
            stopTask();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopTask() {
        try {
            if (task != null) task.shutdown();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sendBroadcast(Utils.getErrorIntent(e.getMessage()));
        } finally {
            if (!executorService.isShutdown()) executorService.shutdownNow();
        }
    }

    private void startTask() {
        executorService.execute(task);

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
    public IBinder onBind(Intent intent) { return null; }
}
