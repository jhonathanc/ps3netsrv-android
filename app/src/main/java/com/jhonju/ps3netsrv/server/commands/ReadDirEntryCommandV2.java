package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;

public class ReadDirEntryCommandV2 extends AbstractCommand {

    public ReadDirEntryCommandV2(Context ctx) {
        super(ctx);
    }

    private static class ReadDirEntryResultV2 {
        public long fileSize;

        public long modifiedTime;

        public long creationTime;

        public long accessedTime;
        public short fileNameLength;
        public boolean isDirectory;

        public ReadDirEntryResultV2() {
            this.fileSize = 0L;
            this.modifiedTime = 0L;
            this.creationTime = 0L;
            this.accessedTime = 0L;
            this.fileNameLength = 0;
            this.isDirectory = false;
        }
    }

    @Override
    public void executeTask() throws Exception {
        File file = ctx.getFile();
        ReadDirEntryResultV2 result = new ReadDirEntryResultV2();
        if (file == null || !file.isDirectory()) {
            ctx.getOutputStream().write(Utils.toByteArray(result));
            return;
        } else {
            File fileAux = null;
            for (String fileName : file.list()) {
                fileAux = new File(file.getCanonicalPath() + "/" + fileName);
                if (fileAux.getName().length() <= 255) {
                    break;
                }
            }
            if (fileAux == null) {
                ctx.setFile(null);
                ctx.getOutputStream().write(Utils.toByteArray(result));
                return;
            }
            long[] fileTimes = Utils.getFileStats(fileAux);
            result.fileSize = fileAux.isDirectory() ? 0L : file.length();
            result.modifiedTime = fileAux.lastModified() / 1000;
            result.creationTime = fileTimes[0] / 1000;
            result.accessedTime = fileTimes[1] / 1000;
            result.isDirectory = fileAux.isDirectory();
            result.fileNameLength = (short) fileAux.getName().length();
            //ctx.getOutputStream().write(Utils.toByteArray(result)); //check if it is correct...
            ctx.getOutputStream().write(Utils.toByteArray(fileAux.getName()));
        }
    }
}
