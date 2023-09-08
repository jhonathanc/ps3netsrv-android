package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public interface IRandomAccessFile {
    int read(byte[] buffer) throws IOException;
    void seek(long pos) throws IOException;
    long length() throws IOException;
    void close() throws IOException;
    long lastModified();
}
