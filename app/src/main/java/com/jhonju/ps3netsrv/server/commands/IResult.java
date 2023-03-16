package com.jhonju.ps3netsrv.server.commands;

import java.io.IOException;

public interface IResult {
    byte[] toByteArray() throws IOException;
}
