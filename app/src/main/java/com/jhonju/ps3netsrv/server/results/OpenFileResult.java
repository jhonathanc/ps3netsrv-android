package com.jhonju.ps3netsrv.server.results;

public class OpenFileResult {
    public final long aFileSize;
    public final long bMTime;

    public OpenFileResult(long fileSize, long mTime) {
        this.aFileSize = fileSize;
        this.bMTime = mTime;
    }
}
