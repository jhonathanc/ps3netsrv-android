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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class SettingsActivity extends AppCompatActivity {

  private List<String> listIps = new ArrayList<>();
  private List<String> listFolders = new ArrayList<>();
  private ListView listViewIps;
  private ListView listViewFolders;

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
              listFolders.add(selectedData.toString());
              SettingsService.setFolders(listFolders);
              ((ArrayAdapter) listViewFolders.getAdapter()).notifyDataSetChanged();
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
    ((TextInputLayout) findViewById(R.id.tilMaximumClientsNumber)).getEditText()
        .setText(SettingsService.getMaxConnections() + "");
    listIps.addAll(SettingsService.getIps());
    listFolders.addAll(SettingsService.getFolders());
    int listType = SettingsService.getListType();
    if (listType > 0) {
      RadioButton radio = findViewById(listType);
      if (radio != null)
        radio.setChecked(true);
    }
    ((android.widget.CheckBox) findViewById(R.id.cbReadOnly)).setChecked(SettingsService.isReadOnly());
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
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(com.jhonju.ps3netsrv.app.utils.LocaleHelper.onAttach(newBase));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);
    setTitle(R.string.title_activity_settings);
    listViewIps = findViewById(R.id.lvIps);
    listViewFolders = findViewById(R.id.lvFolders);
    Toolbar toolbar = findViewById(R.id.toolbar);

    setSupportActionBar(toolbar);

    loadSettings();

    // Fallback for older Android versions
    fileDialog = new SimpleFileChooser(this, Environment.getExternalStorageDirectory(), onFileSelectedListener, true);

    // Language Spinner Setup
    android.widget.Spinner languageSpinner = findViewById(R.id.language_spinner);
    String currentLanguage = com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(this);
    int position = getIndexFromLanguage(currentLanguage);
    languageSpinner.setSelection(position);

    languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedLang = getLanguageFromIndex(position);
        if (!selectedLang.equals(com.jhonju.ps3netsrv.app.utils.LocaleHelper.getLanguage(SettingsActivity.this))) {
          Context localizedContext = com.jhonju.ps3netsrv.app.utils.LocaleHelper.setLocale(SettingsActivity.this,
              selectedLang);
          updateViewResources(localizedContext);
          recreate(); // Restart activity to apply changes
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
      }
    });

    Button btnSave = findViewById(R.id.btnSave);
    btnSave.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (PS3NetService.isRunning()) {
          new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
              .setTitle(getString(R.string.title_activity_settings))
              .setMessage("Server is running. Restart to apply changes?")
              .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                if (saveSettings(view)) {
                  // Restart Service
                  stopService(new Intent(SettingsActivity.this, PS3NetService.class));
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(SettingsActivity.this, PS3NetService.class));
                  } else {
                    startService(new Intent(SettingsActivity.this, PS3NetService.class));
                  }
                }
              })
              .setNegativeButton(android.R.string.no, null)
              .show();
        } else {
          saveSettings(view);
        }
      }
    });

    final Button btnSelectFolder = findViewById(R.id.btnSelectFolder);
    btnSelectFolder.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // For Lollipop and above, use Storage Access Framework (SAF)
        // This does NOT require READ_EXTERNAL_STORAGE permission to launch the picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
          folderPickerLauncher.launch(intent);
        } else {
          // For legacy devices (Pre-Lollipop), use internal file chooser which requires
          // permission
          int permission = ContextCompat.checkSelfPermission(SettingsActivity.this,
              Manifest.permission.READ_EXTERNAL_STORAGE);
          if (permission == PERMISSION_GRANTED) {
            fileDialog.showDialog();
          } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
          }
        }
      }
    });

    final ArrayAdapter<String> adapterIps = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listIps);
    final ArrayAdapter<String> adapterFolders = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
        listFolders);

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
              if (!split.isEmpty() && Integer.parseInt(split) > 255) {
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

    listViewIps.setOnItemClickListener(new ListViewItemClickListener(listIps, R.string.ipRemoved));
    listViewFolders.setOnItemClickListener(new ListViewItemClickListener(listFolders, R.string.folderRemoved));

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

  private int getIndexFromLanguage(String language) {
    switch (language) {
      case "it":
        return 0;
      case "en":
        return 1;
      case "es":
        return 2;
      case "pt":
        return 3;
      case "jv":
        return 4;
      case "ja":
        return 4;
      default:
        return 1; // Default to English
    }
  }

  private String getLanguageFromIndex(int index) {
    switch (index) {
      case 0:
        return "it";
      case 1:
        return "en";
      case 2:
        return "es";
      case 3:
        return "pt";
      case 4:
        return "jv";
      default:
        return "en";
    }
  }

  // Event when a file is selected on file dialog.
  private final SimpleFileChooser.FileSelectedListener onFileSelectedListener = new SimpleFileChooser.FileSelectedListener() {
    @Override
    public void onFileSelected(File file) {
      listFolders.add(file.getAbsolutePath());
      SettingsService.setFolders(listFolders);
      ((ArrayAdapter) listViewFolders.getAdapter()).notifyDataSetChanged();
    }
  };

  private class ListViewItemClickListener implements AdapterView.OnItemClickListener {
    private final List<String> list;
    private final int removedMessageResId;

    public ListViewItemClickListener(List<String> list, int removedMessageResId) {
      this.list = list;
      this.removedMessageResId = removedMessageResId;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
      ListView listView = (ListView) parent;
      final ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();
      final String value = list.get(position);
      list.remove(position);
      adapter.notifyDataSetChanged();

      Snackbar.make(view, getResources().getString(removedMessageResId) + value, Snackbar.LENGTH_SHORT)
          .setAction(getResources().getString(R.string.undo), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              list.add(position, value);
              adapter.notifyDataSetChanged();
            }
          }).show();
    }
  }

  private boolean saveSettings(View view) {
    String message = savePortValue();
    boolean hasError = showMessage(view, message);
    hasError = showMessage(view, message) || hasError;
    message = saveMaxConnection();
    hasError = showMessage(view, message) || hasError;
    if (!hasError) {
      SettingsService.setIps(new HashSet<>(listIps));
      SettingsService.setFolders(listFolders);
      SettingsService.setListType(((RadioGroup) findViewById(R.id.rgIpListType)).getCheckedRadioButtonId());
      SettingsService.setReadOnly(((android.widget.CheckBox) findViewById(R.id.cbReadOnly)).isChecked());
      showMessage(view, getResources().getString(R.string.saveSuccess));
      return true;
    }
    return false;
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
    ((android.widget.CheckBox) findViewById(R.id.cbReadOnly)).setText(resources.getString(R.string.readOnly));
    ((TextView) findViewById(R.id.tvListType)).setText(resources.getString(R.string.listType));
    ((RadioButton) findViewById(R.id.rbNone)).setText(resources.getString(R.string.rbNone));
    ((RadioButton) findViewById(R.id.rbAllowed)).setText(resources.getString(R.string.rbAllowed));
    ((RadioButton) findViewById(R.id.rbBlocked)).setText(resources.getString(R.string.rbBlocked));
    ((TextInputLayout) findViewById(R.id.tilIp)).setHint(resources.getString(R.string.ipAddress));
    ((Button) findViewById(R.id.btnAddIp)).setText(resources.getString(R.string.add));
    ((Button) findViewById(R.id.btnSave)).setText(resources.getString(R.string.save));
  }

}