package com.jhonju.ps3netsrv.server.commands;

import java.io.IOException;

public interface Result {
    byte[] toByteArray() throws IOException;
}
