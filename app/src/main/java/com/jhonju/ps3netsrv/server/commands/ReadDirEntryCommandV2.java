package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;

public class ReadDirEntryCommandV2 extends AbstractCommand {

    private static final short MAX_FILE_NAME_LENGTH = 255;
    private static final short EMPTY_FILE_NAME_LENGTH = 0;

    public ReadDirEntryCommandV2(Context ctx) {
        super(ctx);
    }

    private static class ReadDirEntryResultV2 {
        public long aFileSize;
        public long bModifiedTime;
        public long cCreationTime;
        public long dAccessedTime;
        public short eFileNameLength;
        public boolean fIsDirectory;

        public ReadDirEntryResultV2() {
            this.aFileSize = EMPTY_SIZE;
            this.bModifiedTime = 0L;
            this.cCreationTime = 0L;
            this.dAccessedTime = 0L;
            this.eFileNameLength = EMPTY_FILE_NAME_LENGTH;
            this.fIsDirectory = false;
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
                if (fileAux.getName().length() <= MAX_FILE_NAME_LENGTH) {
                    break;
                }
            }
            if (fileAux == null) {
                ctx.setFile(null);
                send(Utils.toByteArray(entryResult));
                return;
            }
            long[] fileTimes = Utils.getFileStats(fileAux);
            entryResult.aFileSize = fileAux.isDirectory() ? EMPTY_SIZE : file.length();
            entryResult.bModifiedTime = fileAux.lastModified() / MILLISECONDS_IN_SECOND;
            entryResult.cCreationTime = fileTimes[0] / MILLISECONDS_IN_SECOND;
            entryResult.dAccessedTime = fileTimes[1] / MILLISECONDS_IN_SECOND;
            entryResult.fIsDirectory = fileAux.isDirectory();
            entryResult.eFileNameLength = (short) fileAux.getName().length();

            byte[][] result = { Utils.toByteArray(entryResult), Utils.toByteArray(fileAux.getName()) };
            send(result);
        }
    }
}
