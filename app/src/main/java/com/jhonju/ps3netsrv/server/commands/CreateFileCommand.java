package com.jhonju.ps3netsrv.server.commands;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.File;
import java.io.IOException;

public class CreateFileCommand extends FileCommand {

    public CreateFileCommand(Context ctx, short filePathLength) { super(ctx, filePathLength); }

    @Override
    public void executeTask() throws PS3NetSrvException, IOException {
        if (ctx.isReadOnly()) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("Failed to create file: server is executing as read only");
        }

        try {
            DocumentFile file = getDocumentFile();
            ctx.setWriteOnlyFile(null);
            if (file.isDirectory()) {
                throw new IOException("ERROR: file is a directory: " + file.getName());
            }

            if (file.getParentFile().createFile(file.getType(), file.getName()) == null) {
                throw new IOException("ERROR: create error on " + file.getName());
            }
            //ctx.setWriteOnlyFile(file);
            //TODO: FIX the writeOnlyFile on ctx
            send(SUCCESS_CODE_BYTEARRAY);
        } catch (IOException ex) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException(ex.getMessage());
        }
    }
}