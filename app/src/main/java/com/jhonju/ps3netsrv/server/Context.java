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
import java.util.Set;

public class Context {
    private Socket socket;
    private final Set<String> rootDirectorys;
    private Set<IFile> file;
    private IRandomAccessFile readOnlyFile;
    private CDSectorSize cdSectorSize;

    public Context(Socket socket, Set<String> rootDirectorys) {
        this.rootDirectorys = rootDirectorys;
        this.socket = socket;
        this.cdSectorSize = CDSectorSize.CD_SECTOR_2352;
    }

    public Set<String> getRootDirectorys() { return rootDirectorys; }

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

    public void setFile(Set<IFile> files) throws IOException {
        this.file = files;
        this.readOnlyFile = null;
        if (files != null) {
            for (IFile file : files) {
                if (file != null && file.isFile()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        readOnlyFile = new RandomAccessFile(((File) file).getFile(), "r");
                    } else {
                        readOnlyFile = new RandomAccessFileLollipop(PS3NetSrvApp.getAppContext(), ((DocumentFile) file).getDocumentFile(), "r");
                    }
                }
                break;
            }
        }
    }

    public Set<IFile> getFile() {
        return file;
    }

    public IRandomAccessFile getReadOnlyFile() {
        return readOnlyFile;
    }

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
