package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.R;

import java.io.IOException;

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

    java.util.Set<com.jhonju.ps3netsrv.server.io.IFile> files = getFile();
    boolean success = false;
    for (com.jhonju.ps3netsrv.server.io.IFile file : files) {
      if (file.delete()) {
        success = true;
      }
    }

    send(success ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
  }
}