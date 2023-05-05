package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public interface IRandomAccessFile {
    public int read(byte[] buffer) throws IOException;
    public void seek(long pos) throws IOException;
    public long length() throws IOException;
    public void close() throws IOException;
}
