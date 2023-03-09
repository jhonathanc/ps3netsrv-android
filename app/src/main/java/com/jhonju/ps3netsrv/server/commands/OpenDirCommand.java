package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;

public class OpenDirCommand  extends FileCommand {

    public OpenDirCommand(Context ctx) { super(ctx); }

    @Override
    public void executeTask() throws Exception {
        File file = getFile();
        if (file.exists()) {
            ctx.setFile(file);
            send(Utils.intToBytes(file.isDirectory() ? SUCCESS_CODE : ERROR_CODE));
        } else {
            ctx.setFile(null);
            send(Utils.intToBytes(ERROR_CODE));
        }
    }
}
