package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.io.IFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class Context {
    private Socket socket;

    private final List<String> rootDirectorys;
    private Set<IFile> file;
    private CDSectorSize cdSectorSize;

    public Context(Socket socket, List<String> rootDirectorys) {
        this.rootDirectorys = rootDirectorys;
        this.socket = socket;
        this.cdSectorSize = CDSectorSize.CD_SECTOR_2352;
    }

    public List<String> getRootDirectorys() { return rootDirectorys; }

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

    public void setFile(Set<IFile> files) {
        this.file = files;
    }

    public Set<IFile> getFile() {
        return file;
    }

    public boolean isReadOnly() {
        return com.jhonju.ps3netsrv.app.SettingsService.isReadOnly();
    }

    public void close() {
        if (file != null) {
            for (IFile f : file) {
                try {
                    f.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
            file = null;
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
