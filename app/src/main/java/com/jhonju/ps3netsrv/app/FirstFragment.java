package com.jhonju.ps3netsrv.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class FirstFragment extends Fragment {

  private boolean isServerRunning = false;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_first, container, false);
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final TextView tvServerState = view.findViewById(R.id.tvServerStopped);
    final Button btnStartServer = view.findViewById(R.id.button_start_stop_server);
    view.findViewById(R.id.button_start_stop_server).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          if (isServerRunning) {
            requireActivity().stopService(new Intent(getActivity(), PS3NetService.class));
          } else {
            if (!Utils.isConnectedToLocal()) {
              Snackbar.make(view, R.string.connection_disabled, Snackbar.LENGTH_LONG)
                  .setAction(R.string.action_ok, null).show();
              return;
            }
            startPs3NetService();
          }
          isServerRunning = !isServerRunning;
          btnStartServer.setText(isServerRunning ? R.string.stop_server : R.string.start_server);

          List<String> folderPaths = SettingsService.getFolders();
          Set<String> folderPathsAux = new HashSet<>();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String folderPath : folderPaths) {
              folderPathsAux.add(URLDecoder.decode(folderPath, StandardCharsets.UTF_8.displayName()));
            }
          } else {
            folderPathsAux = new HashSet<>(folderPaths);
          }
          int port = SettingsService.getPort();

          StringBuilder sb = new StringBuilder();
          for (String item : folderPathsAux) {
            sb.append(item).append("\n");
          }
          String serverRunningMsg = isServerRunning
              ? String.format(getResources().getString(R.string.server_running), Utils.getIPAddress(true), port,
                  sb.toString())
              : getResources().getString(R.string.server_stopped);

          tvServerState.setText(serverRunningMsg);
        } catch (Exception e) {
          Snackbar.make(view, String.format(getString(R.string.error_occurred), e.getMessage()), Snackbar.LENGTH_LONG)
              .setAction(R.string.action_ok, null).show();
        }
      }
    });
  }

  private void startPs3NetService() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      requireActivity().startForegroundService(new Intent(getActivity(), PS3NetService.class));
    } else {
      requireActivity().startService(new Intent(getActivity(), PS3NetService.class));
    }
  }
}