package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.IOException;
import java.io.OutputStream;

public class ReadFileCommand extends AbstractCommand {
  protected int numBytes;
  protected long offset;

  public ReadFileCommand(Context ctx, int numBytes, long offset) {
    super(ctx);
    this.numBytes = numBytes;
    this.offset = offset;
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    try {
      int bytesRead = 0;
      java.util.Set<IFile> files = ctx.getFile();
      if (files != null && !files.isEmpty()) {
        IFile file = files.iterator().next();
        bytesRead = file.read(ctx.getOutputBuffer(), 0, numBytes, offset);
      } else {
        throw new IOException(ctx.getAndroidContext().getString(R.string.error_no_file_open));
      }
      if (bytesRead <= EMPTY_SIZE) {
        throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_read_file_eof));
      }
      OutputStream os = ctx.getOutputStream();
      os.write(BinaryUtils.intToBytesBE(bytesRead));
      os.write(ctx.getOutputBuffer(), 0, bytesRead);
      os.flush();
    } catch (IOException e) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_read_file_generic));
    }
  }
}
