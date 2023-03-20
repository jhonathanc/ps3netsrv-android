package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.File;
import java.io.IOException;

public class OpenDirCommand  extends FileCommand {

    public OpenDirCommand(Context ctx, short filePathLength) { super(ctx, filePathLength); }

    @Override
    public void executeTask() throws PS3NetSrvException, IOException {
        File file = getFile();
        if (file.exists()) {
            ctx.setFile(file);
            send(file.isDirectory() ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
        } else {
            ctx.setFile(null);
            send(ERROR_CODE_BYTEARRAY);
        }
    }
}
