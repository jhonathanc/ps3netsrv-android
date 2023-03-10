package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class FileCommand extends AbstractCommand {

    protected short filePathLength;

    public FileCommand(Context ctx, short filePathLength) {
        super(ctx);
        this.filePathLength = filePathLength;
    }

    protected File getFile() throws IOException {
        byte[] bfilePath = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (bfilePath == null) {
            throw new IOException("ERROR: command failed receiving filename.");
        }
        return new File(ctx.getRootDirectory(), new String(bfilePath, StandardCharsets.UTF_8).replaceAll("\\x00+$", ""));
    }
}
