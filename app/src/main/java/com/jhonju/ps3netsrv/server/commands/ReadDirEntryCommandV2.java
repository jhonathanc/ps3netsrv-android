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
        ReadDirEntryResultV2 entryResult = new ReadDirEntryResultV2();
        if (file == null || !file.isDirectory()) {
            send(Utils.toByteArray(entryResult));
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
                send(Utils.toByteArray(entryResult));
                return;
            }
            long[] fileTimes = Utils.getFileStats(fileAux);
            entryResult.fileSize = fileAux.isDirectory() ? 0L : file.length();
            entryResult.modifiedTime = fileAux.lastModified() / 1000;
            entryResult.creationTime = fileTimes[0] / 1000;
            entryResult.accessedTime = fileTimes[1] / 1000;
            entryResult.isDirectory = fileAux.isDirectory();
            entryResult.fileNameLength = (short) fileAux.getName().length();

            byte[][] result = { Utils.toByteArray(entryResult), Utils.toByteArray(fileAux.getName()) };
            send(result);
        }
    }
}
