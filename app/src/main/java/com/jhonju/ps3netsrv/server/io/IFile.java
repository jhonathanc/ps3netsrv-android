package com.jhonju.ps3netsrv.server.io;

import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
    String getDecryptionKey();
    int read(byte[] buffer, long position) throws IOException;
    void close() throws IOException;
}
