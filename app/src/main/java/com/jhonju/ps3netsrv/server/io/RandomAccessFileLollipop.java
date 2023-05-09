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
    private final String mode;
    private final ContentResolver resolver;

    public RandomAccessFileLollipop(Context context, DocumentFile documentFile, String mode) throws IOException {
        this.resolver = context.getContentResolver();
        this.documentFile = documentFile;
        this.mode = mode;
        fileSize = documentFile.length();
    }

    public int read(byte[] buffer) throws IOException {
        ParcelFileDescriptor pfd = resolver.openFileDescriptor(documentFile.getUri(), mode);
        FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
        FileChannel fileChannel = fis.getChannel();
        try {
            fileChannel.position(position);
            int bytesRead = fileChannel.read(ByteBuffer.wrap(buffer));
            position = fileChannel.position();
            return bytesRead;
        } finally {
            fileChannel.close();
            fis.close();
            pfd.close();
        }
    }

    public void seek(long pos) throws IOException {
        position = pos;
    }

    public long length() throws IOException {
        return fileSize;
    }

    @Override
    public void close() throws IOException {

    }
}
