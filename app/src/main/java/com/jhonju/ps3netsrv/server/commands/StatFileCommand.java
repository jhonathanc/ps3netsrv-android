package com.jhonju.ps3netsrv.server.commands;

import java.io.File;
import java.nio.ByteBuffer;
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

        byte[] bfilePath = new byte[this.fpLen];
        if (!Utils.readCommandData(ctx.getInputStream(), bfilePath))
            return;

        String filePath = ctx.getRootDirectory() + new String(bfilePath).replaceAll("\0", "");
        File file = new File(filePath);
        if (file.exists()) {
            ctx.setFile(file);
            StatFileResult statResult;
            if (file.isDirectory()) {
                statResult = new StatFileResult(0, 0, 0, 0, true);
            } else {
//                javaxt.io.File jxtFile = new javaxt.io.File(file);
                long modifiedTime = file.lastModified();
                long creationTime = 0;
                long lastAccessTime = 0;
//
//                Date creationDate = jxtFile.getCreationTime();
//                if (creationDate != null)
//                    creationTime = creationDate.getTime();
//
//                Date lastAccessDate = jxtFile.getLastAccessTime();
//                if (lastAccessDate != null)
//                    lastAccessTime = lastAccessDate.getTime();

                statResult = new StatFileResult(file.length(), modifiedTime, creationTime, lastAccessTime, false);
            }
            ctx.getOutputStream().write(Utils.toByteArray(statResult));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new StatFileResult()));
        }
    }
}
