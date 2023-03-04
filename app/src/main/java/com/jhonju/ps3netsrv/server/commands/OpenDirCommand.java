package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.results.OpenDirResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class OpenDirCommand  extends AbstractCommand {
    private short dpLen;

    public OpenDirCommand(Context ctx) {
        super(ctx);
        CommandData cmd = ctx.getCommandData();
        this.dpLen = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 0, 2)).getShort();
    }

    @Override
    public void executeTask() throws Exception {
        byte[] bFolderPath = Utils.readCommandData(ctx.getInputStream(), this.dpLen);
        File file = new File(ctx.getRootDirectory(), new String(bFolderPath, StandardCharsets.UTF_8).replaceAll("\\x00+$", ""));
        if (file.exists()) {
            ctx.setFile(file);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenDirResult(file.isDirectory() ? 0 : -1)));
        } else {
            ctx.setFile(null);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenDirResult(-1)));
        }
    }
}
