package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.results.OpenFileResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class OpenFileCommand extends AbstractCommand {
    private short fpLen;
    private static final long CD_MINIMUM_SIZE = 0x200000L;
    private static final long CD_MAXIMUM_SIZE = 0x35000000L;

    public OpenFileCommand(Context ctx) {
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
            long fileLength = file.length();
            RandomAccessFile readOnlyFile = ctx.getReadOnlyFile();
            if (fileLength >= CD_MINIMUM_SIZE && fileLength <= CD_MAXIMUM_SIZE) {
                for (CDSectorSize cdSec : CDSectorSize.values()) {
                    long position = (cdSec.cdSectorSize<<4) + 0x18;
                    byte[] buffer = new byte[20];
                    readOnlyFile.seek(position);
                    readOnlyFile.read(buffer);
                    String strBuffer = new String(buffer);
                    if (strBuffer.contains("PLAYSTATION ") || strBuffer.contains("CD001")) {
                        ctx.setCdSectorSize(cdSec);
                        break;
                    }
                }
            }
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(fileLength, file.lastModified())));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(-1, file.lastModified())));
        }
    }
}
