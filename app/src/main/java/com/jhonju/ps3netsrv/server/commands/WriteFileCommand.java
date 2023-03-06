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
        CommandData cmd = ctx.getCommandData();
        this.numBytes = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 2, 6)).getInt();
    }

    @Override
    public void executeTask() throws Exception {
        if (ctx.getReadOnlyFile() == null) {
            ctx.getOutputStream().write(Utils.intToBytes(-1));
            return;
        }

        if (numBytes > BUFFER_SIZE) {
            System.err.println(String.format("ERROR: data to write (%i) is larger than buffer size (%i)", numBytes, BUFFER_SIZE));
            ctx.getOutputStream().write(Utils.intToBytes(-1));
            return;
        }

        byte[] content = Utils.readCommandData(ctx.getInputStream(), numBytes);
        try (FileOutputStream fos = new FileOutputStream(ctx.getWriteOnlyFile())) {
            try {
                fos.write(content);
                ctx.getOutputStream().write(Utils.intToBytes(content.length));
            } catch (IOException ex) {
                ctx.getOutputStream().write(Utils.intToBytes(-1));
                throw ex;
            }
        }
    }
}
