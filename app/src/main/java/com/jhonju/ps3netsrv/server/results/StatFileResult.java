package com.jhonju.ps3netsrv.server.results;

public class StatFileResult {
    public final long aFileSize;
    public final long bModifiedTime;
    public final long cCreationTime;
    public final long dLastAccessTime;
    public final boolean eIsDirectory;

    public StatFileResult(long fileSize, long modifiedTime, long creationTime, long lastAccessTime, boolean isDirectory) {
        this.aFileSize = fileSize;
        this.bModifiedTime = modifiedTime;
        this.cCreationTime = creationTime;
        this.dLastAccessTime = lastAccessTime;
        this.eIsDirectory = isDirectory;
    }

    public StatFileResult() {
        this.aFileSize = -1;
        this.bModifiedTime = 0;
        this.cCreationTime = 0;
        this.dLastAccessTime = 0;
        this.eIsDirectory = false;
    }
}
