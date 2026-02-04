package com.jhonju.ps3netsrv.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.components.SimpleFileChooser;

import java.util.Collections;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class SettingsActivity extends AppCompatActivity {

  private SettingsViewModel viewModel;
  private List<String> listIps = new ArrayList<>();
  private List<String> listFolders = new ArrayList<>();
  private ListView listViewIps;
  private ListView listViewFolders;
  private ArrayAdapter<String> adapterIps;
  private ArrayAdapter<String> adapterFolders;

  private SimpleFileChooser fileDialog;

  private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
      new ActivityResultContracts.StartActivityForResult(),
      new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
          if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri selectedData = result.getData().getData();
            if (selectedData != null) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                  getContentResolver().takePersistableUriPermission(selectedData,
                      Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (SecurityException e) {
                  Log.e("SettingsActivity", "Failed to persist URI permission: " + e.getMessage());
                }
              }
              // Add to ViewModel
              viewModel.addFolder(selectedData.toString());
            }
          }
        }
      });

  private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
      new ActivityResultContracts.RequestPermission(),
      new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean isGranted) {
          if (isGranted) {
            // Permission granted, automatically click the button again or show message
            findViewById(R.id.btnSelectFolder).performClick();
          } else {
            // Permission denied
            Snackbar
                .make(findViewById(android.R.id.content), R.string.read_external_permission_error, Snackbar.LENGTH_LONG)
                .show();
          }
        }
      });

  private boolean showMessage(View view, String message) {
    if (!message.equals("")) {
      Snackbar.make(view, message, Snackbar.LENGTH_LONG)
          .setAction("Action", null).show();
      return true;
    }
    return false;
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(newBase));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    // Initialize ViewModel
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

    setTitle(R.string.title_activity_settings);
    listViewIps = findViewById(R.id.lvIps);
    listViewFolders = findViewById(R.id.lvFolders);
    Toolbar toolbar = findViewById(R.id.toolbar);

    setSupportActionBar(toolbar);

    // Setup Adapters
    adapterIps = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listIps);
    adapterFolders = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listFolders);

    listViewIps.setAdapter(adapterIps);
    listViewFolders.setAdapter(adapterFolders);

    // Observe ViewModel LiveData
    observeViewModel();

    // Fallback for older Android versions
    fileDialog = new SimpleFileChooser(this, Environment.getExternalStorageDirectory(), onFileSelectedListener, true);

    setupLanguageSpinner();
    setupSaveButton();
    setupAddFolderButton();
    setupIpControls();

    listViewIps.setOnItemClickListener(new ListViewItemClickListener(true, R.string.ipRemoved));
    listViewFolders.setOnItemClickListener(new ListViewItemClickListener(false, R.string.folderRemoved));
  }

  private void observeViewModel() {
    viewModel.getPort().observe(this, port -> {
      ((TextInputLayout) findViewById(R.id.tilPort)).getEditText().setText(String.valueOf(port));
    });

    viewModel.getMaxConnections().observe(this, max -> {
      ((TextInputLayout) findViewById(R.id.tilMaximumClientsNumber)).getEditText().setText(String.valueOf(max));
    });

    viewModel.getIps().observe(this, ips -> {
      listIps.clear();
      if (ips != null)
        listIps.addAll(ips);
      adapterIps.notifyDataSetChanged();
    });

    viewModel.getFolders().observe(this, folders -> {
      listFolders.clear();
      if (folders != null)
        listFolders.addAll(folders);
      adapterFolders.notifyDataSetChanged();
    });

    viewModel.getListType().observe(this, type -> {
      if (type > 0) {
        RadioButton radio = findViewById(type);
        if (radio != null)
          radio.setChecked(true);
      }
    });

    viewModel.getReadOnly().observe(this, readOnly -> {
      ((CheckBox) findViewById(R.id.cbReadOnly)).setChecked(readOnly);
    });

    viewModel.getLogErrors().observe(this, logErrors -> {
      ((CheckBox) findViewById(R.id.cbLogErrors)).setChecked(logErrors);
    });

    viewModel.getLogCommands().observe(this, logCommands -> {
      ((CheckBox) findViewById(R.id.cbLogCommands)).setChecked(logCommands);
    });
  }

  private void setupLanguageSpinner() {
    Spinner languageSpinner = findViewById(R.id.language_spinner);
    String currentLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);
    int position = Language.fromCode(currentLanguage).getIndex();
    languageSpinner.setSelection(position);

    languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedLang = Language.fromIndex(position).getCode();
        if (!selectedLang.equals(com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(SettingsActivity.this))) {
          Context localizedContext = com.jhonju.ps3netsrv.app.utils.LocaleHelper.setLocale(SettingsActivity.this,
              selectedLang);
          updateViewResources(localizedContext);
          recreate(); // Restart activity to apply changes
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  private void setupSaveButton() {
    Button btnSave = findViewById(R.id.btnSave);
    btnSave.setOnClickListener(view -> {
      if (PS3NetService.isRunning()) {
        new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
            .setTitle(getString(R.string.title_activity_settings))
            .setMessage(getString(R.string.server_restart_prompt))
            .setPositiveButton(android.R.string.yes, (dialog, which) -> attemptSave(view))
            .setNegativeButton(android.R.string.no, null)
            .show();
      } else {
        attemptSave(view);
      }
    });
  }

  private void attemptSave(View view) {
    // Validate Inputs
    TextInputLayout tilPort = findViewById(R.id.tilPort);
    TextInputLayout tilMaxClients = findViewById(R.id.tilMaximumClientsNumber);
    String portStr = tilPort.getEditText().getText().toString().trim();
    String maxConnStr = tilMaxClients.getEditText().getText().toString().trim();
    int listType = ((RadioGroup) findViewById(R.id.rgIpListType)).getCheckedRadioButtonId();
    boolean readOnly = ((CheckBox) findViewById(R.id.cbReadOnly)).isChecked();
    boolean logErrors = ((CheckBox) findViewById(R.id.cbLogErrors)).isChecked();
    boolean logCommands = ((CheckBox) findViewById(R.id.cbLogCommands)).isChecked();

    // Perform simplistic validation here for UI feedback before calling VM
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

    if (viewModel.validateAndSave(portStr, maxConnStr, listType, readOnly, logErrors, logCommands)) {
      showMessage(view, getString(R.string.saveSuccess));
      // Restart Service
      if (PS3NetService.isRunning()) {
        stopService(new Intent(SettingsActivity.this, PS3NetService.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          startForegroundService(new Intent(SettingsActivity.this, PS3NetService.class));
        } else {
          startService(new Intent(SettingsActivity.this, PS3NetService.class));
        }
      }
    } else {
      showMessage(view, getString(R.string.invalidPortValue)); // Fallback error
    }
  }

  private void setupAddFolderButton() {
    final Button btnSelectFolder = findViewById(R.id.btnSelectFolder);
    btnSelectFolder.setOnClickListener(view -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
      } else {
        int permission = ContextCompat.checkSelfPermission(SettingsActivity.this,
            Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission == PERMISSION_GRANTED) {
          fileDialog.showDialog();
        } else {
          requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
      }
    });
  }

  private void setupIpControls() {
    final EditText editTextIp = findViewById(R.id.etIp);
    InputFilter[] filters = new InputFilter[1];
    filters[0] = (source, start, end, dest, dstart, dend) -> {
      if (end > start) {
        String destTxt = dest.toString();
        String resultingTxt = destTxt.substring(0, dstart)
            + source.subSequence(start, end)
            + destTxt.substring(dend);
        if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
          return "";
        } else {
          String[] splits = resultingTxt.split("\\.");
          for (String split : splits) {
            if (!split.isEmpty() && Integer.parseInt(split) > 255) {
              return "";
            }
          }
        }
      }
      return null;
    };
    editTextIp.setFilters(filters);

    final Button btnAddIp = findViewById(R.id.btnAddIp);
    btnAddIp.setOnClickListener(v -> {
      String newIp = editTextIp.getText().toString();
      if (!newIp.isEmpty()) {
        viewModel.addIp(newIp);
        editTextIp.setText("");
      }
    });

    RadioGroup rgIpListType = findViewById(R.id.rgIpListType);
    rgIpListType.setOnCheckedChangeListener((group, checkedId) -> {
      editTextIp.setText("");
      editTextIp.setEnabled(checkedId != R.id.rbNone);
      btnAddIp.setEnabled(checkedId != R.id.rbNone);
    });
    // Initial state set by Observer
  }

  // Event when a file is selected on file dialog (Legacy)
  private final SimpleFileChooser.FileSelectedListener onFileSelectedListener = file -> {
    viewModel.addFolder(file.getAbsolutePath());
  };

  private class ListViewItemClickListener implements AdapterView.OnItemClickListener {
    private final boolean isIpList;
    private final int removedMessageResId;

    public ListViewItemClickListener(boolean isIpList, int removedMessageResId) {
      this.isIpList = isIpList;
      this.removedMessageResId = removedMessageResId;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
      final String value = isIpList ? listIps.get(position) : listFolders.get(position);

      if (isIpList)
        viewModel.removeIp(position);
      else
        viewModel.removeFolder(position);

      Snackbar.make(view, getResources().getString(removedMessageResId) + value, Snackbar.LENGTH_SHORT)
          .setAction(getResources().getString(R.string.undo), v -> {
            if (isIpList)
              viewModel.undoRemoveIp(position, value);
            else
              viewModel.undoRemoveFolder(position, value);
          }).show();
    }
  }

  private void updateViewResources(Context context) {
    Resources resources = context.getResources();
    setTitle(R.string.title_activity_settings);
    ((TextView) findViewById(R.id.tvSectionGeneral)).setText(resources.getString(R.string.section_general));
    ((TextView) findViewById(R.id.tvSectionContent)).setText(resources.getString(R.string.section_content));
    ((TextView) findViewById(R.id.tvSectionSecurity)).setText(resources.getString(R.string.section_security));
    ((TextView) findViewById(R.id.tvAppLanguage)).setText(resources.getString(R.string.select_language));
    ((TextInputLayout) findViewById(R.id.tilPort)).setHint(resources.getString(R.string.port));
    ((TextInputLayout) findViewById(R.id.tilFolder)).setHint(resources.getString(R.string.folder));
    ((Button) findViewById(R.id.btnSelectFolder)).setText(resources.getString(R.string.add));
    ((TextInputLayout) findViewById(R.id.tilMaximumClientsNumber))
        .setHint(resources.getString(R.string.maxConnectedClients));
    ((CheckBox) findViewById(R.id.cbReadOnly)).setText(resources.getString(R.string.readOnly));
    ((CheckBox) findViewById(R.id.cbLogErrors)).setText(resources.getString(R.string.settings_log_errors));
    ((CheckBox) findViewById(R.id.cbLogCommands)).setText(resources.getString(R.string.settings_log_commands));
    ((TextView) findViewById(R.id.tvListType)).setText(resources.getString(R.string.listType));
    ((RadioButton) findViewById(R.id.rbNone)).setText(resources.getString(R.string.rbNone));
    ((RadioButton) findViewById(R.id.rbAllowed)).setText(resources.getString(R.string.rbAllowed));
    ((RadioButton) findViewById(R.id.rbBlocked)).setText(resources.getString(R.string.rbBlocked));
    ((TextInputLayout) findViewById(R.id.tilIp)).setHint(resources.getString(R.string.ipAddress));
    ((Button) findViewById(R.id.btnAddIp)).setText(resources.getString(R.string.add));
    ((Button) findViewById(R.id.btnSave)).setText(resources.getString(R.string.save));
  }
}