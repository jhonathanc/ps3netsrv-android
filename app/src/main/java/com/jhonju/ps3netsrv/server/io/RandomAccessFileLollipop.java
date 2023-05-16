package com.jhonju.ps3netsrv.server.io;

import android.content.ContentResolver;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RandomAccessFileLollipop implements IRandomAccessFile {

    private long position;
    private final long fileSize;
    private final DocumentFile documentFile;
    private final ContentResolver resolver;
    private ParcelFileDescriptor pfd;
    private FileInputStream fis;
    private FileChannel fileChannel;

    public RandomAccessFileLollipop(Context context, DocumentFile documentFile, String mode) throws IOException {
        this.resolver = context.getContentResolver();
        this.documentFile = documentFile;
        long fileSize = 0;
        if (documentFile != null && documentFile.isFile()) {
            this.pfd = resolver.openFileDescriptor(documentFile.getUri(), mode);
            this.fis = new FileInputStream(pfd.getFileDescriptor());
            this.fileChannel = fis.getChannel();
            fileSize = documentFile.length();
        }
        this.fileSize = fileSize;
    }

    public int read(byte[] buffer) throws IOException {
        fileChannel.position(position);
        int bytesRead = fileChannel.read(ByteBuffer.wrap(buffer));
        position = fileChannel.position();
        return bytesRead;
    }

    public void seek(long pos) throws IOException {
        position = pos;
    }

    public long length() throws IOException {
        return fileSize;
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
