package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ReadDirEntryCommand extends AbstractCommand {

    private static final int RESULT_LENGTH = 266;
    private static final short MAX_FILE_NAME_LENGTH = 255;
    private static final short EMPTY_FILE_NAME_LENGTH = 0;

    public ReadDirEntryCommand(Context ctx) {
        super(ctx);
    }

    private static class ReadDirEntryResult implements IResult {
        public final long aFileSize;
        public final short bFileNameLength;
        public final boolean cIsDirectory;
        public final String dFileName;

        public ReadDirEntryResult() {
            this.aFileSize = EMPTY_SIZE;
            this.bFileNameLength = EMPTY_FILE_NAME_LENGTH;
            this.cIsDirectory = false;
            this.dFileName = null;
        }

        public ReadDirEntryResult(long aFileSize, short bFileNameLength, boolean cIsDirectory, String dFileName) {
            this.aFileSize = aFileSize;
            this.bFileNameLength = bFileNameLength;
            this.cIsDirectory = cIsDirectory;
            this.dFileName = dFileName;
        }

        public byte[] toByteArray() throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH)) {
                out.write(Utils.longToBytesBE(this.aFileSize));
                out.write(Utils.shortToBytesBE(this.bFileNameLength));
                out.write(cIsDirectory ? 1 : 0);
                if (dFileName != null) {
                    out.write(dFileName.getBytes(StandardCharsets.UTF_8));
                }
                return out.toByteArray();
            }
        }
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        File file = ctx.getFile();
        if (file == null || !file.isDirectory()) {
            send(new ReadDirEntryResult());
            return;
        }
        File fileAux = null;
        String[] fileList = file.list();
        if (fileList != null) {
            for (String fileName : fileList) {
                fileAux = new File(file.getCanonicalPath() + "/" + fileName);
                if (fileAux.getName().length() <= MAX_FILE_NAME_LENGTH) {
                    break;
                }
            }
        }
        if (fileAux == null) {
            ctx.setFile(null);
            send(new ReadDirEntryResult());
            return;
        }
        send(new ReadDirEntryResult(
                fileAux.isDirectory() ? EMPTY_SIZE : file.length()
                , (short) fileAux.getName().length()
                , fileAux.isDirectory()
                , fileAux.getName())
        );
    }
}
