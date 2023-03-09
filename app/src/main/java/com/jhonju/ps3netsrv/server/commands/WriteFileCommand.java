package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WriteFileCommand extends AbstractCommand {

    private final int numBytes;

    public WriteFileCommand(Context ctx) {
        super(ctx);
        this.numBytes = ByteBuffer.wrap(ctx.getCommandData().getData()).getInt(2);
    }

    @Override
    public void executeTask() throws Exception {
        if (ctx.getReadOnlyFile() == null) {
            System.err.println("ERROR: file is null");
            send(Utils.intToBytes(ERROR_CODE));
            return;
        }

        if (numBytes > BUFFER_SIZE) {
            System.err.printf("ERROR: data to write (%d) is larger than buffer size (%d)/n", numBytes, BUFFER_SIZE);
            send(Utils.intToBytes(ERROR_CODE));
            return;
        }

        byte[] content = Utils.readCommandData(ctx.getInputStream(), numBytes);
        if (content != null) {
            try (FileOutputStream fos = new FileOutputStream(ctx.getWriteOnlyFile())) {
                try {
                    fos.write(content);
                    send(Utils.intToBytes(content.length));
                } catch (IOException ex) {
                    send(Utils.intToBytes(ERROR_CODE));
                    throw ex;
                }
            }
        }
    }
}
