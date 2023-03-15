package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;

public class CreateFileCommand extends FileCommand {

    public CreateFileCommand(Context ctx, short filePathLength) { super(ctx, filePathLength); }

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
            send(Utils.intToBytesBE(SUCCESS_CODE));
        } catch (IOException ex) {
            send(Utils.intToBytesBE(ERROR_CODE));
            throw ex;
        }
    }
}