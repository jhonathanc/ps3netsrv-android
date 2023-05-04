package com.jhonju.ps3netsrv.server;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Context implements AutoCloseable {
    private Socket socket;
    private final String rootDirectory;
    private final boolean readOnly;
    private DocumentFile documentFile;
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

    public InputStream getInputStream() throws IOException { return socket.getInputStream(); }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public DocumentFile getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(DocumentFile documentFile) {
        this.documentFile = documentFile;

        if (documentFile != null && documentFile.isFile()) {
            try {
                readOnlyFile = new RandomAccessFile(PS3NetSrvApp.getAppContext(), documentFile.getUri(), "r");
            } catch (IOException fe) {
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
        if (readOnlyFile != null) {
            try {
                readOnlyFile.close();
            } catch (IOException e) {
                readOnlyFile = null;
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
