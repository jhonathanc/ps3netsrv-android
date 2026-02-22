package com.jhonju.ps3netsrv.server.utils;

import android.util.Log;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.app.SettingsService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {

  private static final String ERROR_LOG_FILE = "ps3netsrv_error.log";
  private static final String COMMAND_LOG_FILE = "ps3netsrv_commands.log";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

  public static void logInfo(String message) {
    if (!SettingsService.isLogCommands())
      return;
    writeLog(ERROR_LOG_FILE, "INFO: " + message);
  }

  public static void logWarning(String message) {
    if (!SettingsService.isLogErrors())
      return;
    writeLog(ERROR_LOG_FILE, "WARNING: " + message);
  }

  public static void logWarning(String message, Throwable t) {
    if (!SettingsService.isLogErrors())
      return;
    StringBuilder sb = new StringBuilder();
    sb.append(message).append("\n");
    sb.append(t.toString()).append("\n");
    for (StackTraceElement element : t.getStackTrace()) {
      sb.append("\tat ").append(element.toString()).append("\n");
    }
    writeLog(ERROR_LOG_FILE, "WARNING: " + sb);
  }

  public static void logError(String message) {
    if (!SettingsService.isLogErrors())
      return;
    writeLog(ERROR_LOG_FILE, "ERROR: " + message);
  }

  public static void logError(String message, Throwable t) {
    if (!SettingsService.isLogErrors())
      return;
    StringBuilder sb = new StringBuilder();
    sb.append(message).append("\n");
    sb.append(t.toString()).append("\n");
    for (StackTraceElement element : t.getStackTrace()) {
      sb.append("\tat ").append(element.toString()).append("\n");
    }
    writeLog(ERROR_LOG_FILE, "ERROR: " + sb);
  }

  public static void logError(Throwable t) {
    if (!SettingsService.isLogErrors())
      return;
    StringBuilder sb = new StringBuilder();
    sb.append(t.toString()).append("\n");
    for (StackTraceElement element : t.getStackTrace()) {
      sb.append("\tat ").append(element.toString()).append("\n");
    }
    writeLog(ERROR_LOG_FILE, "EXCEPTION: " + sb);
  }

  public static void logCommand(String opcodeName, byte[] commandData) {
    if (!SettingsService.isLogCommands())
      return;
    // Format: [OPCODE] HEX_DATA
    String dataHex = commandData != null ? BinaryUtils.bytesToHex(commandData) : "null";
    writeLog(COMMAND_LOG_FILE, "[" + opcodeName + "] " + dataHex);
  }

  private static synchronized void writeLog(String fileName, String content) {
    File logFile = getLogFile(fileName);
    if (logFile == null)
      return;

    FileOutputStream fos = null;
    OutputStreamWriter writer = null;

    try {
      fos = new FileOutputStream(logFile, true);
      writer = new OutputStreamWriter(fos);

      String timestamp = DATE_FORMAT.format(new Date());
      writer.append("[").append(timestamp)
          .append("] ")
          .append(content)
          .append("\n");

      writer.flush();

    } catch (IOException e) {
      Log.e("FileLogger", "Failed to write to " + fileName, e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          Log.e("FileLogger", "Failed to close writer", e);
        }
      } else if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          Log.e("FileLogger", "Failed to close fos", e);
        }
      }
    }
  }

  private static File getLogFile(String fileName) {
    File dir = PS3NetSrvApp.getAppContext().getExternalFilesDir(null);
    if (dir == null) {
      dir = PS3NetSrvApp.getAppContext().getFilesDir();
    }
    if (dir == null)
      return null;

    return new File(dir, fileName);
  }
}
