package com.jhonju.ps3netsrv.server.commands;

import java.io.RandomAccessFile;
import java.util.Arrays;

import com.jhonju.ps3netsrv.server.Context;

public class ReadFileCriticalCommand extends ReadFileCommand {

    public ReadFileCriticalCommand(Context ctx, int numBytes, long offset) {
        super(ctx, numBytes, offset);
    }

    @Override
    public void executeTask() throws Exception {
        byte[] result = new byte[numBytes];
        RandomAccessFile file = ctx.getReadOnlyFile();
        file.seek(offset);
        int bytesRead = file.read(result);
        if (bytesRead < EMPTY_SIZE) {
            throw new Exception("Error reading file.");
        }
        send(Arrays.copyOfRange(result, 0, bytesRead));
    }
}
