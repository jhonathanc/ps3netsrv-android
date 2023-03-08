package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;

public class DeleteFileCommand extends FileCommand {

    public DeleteFileCommand(Context ctx) {
        super(ctx);
    }

    @Override
    public void executeTask() throws Exception {
        send(Utils.intToBytes(getFile().delete() ? 0 : -1));
    }
}
