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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    protected IFile getFile() throws IOException, PS3NetSrvException {
        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (buffer == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: command failed receiving filename.");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return new FileCustom(new File(ctx.getRootDirectory(), new String(buffer.array(), StandardCharsets.UTF_8).replaceAll("\\x00+$", "")));
        }

        DocumentFile documentFile = DocumentFile.fromTreeUri(PS3NetSrvApp.getAppContext(), Uri.parse(ctx.getRootDirectory()));
        if (documentFile == null || !documentFile.exists()) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: wrong path configuration.");
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
        return new DocumentFileCustom(documentFile);
    }
}
