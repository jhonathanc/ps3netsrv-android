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
            java.util.Set<com.jhonju.ps3netsrv.server.io.IFile> parents = getFile(true);
            boolean success = false;
            
            for (com.jhonju.ps3netsrv.server.io.IFile parent : parents) {
                if (parent != null && parent.exists() && parent.isDirectory()) {
                    if (parent.createFile(fileName)) {
                        success = true;
                    }
                }
            }
            
            if (!success) {
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