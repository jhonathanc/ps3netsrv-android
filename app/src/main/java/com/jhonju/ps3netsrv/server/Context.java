package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.CDSectorSize;
import com.jhonju.ps3netsrv.server.enums.ENetIsoCommand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Context implements AutoCloseable {
    private static final byte OP_CODE_SIZE = 2;

    private String rootDirectory;
    private Socket socket;
    private CommandData commandData;
    private File file;
    private RandomAccessFile readOnlyFile;
    private CDSectorSize cdSectorSize;

    public Context(Socket socket, String rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.socket = socket;
        this.cdSectorSize = CDSectorSize.CD_SECTOR_2352;
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

    public RandomAccessFile getReadOnlyFile() {
        return readOnlyFile;
    }

    public CommandData getCommandData() {
        return commandData;
    }

    public void setCommandData(byte[] data) {
        this.commandData = new CommandData(ENetIsoCommand.valueOf(ByteBuffer.wrap(Arrays.copyOfRange(data, 0, OP_CODE_SIZE)).getShort()), Arrays.copyOfRange(data, OP_CODE_SIZE, data.length));
    }

    @Override
    public void close() throws IOException {
        if (readOnlyFile != null) readOnlyFile.close();
    }
}
