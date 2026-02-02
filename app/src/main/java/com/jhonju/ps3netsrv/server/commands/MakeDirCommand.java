package com.jhonju.ps3netsrv.server.commands;

import android.os.Build;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.File;

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            send(((File)getFile()).mkdir() ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
        } else {
            send(currentDirectory.createDirectory(fileName) != null ? SUCCESS_CODE_BYTEARRAY : ERROR_CODE_BYTEARRAY);
        }
    }
}