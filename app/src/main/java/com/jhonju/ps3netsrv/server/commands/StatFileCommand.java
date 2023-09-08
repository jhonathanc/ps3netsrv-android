package com.jhonju.ps3netsrv.server.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.Utils;

public class StatFileCommand extends FileCommand {

    private static final int RESULT_LENGTH = 33;

    public StatFileCommand(Context ctx, short filePathLength) {
        super(ctx, filePathLength);
    }

    private static class StatFileResult implements IResult {
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

        public byte[] toByteArray() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream(RESULT_LENGTH);
            try {
                out.write(Utils.longToBytesBE(this.aFileSize));
                out.write(Utils.longToBytesBE(this.bModifiedTime));
                out.write(Utils.longToBytesBE(this.cCreationTime));
                out.write(Utils.longToBytesBE(this.dLastAccessTime));
                out.write(eIsDirectory ? 1 : 0);
                return out.toByteArray();
            } finally {
                out.close();
            }
        }
    }

    @Override
    public void executeTask() throws IOException, PS3NetSrvException {
        ctx.setFile(null);
        Set<IFile> files = getFile();
        if (files != null) {
            for (IFile file : files) {
                if (!file.exists()) break;
                ctx.setFile(files);
                StatFileResult statResult;
                if (file.isDirectory()) {
                    statResult = new StatFileResult(EMPTY_SIZE, file.lastModified() / MILLISECONDS_IN_SECOND, file.lastModified() / MILLISECONDS_IN_SECOND, 0, true);
                } else {
                    long[] fileStats = {0, 0};
                    //TODO: fix file stats
                    //statResult = new StatFileResult(file.length(), file.lastModified() / MILLISECONDS_IN_SECOND, fileStats[0] / MILLISECONDS_IN_SECOND, fileStats[1] / MILLISECONDS_IN_SECOND, false);

                    statResult = new StatFileResult(file.length(), file.lastModified() / MILLISECONDS_IN_SECOND, fileStats[0], fileStats[1], false);
                }
                send(statResult);
                return;
            }
        }
        send(new StatFileResult());
    }
}
