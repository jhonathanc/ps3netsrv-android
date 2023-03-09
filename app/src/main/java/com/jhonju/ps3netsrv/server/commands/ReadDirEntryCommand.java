package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;

public class ReadDirEntryCommand extends AbstractCommand {

    private static final short MAX_FILE_NAME_LENGTH = 255;
    private static final short EMPTY_FILE_NAME_LENGTH = 0;

    public ReadDirEntryCommand(Context ctx) {
        super(ctx);
    }

    private static class ReadDirEntryResult {
        public long aFileSize;
        public short bFileNameLength;
        public boolean cIsDirectory;

        public ReadDirEntryResult() {
            this.aFileSize = EMPTY_SIZE;
            this.bFileNameLength = EMPTY_FILE_NAME_LENGTH;
            this.cIsDirectory = false;
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
                if (fileAux.getName().length() <= MAX_FILE_NAME_LENGTH) {
                    break;
                }
            }
            if (fileAux == null) {
                ctx.setFile(null);
                send(Utils.toByteArray(entryResult));
                return;
            }
            entryResult.cIsDirectory = fileAux.isDirectory();
            entryResult.aFileSize = fileAux.isDirectory() ? EMPTY_SIZE : file.length();
            entryResult.bFileNameLength = (short) fileAux.getName().length();

            byte[][] result = { Utils.toByteArray(entryResult), Utils.toByteArray(fileAux.getName()) };
            send(result);
        }
    }
}
