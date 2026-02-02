package com.jhonju.ps3netsrv.app;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.Menu;
import android.view.MenuItem;

import com.jhonju.ps3netsrv.R;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> manageAllFilesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // Check if permission is granted after returning from settings
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                       if (!Environment.isExternalStorageManager()) {
                           // Permission not granted, handle appropriately (maybe show a dialog or exit)
                       }
                    }
                }
            }
    );


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(newBase));
    }

    private String currentLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);
        setContentView(R.layout.activity_main);

        setupNetworkMonitoring();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String storedLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);
        if (!currentLanguage.equals(storedLanguage)) {
             if (PS3NetService.isRunning()) {
                   new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage("Language changed. Application needs to restart to apply changes. The server will be restarted.")
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            // Stop server to ensure clean restart state or just trust recreate/service lifecycle
                            // Just recreating activity doesn't kill service (started service).
                            // But user asked for warning that "server will be restarted" - implies we SHOULD might restart it?
                            // Actually the user request "dizendo que o servidor será reiniciado".
                            // If I recreate the activity, the service keeps running.
                            // If I want to "restart the server", I should stop and start it.
                            // But MainActivity isn't the one starting it usually (FirstFragment does).
                            // Let's assume the user just means "App restart" might imply interruption.
                            // But wait, "o servidor derverá ser reiniciado".
                            // If I just recreate MainActivity, the service persists.
                            // To restart server, I should stop it here.
                            
                            // Stopping service before recreate:
                            stopService(new Intent(MainActivity.this, PS3NetService.class));
                            // Since FirstFragment handles auto-start or we normally start manually, 
                            // if we stop it here, it will be DEAD when activity comes back.
                            // Unless we pass an intent extra to restart it?
                            // Or maybe the user just wants the APP to restart.
                            
                            // Let's stick to "recreate()" for the App. 
                            // If the service needs restart to picking up new strings in notifications (if any), 
                            // we definitely need to stop/start.
                            stopService(new Intent(MainActivity.this, PS3NetService.class));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(new Intent(MainActivity.this, PS3NetService.class));
                            } else {
                                startService(new Intent(MainActivity.this, PS3NetService.class));
                            }
                            recreate();
                        })
                        .setNegativeButton(android.R.string.no, null) // Keep old language in UI?
                        .show();
             } else {
                 recreate();
             }
        }
    }

    private void setupNetworkMonitoring() {
        final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connManager != null) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            connManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    // Bind process to network if needed, or rely on system default.
                    // Prioritizing Ethernet over Wi-Fi is usually handled by the OS.
                    // If strict binding is required:
                    try {
                        NetworkCapabilities caps = connManager.getNetworkCapabilities(network);
                        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            connManager.bindProcessToNetwork(network);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void checkPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
             boolean hasStorage = Environment.isExternalStorageManager();
             boolean hasNotification = true;
             if (SDK_INT >= 33) {
                 hasNotification = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
             }

             if (!hasStorage || !hasNotification) {
                requestPermission();
             }
        } else {
            if (ContextCompat.checkSelfPermission(PS3NetSrvApp.getAppContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
            }
        }
    }

    private void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                manageAllFilesLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                manageAllFilesLauncher.launch(intent);
            }
            if (SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}