package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;

public class ReadDirEntryCommand extends AbstractCommand {

    public ReadDirEntryCommand(Context ctx) {
        super(ctx);
    }

    private static class ReadDirEntryResult {
        public long fileSize;
        public short fileNameLength;
        public boolean isDirectory;

        public ReadDirEntryResult() {
            this.fileSize = 0L;
            this.fileNameLength = 0;
            this.isDirectory = false;
        }
    }

    @Override
    public void executeTask() throws Exception {
        File file = ctx.getFile();
        ReadDirEntryResult entryResult = new ReadDirEntryResult();
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
            entryResult.isDirectory = fileAux.isDirectory();
            entryResult.fileSize = fileAux.isDirectory() ? 0L : file.length();
            entryResult.fileNameLength = (short) fileAux.getName().length();

            byte[][] result = { Utils.toByteArray(entryResult), Utils.toByteArray(fileAux.getName()) };
            send(result);
        }
    }
}
