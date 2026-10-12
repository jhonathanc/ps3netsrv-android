package com.jhonju.ps3netsrv.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;

import com.jhonju.ps3netsrv.server.utils.FileLogger;

/** Isolates API 21 network types from the service loaded on API 14–20. */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class ServerNetworkMonitor {
  private final ConnectivityManager manager;
  private final ConnectivityManager.NetworkCallback callback;
  private Network ethernet;
  private boolean stopped;

  ServerNetworkMonitor(Context context, Handler mainHandler) {
    manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    callback = new ConnectivityManager.NetworkCallback() {
      @Override
      public void onAvailable(Network network) {
        mainHandler.post(() -> {
          if (stopped) return;
          try {
            NetworkCapabilities caps = manager.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
              bind(network);
              ethernet = network;
            }
          } catch (RuntimeException e) {
            FileLogger.logWarning("Unable to inspect server network", e);
          }
        });
      }

      @Override
      public void onLost(Network network) {
        mainHandler.post(() -> {
          if (!stopped && network.equals(ethernet)) {
            bind(null);
            ethernet = null;
          }
        });
      }
    };
    if (manager != null) {
      manager.registerNetworkCallback(new NetworkRequest.Builder()
          .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build(), callback);
    }
  }

  private void bind(Network network) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        manager.bindProcessToNetwork(network);
      } else {
        ConnectivityManager.setProcessDefaultNetwork(network);
      }
    } catch (RuntimeException e) {
      FileLogger.logWarning("Unable to bind server network", e);
    }
  }

  void stop() {
    if (stopped) return;
    stopped = true;
    if (manager != null) {
      try {
        manager.unregisterNetworkCallback(callback);
      } catch (IllegalArgumentException e) {
        FileLogger.logWarning("Network callback already removed", e);
      }
      if (ethernet != null) bind(null);
      ethernet = null;
    }
  }
}
