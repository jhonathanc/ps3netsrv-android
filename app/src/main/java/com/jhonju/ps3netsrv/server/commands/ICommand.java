package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;

import java.io.IOException;

public interface ICommand {

    void executeTask() throws IOException, PS3NetSrvException;

}
