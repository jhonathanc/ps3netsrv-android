package com.jhonju.ps3netsrv.app;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.components.SimpleFileChooser;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class SettingsActivity extends AppCompatActivity {

    List<String> listIps = new ArrayList<>();

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
        Objects.requireNonNull(((TextInputLayout) findViewById(R.id.tilFolder)).getEditText()).setText(SettingsService.getFolder());
        Objects.requireNonNull(((TextInputLayout) findViewById(R.id.tilPort)).getEditText()).setText(SettingsService.getPort() + "");
    }

    private boolean showMessage(View view, String message) {
        if (!message.equals("")) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadSettings();

        final SimpleFileChooser fileDialog = new SimpleFileChooser(this, Environment.getExternalStorageDirectory(), onFileSelectedListener, true);

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
        });

        final TextInputEditText etFolder = findViewById(R.id.etFolder);

        etFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(PS3NetSrvApp.getAppContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
                    fileDialog.showDialog();
                else
                    showMessage(view, getResources().getString(R.string.read_external_permission_error));
            }
        });

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listIps);
        final ListView listView = findViewById(R.id.lvIps);
        listView.setAdapter(adapter);

        final EditText editTextIp = findViewById(R.id.etIp);
        final Button btnAddIp = findViewById(R.id.btnAddIp);
        btnAddIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newIp = editTextIp.getText().toString();

                if (!newIp.isEmpty()) {
                    listIps.add(newIp);
                    adapter.notifyDataSetChanged();
                    editTextIp.setText("");
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final String ip = listIps.get(position);
                listIps.remove(position);
                adapter.notifyDataSetChanged();

                Snackbar.make(view, getResources().getString(R.string.ipRemoved) + ip, Snackbar.LENGTH_SHORT)
                        .setAction(getResources().getString(R.string.undo), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                listIps.add(position, ip);
                                adapter.notifyDataSetChanged();
                            }
                        }).show();
            }
        });

        RadioGroup rgIpListType = findViewById(R.id.rgIpListType);
        //rgIpListType.getCheckedRadioButtonId();
        rgIpListType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                editTextIp.setText("");
                editTextIp.setEnabled(checkedId != R.id.rbNone);
                btnAddIp.setEnabled(checkedId != R.id.rbNone);
            }
        });
    }

    // Event when a file is selected on file dialog.
    private final SimpleFileChooser.FileSelectedListener onFileSelectedListener = new SimpleFileChooser.FileSelectedListener() {
        @Override
        public void onFileSelected(File file) {
            // create shortcut using file path
            SettingsService.setFolder(file.getAbsolutePath());
            Objects.requireNonNull(((TextInputLayout) findViewById(R.id.tilFolder)).getEditText()).setText(file.getAbsolutePath());
        }
    };
}