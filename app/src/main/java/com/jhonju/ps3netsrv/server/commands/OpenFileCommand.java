package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.results.OpenFileResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class OpenFileCommand extends AbstractCommand {
    private short fpLen;
    private byte[] pad = new byte[12];

    public OpenFileCommand(Context ctx) {
        super(ctx);
        CommandData cmd = ctx.getCommandData();
        this.fpLen = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 0, 2)).getShort();
        for (byte i = 2; i < cmd.getData().length; i++)
            pad[i-2] = cmd.getData()[i];
    }

    @Override
    public void executeTask() throws Exception {
        ctx.setFile(null);

        byte[] bfilePath = new byte[16 + this.fpLen];
        ctx.getInputStream().read(bfilePath, 16, fpLen);
        String filePath = ctx.getRootDirectory() + new String(bfilePath).replaceAll("\0", "");
        File file = new File(filePath);
        if (file.exists()) {
            ctx.setFile(file);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(file.length(), file.lastModified())));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(-1, file.lastModified())));
        }
    }
}
