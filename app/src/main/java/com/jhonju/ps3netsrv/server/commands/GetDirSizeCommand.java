package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;

public class GetDirSizeCommand extends FileCommand {

    public GetDirSizeCommand(Context ctx) {
        super(ctx);
    }

    @Override
    public void executeTask() throws Exception {
        try {
            send(Utils.longToBytes(calculateFileSize(getFile())));
        } catch (IOException ex) {
            System.err.println("Error: command GetDirSizeCommand failed");
            send(Utils.longToBytes(-1));
            throw ex;
        }
    }

    private static long calculateFileSize(File file) {
        long fileSize = 0;
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
