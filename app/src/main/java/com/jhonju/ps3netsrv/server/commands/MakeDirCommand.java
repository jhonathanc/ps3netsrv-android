package com.jhonju.ps3netsrv.server.commands;

import android.os.Build;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import java.io.IOException;

public class MakeDirCommand extends FileCommand {

    public MakeDirCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    @Override
    public void executeTask() throws PS3NetSrvException, IOException {
        if (ctx.isReadOnly()) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("Failed to make dir: server is executing as read only");
        }

        // Get parents
        java.util.Set<com.jhonju.ps3netsrv.server.io.IFile> parents = getFile(true);
        boolean success = false;
        
        for (com.jhonju.ps3netsrv.server.io.IFile parent : parents) {
            if (parent != null && parent.exists() && parent.isDirectory()) {
                 if (parent.createDirectory(fileName)) {
                     success = true;
                     // Assuming we want to create in all matching roots? or just first?
                     // If we succeed in one, we count it as success? behavior consistency?
                     // Usually merging implies mirroring. If we create, we assume success.
                 }
            }
        }
        
        send(success ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
    }
}