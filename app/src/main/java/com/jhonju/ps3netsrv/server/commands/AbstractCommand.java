package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;

import java.io.IOException;

public abstract class AbstractCommand implements ICommand {
    protected Context ctx;
    protected static final int ERROR_CODE = -1;
    protected static final int SUCCESS_CODE = 0;
    protected static final int EMPTY_SIZE = 0;
    protected static final int BUFFER_SIZE = 4 * 1048576; //4MB
    protected static final int BYTES_TO_SKIP = 24;
    protected static final short MILLISECONDS_IN_SECOND = 1000;

    public AbstractCommand(Context ctx) {
        this.ctx = ctx;
    }

    protected void send(byte[]... results) throws IOException {
        int size = EMPTY_SIZE;
        if (results != null && results.length > EMPTY_SIZE) {
            for (byte[] aux : results) {
                if (aux != null && aux.length > EMPTY_SIZE) {
                    size += aux.length;
                }
            }
        }
        if (size == EMPTY_SIZE) {
            throw new IllegalArgumentException("Error: send data is empty");
        }

        byte[] result = new byte[size];

        int destPos = 0;
        for (int i = 0; i < results.length; i++) {
            if (results[i] != null && results[i].length > EMPTY_SIZE) {
                System.arraycopy(results[i], 0, result, destPos, results[i].length);
                destPos += results[i].length;
            }
        }
        ctx.getOutputStream().write(result);
        ctx.getOutputStream().flush();
    }

}
