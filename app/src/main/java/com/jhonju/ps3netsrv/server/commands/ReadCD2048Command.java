package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadCD2048Command extends AbstractCommand {

    private static final short MAX_RESULT_SIZE = 2048;

    private int startSector;
    private int sectorCount;

    public ReadCD2048Command(Context ctx) {
        super(ctx);
        CommandData cmd = ctx.getCommandData();
        this.startSector = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 2, 6)).getInt();
        this.sectorCount = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 6, 10)).getInt();
    }

    @Override
    public void executeTask() throws Exception {
        final int MAX_SECTORS = BUFFER_SIZE / MAX_RESULT_SIZE;
        if (sectorCount > MAX_SECTORS) {
            throw new IllegalArgumentException("Too many sectors read!");
        }
        if (ctx.getFile() == null) {
            throw new IllegalArgumentException("File shouldn't be null");
        }
        send(readSectors(ctx.getReadOnlyFile(), startSector * ctx.getCdSectorSize().cdSectorSize, sectorCount));
    }

    private byte[][] readSectors(RandomAccessFile file, long offset, int count) throws IOException {
        final int SECTOR_SIZE = ctx.getCdSectorSize().cdSectorSize;
        final int BYTES_TO_SKIP = 24;

        byte[][] result = new byte[count][MAX_RESULT_SIZE];
        for (int i = 0; i < count; i++) {
            file.seek(offset + BYTES_TO_SKIP);
            file.read(result[i]);
            offset += SECTOR_SIZE;
        }
        return result;
    }

}
