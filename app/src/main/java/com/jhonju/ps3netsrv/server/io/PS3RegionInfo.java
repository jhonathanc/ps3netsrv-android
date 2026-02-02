package com.jhonju.ps3netsrv.server.io;

public class PS3RegionInfo {
    private final boolean isEncrypted;
    private final long firstAddress;
    private final long lastAddress;

    public PS3RegionInfo(boolean isEncrypted, long firstAddress, long lastAddress) {
        this.isEncrypted = isEncrypted;
        this.firstAddress = firstAddress;
        this.lastAddress = lastAddress;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public long getFirstAddress() {
        return firstAddress;
    }

    public long getLastAddress() {
        return lastAddress;
    }
}
