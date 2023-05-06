package com.jhonju.ps3netsrv.server;

import android.os.Build;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.io.DocumentFile;
import com.jhonju.ps3netsrv.server.io.File;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.io.IRandomAccessFile;
import com.jhonju.ps3netsrv.server.io.RandomAccessFile;
import com.jhonju.ps3netsrv.server.io.RandomAccessFileLollipop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Context {
    private Socket socket;
    private final String rootDirectory;
    private final boolean readOnly;
    private IFile file;
    private IRandomAccessFile readOnlyFile;
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

        if (file != null && file.isFile()) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    readOnlyFile = new RandomAccessFile(((File)file).getFile(), "r");
                } else {
                    readOnlyFile = new RandomAccessFileLollipop(PS3NetSrvApp.getAppContext(), ((DocumentFile)file).getDocumentFile(), "r");
                }
            } catch (IOException fe) {
                readOnlyFile = null;
                fe.printStackTrace();
            }
        } else {
            readOnlyFile = null;
        }
    }

    public IFile getFile() {
        return file;
    }

    public IRandomAccessFile getReadOnlyFile() {
        return readOnlyFile;
    }

    public boolean isReadOnly() { return readOnly; }

    public void close() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (readOnlyFile != null) {
                try {
                    readOnlyFile.close();
                } catch (IOException e) {
                    readOnlyFile = null;
                }
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
