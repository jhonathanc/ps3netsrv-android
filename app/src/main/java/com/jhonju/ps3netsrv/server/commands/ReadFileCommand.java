package com.jhonju.ps3netsrv.server.commands;

import static com.jhonju.ps3netsrv.server.utils.Utils.INT_CAPACITY;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReadFileCommand extends AbstractCommand {
    protected int numBytes;
    protected long offset;

    public ReadFileCommand(Context ctx, int numBytes, long offset) {
        super(ctx);
        this.numBytes = numBytes;
        this.offset = offset;
    }

    private static class ReadFileResult implements IResult {
        private final int bytesReadLength;
        private final byte[] bytesRead;

        public ReadFileResult(int bytesReadLength, byte[] bytesRead) {
            this.bytesReadLength = bytesReadLength;
            this.bytesRead = bytesRead;
        }

        @Override
        public byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INT_CAPACITY + bytesReadLength);
            try {
                out.write(Utils.intToBytesBE(bytesReadLength));
                out.write(bytesRead);
                return out.toByteArray();
            } finally {
                out.close();
            }
        }
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        byte[] readFileResult = new byte[numBytes];
        try {
            int bytesRead = 0;
            java.util.Set<com.jhonju.ps3netsrv.server.io.IFile> files = ctx.getFile();
            if (files != null && !files.isEmpty()) {
                 com.jhonju.ps3netsrv.server.io.IFile file = files.iterator().next();
                 bytesRead = file.read(readFileResult, offset);
            } else {
                 throw new IOException("No file open");
            }
            if (bytesRead < EMPTY_SIZE) {
                throw new PS3NetSrvException("Error reading file: EOF.");
            }
            send(new ReadFileResult(bytesRead, readFileResult));
        } catch (IOException e) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("Error reading file.");
        }
    }
}
