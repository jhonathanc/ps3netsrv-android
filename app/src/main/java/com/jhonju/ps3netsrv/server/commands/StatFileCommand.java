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
            this.aFileSize = -1;
            this.bModifiedTime = 0;
            this.cCreationTime = 0;
            this.dLastAccessTime = 0;
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
                statResult = new StatFileResult(0, file.lastModified() / 1000, file.lastModified() / 1000, 0, true);
            } else {
                long[] fileStats = Utils.getFileStats(file);
                statResult = new StatFileResult(file.length(), file.lastModified() / 1000, fileStats[0] / 1000, fileStats[1] / 1000, false);
            }
            ctx.getOutputStream().write(Utils.toByteArray(statResult));
        } else {
            ctx.getOutputStream().write(Utils.toByteArray(new StatFileResult()));
        }
    }
}
