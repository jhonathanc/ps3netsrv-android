package com.jhonju.ps3netsrv.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.PS3NetSrvTask;
import com.jhonju.ps3netsrv.server.enums.EListType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetService extends Service {
    private static ExecutorService executorService;
    private static PS3NetSrvTask task;

    private Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            final String message = throwable.getMessage();
            HandlerThread handlerThread = new HandlerThread("ToastThread");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            Handler handler = new Handler(looper);
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        int idListType = SettingsService.getListType();
        EListType eListType = idListType == R.id.rbNone ? EListType.LIST_TYPE_NONE : idListType == R.id.rbAllowed ? EListType.LIST_TYPE_ALLOWED : EListType.LIST_TYPE_BLOCKED;
        task = new PS3NetSrvTask(SettingsService.getPort(), SettingsService.getFolders(), SettingsService.getMaxConnections(), SettingsService.getIps(), eListType, exceptionHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startTask();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            stopTask();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopTask() {
        try {
            if (task != null) task.shutdown();
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
