package com.jhonju.ps3netsrv.server.io;

import java.io.File;
import java.io.FileNotFoundException;

public class RandomAccessFile extends java.io.RandomAccessFile implements IRandomAccessFile {
    private final long lastModified;
    public RandomAccessFile(File file, String mode) throws FileNotFoundException {
        super(file, mode);
        lastModified = file.lastModified();
    }

    @Override
    public long lastModified() {
        return lastModified;
    }
}
