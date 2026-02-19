package com.jhonju.ps3netsrv.server.commands;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.DocumentFileCustom;
import com.jhonju.ps3netsrv.server.io.FileCustom;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

import com.jhonju.ps3netsrv.R;

public abstract class FileCommand extends AbstractCommand {
  protected short filePathLength;
  protected String fileName;
  protected String requestedPath;
  protected DocumentFile currentDirectory;

  public FileCommand(Context ctx, short filePathLength) {
    super(ctx);
    this.filePathLength = filePathLength;
  }

  private String getFormattedPath(String path) {
    path = path.replaceAll("\\x00+$", "");

    // Path Traversal Protection: Reject any path containing ".."
    if (path.contains("..")) {
      return null;
    }

    if (path.equals("/.") || path.equals("/"))
      path = "";
    if (path.startsWith("/"))
      path = path.replaceFirst("/", "");
    return path;
  }

  protected Set<IFile> getFile() throws IOException, PS3NetSrvException {
    return getFile(false);
  }

  protected Set<IFile> getFile(boolean resolveParent) throws IOException, PS3NetSrvException {
    ByteBuffer buffer = BinaryUtils.readCommandData(ctx.getInputStream(), this.filePathLength);
    if (buffer == null) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_receiving_filename));
    }

    String path = new String(buffer.array(), StandardCharsets.UTF_8);

    // Strip Virtual ISO prefixes if present (based on C++ ps3netsrv implementation)
    if (path.startsWith("/***PS3***/") || path.startsWith("/***DVD***/")) {
      path = path.substring(10);
    }
    this.requestedPath = path;

    HashSet<IFile> files = new HashSet<>();

    String formattedPath = getFormattedPath(path);
    if (formattedPath == null) {
      send(ERROR_CODE_BYTEARRAY);
      throw new PS3NetSrvException(PS3NetSrvApp.getAppContext().getString(R.string.error_invalid_path));
    }

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

    for (String rootDirectory : ctx.getRootDirectories()) {
      if (rootDirectory.startsWith("content:")) {
        // Use SAF (DocumentFile)
        androidx.documentfile.provider.DocumentFile documentFile = androidx.documentfile.provider.DocumentFile
            .fromTreeUri(PS3NetSrvApp.getAppContext(), Uri.parse(rootDirectory));
        if (documentFile == null || !documentFile.exists()) {
          continue;
        }

        if (!formattedPath.isEmpty()) {
          String[] paths = formattedPath.split("/");
          for (String s : paths) {
            if (s.isEmpty())
              continue;
            
            // Check for potential traversal in individual segments (redundant but safe)
            if (s.equals("..") || s.equals(".")) continue;

            DocumentFile found = findFileSafely(documentFile, s);
            if (found == null) {
              documentFile = null;
              break;
            }
            documentFile = found;
          }
        }

        if (documentFile != null && documentFile.exists()) {
          files.add(new DocumentFileCustom(documentFile, ctx.getContentResolver()));
        }
      } else {
        // Use Standard File I/O
        java.io.File rootDir = new java.io.File(rootDirectory);
        java.io.File targetFile;
        if (formattedPath.isEmpty()) {
          targetFile = rootDir;
        } else {
          targetFile = new java.io.File(rootDir, formattedPath);
        }

        // Final security check: Ensure the canonical path still starts with the root directory
        try {
          String rootCanonical = rootDir.getCanonicalPath();
          String targetCanonical = targetFile.getCanonicalPath();
          
          if (!targetCanonical.startsWith(rootCanonical)) {
            FileLogger.logWarning("Path traversal attempt blocked: " + path + " resolves to " + targetCanonical);
            continue;
          }
          
          if (targetFile.exists()) {
            files.add(new FileCustom(targetFile));
          }
        } catch (IOException e) {
          FileLogger.logError("Error resolving canonical path", e);
        }
      }
    }
    return files;
  }

  private DocumentFile findFileSafely(DocumentFile parent, String name) {
    if (parent == null)
      return null;

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
