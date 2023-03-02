package com.jhonju.ps3netsrv;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.jhonju.ps3netsrv.utils.Utils;

public class FirstFragment extends Fragment {

    private boolean isServerRunning = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
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
                        getActivity().stopService(new Intent(getActivity(), PS3NetService.class));
                    } else {
                        if (!Utils.isConnectedToLocal()) {
                            Snackbar.make(view, "The ethernet/wifi connection is disabled", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            return;
                        }
                        startPs3NetService();
                    }
                    isServerRunning = !isServerRunning;
                    btnStartServer.setText(isServerRunning ? R.string.stop_server : R.string.start_server);

                    String folderPath = SettingsService.getFolder();
                    int port = SettingsService.getPort();

                    String serverRunningMsg = isServerRunning ? String.format(getResources().getString(R.string.server_running), Utils.getIPAddress(true), port, folderPath) :
                            getResources().getString(R.string.server_stopped);

                    tvServerState.setText(serverRunningMsg);
                } catch (Exception e) {
                    Snackbar.make(view, "Ocorreu um erro: " + e.getMessage(), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
    }

    private void startPs3NetService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(new Intent(getActivity(), PS3NetService.class));
        } else {
            getActivity().startService(new Intent(getActivity(), PS3NetService.class));
        }
    }
}