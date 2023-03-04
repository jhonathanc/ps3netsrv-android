package com.jhonju.ps3netsrv.server.commands;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.results.StatFileResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

public class StatFileCommand extends AbstractCommand {
    private short fpLen;

    public StatFileCommand(Context ctx) {
        super(ctx);
        CommandData cmd = ctx.getCommandData();
        this.fpLen = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 0, 2)).getShort();
    }

    @Override
    public void executeTask() throws Exception {
        ctx.setFile(null);
        byte[] bfilePath = Utils.readCommandData(ctx.getInputStream(), this.fpLen);

        File file = new File(ctx.getRootDirectory(), new String(bfilePath, StandardCharsets.UTF_8).replaceAll("\\x00+$", ""));
        if (file.exists()) {
            ctx.setFile(file);
            StatFileResult statResult;
            if (file.isDirectory()) {
                statResult = new StatFileResult(0, file.lastModified() / 1000, file.lastModified() / 1000, 0, true);
            } else {
                long[] fileStats = Utils.getFileStats(file);
                statResult = new StatFileResult(file.length(), file.lastModified() / 1000, fileStats[0] / 1000, fileStats[1] / 1000, false);
            }
            ctx.getOutputStream().write(Utils.toByteArray(statResult));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new StatFileResult()));
        }
    }
}
