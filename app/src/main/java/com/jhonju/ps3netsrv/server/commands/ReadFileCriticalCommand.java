package com.jhonju.ps3netsrv.server.commands;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

public class ReadFileCriticalCommand extends ReadFileCommand {

    public ReadFileCriticalCommand(Context ctx, int numBytes, long offset) {
        super(ctx, numBytes, offset);
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        byte[] result = new byte[numBytes];
        RandomAccessFile file = ctx.getReadOnlyFile();
        try {
            file.seek(offset);
            if (file.read(result) < EMPTY_SIZE) {
                throw new PS3NetSrvException("Error reading file. EOF");
            }
        } catch (IOException e) {
            throw new PS3NetSrvException("Error reading file.");
        }
        send(result);
    }
}
