package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class GetDirSizeCommand extends FileCommand {

    public GetDirSizeCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    @Override
    protected File getFile() throws IOException, PS3NetSrvException {
        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (buffer == null) {
            send(Utils.longToBytesBE(ERROR_CODE));
            throw new PS3NetSrvException("ERROR: command failed receiving filename.");
        }
        return new File(ctx.getRootDirectory(), new String(buffer.array(), StandardCharsets.UTF_8).replaceAll("\\x00+$", ""));
    }

    @Override
    protected void send(byte[] result) throws IOException, PS3NetSrvException {
        OutputStream os = ctx.getOutputStream();
        try {
            if (result.length == EMPTY_SIZE) {
                os.write(Utils.longToBytesBE(ERROR_CODE));
                throw new PS3NetSrvException("Empty byte array to send to response");
            }
            os.write(result);
        } finally {
            os.flush();
        }
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        send(Utils.longToBytesBE(calculateFileSize(getFile())));
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
