package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;

public abstract class AbstractCommand implements ICommand {
    protected Context ctx;
    protected static final int BUFFER_SIZE = 4194304;

    public AbstractCommand(Context ctx) {
        this.ctx = ctx;
    }

}
