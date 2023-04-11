package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.CDSectorSize;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;

public class Context implements AutoCloseable {
    private Socket socket;
    private final String rootDirectory;
    private final boolean readOnly;
    private File file;
    private RandomAccessFile readOnlyFile;
    private File writeOnlyFile;
    private CDSectorSize cdSectorSize;

    public Context(Socket socket, String rootDirectory, boolean readOnly) {
        this.rootDirectory = rootDirectory;
        this.socket = socket;
        this.cdSectorSize = CDSectorSize.CD_SECTOR_2352;
        this.readOnly = readOnly;
    }

    public String getRootDirectory() { return rootDirectory; }

    public boolean isSocketConnected() { return socket.isConnected(); }

    public CDSectorSize getCdSectorSize() {
        return cdSectorSize;
    }

    public void setCdSectorSize(CDSectorSize cdSectorSize) {
        this.cdSectorSize = cdSectorSize;
    }

    public InetAddress getRemoteAddress() {
        return socket.getInetAddress();
    }

    public InputStream getInputStream() throws IOException { return socket.getInputStream(); }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        if (file != null && file.isFile()) {
            try {
                readOnlyFile = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException fe) {
                readOnlyFile = null;
                fe.printStackTrace();
            }
        } else {
            readOnlyFile = null;
        }
    }

    public File getWriteOnlyFile() {
        return writeOnlyFile;
    }

    public void setWriteOnlyFile(File writeOnlyFile) {
        this.writeOnlyFile = writeOnlyFile;
    }

    public RandomAccessFile getReadOnlyFile() {
        return readOnlyFile;
    }

    public boolean isReadOnly() { return readOnly; }

    @Override
    public void close() {
        try {
            if (readOnlyFile != null) readOnlyFile.close();
        } catch (IOException ignored) {
        } finally {
            readOnlyFile = null;
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            } finally {
                socket = null;
            }
        }
    }
}
