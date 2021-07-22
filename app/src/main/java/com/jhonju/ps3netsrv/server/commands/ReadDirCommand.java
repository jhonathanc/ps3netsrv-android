package com.jhonju.ps3netsrv.server.commands;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.results.ReadDirResult;
import com.jhonju.ps3netsrv.server.results.ReadDirResultData;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadDirCommand implements ICommand {
    private Context ctx;
    private static final long MAX_ENTRIES = 4096;

    public ReadDirCommand(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void executeTask() throws Exception {
        ReadDirResult result;
        File file = ctx.getFile();
        if (file == null || !(file.exists() && file.isDirectory())) {
            ctx.getOutputStream().write(Utils.toByteArray(new ReadDirResult(0)));
        } else {
            File[] files = file.listFiles();

            List<ReadDirResultData> entries = new ArrayList<>();
            for(File f : files) {
                if (entries.size() == MAX_ENTRIES) break;
                entries.add(new ReadDirResultData(f.isDirectory() ? 0 : f.length(), f.lastModified(), f.isDirectory(), f.getName()));
            }
            ctx.getOutputStream().write(Utils.toByteArray(new ReadDirResult(entries.size())));
            if (entries.size() > 0)
                ctx.getOutputStream().write(Utils.toByteArray(entries));
        }
        ctx.setFile(null);
    }
}
