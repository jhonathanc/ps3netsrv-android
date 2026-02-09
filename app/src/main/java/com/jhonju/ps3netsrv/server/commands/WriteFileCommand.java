package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

public class WriteFileCommand extends FileCommand {

  private final int numBytes;

  public WriteFileCommand(Context ctx, short filePathLength, int numBytes) {
    super(ctx, filePathLength);
    this.numBytes = numBytes;
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    if (ctx.isReadOnly()) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_write_file_readonly));
    }

    // Read filename and resolve files
    // Note: WriteFile usually expects the file to exist (created by CreateFile)
    // So we use getFile() without parent resolution.
    Set<IFile> files = getFile();

    if (numBytes > BUFFER_SIZE) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(
          PS3NetSrvApp.getAppContext().getString(R.string.error_write_file_size, numBytes, BUFFER_SIZE));
    }

    ByteBuffer buffer = BinaryUtils.readCommandData(ctx.getInputStream(), numBytes);
    if (buffer == null) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_write_file_null));
    }

    byte[] content = buffer.array();
    for (IFile file : files) {
      file.write(content);
    }

    send(BinaryUtils.intToBytesBE(content.length));
  }
}
