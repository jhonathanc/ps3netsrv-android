package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.R;

import java.io.IOException;
import java.io.OutputStream;

public class ReadCD2048Command extends AbstractCommand {
  private static final int MAX_SECTORS = BinaryUtils.BUFFER_SIZE / BinaryUtils.SECTOR_SIZE;

  private final int startSector;
  private final int sectorCount;

  public ReadCD2048Command(Context ctx, int startSector, int sectorCount) {
    super(ctx);
    this.startSector = startSector;
    this.sectorCount = sectorCount;
  }

  @Override
  public void executeTask() throws IOException {
    if (sectorCount > MAX_SECTORS) {
      throw new IllegalArgumentException(ctx.getAndroidContext().getString(R.string.error_too_many_sectors));
      // TODO: VERIFICAR O QUE PODE SER DEVOLVIDO COMO RESPOSTA
    }
    java.util.Set<IFile> files = ctx.getFile();
    if (files == null || files.isEmpty()) {
      throw new IllegalArgumentException(ctx.getAndroidContext().getString(R.string.error_file_null));
    }
    IFile file = files.iterator().next();
    readAndSendSectors(file, (long) startSector * ctx.getCdSectorSize().cdSectorSize, sectorCount);
  }

  private void readAndSendSectors(IFile file, long offset, int count) throws IOException {
    final int SECTOR_SIZE = BinaryUtils.SECTOR_SIZE;
    OutputStream os = ctx.getOutputStream();
    byte[] buffer = ctx.getOutputBuffer();
    for (int i = 0; i < count; i++) {
      int bytesLength = file.read(buffer, 0, SECTOR_SIZE, offset + BYTES_TO_SKIP);
      if (bytesLength == -1) {
        break;
      }
      os.write(buffer, 0, bytesLength);
      offset += ctx.getCdSectorSize().cdSectorSize;
    }
    os.flush();
  }
}
