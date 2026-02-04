package com.jhonju.ps3netsrv.app;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.jhonju.ps3netsrv.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsViewModel extends ViewModel {

  private final MutableLiveData<Integer> port = new MutableLiveData<>();
  private final MutableLiveData<Integer> maxConnections = new MutableLiveData<>();
  private final MutableLiveData<List<String>> ips = new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<List<String>> folders = new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<Integer> listType = new MutableLiveData<>();
  private final MutableLiveData<Boolean> readOnly = new MutableLiveData<>();
  private final MutableLiveData<Boolean> logErrors = new MutableLiveData<>();
  private final MutableLiveData<Boolean> logCommands = new MutableLiveData<>();

  // Validation messages or events could be handled via LiveData (e.g.
  // SingleLiveEvent),
  // but for now we'll return results from the save method.

  public SettingsViewModel() {
    // Load initial state
    loadSettings();
  }

  public void loadSettings() {
    port.setValue(SettingsService.getPort());
    maxConnections.setValue(SettingsService.getMaxConnections());
    ips.setValue(new ArrayList<>(SettingsService.getIps()));
    folders.setValue(SettingsService.getFolders());
    listType.setValue(SettingsService.getListType());
    readOnly.setValue(SettingsService.isReadOnly());
    logErrors.setValue(SettingsService.isLogErrors());
    logCommands.setValue(SettingsService.isLogCommands());
  }

  public LiveData<Integer> getPort() {
    return port;
  }

  public LiveData<Integer> getMaxConnections() {
    return maxConnections;
  }

  public LiveData<List<String>> getIps() {
    return ips;
  }

  public LiveData<List<String>> getFolders() {
    return folders;
  }

  public LiveData<Integer> getListType() {
    return listType;
  }

  public LiveData<Boolean> getReadOnly() {
    return readOnly;
  }

  public LiveData<Boolean> getLogErrors() {
    return logErrors;
  }

  public LiveData<Boolean> getLogCommands() {
    return logCommands;
  }

  public void addIp(String ip) {
    if (ip == null || ip.isEmpty())
      return;
    List<String> current = ips.getValue();
    if (current == null)
      current = new ArrayList<>();
    current.add(ip);
    ips.setValue(current);
  }

  public void removeIp(int index) {
    List<String> current = ips.getValue();
    if (current != null && index >= 0 && index < current.size()) {
      current.remove(index);
      ips.setValue(current);
    }
  }

  public void undoRemoveIp(int index, String value) {
    List<String> current = ips.getValue();
    if (current != null && index >= 0 && index <= current.size()) {
      current.add(index, value);
      ips.setValue(current);
    }
  }

  public void addFolder(String folderPath) {
    if (folderPath == null || folderPath.isEmpty())
      return;
    List<String> current = folders.getValue();
    if (current == null)
      current = new ArrayList<>();
    current.add(folderPath);
    folders.setValue(current);
  }

  public void removeFolder(int index) {
    List<String> current = folders.getValue();
    if (current != null && index >= 0 && index < current.size()) {
      current.remove(index);
      folders.setValue(current);
    }
  }

  public void undoRemoveFolder(int index, String value) {
    List<String> current = folders.getValue();
    if (current != null && index >= 0 && index <= current.size()) {
      current.add(index, value);
      folders.setValue(current);
    }
  }

  public boolean validateAndSave(String portStr, String maxConnectionsStr, int listTypeVal, boolean readOnlyVal,
      boolean logErrorsVal, boolean logCommandsVal) {
    // Parse and Validate
    int parsedPort;
    try {
      parsedPort = Integer.parseInt(portStr.trim());
      if (parsedPort <= 1024)
        return false; // Or handle specific error
    } catch (NumberFormatException e) {
      return false;
    }

    int parsedMaxConn;
    try {
      parsedMaxConn = Integer.parseInt(maxConnectionsStr.trim());
      if (parsedMaxConn < 0)
        return false;
    } catch (NumberFormatException e) {
      return false;
    }

    // Save to Persistence
    SettingsService.setPort(parsedPort);
    SettingsService.setMaxConnections(parsedMaxConn);
    SettingsService.setListType(listTypeVal);
    SettingsService.setReadOnly(readOnlyVal);
    SettingsService.setLogErrors(logErrorsVal);
    SettingsService.setLogCommands(logCommandsVal);

    List<String> currentIps = ips.getValue();
    SettingsService.setIps(new HashSet<>(currentIps != null ? currentIps : new ArrayList<>()));

    List<String> currentFolders = folders.getValue();
    SettingsService.setFolders(currentFolders != null ? currentFolders : new ArrayList<>());

    // Refresh internal state to match saved
    loadSettings();
    return true;
  }

  // Helpers for simple error checking in View if needed,
  // though the validateAndSave abstraction is cleaner for the 'Save' button
  // action.
}
