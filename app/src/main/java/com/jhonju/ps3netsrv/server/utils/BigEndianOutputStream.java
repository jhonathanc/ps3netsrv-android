package com.jhonju.ps3netsrv.server.utils;

import java.io.DataOutputStream;
import java.io.OutputStream;

public class BigEndianOutputStream extends DataOutputStream {
    public BigEndianOutputStream(OutputStream os) {
        super(os);
    }
}
