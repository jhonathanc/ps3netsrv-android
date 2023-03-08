package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;

import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractCommand implements ICommand {
    protected Context ctx;
    protected static final int BUFFER_SIZE = 4 * 1048576; //4MB

    public AbstractCommand(Context ctx) {
        this.ctx = ctx;
    }

    protected void send(byte[]... results) throws IOException {
        int size = 0;
        if (results != null && results.length > 0) {
            for (byte[] aux : results) {
                if (aux != null && aux.length > 0) {
                    size += aux.length;
                }
            }
        }
        if (size == 0) {
            throw new IllegalArgumentException("Error: send data is empty");
        }

        byte[] result = new byte[size];

        int destPos = 0;
        for (byte[] aux : results) {
            if (aux != null && aux.length > 0) {
                System.arraycopy(aux, 0, result, destPos, aux.length);
                destPos += aux.length;
            }
        }
        ctx.getOutputStream().write(result);
    }

}
