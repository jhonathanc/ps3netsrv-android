package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.results.OpenFileResult;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OpenFileCommand extends FileCommand {

    private static final long CD_MINIMUM_SIZE = 0x200000L;
    private static final long CD_MAXIMUM_SIZE = 0x35000000L;

    public OpenFileCommand(Context ctx) {
        super(ctx);
    }

    @Override
    public void executeTask() throws Exception {
        try {
            File file = getFile();
            if (!file.exists()) {
                ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(-1, file.lastModified())));
                return;
            }
            ctx.setFile(file);
            determineCdSectorSize(file);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(file.length(), file.lastModified())));
        } catch (IOException e) {
            ctx.getOutputStream().write(Utils.toByteArray(new OpenFileResult(-1, -1)));
            throw e;
        }
    }

    private void determineCdSectorSize(File file) throws IOException {
        if (file.length() < CD_MINIMUM_SIZE || file.length() > CD_MAXIMUM_SIZE) {
            return;
        }

        for (CDSectorSize cdSec : CDSectorSize.values()) {
            long position = (cdSec.cdSectorSize << 4) + 0x18;
            byte[] buffer = new byte[20];
            ctx.getReadOnlyFile().seek(position);
            ctx.getReadOnlyFile().read(buffer);
            String strBuffer = new String(buffer, StandardCharsets.US_ASCII);
            if (strBuffer.contains("PLAYSTATION ") || strBuffer.contains("CD001")) {
                ctx.setCdSectorSize(cdSec);
                break;
            }
        }
    }
}
