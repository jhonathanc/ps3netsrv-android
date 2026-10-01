package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.io.VirtualIsoFile;
import com.jhonju.ps3netsrv.server.utils.FileLogger;
import com.jhonju.ps3netsrv.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Set;

public class OpenFileCommand extends FileCommand {

  private static final int RESULT_LENGTH = 16;
  private static final long CD_MINIMUM_SIZE = 0x200000L;
  private static final long CD_MAXIMUM_SIZE = 0x35000000L;
  private static final String PLAYSTATION_IDENTIFIER = "PLAYSTATION ";
  private static final String CD001_IDENTIFIER = "CD001";

  public OpenFileCommand(Context ctx, short filePathLength) {
    super(ctx, filePathLength);
  }

  private static class OpenFileResult implements IResult {
    private long aFileSize = ERROR_CODE;
    private long bModifiedTime = ERROR_CODE;

    public OpenFileResult() {
    }

    public OpenFileResult(long fileSize, long modifiedTime) {
      this.aFileSize = fileSize;
      this.bModifiedTime = modifiedTime;
    }

    public byte[] toByteArray() throws IOException {
      return ByteBuffer.allocate(RESULT_LENGTH)
          .putLong(aFileSize).putLong(bModifiedTime).array();
    }
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    Set<IFile> files = getFile();
    if (files == null || files.isEmpty()) {
      ctx.setFile(null);
      send(new OpenFileResult());
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_open_file_not_exists));
    }

    // Use the first file in the set, or iterate if needed.
    // For OpenFile, we typically expect one valid file.
    IFile file = files.iterator().next();

    String upperPath = requestedPath == null ? "" : requestedPath.toUpperCase(Locale.US);
    boolean isGamesFolder = upperPath.startsWith("/GAMES/") || upperPath.startsWith("GAMES/");

    if (isGamesFolder && file.isDirectory()) {
      try {
        file = new VirtualIsoFile(file, ctx.getAndroidContext());
      } finally {
        Context.closeFiles(files);
      }

      Set<IFile> newFiles = new java.util.HashSet<>();
      newFiles.add(file);
      files = newFiles;
    }

    ctx.setFile(files);

    try {
      determineCdSectorSize(file);
    } catch (IOException e) {
      FileLogger.logError(e);
      ctx.setFile(null);
      send(new OpenFileResult());
      throw new PS3NetSrvException(ctx.getAndroidContext().getString(R.string.error_cd_sector_size));
    }

    send(new OpenFileResult(file.length(), file.lastModified() / MILLISECONDS_IN_SECOND));
  }

  private void determineCdSectorSize(IFile file) throws IOException {
    if (file.length() < CD_MINIMUM_SIZE || file.length() > CD_MAXIMUM_SIZE) {
      ctx.setCdSectorSize(null);
      return;
    }
    for (CDSectorSize cdSec : CDSectorSize.values()) {
      byte[] buffer = new byte[20];
      file.read(buffer, (cdSec.cdSectorSize << 4) + BYTES_TO_SKIP);
      String strBuffer = new String(buffer, StandardCharsets.US_ASCII);
      if (strBuffer.contains(PLAYSTATION_IDENTIFIER) || strBuffer.contains(CD001_IDENTIFIER)) {
        ctx.setCdSectorSize(cdSec);
        break;
      }
    }
  }
}
