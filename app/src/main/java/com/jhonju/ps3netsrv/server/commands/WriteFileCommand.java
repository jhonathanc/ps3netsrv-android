package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFileCommand extends AbstractCommand {

    private final int numBytes;

    public WriteFileCommand(Context ctx, int numBytes) {
        super(ctx);
        this.numBytes = numBytes;
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
        if (content == null) {
            System.err.println("ERROR: on write file - content is null/n");
            send(Utils.intToBytes(ERROR_CODE));
            return;
        }
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
