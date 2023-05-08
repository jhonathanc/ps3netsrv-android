package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public interface IFile {
    boolean exists();
    boolean isFile();
    boolean isDirectory();
    boolean delete();
    long length();
    IFile[] listFiles();
    long lastModified();
    String getName();
    String[] list();
    IFile findFile(String fileName) throws IOException;
}
