package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WriteFileCommand extends AbstractCommand {

    private final int numBytes;

    public WriteFileCommand(Context ctx, int numBytes) {
        super(ctx);
        this.numBytes = numBytes;
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        if (ctx.isReadOnly()) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("Failed to write file: server is executing as read only");
        }

        if (ctx.getReadOnlyFile() == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: file is null");
        }

        if (numBytes > BUFFER_SIZE) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException(String.format("ERROR: data to write (%d) is larger than buffer size (%d)", numBytes, BUFFER_SIZE));
        }

        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), numBytes);
        if (buffer == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: on write file - content is null");
        }
        try (FileOutputStream fos = new FileOutputStream(ctx.getWriteOnlyFile())) {
            byte[] content;
            try {
                content = buffer.array();
                fos.write(content);
            } catch (IOException ex) {
                send(ERROR_CODE_BYTEARRAY);
                throw new PS3NetSrvException("ERROR: writing file " + ex.getMessage());
            }
            send(Utils.intToBytesBE(content.length));
        }
    }
}
