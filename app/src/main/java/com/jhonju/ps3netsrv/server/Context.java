package com.jhonju.ps3netsrv.server;

import com.jhonju.ps3netsrv.server.enums.ENetIsoCommand;
import com.jhonju.ps3netsrv.server.utils.BigEndianInputStream;
import com.jhonju.ps3netsrv.server.utils.BigEndianOutputStream;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Context {
    private static final byte OP_CODE_SIZE = 2;

    private String rootDirectory;
    private Socket socket;
    private CommandData commandData;
    private InetAddress remoteAddress;
    private BigEndianInputStream inputStream;
    private BigEndianOutputStream outputStream;
    private File file;

    public Context(Socket socket, String rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory;
        this.socket = socket;
        remoteAddress = socket.getInetAddress();
        inputStream = new BigEndianInputStream(socket.getInputStream());
        outputStream = new BigEndianOutputStream(socket.getOutputStream());
    }

    public String getRootDirectory() { return rootDirectory; }

    public boolean isSocketConnected() { return socket.isConnected(); }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }

    public BigEndianInputStream getInputStream() { return inputStream; }

    public BigEndianOutputStream getOutputStream() {
        return outputStream;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public CommandData getCommandData() {
        return commandData;
    }

    public void setCommandData(byte[] data) {
        this.commandData = new CommandData(ENetIsoCommand.valueOf(ByteBuffer.wrap(Arrays.copyOfRange(data, 0, OP_CODE_SIZE)).getShort()), Arrays.copyOfRange(data, OP_CODE_SIZE, data.length));
    }
}
