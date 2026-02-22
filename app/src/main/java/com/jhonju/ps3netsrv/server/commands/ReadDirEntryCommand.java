package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

public class ReadDirEntryCommand extends AbstractCommand {

  private static final int RESULT_LENGTH = 266;
  private static final short MAX_FILE_NAME_LENGTH = 255;
  private static final short EMPTY_FILE_NAME_LENGTH = 0;

  public ReadDirEntryCommand(Context ctx) {
    super(ctx);
  }

  private static class ReadDirEntryResult implements IResult {
    public final long aFileSize;
    public final short bFileNameLength;
    public final boolean cIsDirectory;
    public final String dFileName;

    public ReadDirEntryResult() {
      this.aFileSize = EMPTY_SIZE;
      this.bFileNameLength = EMPTY_FILE_NAME_LENGTH;
      this.cIsDirectory = false;
      this.dFileName = null;
    }

    public ReadDirEntryResult(long aFileSize, short bFileNameLength, boolean cIsDirectory, String dFileName) {
      this.aFileSize = aFileSize;
      this.bFileNameLength = bFileNameLength;
      this.cIsDirectory = cIsDirectory;
      this.dFileName = dFileName;
    }

    public byte[] toByteArray() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH);
      try {
        out.write(BinaryUtils.longToBytesBE(this.aFileSize));
        out.write(BinaryUtils.shortToBytesBE(this.bFileNameLength));
        out.write(cIsDirectory ? 1 : 0);
        if (dFileName != null) {
          out.write(dFileName.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
      } finally {
        out.close();
      }
    }
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    Set<IFile> directories = ctx.getFile();
    if (directories != null) {
      for (IFile file : directories) {
        if (file == null || !file.isDirectory()) {
          send(new ReadDirEntryResult());
          return;
        }
        IFile fileAux = null;
        String[] fileList = file.list();
        if (fileList != null) {
          for (String fileName : fileList) {
            fileAux = file.findFile(fileName);
            if (fileName.length() <= MAX_FILE_NAME_LENGTH) {
              break;
            }
          }
        }
        if (fileAux == null) {
          ctx.setFile(null);
          send(new ReadDirEntryResult());
          return;
        }
        send(new ReadDirEntryResult(
            fileAux.isDirectory() ? EMPTY_SIZE : file.length(), (short) fileAux.getName().length(),
            fileAux.isDirectory(), fileAux.getName()));
        return;
      }
    }
    ctx.setFile(null);
    send(new ReadDirEntryResult());
  }
}
