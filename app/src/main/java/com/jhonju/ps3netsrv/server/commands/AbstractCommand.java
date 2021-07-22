package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;

public abstract class AbstractCommand {
    protected Context ctx;

    public AbstractCommand(Context ctx) {
        this.ctx = ctx;
    }

}
