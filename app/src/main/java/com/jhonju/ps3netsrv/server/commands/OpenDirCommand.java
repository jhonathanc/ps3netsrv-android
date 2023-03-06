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
            ctx.getOutputStream().write(Utils.intToBytes(file.isDirectory() ? 0 : -1));
        } else {
            ctx.setFile(null);
            ctx.getOutputStream().write(Utils.intToBytes(-1));
        }
    }
}
