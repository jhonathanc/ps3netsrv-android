package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class OpenFileCommand extends FileCommand {

    private static final long CD_MINIMUM_SIZE = 0x200000L;
    private static final long CD_MAXIMUM_SIZE = 0x35000000L;

    public OpenFileCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    private static class OpenFileResult {
        public long aFileSize = ERROR_CODE;
        public long bModifiedTime = ERROR_CODE;

        public OpenFileResult() { }

        public OpenFileResult(long fileSize, long modifiedTime) {
            this.aFileSize = fileSize;
            this.bModifiedTime = modifiedTime;
        }
    }

    @Override
    public void executeTask() throws Exception {
        try {
            File file = getFile();
            if (!file.exists()) {
                ctx.setFile(null);
                System.err.println("Error: on OpenFileCommand - file not exists");
                send(Utils.toByteArray(new OpenFileResult()));
                return;
            }
            ctx.setFile(file);
            determineCdSectorSize(ctx.getReadOnlyFile());
            send(Utils.toByteArray(new OpenFileResult(file.length(), file.lastModified() / MILLISECONDS_IN_SECOND)));
        } catch (IOException e) {
            System.err.println("Error: on OpenFileCommand" + e.getMessage());
            send(Utils.toByteArray(new OpenFileResult()));
            throw e;
        }
    }

    private void determineCdSectorSize(RandomAccessFile file) throws IOException {
        if (file.length() < CD_MINIMUM_SIZE || file.length() > CD_MAXIMUM_SIZE) {
            ctx.setCdSectorSize(null);
            return;
        }
        for (CDSectorSize cdSec : CDSectorSize.values()) {
            long position = (cdSec.cdSectorSize << 4) + BYTES_TO_SKIP;
            byte[] buffer = new byte[20];
            file.seek(position);
            file.read(buffer);
            String strBuffer = new String(buffer, StandardCharsets.US_ASCII);
            if (strBuffer.contains("PLAYSTATION ") || strBuffer.contains("CD001")) {
                ctx.setCdSectorSize(cdSec);
                break;
            }
        }
    }
}
