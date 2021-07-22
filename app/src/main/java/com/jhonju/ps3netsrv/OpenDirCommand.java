package com.jhonju.ps3netsrv;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class OpenDirCommand implements ICommand {
    private Context ctx;
    private int dpLen;
    private short[] pad = new short[12];

    public OpenDirCommand(Context ctx) {
        this.ctx = ctx;
        CommandData cmd = ctx.getCommandData();
        this.dpLen = ByteBuffer.wrap(Arrays.copyOfRange(cmd.getData(), 0, 2)).getShort();
        for (byte i = 2; i < cmd.getData().length; i++)
            pad[i-2] = cmd.getData()[i];
    }

    @Override
    public void executeTask() throws Exception {
        byte[] bFolderPath = new byte[16 + this.dpLen];
        ctx.getInputStream().read(bFolderPath, 16, dpLen);
        String folderPath = SettingsService.getFolder() + new String(bFolderPath).replaceAll("\0", "");
        File file = new File(folderPath);
        if (file.exists()) {
            ctx.setFile(file);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenDirResult(file.isDirectory() ? 0 : -1)));
        } else {
            ctx.setFile(null);
            ctx.getOutputStream().write(Utils.toByteArray(new OpenDirResult(-1)));
        }
        //os.write(new OpenDirResult(file.isDirectory() ? 0 : -1).toByteArray());
        //os.write(Utils.intToBytes(file.isDirectory() ? 0 : -1));
    }
}
