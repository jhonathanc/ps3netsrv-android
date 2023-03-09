package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;

public class CreateFileCommand extends FileCommand {

    public CreateFileCommand(Context ctx) { super(ctx); }

    @Override
    public void executeTask() throws Exception {
        try {
            File file = getFile();
            ctx.setWriteOnlyFile(null);
            if (file.isDirectory()) {
                throw new IOException("ERROR: file is a directory: " + file.getCanonicalPath());
            }

            if (!file.createNewFile()) {
                throw new IOException("ERROR: create error on " + file.getCanonicalPath());
            }
            ctx.setWriteOnlyFile(file);
            send(Utils.intToBytes(SUCCESS_CODE));
        } catch (IOException ex) {
            send(Utils.intToBytes(ERROR_CODE));
            throw ex;
        }
    }
}