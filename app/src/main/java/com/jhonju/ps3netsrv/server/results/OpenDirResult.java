package com.jhonju.ps3netsrv.server.results;

import java.io.Serializable;

public class OpenDirResult implements Serializable {
    public final int openResult;

    public OpenDirResult(int openResult) {
        this.openResult = openResult;
    }

//    public byte[] toByteArray() {
//        return Utils.intToBytes(openResult);
//    }
//
//    private void writeObject(java.io.ObjectOutputStream stream)
//            throws IOException {
//        stream.writeInt(openResult);
//    }
}
