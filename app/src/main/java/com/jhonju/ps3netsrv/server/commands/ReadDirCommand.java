package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadDirCommand extends AbstractCommand {
    private static final long MAX_ENTRIES = 4096;
    private static final short MAX_FILE_NAME_LENGTH = 512;

    public ReadDirCommand(Context ctx) {
        super(ctx);
    }

    private static class ReadDirResultData {
        public final long aFileSize;
        public final long bModifiedTime;
        public final boolean cIsDirectory;
        public char[] dName;

        public ReadDirResultData(long fileSize, long modifiedTime, boolean isDirectory, String name) {
            this.aFileSize = fileSize;
            this.bModifiedTime = modifiedTime;
            this.cIsDirectory = isDirectory;
            int length = Math.min(name.length(), MAX_FILE_NAME_LENGTH);
            this.dName = new char[MAX_FILE_NAME_LENGTH];
            for(int i = 0; i < length; i++) {
                this.dName[i] = name.charAt(i);
            }
        }
    }

    @Override
    public void executeTask() throws Exception {
        File file = ctx.getFile();
        if (file == null || !(file.exists() && file.isDirectory())) {
            send(Utils.longToBytes(EMPTY_SIZE));
        } else {
            List<ReadDirResultData> entries = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (entries.size() == MAX_ENTRIES) break;
                    entries.add(new ReadDirResultData(f.isDirectory() ? EMPTY_SIZE : f.length(), f.lastModified() / MILLISECONDS_IN_SECOND, f.isDirectory(), f.getName()));
                }
            }
            send(Utils.longToBytes(entries.size()), entries.size() > EMPTY_SIZE ? Utils.toByteArray(entries) : null);
        }
        ctx.setFile(null);
    }
}
