package com.jhonju.ps3netsrv.server.io;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RandomAccessFile implements Closeable {

    private FileChannel fileChannel;
    private long position;
    private ParcelFileDescriptor pfd;
    private FileInputStream fis;

    public RandomAccessFile(Context context, Uri treeUri, String mode) throws IOException {
        try {
            pfd = context.getContentResolver().openFileDescriptor(treeUri, mode);
            fis = new FileInputStream(pfd.getFileDescriptor());
            fileChannel = fis.getChannel();
        } catch (FileNotFoundException e) {
            throw new IOException(e);
        }
    }

    public int read(byte[] buffer) {
        try {
            fileChannel.position(position);
            int bytesRead = fileChannel.read(ByteBuffer.wrap(buffer));
            position = fileChannel.position();
            return bytesRead;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void seek(long pos) throws IOException {
        position = pos;
    }

    public long length() throws IOException {
        return fileChannel.size();
    }

    @Override
    public void close() throws IOException {
        try {
            fileChannel.close();
        } finally {
            fileChannel = null;
        }

        try {
            fis.close();
        } finally {
            fis = null;
        }

        try {
            pfd.close();
        } finally {
            pfd = null;
        }
    }
}
