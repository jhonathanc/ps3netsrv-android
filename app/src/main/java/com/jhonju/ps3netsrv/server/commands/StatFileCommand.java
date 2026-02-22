package com.jhonju.ps3netsrv.server.commands;

import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.FileCustom;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

public class StatFileCommand extends FileCommand {

  private static final int RESULT_LENGTH = 33;

  public StatFileCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
  }

  private static class StatFileResult implements IResult {
    public final long aFileSize;
    public final long bModifiedTime;
    public final long cCreationTime;
    public final long dLastAccessTime;
    public final boolean eIsDirectory;

    public StatFileResult(long fileSize, long modifiedTime, long creationTime, long lastAccessTime,
        boolean isDirectory) {
      this.aFileSize = fileSize;
      this.bModifiedTime = modifiedTime;
      this.cCreationTime = creationTime;
      this.dLastAccessTime = lastAccessTime;
      this.eIsDirectory = isDirectory;
    }

    public StatFileResult() {
      this.aFileSize = -1L;
      this.bModifiedTime = 0L;
      this.cCreationTime = 0L;
      this.dLastAccessTime = 0L;
      this.eIsDirectory = false;
    }

    public byte[] toByteArray() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH);
      try {
        out.write(BinaryUtils.longToBytesBE(this.aFileSize));
        out.write(BinaryUtils.longToBytesBE(this.bModifiedTime));
        out.write(BinaryUtils.longToBytesBE(this.cCreationTime));
        out.write(BinaryUtils.longToBytesBE(this.dLastAccessTime));
        out.write(eIsDirectory ? 1 : 0);
        return out.toByteArray();
      } finally {
        out.close();
      }
    }
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    ctx.setFile(null);
    Set<IFile> files = getFile();
    if (files != null) {
      for (IFile file : files) {
        if (!file.exists())
          break;
        ctx.setFile(files);
        StatFileResult statResult;
        if (file.isDirectory()) {
          statResult = new StatFileResult(EMPTY_SIZE, file.lastModified() / MILLISECONDS_IN_SECOND,
              file.lastModified() / MILLISECONDS_IN_SECOND, 0, true);
        } else {
          long[] fileStats = { 0, 0 };
          long modifiedTime = file.lastModified() / MILLISECONDS_IN_SECOND;

          try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              if (file instanceof FileCustom) {
                Path path = ((FileCustom) file).getRealFile().toPath();
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                fileStats[0] = attrs.creationTime().toMillis() / MILLISECONDS_IN_SECOND;
                fileStats[1] = attrs.lastAccessTime().toMillis() / MILLISECONDS_IN_SECOND;
              }
            }
          } catch (Exception e) {
             FileLogger.logError(e);
          }

          if (fileStats[0] == 0) fileStats[0] = modifiedTime;
          if (fileStats[1] == 0) fileStats[1] = modifiedTime;

          statResult = new StatFileResult(file.length(), modifiedTime, fileStats[0], fileStats[1], false);
        }
        send(statResult);
        return;
      }
    }
    send(new StatFileResult());
  }
}
