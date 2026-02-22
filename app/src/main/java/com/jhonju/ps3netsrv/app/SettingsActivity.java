package com.jhonju.ps3netsrv.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.components.SimpleFileChooser;

import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class SettingsActivity extends AppCompatActivity {

    private SettingsViewModel viewModel;

    private final List<String> listIps = new ArrayList<>();
    private final List<String> listFolders = new ArrayList<>();
    private ArrayAdapter<String> adapterIps;
    private ArrayAdapter<String> adapterFolders;

    private SimpleFileChooser fileDialog;

    private EditText etPort, etMaximumClientsNumber, etIp;
    private CheckBox cbReadOnly, cbLogErrors, cbLogCommands;
    private RadioGroup rgIpListType;
    private Button btnSave, btnSelectFolder, btnAddIp;
    private Spinner languageSpinner;
    private ListView listViewIps, listViewFolders;

    private final ActivityResultLauncher<Intent> folderPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleFolderPickerResult
            );

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            btnSelectFolder.performClick();
                        } else {
                            Snackbar.make(findViewById(android.R.id.content),
                                    R.string.read_external_permission_error,
                                    Snackbar.LENGTH_LONG).show();
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
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        initViews();
        setupToolbar();
        setupAdapters();
        observeViewModel();

        fileDialog = new SimpleFileChooser(
                this,
                Environment.getExternalStorageDirectory(),
                file -> viewModel.addFolder(file.getAbsolutePath()),
                true
        );

        setupLanguageSpinner();
        setupSaveButton();
        setupAddFolderButton();
        setupIpControls();
    }

    private void initViews() {
        etPort = findViewById(R.id.etPort);
        etMaximumClientsNumber = findViewById(R.id.etMaximumClientsNumber);
        etIp = findViewById(R.id.etIp);

        cbReadOnly = findViewById(R.id.cbReadOnly);
        cbLogErrors = findViewById(R.id.cbLogErrors);
        cbLogCommands = findViewById(R.id.cbLogCommands);

        rgIpListType = findViewById(R.id.rgIpListType);

        btnSave = findViewById(R.id.btnSave);
        btnSelectFolder = findViewById(R.id.btnSelectFolder);
        btnAddIp = findViewById(R.id.btnAddIp);

        languageSpinner = findViewById(R.id.language_spinner);

        listViewIps = findViewById(R.id.lvIps);
        listViewFolders = findViewById(R.id.lvFolders);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_settings);
    }

    private void setupAdapters() {
        adapterIps = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, listIps);
        adapterFolders = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, listFolders);

        listViewIps.setAdapter(adapterIps);
        listViewFolders.setAdapter(adapterFolders);

        listViewIps.setOnItemClickListener(new ListViewItemClickListener(true, R.string.ipRemoved));
        listViewFolders.setOnItemClickListener(new ListViewItemClickListener(false, R.string.folderRemoved));
    }

    private void observeViewModel() {

        viewModel.getPort().observe(this,
                port -> etPort.setText(String.valueOf(port)));

        viewModel.getMaxConnections().observe(this,
                max -> etMaximumClientsNumber.setText(String.valueOf(max)));

        viewModel.getIps().observe(this, ips -> {
            listIps.clear();
            if (ips != null) listIps.addAll(ips);
            adapterIps.notifyDataSetChanged();
        });

        viewModel.getFolders().observe(this, folders -> {
            listFolders.clear();
            if (folders != null) listFolders.addAll(folders);
            adapterFolders.notifyDataSetChanged();
        });

        viewModel.getListType().observe(this,
                type -> rgIpListType.check(type));

        viewModel.getReadOnly().observe(this,
                cbReadOnly::setChecked);

        viewModel.getLogErrors().observe(this,
                cbLogErrors::setChecked);

        viewModel.getLogCommands().observe(this,
                cbLogCommands::setChecked);
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(view -> {
            if (PS3NetService.isRunning()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.title_activity_settings)
                        .setMessage(R.string.server_restart_prompt)
                        .setPositiveButton(android.R.string.yes,
                                (dialog, which) -> attemptSave(view))
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            } else {
                attemptSave(view);
            }
        });
    }

    private void attemptSave(View view) {

        String portStr = etPort.getText().toString().trim();
        String maxConnStr = etMaximumClientsNumber.getText().toString().trim();

        int listType = rgIpListType.getCheckedRadioButtonId();
        boolean readOnly = cbReadOnly.isChecked();
        boolean logErrors = cbLogErrors.isChecked();
        boolean logCommands = cbLogCommands.isChecked();

        try {
            int p = Integer.parseInt(portStr);
            if (p <= 1024) {
                showMessage(view, getString(R.string.negativePortValue));
                return;
            }
        } catch (NumberFormatException e) {
            showMessage(view, getString(R.string.invalidPortValue));
            return;
        }

        try {
            int m = Integer.parseInt(maxConnStr);
            if (m < 0) {
                showMessage(view, getString(R.string.negativeMaxConnectedClients));
                return;
            }
        } catch (NumberFormatException e) {
            showMessage(view, getString(R.string.invalidMaxConnectedClients));
            return;
        }

        if (viewModel.validateAndSave(portStr, maxConnStr,
                listType, readOnly, logErrors, logCommands)) {

            showMessage(view, getString(R.string.saveSuccess));

            if (PS3NetService.isRunning()) {
                stopService(new Intent(this, PS3NetService.class));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(this, PS3NetService.class));
                } else {
                    startService(new Intent(this, PS3NetService.class));
                }
            }
        }
    }

    private void setupAddFolderButton() {
        btnSelectFolder.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                folderPickerLauncher.launch(intent);
            } else {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    fileDialog.showDialog();
                } else {
                    int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permission == PERMISSION_GRANTED) {
                        fileDialog.showDialog();
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }
            }
        });
    }

    private void handleFolderPickerResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedData = result.getData().getData();
            if (selectedData != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                selectedData,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (SecurityException e) {
                        Log.e("SettingsActivity",
                                "Persist URI failed: " + e.getMessage());
                    }
                }
                viewModel.addFolder(selectedData.toString());
            }
        }
    }

    private void setupIpControls() {

        etIp.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            if (end > start) {
                String result = dest.subSequence(0, dstart)
                        + source.toString()
                        + dest.subSequence(dend, dest.length());

                if (!result.matches("^\\d{0,3}(\\.\\d{0,3}){0,3}$"))
                    return "";

                String[] parts = result.split("\\.");
                for (String part : parts) {
                    if (!part.isEmpty() && Integer.parseInt(part) > 255)
                        return "";
                }
            }
            return null;
        }});

        btnAddIp.setOnClickListener(v -> {
            String newIp = etIp.getText().toString();
            if (!newIp.isEmpty()) {
                viewModel.addIp(newIp);
                etIp.setText("");
            }
        });

        rgIpListType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean enabled = checkedId != R.id.rbNone;
            etIp.setEnabled(enabled);
            btnAddIp.setEnabled(enabled);
            etIp.setText("");
        });
    }

    private void setupLanguageSpinner() {
        String currentLanguage =
                com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);

        int position = Language.fromCode(currentLanguage).getIndex();
        languageSpinner.setSelection(position);

        languageSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {

                        String selectedLang =
                                Language.fromIndex(position).getCode();

                        if (!selectedLang.equals(
                                com.jhonju.ps3netsrv.app.utils.LocaleHelper
                                        .getLanguage(SettingsActivity.this))) {

                            com.jhonju.ps3netsrv.app.utils.LocaleHelper
                                    .setLocale(SettingsActivity.this,
                                            selectedLang);

                            recreate();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
    }

    private void showMessage(View view, String message) {
        if (!message.isEmpty()) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        }
    }

    private class ListViewItemClickListener
            implements AdapterView.OnItemClickListener {

        private final boolean isIpList;
        private final int removedMessageResId;

        ListViewItemClickListener(boolean isIpList,
                                  int removedMessageResId) {
            this.isIpList = isIpList;
            this.removedMessageResId = removedMessageResId;
        }

        @Override
        public void onItemClick(AdapterView<?> parent,
                                View view,
                                int position,
                                long id) {

            String value = isIpList ?
                    listIps.get(position) :
                    listFolders.get(position);

            if (isIpList)
                viewModel.removeIp(position);
            else
                viewModel.removeFolder(position);

            Snackbar.make(view,
                            getString(removedMessageResId) + value,
                            Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, v -> {
                        if (isIpList)
                            viewModel.undoRemoveIp(position, value);
                        else
                            viewModel.undoRemoveFolder(position, value);
                    })
                    .show();
        }
    }
}