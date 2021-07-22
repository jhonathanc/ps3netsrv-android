package com.jhonju.ps3netsrv.server.utils;

import java.io.DataInputStream;
import java.io.InputStream;

public class BigEndianInputStream extends DataInputStream {
    public BigEndianInputStream(InputStream in) {
        super(in);
    }
}