package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.util.Set;

public class DeleteFileCommand extends FileCommand {

  public DeleteFileCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
  }

  @Override
  public void executeTask() throws PS3NetSrvException, IOException {
    if (ctx.isReadOnly()) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_delete_file_readonly));
    }

    Set<IFile> files = getFile();
    boolean success = false;
    for (IFile file : files) {
      if (file.delete()) {
        success = true;
      } else {
        FileLogger.logError("Failed to delete file: " + file.getName());
      }
    }

    send(success ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
  }
}