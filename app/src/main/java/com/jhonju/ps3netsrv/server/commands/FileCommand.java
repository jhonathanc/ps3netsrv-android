package com.jhonju.ps3netsrv.server.commands;

import android.net.Uri;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.DocumentFileCustom;
import com.jhonju.ps3netsrv.server.io.FileCustom;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

public abstract class FileCommand extends AbstractCommand {
    protected short filePathLength;
    protected String fileName;
    protected DocumentFile currentDirectory;

    public FileCommand(Context ctx, short filePathLength) {
        super(ctx);
        this.filePathLength = filePathLength;
    }

    private String getFormattedPath(String path) {
        path = path.replaceAll("\\x00+$", "");
        if (path.equals("/.") || path.equals("/")) path = "";
        if (path.startsWith("/")) path = path.replaceFirst("/", "");
        return path;
    }

    protected Set<IFile> getFile() throws IOException, PS3NetSrvException {
        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (buffer == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: command failed receiving filename.");
        }

        String path = new String(buffer.array(), StandardCharsets.UTF_8);

        HashSet<IFile> files = new HashSet<>();
        if (files == null) {
             files = new HashSet<>();
        }
        
        String formattedPath = getFormattedPath(path);
        
        for (String rootDirectory : ctx.getRootDirectorys()) {
            if (rootDirectory.startsWith("content:")) {
                // Use SAF (DocumentFile)
                androidx.documentfile.provider.DocumentFile documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(PS3NetSrvApp.getAppContext(), Uri.parse(rootDirectory));
                if (documentFile == null || !documentFile.exists()) {
                    continue;
                }
                
                if (!formattedPath.isEmpty()) {
                    String[] paths = formattedPath.split("/");
                    if (paths.length > 0) {
                         for (String s : paths) {
                            documentFile = documentFile.findFile(s);
                            if (documentFile == null) break;
                        }
                    }
                }
                
                if (documentFile != null && documentFile.exists()) {
                    files.add(new DocumentFileCustom(documentFile));
                }
            } else {
                // Use Standard File I/O
                java.io.File javaFile = new java.io.File(rootDirectory, path.replaceAll("\\x00+$", ""));
                if (javaFile.exists()) {
                     files.add(new FileCustom(javaFile));
                }
            }
        }
        
        if (files.isEmpty()) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: file not found.");
        }
        return files;
    }
}
