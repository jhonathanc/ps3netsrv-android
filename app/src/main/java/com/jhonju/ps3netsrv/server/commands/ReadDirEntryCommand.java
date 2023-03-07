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
        ReadDirEntryResult result = new ReadDirEntryResult();
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
            result.isDirectory = fileAux.isDirectory();
            result.fileSize = fileAux.isDirectory() ? 0L : file.length();
            result.fileNameLength = (short) fileAux.getName().length();
            ctx.getOutputStream().write(Utils.toByteArray(result));
            ctx.getOutputStream().write(Utils.toByteArray(fileAux.getName()));
        }
    }
}
