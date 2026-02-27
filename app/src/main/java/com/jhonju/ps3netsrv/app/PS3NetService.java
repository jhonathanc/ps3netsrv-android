package com.jhonju.ps3netsrv.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.PS3NetSrvTask;
import com.jhonju.ps3netsrv.server.enums.EListType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetService extends Service {
  private static final String TAG = "PS3NetService";
  private static final int NOTIFICATION_ID = 2;

  private ExecutorService executorService;
  private PS3NetSrvTask task;
  private PowerManager.WakeLock wakeLock;
  private WifiManager.WifiLock wifiLock;
  private static boolean serviceRunning = false;

  private final Thread.UncaughtExceptionHandler exceptionHandler = (thread, throwable) -> {
    final String message = throwable.getMessage();
    HandlerThread handlerThread = new HandlerThread("ToastThread");
    handlerThread.start();
    Looper looper = handlerThread.getLooper();
    Handler handler = new Handler(looper);
    handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
  };

  @Override
  public void onCreate() {
    super.onCreate();
    executorService = Executors.newSingleThreadExecutor();
    acquireWakeLocks();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // Promote to foreground IMMEDIATELY before doing any work.
    // On Android 8+ the system requires startForeground() within ~5 seconds.
    startForegroundNotification();

    try {
      startTask();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      stopTask();
    }

    // START_STICKY ensures the system restarts this service if it gets killed.
    return START_STICKY;
  }

  private void startForegroundNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String channelId = getPackageName();
      String channelName = getString(R.string.notification_channel_name);
      NotificationChannel channel = new NotificationChannel(
          channelId, channelName, NotificationManager.IMPORTANCE_LOW);
      channel.setLightColor(Color.BLUE);
      channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
      channel.setShowBadge(false);
      NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      manager.createNotificationChannel(channel);

      Notification notification = new NotificationCompat.Builder(this, channelId)
          .setOngoing(true)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(getString(R.string.notification_title))
          .setPriority(NotificationCompat.PRIORITY_LOW)
          .setCategory(Notification.CATEGORY_SERVICE)
          .build();
      startForeground(NOTIFICATION_ID, notification);
    } else {
      Notification notification = new NotificationCompat.Builder(this, "")
          .setOngoing(true)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(getString(R.string.notification_title))
          .setPriority(NotificationCompat.PRIORITY_LOW)
          .build();
      startForeground(NOTIFICATION_ID, notification);
    }
  }

  private void acquireWakeLocks() {
    // Partial wake lock keeps the CPU running even when the screen is off
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    if (pm != null) {
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":ServerWakeLock");
      wakeLock.acquire();
    }

    // Wi-Fi lock keeps the Wi-Fi radio active during sleep
    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    if (wm != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG + ":WifiLock");
      } else {
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG + ":WifiLock");
      }
      wifiLock.acquire();
    }
  }

  private void releaseWakeLocks() {
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      wakeLock = null;
    }
    if (wifiLock != null && wifiLock.isHeld()) {
      wifiLock.release();
      wifiLock = null;
    }
  }

  private void stopTask() {
    serviceRunning = false;
    try {
      if (task != null)
        task.shutdown();
    } catch (Exception e) {
      System.err.println(e.getMessage());
    } finally {
      if (executorService != null && !executorService.isShutdown())
        executorService.shutdownNow();
    }
  }

  private void startTask() {
    serviceRunning = true;
    int idListType = SettingsService.getListType();
    EListType eListType = idListType == R.id.rbNone ? EListType.LIST_TYPE_NONE
        : idListType == R.id.rbAllowed ? EListType.LIST_TYPE_ALLOWED : EListType.LIST_TYPE_BLOCKED;
    task = new PS3NetSrvTask(SettingsService.getPort(), SettingsService.getFolders(),
        SettingsService.getMaxConnections(), SettingsService.getIps(), eListType, exceptionHandler,
        getContentResolver(), getApplicationContext());
    executorService.execute(task);
  }

  @Override
  public void onDestroy() {
    stopTask();
    task = null;
    releaseWakeLocks();
    stopForeground(true);
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public static boolean isRunning() {
    return serviceRunning;
  }
}
