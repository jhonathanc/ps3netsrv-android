package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractCommand implements ICommand {
    protected Context ctx;
    protected static final int ERROR_CODE = -1;
    protected byte[] ERROR_CODE_BYTEARRAY = Utils.intToBytesBE(ERROR_CODE);
    protected byte[] SUCCESS_CODE_BYTEARRAY = Utils.intToBytesBE(0);
    protected static final int EMPTY_SIZE = 0;
    protected static final int BUFFER_SIZE = 4 * 1048576; //4MB
    protected static final int BYTES_TO_SKIP = 24;
    protected static final short MILLISECONDS_IN_SECOND = 1000;

    public AbstractCommand(Context ctx) {
        this.ctx = ctx;
    }

    protected void send(IResult result) throws IOException, PS3NetSrvException {
        byte[] byteArray;
        try {
             byteArray = result.toByteArray();
        } catch (IOException e) {
            throw new PS3NetSrvException("ERROR on byte array conversion");
        }
        send(byteArray);
    }

    protected void send(byte[] result) throws IOException, PS3NetSrvException {
        OutputStream os = ctx.getOutputStream();
        try {
            if (result.length == EMPTY_SIZE) {
                os.write(ERROR_CODE_BYTEARRAY);
                throw new PS3NetSrvException("Empty byte array to send to response");
            }
            os.write(result);
        } finally {
            os.flush();
        }
    }

}
