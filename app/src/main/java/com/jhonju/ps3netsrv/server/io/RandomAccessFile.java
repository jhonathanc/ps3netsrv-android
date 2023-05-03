package com.jhonju.ps3netsrv.server.io;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.FileDescriptor;
import java.io.IOException;

public class RandomAccessFile {

    private final FileDescriptor fd;

    public RandomAccessFile(Context context, Uri uri, String mode) throws IOException
    {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, mode)) {
            fd = pfd.getFileDescriptor();
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return android.system.Os.read(fd, b, off, len);
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public void write(byte[] b) throws IOException {
        writeBytes(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    public void seek(long pos) throws IOException {
        try {
            android.system.Os.lseek(fd, pos, OsConstants.SEEK_SET);
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    public long length() throws IOException {
        try {
            return Os.fstat(fd).st_size;
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    private void writeBytes(byte[] b, int off, int len) throws IOException {
        try {
            android.system.Os.write(fd, b, off, len);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
