package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.RandomAccessFile;

public class ReadFileCommand extends AbstractCommand {
    protected int numBytes;
    protected long offset;

    public ReadFileCommand(Context ctx, int numBytes, long offset) {
        super(ctx);
        this.numBytes = numBytes;
        this.offset = offset;
    }

    @Override
    public void executeTask() throws Exception {
        byte[] readFileResult = new byte[numBytes];
        RandomAccessFile file = ctx.getReadOnlyFile();
        file.seek(offset);
        int bytesRead = file.read(readFileResult);
        if (bytesRead < EMPTY_SIZE) {
            throw new Exception("Error on read file");
        }

        byte[][] result = { Utils.toByteArray(Utils.intToBytes(bytesRead)), readFileResult };
        send(result);
    }
}
