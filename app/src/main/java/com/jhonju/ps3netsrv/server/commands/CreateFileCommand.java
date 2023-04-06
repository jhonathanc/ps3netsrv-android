package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.File;
import java.io.IOException;

public class CreateFileCommand extends FileCommand {

    public CreateFileCommand(Context ctx, short filePathLength) { super(ctx, filePathLength); }

    @Override
    public void executeTask() throws PS3NetSrvException, IOException {
        if (ctx.isReadOnly()) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("Failed to create file: server is executing as read only");
        }

        try {
            File file = getFile();
            ctx.setWriteOnlyFile(null);
            if (file.isDirectory()) {
                throw new IOException("ERROR: file is a directory: " + file.getCanonicalPath());
            }

            if (!file.createNewFile()) {
                throw new IOException("ERROR: create error on " + file.getCanonicalPath());
            }
            ctx.setWriteOnlyFile(file);
            send(SUCCESS_CODE_BYTEARRAY);
        } catch (IOException ex) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException(ex.getMessage());
        }
    }
}