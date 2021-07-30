package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.results.OpenDirResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.nio.ByteBuffer;
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
        byte[] bFolderPath = new byte[this.dpLen];
        if (!Utils.readCommandData(ctx.getInputStream(), bFolderPath))
            return;

        String folderPath = ctx.getRootDirectory() + new String(bFolderPath).replaceAll("\0", "");
        File file = new File(folderPath);
        if (file.exists()) {
            ctx.setFile(file);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenDirResult(file.isDirectory() ? 0 : -1)));
        } else {
            ctx.setFile(null);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenDirResult(-1)));
        }
    }
}
