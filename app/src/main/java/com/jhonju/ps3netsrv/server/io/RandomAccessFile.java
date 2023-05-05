package com.jhonju.ps3netsrv.server.io;

import java.io.File;
import java.io.FileNotFoundException;

public class RandomAccessFile extends java.io.RandomAccessFile implements IRandomAccessFile {
    public RandomAccessFile(File file, String mode) throws FileNotFoundException {
        super(file, mode);
    }
}
