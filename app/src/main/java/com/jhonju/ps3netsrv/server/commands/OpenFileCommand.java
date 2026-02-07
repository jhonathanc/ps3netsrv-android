package com.jhonju.ps3netsrv.server.commands;

import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.longToBytesBE;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.io.IFile;

import com.jhonju.ps3netsrv.server.io.VirtualIsoFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
      ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH);
      try {
        out.write(longToBytesBE(this.aFileSize));
        out.write(longToBytesBE(this.bModifiedTime));
        return out.toByteArray();
      } finally {
        out.close();
      }
    }
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    Set<IFile> files = getFile();
    if (files == null || files.isEmpty()) {
      ctx.setFile(null);
      send(new OpenFileResult());
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_open_file_not_exists));
    }

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "OpenFileCommand file resolution complete. Files found: " + (files != null ? files.size() : 0));

    // Use the first file in the set, or iterate if needed.
    // For OpenFile, we typically expect one valid file.
    IFile file = files.iterator().next();

    boolean isGamesFolder = requestedPath != null &&
        (requestedPath.toUpperCase().startsWith("/GAMES/") || requestedPath.toUpperCase().startsWith("GAMES/"));

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "Checking isGamesFolder: " + isGamesFolder + ", isDirectory: " + file.isDirectory());

    if (isGamesFolder && file.isDirectory()) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "Creating VirtualIsoFile...");
      file = new VirtualIsoFile(file);
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile created successfully.");

      Set<IFile> newFiles = new java.util.HashSet<>();
      newFiles.add(file);
      files = newFiles;
    }

    ctx.setFile(files);

    try {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "Determining CD sector size...");
      determineCdSectorSize(file);
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "CD sector size determined.");
    } catch (IOException e) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logError(e);
      ctx.setFile(null);
      send(new OpenFileResult());
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_cd_sector_size));
    }

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "Sending OpenFileResult. Size: " + file.length());
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
