package com.jhonju.ps3netsrv.server.commands;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.io.IFile;

public class ReadFileCriticalCommand extends ReadFileCommand {

  public ReadFileCriticalCommand(Context ctx, int numBytes, long offset) {
    super(ctx, numBytes, offset);
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    // The wire length is uint32; requests larger than the buffer are streamed.
    long remaining = numBytes & 0xffffffffL;
    if (offset < 0 || offset > Long.MAX_VALUE - remaining) {
      throw new IOException("Invalid critical read offset");
    }
    java.util.Set<IFile> files = ctx.getFile();
    if (files == null || files.isEmpty()) {
      throw new IOException(ctx.getAndroidContext().getString(R.string.error_no_file_open));
    }
    IFile file = files.iterator().next();
    byte[] buffer = ctx.getOutputBuffer();
    OutputStream os = ctx.getOutputStream();
    long position = offset;
    while (remaining > 0) {
      int wanted = (int) Math.min(remaining, buffer.length);
      int read = 0;
      while (read < wanted) {
        int count = file.read(buffer, read, wanted - read, position + read);
        if (count <= 0) {
          // This command has no length/error header. An incomplete reply is fatal.
          throw new EOFException(ctx.getAndroidContext().getString(R.string.error_read_file_eof));
        }
        read += count;
      }
      os.write(buffer, 0, wanted);
      position += wanted;
      remaining -= wanted;
    }
    os.flush();
  }
}
