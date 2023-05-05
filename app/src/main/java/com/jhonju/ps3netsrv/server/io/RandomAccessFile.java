package com.jhonju.ps3netsrv.server.io;

import android.content.ContentResolver;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class RandomAccessFile {
    private final long fileSize;
    private final DocumentFile documentFile;
    private final String mode;
    private final ContentResolver resolver;

    public RandomAccessFile(Context context, DocumentFile documentFile, String mode) throws IOException {
        this.resolver = context.getContentResolver();
        this.documentFile = documentFile;
        this.mode = mode;
        fileSize = documentFile.length();
    }

    public int read(byte[] buffer, long pos) {
        try (ParcelFileDescriptor pfd = resolver.openFileDescriptor(documentFile.getUri(), mode);
             FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             FileChannel fileChannel = fis.getChannel()
        ) {
            fileChannel.position(pos);
            return fileChannel.read(ByteBuffer.wrap(buffer));
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public long length() throws IOException {
        return fileSize;
    }
}
