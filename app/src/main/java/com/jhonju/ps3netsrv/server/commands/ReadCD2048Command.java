package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IRandomAccessFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReadCD2048Command extends AbstractCommand {

    private static final short MAX_RESULT_SIZE = 2048;
    private static final int MAX_SECTORS = BUFFER_SIZE / MAX_RESULT_SIZE;

    private final int startSector;
    private final int sectorCount;

    public ReadCD2048Command(Context ctx, int startSector, int sectorCount) {
        super(ctx);
        this.startSector = startSector;
        this.sectorCount = sectorCount;
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        if (sectorCount > MAX_SECTORS) {
            throw new IllegalArgumentException("Too many sectors read!");
            //TODO: VERIFICAR O QUE PODE SER DEVOLVIDO COMO RESPOSTA
        }
        IRandomAccessFile file = ctx.getReadOnlyFile();
        if (file == null) {
            throw new IllegalArgumentException("File shouldn't be null");
            //TODO: VERIFICAR O QUE PODE SER DEVOLVIDO COMO RESPOSTA
        }
        send(readSectors(file, (long) startSector * ctx.getCdSectorSize().cdSectorSize, sectorCount));
    }

    private byte[] readSectors(IRandomAccessFile file, long offset, int count) throws IOException {
        final int SECTOR_SIZE = ctx.getCdSectorSize().cdSectorSize;

        ByteArrayOutputStream out = new ByteArrayOutputStream(count * MAX_RESULT_SIZE);
        try {
            for (int i = 0; i < count; i++) {
                byte[] sectorRead = new byte[MAX_RESULT_SIZE];
                int bytesLength = file.read(sectorRead, offset + BYTES_TO_SKIP);
                out.write(sectorRead, 0, bytesLength);
                offset += SECTOR_SIZE;
            }
            return out.toByteArray();
        } finally {
            out.close();
        }
    }
}
