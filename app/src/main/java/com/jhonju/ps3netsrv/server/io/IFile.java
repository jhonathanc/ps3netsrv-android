package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public interface IFile {
    boolean exists();
    boolean isFile();
    boolean isDirectory();
    boolean delete();
    long length();
    IFile[] listFiles() throws IOException;
    long lastModified();
    String getName();
    String[] list();
    IFile findFile(String fileName) throws IOException;
    int read(byte[] buffer, long position) throws IOException;
    void close() throws IOException;
    void write(byte[] buffer) throws IOException;
    boolean createDirectory(String name);
    boolean createFile(String name);
}
