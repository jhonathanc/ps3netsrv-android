package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReadDirCommand extends AbstractCommand {
    private static final long MAX_ENTRIES = 4096;

    public ReadDirCommand(Context ctx) {
        super(ctx);
    }

    private static class ReadDirResult {
        public final long dirSize;

        public ReadDirResult(long dirSize) {
            this.dirSize = dirSize;
        }
    }

    private static class ReadDirResultData {
        public final long aFileSize;
        public final long bMTime;
        public final boolean cIsDirectory;
        public char[] dName;

        public ReadDirResultData(long fileSize, long mTime, boolean isDirectory, String name) {
            this.aFileSize = fileSize;
            this.bMTime = mTime;
            this.cIsDirectory = isDirectory;
            if (name.length() > 512) {
                this.dName = name.substring(0, 512).toCharArray();
            } else {
                this.dName = new char[512];
                for(int i = 0; i < name.length(); i++) {
                    this.dName[i] = name.charAt(i);
                }
            }
        }
    }

    @Override
    public void executeTask() throws Exception {
        File file = ctx.getFile();
        if (file == null || !(file.exists() && file.isDirectory())) {
            ctx.getOutputStream().write(Utils.toByteArray(new ReadDirResult(0)));
        } else {
            List<ReadDirResultData> entries = new ArrayList<>();
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (entries.size() == MAX_ENTRIES) break;
                    entries.add(new ReadDirResultData(f.isDirectory() ? 0 : f.length(), f.lastModified() / 1000, f.isDirectory(), f.getName()));
                }
            }
            ctx.getOutputStream().write(Utils.toByteArray(new ReadDirResult(entries.size())));
            if (entries.size() > 0)
                ctx.getOutputStream().write(Utils.toByteArray(entries));
        }
        ctx.setFile(null);
    }
}
