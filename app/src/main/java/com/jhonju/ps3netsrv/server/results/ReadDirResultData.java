package com.jhonju.ps3netsrv.server.results;

public class ReadDirResultData {
    public final long aFileSize;
    public final long bMTime;
    public final boolean cIsDirectory;
    public char[] dName;

    public ReadDirResultData(long fileSize, long mTime, boolean isDirectory, String name) {
        this.aFileSize = fileSize;
        this.bMTime = mTime;
        this.cIsDirectory = isDirectory;
        if (name.length() > 512) {
            this.dName = name.substring(0, 512).toCharArray();
        } else {
            this.dName = new char[512];
            for(int i = 0; i < name.length(); i++) {
                this.dName[i] = name.charAt(i);
            }
        }
    }
}
