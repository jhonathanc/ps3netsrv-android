package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.io.IFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Context {
    private Socket socket;
    private final String rootDirectory;
    private final boolean readOnly;
    private IFile file;
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

    public InputStream getInputStream() throws IOException { return socket.getInputStream(); }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public void setFile(IFile file) {
        this.file = file;
    }

    public IFile getFile() {
        return file;
    }

    public boolean isReadOnly() { return readOnly; }

    public void close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                file = null;
            }
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
