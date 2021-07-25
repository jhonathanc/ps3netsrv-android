package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.results.StatFileResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

public class StatFileCommand extends AbstractCommand {
    private short fpLen;
    private byte[] pad = new byte[12];

    public StatFileCommand(Context ctx) {
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
            StatFileResult statResult;
            if (file.isDirectory()) {
                statResult = new StatFileResult(0, 0, 0, 0, true);
            } else {
                javaxt.io.File jxtFile = new javaxt.io.File(filePath);
                long modifiedTime = 0;
                long creationTime = 0;
                long lastAccessTime = 0;


                Date modifiedDate = jxtFile.getLastModifiedTime();
                if (modifiedDate != null)
                    modifiedTime = modifiedDate.getTime();

                Date creationDate = jxtFile.getCreationTime();
                if (creationDate != null)
                    creationTime = creationDate.getTime();

                Date lastAccessDate = jxtFile.getLastAccessTime();
                if (lastAccessDate != null)
                    lastAccessTime = lastAccessDate.getTime();

                statResult = new StatFileResult(file.length(), modifiedTime, creationTime, lastAccessTime, false);
            }
            ctx.getOutputStream().write(Utils.toByteArray(statResult));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new StatFileResult()));
        }
    }
}
