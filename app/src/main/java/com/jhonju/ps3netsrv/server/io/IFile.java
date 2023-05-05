package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;

public interface IFile {
    public boolean exists();
    public boolean isFile();
    public boolean isDirectory();
    public boolean delete();
    public long length();
    public IFile[] listFiles();
    public long lastModified();
    public String getName();
    public String[] list();
    public IFile findFile(String fileName) throws IOException;
}
