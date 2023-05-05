package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;

public class GetDirSizeCommand extends FileCommand {

    public GetDirSizeCommand(Context ctx, short filePathLength)
    {
        super(ctx, filePathLength);
        ERROR_CODE_BYTEARRAY = Utils.longToBytesBE(ERROR_CODE);
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        send(Utils.longToBytesBE(calculateFileSize(getFile())));
    }

    private static long calculateFileSize(IFile file) {
        long fileSize = EMPTY_SIZE;
        if (file.isDirectory()) {
            IFile[] files = file.listFiles();
            for (IFile subFile : files) {
                fileSize += calculateFileSize(subFile);
            }
        } else {
            fileSize = file.length();
        }
        return fileSize;
    }
}
