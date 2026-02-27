package com.jhonju.ps3netsrv.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.*;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;

import com.jhonju.ps3netsrv.R;

public class MainActivity extends AppCompatActivity {
    private String currentLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);
        setContentView(R.layout.activity_main);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupNetworkMonitoring();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String storedLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);
        if (!currentLanguage.equals(storedLanguage)) {
            if (PS3NetService.isRunning()) {
                new AlertDialog.Builder(this).setTitle(R.string.app_name)
                        .setMessage(R.string.language_changed_dialog_message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            stopService(new Intent(this, PS3NetService.class));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(new Intent(this, PS3NetService.class));
                            } else {
                                startService(new Intent(this, PS3NetService.class));
                            }
                            recreate();
                        }).setNegativeButton(android.R.string.no, null).show();
            } else {
                recreate();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return;

        // Use a dedicated background thread for network callbacks to avoid blocking the
        // main thread
        android.os.HandlerThread networkThread = new android.os.HandlerThread("NetworkMonitor");
        networkThread.start();
        android.os.Handler networkHandler = new android.os.Handler(networkThread.getLooper());

        NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                try {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cm.bindProcessToNetwork(network);
                        } else {
                            ConnectivityManager.setProcessDefaultNetwork(network);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, networkHandler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}