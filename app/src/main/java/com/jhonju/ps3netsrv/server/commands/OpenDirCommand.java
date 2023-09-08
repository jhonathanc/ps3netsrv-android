package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;

import java.io.IOException;
import java.util.Set;

public class OpenDirCommand  extends FileCommand {

    public OpenDirCommand(Context ctx, short filePathLength) { super(ctx, filePathLength); }

    @Override
    public void executeTask() throws PS3NetSrvException, IOException {
        Set<IFile> files = getFile();
        boolean isDirectory = false;
        for (IFile file : files) {
            if (!file.exists()) {
                ctx.setFile(null);
                send(ERROR_CODE_BYTEARRAY);
                return;
            }
            isDirectory = isDirectory || file.isDirectory();
        }
        ctx.setFile(files);
        send(isDirectory ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
    }
}
