package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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

    private static long calculateFileSize(Set<IFile> files) throws IOException {
        long fileSize = EMPTY_SIZE;
        for (IFile file : files) {
            if (file.isDirectory()) {
                IFile[] filesAux = file.listFiles();
                for (IFile subFile : filesAux) {
                    Set<IFile> subFileAux = new HashSet<>();
                    subFileAux.add(subFile);
                    fileSize += calculateFileSize(subFileAux);
                }
            } else {
                fileSize = file.length();
            }
        }
        return fileSize;
    }
}
