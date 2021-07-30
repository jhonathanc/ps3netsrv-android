package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ReadCD2048Command extends AbstractCommand {
    private static final int BUFFER_SIZE = 3145728;
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
        if (sectorCount * MAX_RESULT_SIZE > BUFFER_SIZE) throw new Exception("This situation wasn't expected, too many sectors read!");
        if (ctx.getFile() == null) throw new Exception("File shouldn't be null");

        long offset = this.startSector * ctx.getCdSectorSize().cdSectorSize;

        RandomAccessFile file = ctx.getReadOnlyFile();
        for (int i = 0; i < sectorCount; i++) {
            byte[] result = new byte[MAX_RESULT_SIZE];
            file.seek(offset + 24);
            file.read(result);
            ctx.getOutputStream().write(result);
            offset += ctx.getCdSectorSize().cdSectorSize; // skip subchannel data
        }
    }

}
