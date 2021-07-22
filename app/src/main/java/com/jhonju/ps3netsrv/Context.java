package com.jhonju.ps3netsrv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Context {
    private static final byte OP_CODE_SIZE = 2;

    private CommandData commandData;
    private InetAddress remoteAddress;
    private BigEndianInputStream inputStream;
    private BigEndianOutputStream outputStream;
    private File file;

    public Context(InetAddress address, InputStream is, OutputStream os) throws IOException {
        remoteAddress = address;
        inputStream = new BigEndianInputStream(is);
        outputStream = new BigEndianOutputStream(os);
    }

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
