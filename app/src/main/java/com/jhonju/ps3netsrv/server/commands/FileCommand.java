package com.jhonju.ps3netsrv.server.commands;

import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

    protected DocumentFile getDocumentFile() throws IOException, PS3NetSrvException {
        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (buffer == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: command failed receiving filename.");
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
        return documentFile;
    }
}
