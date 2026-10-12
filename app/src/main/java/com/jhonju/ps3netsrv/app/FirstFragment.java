package com.jhonju.ps3netsrv.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.utils.NetworkUtils;

import java.util.LinkedHashSet;
import java.util.Set;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class FirstFragment extends Fragment {
  private final ActivityResultLauncher<String> notificationPermission =
      registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> startServer());

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_first, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TextView serverState = view.findViewById(R.id.tvServerStopped);
    Button startStop = view.findViewById(R.id.button_start_stop_server);
    PS3NetService.getState().observe(getViewLifecycleOwner(), state -> {
      startStop.setEnabled(state.status == ServerState.Status.STOPPED
          || state.status == ServerState.Status.RUNNING);
      startStop.setText(state.isActive() ? R.string.stop_server : R.string.start_server);
      switch (state.status) {
        case RUNNING:
          Set<String> folders = new LinkedHashSet<>();
          for (String folder : state.folders) {
            folders.add(folder.startsWith("content:") ? Uri.decode(folder) : folder);
          }
          String address = "";
          try {
            address = NetworkUtils.getIPAddress(true);
          } catch (Exception e) {
            showError(e);
          }
          serverState.setText(getString(R.string.server_running, address,
              state.port, android.text.TextUtils.join("\n", folders)));
          break;
        case STARTING:
          serverState.setText(R.string.server_starting);
          break;
        case STOPPING:
          serverState.setText(R.string.server_stopping);
          break;
        default:
          serverState.setText(R.string.server_stopped);
      }
    });
    startStop.setOnClickListener(button -> {
      try {
        if (PS3NetService.isRunning()) {
          PS3NetService.stop(requireContext());
        } else if (!NetworkUtils.isConnectedToLocal()) {
          Snackbar.make(button, R.string.connection_disabled, Snackbar.LENGTH_LONG).show();
        } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
          notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
          startServer();
        }
      } catch (RuntimeException e) {
        showError(e);
      }
    });
    view.findViewById(R.id.button_allow_background).setOnClickListener(button -> {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
      try {
        startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + requireContext().getPackageName())));
      } catch (ActivityNotFoundException | SecurityException e) {
        try {
          startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException fallbackError) {
          showError(fallbackError);
        }
      }
    });
    updateBatteryControls();
  }

  private void startServer() {
    if (!isAdded()) return;
    try {
      // Notification denial does not prevent a foreground service from running.
      PS3NetService.start(requireContext());
    } catch (RuntimeException e) {
      showError(e);
    }
  }

  private void showError(Exception e) {
    View view = getView();
    if (view != null) {
      Snackbar.make(view, getString(R.string.error_occurred, e.getMessage()), Snackbar.LENGTH_LONG).show();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updateBatteryControls();
  }

  private void updateBatteryControls() {
    View view = getView();
    if (view == null) return;
    boolean needsExemption = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
      needsExemption = pm != null && !pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());
    }
    view.findViewById(R.id.battery_optimization_notice)
        .setVisibility(needsExemption ? View.VISIBLE : View.GONE);
  }
}
