package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Set;

import android.content.ContentResolver;

/**
 * Context manages the server-side state for a single PS3 client connection.
 * 
 * This class encapsulates:
 * - Socket connection to the client
 * - Accessible root directories
 * - Open file handles
 * - CD sector size configuration
 * - Read-only mode status
 * 
 * @author JCorrêa
 */
public class Context implements Closeable {
  private Socket socket;
  private final ContentResolver contentResolver;
  private final android.content.Context androidContext;
  private final byte[] outputBuffer = new byte[BinaryUtils.BUFFER_SIZE];

  private final List<String> rootDirectories;
  private Set<IFile> file;
  private CDSectorSize cdSectorSize;

  /**
   * Constructs a Context for a new client connection.
   * 
   * @param socket The TCP socket connection to the PS3 client
   * @param rootDirectories List of root directories accessible to this client
   * @param contentResolver Android ContentResolver for file access
   */
  public Context(Socket socket, List<String> rootDirectories, ContentResolver contentResolver, android.content.Context androidContext) {
    this.rootDirectories = java.util.Objects.requireNonNull(rootDirectories, "rootDirectories cannot be null");
    this.socket = java.util.Objects.requireNonNull(socket, "socket cannot be null");
    this.contentResolver = java.util.Objects.requireNonNull(contentResolver, "contentResolver cannot be null");
    this.androidContext = java.util.Objects.requireNonNull(androidContext, "androidContext cannot be null");
    this.cdSectorSize = CDSectorSize.CD_SECTOR_2352;
    try {
      socket.setSoTimeout(60000);
    } catch (SocketException e) {
      FileLogger.logWarning("Failed to set socket timeout", e);
    }
  }

  /**
   * Gets the list of accessible root directories for this client.
   * 
   * @return List of root directory paths
   */
  public List<String> getRootDirectories() {
    return rootDirectories;
  }

  /**
   * Gets the Android ContentResolver for file operations.
   * 
   * @return ContentResolver instance
   */
  public ContentResolver getContentResolver() {
    return contentResolver;
  }

  public android.content.Context getAndroidContext() {
    return androidContext;
  }

  public byte[] getOutputBuffer() {
    return outputBuffer;
  }

  /**
   * Checks if the socket connection is still active.
   * 
   * @return true if socket is connected, false otherwise
   */
  public boolean isSocketConnected() {
    return socket != null && socket.isConnected();
  }

  /**
   * Gets the current CD sector size configuration.
   * 
   * @return CDSectorSize enum value
   */
  public CDSectorSize getCdSectorSize() {
    return cdSectorSize;
  }

  /**
   * Sets the CD sector size configuration.
   * 
   * @param cdSectorSize The new sector size
   */
  public void setCdSectorSize(CDSectorSize cdSectorSize) {
    this.cdSectorSize = cdSectorSize;
  }

  /**
   * Gets the input stream from the client socket.
   * Used to receive commands from the PS3.
   * 
   * @return InputStream for reading client data
   * @throws IOException If socket communication fails
   */
  public InputStream getInputStream() throws IOException {
    if (socket == null) {
      throw new IOException("Socket is not connected");
    }
    return socket.getInputStream();
  }

  /**
   * Gets the output stream to the client socket.
   * Used to send responses to the PS3.
   * 
   * @return OutputStream for writing response data
   * @throws IOException If socket communication fails
   */
  public OutputStream getOutputStream() throws IOException {
    if (socket == null) {
      throw new IOException("Socket is not connected");
    }
    return socket.getOutputStream();
  }

  /**
   * Sets the file set for current operation.
   * These are the files being accessed in the current command.
   * 
   * @param files Set of IFile objects
   */
  public void setFile(Set<IFile> files) {
    this.file = files;
  }

  /**
   * Gets the file set from the current operation.
   * 
   * @return Set of IFile objects, or null if not set
   */
  public Set<IFile> getFile() {
    return file;
  }

  /**
   * Checks if the server is in read-only mode.
   * 
   * @return true if operations should not modify files
   */
  public boolean isReadOnly() {
    return com.jhonju.ps3netsrv.app.SettingsService.isReadOnly();
  }

  /**
   * Closes all resources associated with this context.
   * 
   * Proper resource cleanup sequence:
   * 1. Close all open file handles
   * 2. Close the socket connection
   * 3. Clear references
   * 
   * This method is idempotent - it can be called multiple times safely.
   * This method is called automatically when using try-with-resources statement.
   */
  @Override
  public void close() {
    // Close all open files
    if (file != null) {
      for (IFile f : file) {
        if (f != null) {
          try {
            f.close();
            FileLogger.logInfo("File closed: " + f.getName());
          } catch (IOException e) {
            FileLogger.logWarning("Error closing file: " + f.getName(), e);
          }
        }
      }
      file = null;
    }

    // Close the socket connection
    if (socket != null && !socket.isClosed()) {
      try {
        socket.close();
        FileLogger.logInfo("Client socket closed");
      } catch (IOException e) {
        FileLogger.logWarning("Error closing socket", e);
      } finally {
        socket = null;
      }
    }
  }

  /**
   * Legacy close method for backward compatibility.
   * New code should use try-with-resources or explicit close() calls.
   * 
   * @deprecated Use try-with-resources or close() instead
   */
  @Deprecated
  public void closeResources() {
    close();
  }
}