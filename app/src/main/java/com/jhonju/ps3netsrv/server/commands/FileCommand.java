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
        return getFile(false);
    }

    protected Set<IFile> getFile(boolean resolveParent) throws IOException, PS3NetSrvException {
        ByteBuffer buffer = Utils.readCommandData(ctx.getInputStream(), this.filePathLength);
        if (buffer == null) {
            send(ERROR_CODE_BYTEARRAY);
            throw new PS3NetSrvException("ERROR: command failed receiving filename.");
        }

        String path = new String(buffer.array(), StandardCharsets.UTF_8);

        HashSet<IFile> files = new HashSet<>();
        
        String formattedPath = getFormattedPath(path);
        String childName = "";

        if (resolveParent) {
            if (formattedPath.endsWith("/")) {
                formattedPath = formattedPath.substring(0, formattedPath.length() - 1);
            }
            int lastSlash = formattedPath.lastIndexOf('/');
            if (lastSlash >= 0) {
                childName = formattedPath.substring(lastSlash + 1);
                formattedPath = formattedPath.substring(0, lastSlash);
            } else {
                childName = formattedPath;
                formattedPath = "";
            }
            this.fileName = childName;
        }
        
        for (String rootDirectory : ctx.getRootDirectorys()) {
            if (rootDirectory.startsWith("content:")) {
                // Use SAF (DocumentFile)
                androidx.documentfile.provider.DocumentFile documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(PS3NetSrvApp.getAppContext(), Uri.parse(rootDirectory));
                if (documentFile == null || !documentFile.exists()) {
                    continue;
                }
                
                if (!formattedPath.isEmpty()) {
                    String[] paths = formattedPath.split("/");
                    for (String s : paths) {
                        if (s.isEmpty()) continue;
                        documentFile = findFileSafely(documentFile, s);
                        if (documentFile == null) break;
                    }
                }
                
                if (documentFile != null && documentFile.exists()) {
                    files.add(new DocumentFileCustom(documentFile));
                }
            } else {
                // Use Standard File I/O
                String fullPath = rootDirectory;
                if (!formattedPath.isEmpty()) {
                     // java.io.File handles paths with slashes correctly
                     fullPath = new java.io.File(rootDirectory, formattedPath).getAbsolutePath();
                }
                
                java.io.File javaFile = new java.io.File(fullPath);
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

    private DocumentFile findFileSafely(DocumentFile parent, String name) {
        if (parent == null) return null;
        
        // Fast path: Try direct lookup
        DocumentFile file = parent.findFile(name);
        if (file != null && file.exists()) {
            return file;
        }

        // Slow path: Iterate to find match (ignoring case)
        // This handles Android 14 SAF issues where findFile might fail or case differs
        DocumentFile[] files = parent.listFiles();
        for (DocumentFile f : files) {
            String fileName = f.getName();
            if (fileName != null && fileName.equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }
}
