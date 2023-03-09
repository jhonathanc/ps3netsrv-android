package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ReadFileCommand extends AbstractCommand {
    protected int numBytes;
    protected long offset;

    public ReadFileCommand(Context ctx) {
        super(ctx);
        ByteBuffer buffer = ByteBuffer.wrap(ctx.getCommandData().getData());
        this.numBytes = buffer.getInt(2);
        this.offset = buffer.getLong(6);
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
