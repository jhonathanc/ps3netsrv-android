package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

public class MakeDirCommand extends FileCommand {

    public MakeDirCommand(Context ctx) {
        super(ctx);
    }

    @Override
    public void executeTask() throws Exception {
        send(Utils.intToBytes(getFile().mkdir() ? SUCCESS_CODE : ERROR_CODE));
    }
}
