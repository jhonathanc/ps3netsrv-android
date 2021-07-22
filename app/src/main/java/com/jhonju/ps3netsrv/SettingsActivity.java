package com.jhonju.ps3netsrv;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private String savePortValue() {
        TextInputLayout tilPort = findViewById(R.id.tilPort);
        try {
            int port = Integer.parseInt(Objects.requireNonNull(tilPort.getEditText()).getText().toString().trim());
            if (port < 0)
                return getResources().getString(R.string.negativePortValue);
            SettingsService.setPort(port);
            return "";
        } catch(NumberFormatException nfe) {
            return getResources().getString(R.string.invalidPortValue);
        }
    }

    private String saveFolderPath() {
        String path = Objects.requireNonNull(((TextInputLayout) findViewById(R.id.tilFolder)).getEditText()).getText().toString();
        File file = new File(path);
        if (!(file.exists() && file.isDirectory()))
            return getResources().getString(R.string.invalidFolder);
        SettingsService.setFolder(path);
        return "";
    }

    private void loadSettings() {
        ((TextInputLayout) findViewById(R.id.tilFolder)).getEditText().setText(SettingsService.getFolder());
        ((TextInputLayout) findViewById(R.id.tilPort)).getEditText().setText(SettingsService.getPort() + "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        loadSettings();

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = savePortValue();
                boolean hasError = showMessage(view, message);
                message = saveFolderPath();
                hasError = showMessage(view, message) || hasError;
                if (!hasError)
                    showMessage(view, getResources().getString(R.string.saveSuccess));
            }

            private boolean showMessage(View view, String message) {
                if (!message.equals("")) {
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return true;
                }
                return false;
            }
        });
    }
}