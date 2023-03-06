package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class FileCommand extends AbstractCommand {

    protected short fpLen;

    public FileCommand(Context ctx) {
        super(ctx);
        CommandData cmd = ctx.getCommandData();
        this.fpLen = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 0, 2)).getShort();
    }

    protected File getFile() throws IOException {
        byte[] bfilePath = Utils.readCommandData(ctx.getInputStream(), this.fpLen);
        if (bfilePath == null) {
            throw new IOException("ERROR: command failed receiving filename.");
        }
        return new File(ctx.getRootDirectory(), new String(bfilePath, StandardCharsets.UTF_8).replaceAll("\\x00+$", ""));
    }
}
