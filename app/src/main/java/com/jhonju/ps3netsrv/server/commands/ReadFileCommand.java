package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ReadFileCommand extends AbstractCommand {
    int numBytes;
    long offset;

    public ReadFileCommand(Context ctx) {
        super(ctx);
        CommandData cmd = ctx.getCommandData();
        this.numBytes = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 2, 6)).getInt();
        this.offset = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 6, cmd.getData().length)).getLong();
    }

    @Override
    public void executeTask() throws Exception {
        byte[] result = new byte[numBytes];
        RandomAccessFile file = ctx.getReadOnlyFile();
        file.seek(offset);
        ctx.getOutputStream().write(Utils.intToBytes(file.read(result)));
    }
}
