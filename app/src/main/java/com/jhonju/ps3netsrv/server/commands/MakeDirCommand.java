package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.FileLogger;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.R;
import java.io.IOException;
import java.util.Set;

public class MakeDirCommand extends FileCommand {

  public MakeDirCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
  }

  @Override
  public void executeTask() throws PS3NetSrvException, IOException {
    if (ctx.isReadOnly()) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_make_dir_readonly));
    }

    Set<IFile> parents = getFile(true);
    boolean success = false;

    for (IFile parent : parents) {
      if (parent != null && parent.exists() && parent.isDirectory()) {
        if (parent.createDirectory(fileName)) {
          success = true;
        } else {
          FileLogger.logError("Failed to create directory: " + parent.getName() + "/" + fileName);
        }
      }
    }

    send(success ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
  }
}