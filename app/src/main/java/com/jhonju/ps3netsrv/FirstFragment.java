package com.jhonju.ps3netsrv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

public class FirstFragment extends Fragment {

    private boolean isServerRunning = false;
    PS3NetSrv server;

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
        final TextView tvServerState = ((TextView)view.findViewById(R.id.tvServerStopped));
        final Button btnStartServer = ((Button)view.findViewById(R.id.button_start_stop_server));
        view.findViewById(R.id.button_start_stop_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String folderPath = SettingsService.getFolder();
                int port = SettingsService.getPort();
                try {
                    if (isServerRunning) {
                        server.cancel(true);
                    } else {
                        server = new PS3NetSrv(port, folderPath);
                        server.execute("");
                    }
                    isServerRunning = !isServerRunning;
                    btnStartServer.setText(isServerRunning ? R.string.stop_server : R.string.start_server);

                    String serverRunningMsg = isServerRunning ? String.format(getResources().getString(R.string.server_running), port, folderPath) :
                            getResources().getString(R.string.server_stopped);

                    tvServerState.setText(serverRunningMsg);
                } catch (Exception e) {
                    Snackbar.make(view, "Deu pau", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            }
        });
    }
}