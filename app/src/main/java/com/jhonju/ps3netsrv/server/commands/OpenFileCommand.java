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
    private byte[] pad = new byte[12];
    private long CD_MINIMUM_SIZE = 0x200000L;
    private long CD_MAXIMUM_SIZE = 0x35000000L;

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
            RandomAccessFile readOnlyFile = ctx.getReadOnlyFile();
            if (file.length() >= CD_MINIMUM_SIZE && file.length() <= CD_MAXIMUM_SIZE) {
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
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(file.length(), file.lastModified())));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(-1, file.lastModified())));
        }
    }
}
