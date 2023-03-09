package com.jhonju.ps3netsrv.server.commands;

import java.io.File;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

public class StatFileCommand extends FileCommand {

    public StatFileCommand(Context ctx) {
        super(ctx);
    }

    private static class StatFileResult {
        public final long aFileSize;
        public final long bModifiedTime;
        public final long cCreationTime;
        public final long dLastAccessTime;
        public final boolean eIsDirectory;

        public StatFileResult(long fileSize, long modifiedTime, long creationTime, long lastAccessTime, boolean isDirectory) {
            this.aFileSize = fileSize;
            this.bModifiedTime = modifiedTime;
            this.cCreationTime = creationTime;
            this.dLastAccessTime = lastAccessTime;
            this.eIsDirectory = isDirectory;
        }

        public StatFileResult() {
            this.aFileSize = -1L;
            this.bModifiedTime = 0L;
            this.cCreationTime = 0L;
            this.dLastAccessTime = 0L;
            this.eIsDirectory = false;
        }
    }

    @Override
    public void executeTask() throws Exception {
        ctx.setFile(null);
        File file = getFile();
        if (file.exists()) {
            ctx.setFile(file);
            StatFileResult statResult;
            if (file.isDirectory()) {
                statResult = new StatFileResult(EMPTY_SIZE, file.lastModified() / MILLISECONDS_IN_SECOND, file.lastModified() / MILLISECONDS_IN_SECOND, 0, true);
            } else {
                long[] fileStats = Utils.getFileStats(file);
                statResult = new StatFileResult(file.length(), file.lastModified() / MILLISECONDS_IN_SECOND, fileStats[0] / MILLISECONDS_IN_SECOND, fileStats[1] / MILLISECONDS_IN_SECOND, false);
            }
            send(Utils.toByteArray(statResult));
        } else {
            send(Utils.toByteArray(new StatFileResult()));
        }
    }
}
