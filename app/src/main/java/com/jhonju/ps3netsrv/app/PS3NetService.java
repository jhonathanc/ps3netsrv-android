package com.jhonju.ps3netsrv.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.utils.LocaleHelper;
import com.jhonju.ps3netsrv.server.PS3NetSrvTask;
import com.jhonju.ps3netsrv.server.enums.EListType;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PS3NetService extends Service {
  private static final String TAG = "PS3NetService";
  private static final int NOTIFICATION_ID = 2;
  private static final String ACTION_STOP = "com.jhonju.ps3netsrv.STOP_SERVER";
  private static final String ACTION_RESTART = "com.jhonju.ps3netsrv.RESTART_SERVER";
  private static final MutableLiveData<ServerState> state = new MutableLiveData<>(
      new ServerState(ServerState.Status.STOPPED, 0, Collections.emptyList()));

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private ExecutorService executorService;
  private PS3NetSrvTask task;
  private PowerManager.WakeLock wakeLock;
  private WifiManager.WifiLock wifiLock;
  private ServerNetworkMonitor networkMonitor;

  private final Thread.UncaughtExceptionHandler exceptionHandler = (thread, throwable) -> {
    FileLogger.logError("Server error", throwable);
    mainHandler.post(() -> Toast.makeText(getApplicationContext(),
        LocaleHelper.onAttach(this).getString(R.string.error_occurred, throwable.getMessage()), Toast.LENGTH_LONG).show());
  };

  public static LiveData<ServerState> getState() {
    return state;
  }

  public static boolean isRunning() {
    return state.getValue().isActive();
  }

  /** UI commands and state changes run on the main thread. */
  public static void start(Context context) {
    if (state.getValue().status != ServerState.Status.STOPPED) return;
    ServerState previous = state.getValue();
    state.setValue(new ServerState(ServerState.Status.STARTING,
        SettingsService.getPort(), SettingsService.getFolders()));
    try {
      ContextCompat.startForegroundService(context, new Intent(context, PS3NetService.class));
    } catch (RuntimeException e) {
      state.setValue(previous);
      throw e;
    }
  }

  public static void stop(Context context) {
    ServerState previous = state.getValue();
    state.setValue(previous.withStatus(ServerState.Status.STOPPING));
    try {
      if (!context.stopService(new Intent(context, PS3NetService.class))) {
        state.setValue(previous.withStatus(ServerState.Status.STOPPED));
      }
    } catch (RuntimeException e) {
      state.setValue(previous);
      throw e;
    }
  }

  public static void restart(Context context) {
    ContextCompat.startForegroundService(context,
        new Intent(context, PS3NetService.class).setAction(ACTION_RESTART));
  }

  @Override
  public void onCreate() {
    super.onCreate();
    executorService = Executors.newSingleThreadExecutor();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      finishServer();
      stopSelf();
      return START_NOT_STICKY;
    }
    try {
      // Promote before acquiring resources or starting the listener.
      startForegroundNotification();
      if (intent != null && ACTION_RESTART.equals(intent.getAction())) stopTask();
      // Repeated starts must not queue a second listener on the same port.
      if (task == null) {
        acquireWakeLocks();
        setupNetworkMonitoring();
        startTask();
      }
      // A null intent after process death rebuilds the server from saved settings.
      return START_STICKY;
    } catch (RuntimeException e) {
      exceptionHandler.uncaughtException(null, e);
      finishServer();
      stopSelf();
      return START_NOT_STICKY;
    }
  }

  private void startForegroundNotification() {
    Context localized = LocaleHelper.onAttach(this);
    String channelId = getPackageName();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(channelId,
          localized.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
      channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
      channel.setShowBadge(false);
      NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      manager.createNotificationChannel(channel);
    }
    int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
    PendingIntent open = PendingIntent.getActivity(this, 0,
        new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), pendingFlags);
    PendingIntent stop = PendingIntent.getService(this, 1,
        new Intent(this, PS3NetService.class).setAction(ACTION_STOP), pendingFlags);
    Notification notification = new NotificationCompat.Builder(this, channelId)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(localized.getString(R.string.notification_title))
        .setContentIntent(open)
        .addAction(R.drawable.ic_notification, localized.getString(R.string.stop_server), stop)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build();
    startForeground(NOTIFICATION_ID, notification);
  }

  private void acquireWakeLocks() {
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    if (wakeLock == null && pm != null) {
      wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":ServerWakeLock");
      wakeLock.setReferenceCounted(false);
      wakeLock.acquire();
    }
    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    if (wifiLock == null && wm != null) {
      wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG + ":WifiLock");
      wifiLock.setReferenceCounted(false);
      wifiLock.acquire();
    }
  }

  private void releaseWakeLocks() {
    if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
    wifiLock = null;
    if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    wakeLock = null;
  }

  private void setupNetworkMonitoring() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkMonitor == null) {
      networkMonitor = new ServerNetworkMonitor(this, mainHandler);
    }
  }

  private void stopNetworkMonitoring() {
    if (networkMonitor != null) {
      networkMonitor.stop();
      networkMonitor = null;
    }
  }

  private void stopTask() {
    PS3NetSrvTask previous = task;
    task = null; // Ignore callbacks from the previous listener during a restart.
    if (previous != null) previous.shutdown();
  }

  private void startTask() {
    ServerState configuration = new ServerState(ServerState.Status.STARTING,
        SettingsService.getPort(), SettingsService.getFolders());
    state.setValue(configuration);
    int idListType = SettingsService.getListType();
    EListType listType = idListType == R.id.rbNone ? EListType.LIST_TYPE_NONE
        : idListType == R.id.rbAllowed ? EListType.LIST_TYPE_ALLOWED : EListType.LIST_TYPE_BLOCKED;
    PS3NetSrvTask next = new PS3NetSrvTask(configuration.port, configuration.folders,
        SettingsService.getMaxConnections(), SettingsService.getIps(), listType, exceptionHandler,
        getContentResolver(), getApplicationContext());
    task = next;
    next.setOnStarted(() -> mainHandler.post(() -> {
      if (task == next && state.getValue().status == ServerState.Status.STARTING) {
        state.setValue(configuration.withStatus(ServerState.Status.RUNNING));
      }
    }));
    executorService.execute(() -> {
      try {
        next.run();
      } catch (RuntimeException e) {
        exceptionHandler.uncaughtException(null, e);
      } finally {
        mainHandler.post(() -> {
          if (task == next) {
            finishServer();
            stopSelf();
          }
        });
      }
    });
  }

  private void finishServer() {
    try {
      stopTask();
      stopNetworkMonitoring();
    } finally {
      releaseWakeLocks();
      stopForeground(true);
      state.setValue(state.getValue().withStatus(ServerState.Status.STOPPED));
    }
  }

  @Override
  public void onDestroy() {
    finishServer();
    executorService.shutdownNow();
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
