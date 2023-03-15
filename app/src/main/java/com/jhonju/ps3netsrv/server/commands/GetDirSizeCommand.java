package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;

public class GetDirSizeCommand extends FileCommand {

    public GetDirSizeCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    @Override
    public void executeTask() throws Exception {
        try {
            send(Utils.longToBytesBE(calculateFileSize(getFile())));
        } catch (IOException ex) {
            System.err.println("Error: command GetDirSizeCommand failed");
            send(Utils.longToBytesBE(ERROR_CODE));
            throw ex;
        }
    }

    private static long calculateFileSize(File file) {
        long fileSize = EMPTY_SIZE;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    fileSize += calculateFileSize(subFile);
                }
            }
        } else {
            fileSize = file.length();
        }
        return fileSize;
    }
}
