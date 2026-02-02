package com.jhonju.ps3netsrv.app;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.components.SimpleFileChooser;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class SettingsActivity extends AppCompatActivity {

    private List<String> listIps = new ArrayList<>();
    private List<String> listFolders = new ArrayList<>();
    private ListView listViewIps;
    private ListView listViewFolders;
    private static final int REQUEST_CODE_PICK_FOLDER = 1002;

    private SimpleFileChooser fileDialog;


    private String savePortValue() {
        TextInputLayout tilPort = findViewById(R.id.tilPort);
        try {
            int port = Integer.parseInt(tilPort.getEditText().getText().toString().trim());
            if (port <= 1024)
                return getResources().getString(R.string.negativePortValue);
            SettingsService.setPort(port);
            return "";
        } catch (NumberFormatException nfe) {
            return getResources().getString(R.string.invalidPortValue);
        }
    }

    private String saveMaxConnection() {
        TextInputLayout tilMaximumClientsNumber = findViewById(R.id.tilMaximumClientsNumber);
        try {
            int maxConn = Integer.parseInt(tilMaximumClientsNumber.getEditText().getText().toString().trim());
            if (maxConn < 0)
                return getResources().getString(R.string.negativeMaxConnectedClients);
            SettingsService.setMaxConnections(maxConn);
            return "";
        } catch (NumberFormatException nfe) {
            return getResources().getString(R.string.invalidMaxConnectedClients);
        }
    }

    private void loadSettings() {
        ((TextInputLayout) findViewById(R.id.tilPort)).getEditText().setText(SettingsService.getPort() + "");
        ((TextInputLayout) findViewById(R.id.tilMaximumClientsNumber)).getEditText().setText(SettingsService.getMaxConnections() + "");
        listIps.addAll(SettingsService.getIps());
        listFolders.addAll(SettingsService.getFolders());
        int listType = SettingsService.getListType();
        if (listType > 0) {
            RadioButton radio = findViewById(listType);
            if (radio != null) radio.setChecked(true);
        }
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
        listViewIps = findViewById(R.id.lvIps);
        listViewFolders = findViewById(R.id.lvFolders);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        loadSettings();

        fileDialog = new SimpleFileChooser(this, Environment.getExternalStorageDirectory(), onFileSelectedListener, true);

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = savePortValue();
                boolean hasError = showMessage(view, message);
                hasError = showMessage(view, message) || hasError;
                message = saveMaxConnection();
                hasError = showMessage(view, message) || hasError;
                if (!hasError) {
                    SettingsService.setIps(new HashSet<>(listIps));
                    SettingsService.setFolders(new HashSet<>(listFolders));
                    SettingsService.setListType(((RadioGroup) findViewById(R.id.rgIpListType)).getCheckedRadioButtonId());
                    showMessage(view, getResources().getString(R.string.saveSuccess));
                }
            }
        });

        final Button btnSelectFolder = findViewById(R.id.btnSelectFolder);
        btnSelectFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permission = -1;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    permission = ContextCompat.checkSelfPermission(PS3NetSrvApp.getAppContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? 0 : getPackageManager().checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getPackageName());
                }

                if (permission == PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
                    } else {
                        fileDialog.showDialog();
                    }
                } else {
                    showMessage(view, getResources().getString(R.string.read_external_permission_error));
                }
            }
        });

        final ArrayAdapter<String> adapterIps = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listIps);
        final ArrayAdapter<String> adapterFolders = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listFolders);

        listViewIps.setAdapter(adapterIps);
        listViewFolders.setAdapter(adapterFolders);

        final EditText editTextIp = findViewById(R.id.etIp);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (String split : splits) {
                            if (Integer.parseInt(split) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };
        editTextIp.setFilters(filters);


        final Button btnAddIp = findViewById(R.id.btnAddIp);
        btnAddIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newIp = editTextIp.getText().toString();

                if (!newIp.isEmpty()) {
                    listIps.add(newIp);
                    adapterIps.notifyDataSetChanged();
                    editTextIp.setText("");
                }
            }
        });

        listViewIps.setOnItemClickListener(new ListViewItemClickListener(listIps));
        listViewFolders.setOnItemClickListener(new ListViewItemClickListener(listFolders));

        RadioGroup rgIpListType = findViewById(R.id.rgIpListType);
        rgIpListType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                editTextIp.setText("");
                editTextIp.setEnabled(checkedId != R.id.rbNone);
                btnAddIp.setEnabled(checkedId != R.id.rbNone);
            }
        });
        editTextIp.setEnabled(rgIpListType.getCheckedRadioButtonId() != R.id.rbNone);
        btnAddIp.setEnabled(rgIpListType.getCheckedRadioButtonId() != R.id.rbNone);
    }

    // Event when a file is selected on file dialog.
    private final SimpleFileChooser.FileSelectedListener onFileSelectedListener = new SimpleFileChooser.FileSelectedListener() {
        @Override
        public void onFileSelected(File file) {
            listFolders.add(file.getAbsolutePath());
            SettingsService.setFolders(new HashSet<String>(listFolders));
            ((ArrayAdapter)listViewFolders.getAdapter()).notifyDataSetChanged();
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri selectedData = data.getData();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    // Persist read/write permissions for the selected URI
                    getContentResolver().takePersistableUriPermission(selectedData, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Ignore if context doesn't have permission to grant/persist, or if URI is not persistable
                    Log.e("SettingsActivity", "Failed to persist URI permission: " + e.getMessage());
                }
            }
            listFolders.add(selectedData.toString());
            SettingsService.setFolders(new HashSet<String>(listFolders));
            ((ArrayAdapter)listViewFolders.getAdapter()).notifyDataSetChanged();
        }
    }

    private class ListViewItemClickListener implements AdapterView.OnItemClickListener {
        private final List<String> list;

        public ListViewItemClickListener(List<String> list) {
            this.list = list;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            ListView listView = (ListView) parent;
            final ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();
            final String value = list.get(position);
            list.remove(position);
            adapter.notifyDataSetChanged();

            Snackbar.make(view, getResources().getString(R.string.ipRemoved) + value, Snackbar.LENGTH_SHORT)
                    .setAction(getResources().getString(R.string.undo), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            list.add(position, value);
                            adapter.notifyDataSetChanged();
                        }
                    }).show();
        }
    }

}