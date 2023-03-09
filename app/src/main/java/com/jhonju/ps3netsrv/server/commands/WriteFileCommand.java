package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.CommandData;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class WriteFileCommand extends AbstractCommand {

    private int numBytes;

    public WriteFileCommand(Context ctx) {
        super(ctx);
        this.numBytes = ByteBuffer.wrap(ctx.getCommandData().getData()).getInt(2);
    }

    @Override
    public void executeTask() throws Exception {
        if (ctx.getReadOnlyFile() == null) {
            send(Utils.intToBytes(ERROR_CODE));
            return;
        }

        if (numBytes > BUFFER_SIZE) {
            System.err.println(String.format("ERROR: data to write (%i) is larger than buffer size (%i)", numBytes, BUFFER_SIZE));
            send(Utils.intToBytes(ERROR_CODE));
            return;
        }

        byte[] content = Utils.readCommandData(ctx.getInputStream(), numBytes);
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
