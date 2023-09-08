package com.jhonju.ps3netsrv.server.commands;

import android.net.Uri;
import android.os.Build;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.DocumentFile;
import com.jhonju.ps3netsrv.server.io.File;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

public abstract class FileCommand extends AbstractCommand {
    protected short filePathLength;

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            for (String rootDirectory : ctx.getRootDirectorys()) {
                File fileAux = new File(new java.io.File(rootDirectory, path.replaceAll("\\x00+$", "")));
                if (fileAux.exists()) {
                    files.add(fileAux);
                }
            }
            if (files.isEmpty()) {
                send(ERROR_CODE_BYTEARRAY);
                throw new PS3NetSrvException("ERROR: file not found.");
            }
            return files;
        }

        String formattedPath = getFormattedPath(path);

        if (!formattedPath.isEmpty()) {
            String[] paths = formattedPath.split("/");
            if (paths.length > 0) {
                for (String rootDirectory : ctx.getRootDirectorys()) {
                    androidx.documentfile.provider.DocumentFile documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(PS3NetSrvApp.getAppContext(), Uri.parse(rootDirectory));
                    if (documentFile != null && documentFile.exists()) {
                        for (String s : paths) {
                            documentFile = documentFile.findFile(s);
                            if (documentFile == null) break;
                        }
                        if (documentFile != null) {
                            files.add(new DocumentFile(documentFile));
                        }
                    }
                }
                if (files.isEmpty()) {
                    send(ERROR_CODE_BYTEARRAY);
                    throw new PS3NetSrvException("ERROR: file not found.");
                }
            }
        }
        return files;
    }
}
