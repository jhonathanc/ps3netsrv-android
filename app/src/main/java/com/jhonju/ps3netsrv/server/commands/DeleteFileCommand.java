package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

public class DeleteFileCommand extends FileCommand {

    public DeleteFileCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    @Override
    public void executeTask() throws Exception {
        send(Utils.intToBytesBE(getFile().delete() ? SUCCESS_CODE : ERROR_CODE));
    }
}
