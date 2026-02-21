package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.R;

import java.io.IOException;
import java.util.Set;

public class CreateFileCommand extends FileCommand {

  public CreateFileCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
  }

  @Override
  public void executeTask() throws PS3NetSrvException, IOException {
    if (ctx.isReadOnly()) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_create_file_readonly));
    }

    try {
      Set<IFile> parents = getFile(true);
      boolean success = false;

      for (IFile parent : parents) {
        if (parent != null && parent.exists() && parent.isDirectory()) {
          if (parent.createFile(fileName)) {
            success = true;
          }
        }
      }

      if (!success) {
        throw new IOException(ctx.getAndroidContext().getString(R.string.error_create_file_generic, fileName));
      }
      // ctx.setWriteOnlyFile(file);
      // TODO: FIX the writeOnlyFile on ctx
      send(SUCCESS_CODE_BYTEARRAY);
    } catch (IOException ex) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(ex.getMessage());
    }
  }
}
