package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

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
            if (currentDirectory == null) {
                send(ERROR_CODE_BYTEARRAY);
                throw new PS3NetSrvException("ERROR: Current directory should not be null");
            }
            //ctx.setWriteOnlyFile(null);

            if (currentDirectory.createFile("application/someType", fileName) == null) {
                throw new IOException("ERROR: create error on " + fileName);
            }
            //ctx.setWriteOnlyFile(file);
            //TODO: FIX the writeOnlyFile on ctx
            send(SUCCESS_CODE_BYTEARRAY);
        } catch (IOException ex) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException(ex.getMessage());
        }
    }
}