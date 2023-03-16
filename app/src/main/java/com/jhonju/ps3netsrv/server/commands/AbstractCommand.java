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

    protected void send(IResult result) throws IOException {
        send(result.toByteArray());
    }

    protected void send(byte[] result) throws IOException {
        if (result.length == EMPTY_SIZE) {
            throw new IllegalArgumentException("Error: send data is empty");
        }
        ctx.getOutputStream().write(result);
        ctx.getOutputStream().flush();
    }

}
