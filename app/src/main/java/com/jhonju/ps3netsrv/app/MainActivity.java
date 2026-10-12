package com.jhonju.ps3netsrv.app;

import android.content.Context;
import android.content.Intent;
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
                            PS3NetService.restart(this);
                            recreate();
                        }).setNegativeButton(android.R.string.no, null).show();
            } else {
                recreate();
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
