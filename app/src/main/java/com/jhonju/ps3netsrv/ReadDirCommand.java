package com.jhonju.ps3netsrv;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReadDirCommand implements ICommand {
    private Context ctx;
    private static final long MAX_ENTRIES = 4096;

    public ReadDirCommand(Context ctx) {
        this.ctx = ctx;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void executeTask() throws Exception {
        ReadDirResult result;
        List<ReadDirResultData> entries = new ArrayList<>();
        File file = ctx.getFile();
        if (file == null || !(file.exists() && file.isDirectory())) {
            result = new ReadDirResult(0);
        } else {
            List<File> files = Files.list(Paths.get(file.getPath()))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for(File f : files) {
                if (entries.size() == MAX_ENTRIES) break;
                entries.add(new ReadDirResultData(f.isDirectory() ? 0 : f.length(), f.lastModified(), f.isDirectory(), f.getName()));
            }
            result = new ReadDirResult(entries.size());
        }
        ctx.setFile(null);
        ctx.getOutputStream().write(Utils.toByteArray(result));
        if (entries.size() > 0)
            ctx.getOutputStream().write(Utils.toByteArray(entries));
    }
}
