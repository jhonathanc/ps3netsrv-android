package com.jhonju.ps3netsrv.app;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
//import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.jhonju.ps3netsrv.R;

public class MainActivity extends AppCompatActivity {

    private String currentLanguage;

    private final ActivityResultLauncher<Intent> manageAllFilesLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                            if (Environment.isExternalStorageManager()) {
                                continueAppFlow();
                            } else {
                                showPermissionDeniedDialog();
                            }
                        }
                    });

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);

        setContentView(R.layout.activity_main);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupNetworkMonitoring();
        }
        checkStorageAccess();
    }

    /* =========================================================
       LANGUAGE CHANGE HANDLING
       ========================================================= */

    @Override
    protected void onResume() {
        super.onResume();

        String storedLanguage =
                com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);

        if (!currentLanguage.equals(storedLanguage)) {

            if (PS3NetService.isRunning()) {

                new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.language_changed_dialog_message)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                            stopService(new Intent(this, PS3NetService.class));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(new Intent(this, PS3NetService.class));
                            } else {
                                startService(new Intent(this, PS3NetService.class));
                            }

                            recreate();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();

            } else {
                recreate();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupNetworkMonitoring() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {

                try {
                    NetworkCapabilities caps =
                            cm.getNetworkCapabilities(network);

                    if (caps != null &&
                            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            cm.bindProcessToNetwork(network);
                        } else {
                            ConnectivityManager.setProcessDefaultNetwork(network);
                        }
                    }

                } catch (Exception ignored) {
                }
            }
        });
    }

    private void checkStorageAccess() {
        if (SDK_INT < Build.VERSION_CODES.M) {
            continueAppFlow();
        } else if (SDK_INT < Build.VERSION_CODES.R) {
            checkLegacyRuntimePermission();
        } else {
            checkManageAllFilesPermission();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void checkLegacyRuntimePermission() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    100);
        } else {
            continueAppFlow();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void checkManageAllFilesPermission() {
        if (Environment.isExternalStorageManager()) {
            continueAppFlow();
        } else {
            showManagePermissionExplanation();
        }
    }

    /* =========================================================
       PERMISSION UI
       ========================================================= */

    private void showManagePermissionExplanation() {

        new AlertDialog.Builder(this)
                .setTitle("Permissão necessária")
                .setMessage("O aplicativo precisa de acesso total aos arquivos para funcionar corretamente.")
                .setPositiveButton("Abrir configurações", (d, w) -> requestManageAllFilesPermission())
                .setNegativeButton("Modo limitado", (d, w) -> enableLimitedMode())
                .show();
    }

    private void requestManageAllFilesPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            try {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageAllFilesLauncher.launch(intent);

            } catch (Exception e) {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                manageAllFilesLauncher.launch(intent);
            }
        }
    }

    private void showPermissionDeniedDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Permissão não concedida")
                .setMessage("Algumas funções ficarão indisponíveis.")
                .setPositiveButton("Tentar novamente", (d, w) -> checkStorageAccess())
                .setNegativeButton("Continuar limitado", (d, w) -> enableLimitedMode())
                .show();
    }

    private void enableLimitedMode() {
        Toast.makeText(this,
                "Modo limitado ativado.",
                Toast.LENGTH_LONG).show();
    }

    private void continueAppFlow() {
        findViewById(R.id.button_start_stop_server).setEnabled(true);
        if (!PS3NetService.isRunning()) {
            Intent intent = new Intent(this, PS3NetService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                continueAppFlow();
            } else {
                showPermissionDeniedDialog();
            }
        }
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