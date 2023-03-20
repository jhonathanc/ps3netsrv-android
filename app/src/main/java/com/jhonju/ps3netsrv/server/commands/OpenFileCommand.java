package com.jhonju.ps3netsrv.server.commands;

import static com.jhonju.ps3netsrv.server.utils.Utils.longToBytesBE;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class OpenFileCommand extends FileCommand {

    private static final int RESULT_LENGTH = 16;
    private static final long CD_MINIMUM_SIZE = 0x200000L;
    private static final long CD_MAXIMUM_SIZE = 0x35000000L;
    private static final String PLAYSTATION_IDENTIFIER = "PLAYSTATION ";
    private static final String CD001_IDENTIFIER = "CD001";

    public OpenFileCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    private static class OpenFileResult implements IResult {
        private long aFileSize = ERROR_CODE;
        private long bModifiedTime = ERROR_CODE;

        public OpenFileResult() { }

        public OpenFileResult(long fileSize, long modifiedTime) {
            this.aFileSize = fileSize;
            this.bModifiedTime = modifiedTime;
        }

        public byte[] toByteArray() throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH)) {
                out.write(longToBytesBE(this.aFileSize));
                out.write(longToBytesBE(this.bModifiedTime));
                return out.toByteArray();
            }
        }
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        File file = getFile();
        if (!file.exists()) {
            ctx.setFile(null);
            send(new OpenFileResult());
            throw new PS3NetSrvException("Error: on OpenFileCommand - file not exists");
        }
        ctx.setFile(file);

        try {
            determineCdSectorSize(ctx.getReadOnlyFile());
        } catch (IOException e) {
            ctx.setFile(null);
            send(new OpenFileResult());
            throw new PS3NetSrvException("Error: not possible to determine CD Sector size");
        }
        send(new OpenFileResult(file.length(), file.lastModified() / MILLISECONDS_IN_SECOND));
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
            if (strBuffer.contains(PLAYSTATION_IDENTIFIER) || strBuffer.contains(CD001_IDENTIFIER)) {
                ctx.setCdSectorSize(cdSec);
                break;
            }
        }
    }
}
