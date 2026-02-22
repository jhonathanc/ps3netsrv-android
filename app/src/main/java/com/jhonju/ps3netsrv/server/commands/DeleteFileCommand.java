package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.util.Set;

/**
 * DeleteFileCommand handles file deletion requests from PS3 clients.
 * 
 * This command:
 * 1. Checks if the server is in read-only mode
 * 2. Attempts to delete files from the provided file set
 * 3. Sends success/failure response to the client
 * 4. Logs all operations for debugging
 * 
 * @author JCorrêa
 */
public class DeleteFileCommand extends FileCommand {

  /**
   * Constructs a DeleteFileCommand for the given context.
   * 
   * @param ctx The server context with connection and configuration info
   * @param filePathLength Length of the file path in the incoming request
   */
  public DeleteFileCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
  }

  /**
   * Executes the file deletion task.
   * 
   * Process:
   * 1. Verifies server is not in read-only mode
   * 2. Retrieves the file set to be deleted
   * 3. Attempts to delete each file, logging results
   * 4. Sends response code indicating overall success/failure
   * 
   * @throws PS3NetSrvException If operation fails or server is read-only
   * @throws IOException If socket communication fails
   */
  @Override
  public void executeTask() throws PS3NetSrvException, IOException {
    if (ctx.isReadOnly()) {
      FileLogger.logWarning("Delete operation attempted in read-only mode");
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(
          R.string.error_delete_file_readonly));
    }

    Set<IFile> files = getFile();
    boolean success = false;
    int deletedCount = 0;
    int failedCount = 0;
    
    if (files != null) {
      for (IFile file : files) {
        try {
          if (file.delete()) {
            success = true;
            deletedCount++;
            FileLogger.logInfo("File deleted successfully: " + file.getName());
          } else {
            failedCount++;
            FileLogger.logWarning("Failed to delete file: " + file.getName());
          }
        } catch (Exception e) {
          failedCount++;
          FileLogger.logError("Exception while deleting file: " + 
              (file != null ? file.getName() : "unknown"), e);
        }
      }
    }
    
    if (deletedCount > 0) {
      FileLogger.logInfo("Delete operation completed: " + deletedCount + " deleted, " + 
          failedCount + " failed");
    }

    send(success ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
  }
}
