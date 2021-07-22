package com.jhonju.ps3netsrv;

public class CommandData {

    private ENetIsoCommand opCode;
    private byte[] data;

    public CommandData(ENetIsoCommand opCode, byte[] data) {
        this.opCode = opCode;
        this.data = data;
    }

    public ENetIsoCommand getOpCode() {
        return opCode;
    }

    public byte[] getData() {
        return data;
    }
}
