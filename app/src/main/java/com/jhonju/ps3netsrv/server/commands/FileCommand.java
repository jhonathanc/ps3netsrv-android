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
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

public abstract class FileCommand extends AbstractCommand {
    protected short filePathLength;
    protected String fileName;
    protected androidx.documentfile.provider.DocumentFile currentDirectory;

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

    protected IFile getFile() throws IOException, PS3NetSrvException {
        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (buffer == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: command failed receiving filename.");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            File file = null;
            for (String rootDirectory : ctx.getRootDirectorys()) {
                File fileAux = new File(new java.io.File(rootDirectory, new String(buffer.array(), StandardCharsets.UTF_8).replaceAll("\\x00+$", "")));
                if (fileAux.exists()) {
                    file = fileAux;
                    break;
                }
            }
            if (file == null) {
                send(ERROR_CODE_BYTEARRAY);
                throw new PS3NetSrvException("ERROR: file not found.");
            }
            return file;
        }

        androidx.documentfile.provider.DocumentFile documentFile = null;
        for (String rootDirectory : ctx.getRootDirectorys()) {
            androidx.documentfile.provider.DocumentFile documentFileAux = androidx.documentfile.provider.DocumentFile.fromTreeUri(PS3NetSrvApp.getAppContext(), Uri.parse(rootDirectory));
            if (documentFileAux.exists()) {
                documentFile = documentFileAux;
                break;
            }
        }
        if (documentFile == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: file not found.");
        }

        String path = getFormattedPath(new String(buffer.array(), StandardCharsets.UTF_8));
        if (!path.isEmpty()) {
            String[] paths = path.split("/");
            if (paths.length > 0) {
                for (String s : paths) {
                    currentDirectory = documentFile;
                    documentFile = documentFile.findFile(s);
                    if (documentFile == null) {
                        fileName = s;
                        break;
                    }
                }
            }
        }
        return new DocumentFile(documentFile);
    }
}
