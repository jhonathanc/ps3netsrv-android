package com.jhonju.ps3netsrv.server.commands;

import android.os.Build;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.FileCustom;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class ReadDirEntryCommandV2 extends AbstractCommand {

  private static final int RESULT_LENGTH = 290;
  private static final short MAX_FILE_NAME_LENGTH = 255;
  private static final short EMPTY_FILE_NAME_LENGTH = 0;

  public ReadDirEntryCommandV2(Context ctx) {
    super(ctx);
  }

  private static class ReadDirEntryResultV2 implements IResult {
    private final long aFileSize;
    private final long bModifiedTime;
    private final long cCreationTime;
    private final long dAccessedTime;
    private final short eFileNameLength;
    private final boolean fIsDirectory;
    private final String gFileName;

    public ReadDirEntryResultV2() {
      this.aFileSize = EMPTY_SIZE;
      this.bModifiedTime = 0L;
      this.cCreationTime = 0L;
      this.dAccessedTime = 0L;
      this.eFileNameLength = EMPTY_FILE_NAME_LENGTH;
      this.fIsDirectory = false;
      this.gFileName = null;
    }

    public ReadDirEntryResultV2(long aFileSize, long bModifiedTime, long cCreationTime, long dAccessedTime,
        short eFileNameLength, boolean fIsDirectory, String gFileName) {
      this.aFileSize = aFileSize;
      this.bModifiedTime = bModifiedTime;
      this.cCreationTime = cCreationTime;
      this.dAccessedTime = dAccessedTime;
      this.eFileNameLength = eFileNameLength;
      this.fIsDirectory = fIsDirectory;
      this.gFileName = gFileName;
    }

    public byte[] toByteArray() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH);
      try {
        out.write(BinaryUtils.longToBytesBE(this.aFileSize));
        out.write(BinaryUtils.longToBytesBE(this.bModifiedTime));
        out.write(BinaryUtils.longToBytesBE(this.cCreationTime));
        out.write(BinaryUtils.longToBytesBE(this.dAccessedTime));
        out.write(BinaryUtils.shortToBytesBE(this.eFileNameLength));
        out.write(fIsDirectory ? 1 : 0);
        if (gFileName != null) {
          out.write(gFileName.getBytes(StandardCharsets.UTF_8));
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
          send(new ReadDirEntryResultV2());
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
          send(new ReadDirEntryResultV2());
          return;
        }

        // Get file attributes
        long[] fileTimes = { 0, 0 };
        long modifiedTime = fileAux.lastModified() / MILLISECONDS_IN_SECOND;

        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (fileAux instanceof FileCustom) {
              Path path = ((FileCustom) fileAux).getRealFile().toPath();
              BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
              fileTimes[0] = attrs.creationTime().toMillis() / MILLISECONDS_IN_SECOND;
              fileTimes[1] = attrs.lastAccessTime().toMillis() / MILLISECONDS_IN_SECOND;
              modifiedTime = attrs.lastModifiedTime().toMillis() / MILLISECONDS_IN_SECOND;
            }
          }
        } catch (Exception e) {
          FileLogger.logError(e);
        }

        if (fileTimes[0] == 0) fileTimes[0] = modifiedTime;
        if (fileTimes[1] == 0) fileTimes[1] = modifiedTime;

        send(new ReadDirEntryResultV2(
            fileAux.isDirectory() ? EMPTY_SIZE : file.length(), modifiedTime, fileTimes[0], fileTimes[1],
            (short) (fileAux.getName() != null ? fileAux.getName().length() : 0), fileAux.isDirectory(),
            fileAux.getName()));
        return;
      }
    }
    ctx.setFile(null);
    send(new ReadDirEntryResultV2());
  }
}
