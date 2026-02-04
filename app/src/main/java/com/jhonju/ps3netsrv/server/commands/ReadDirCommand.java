package com.jhonju.ps3netsrv.server.commands;

import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.LONG_CAPACITY;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ReadDirCommand extends AbstractCommand {
  private static final long MAX_ENTRIES = 4096;
  private static final short MAX_FILE_NAME_LENGTH = 512;
  private static final int READ_DIR_ENTRY_LENGTH = 529;

  public ReadDirCommand(Context ctx) {
    super(ctx);
  }

  private static class ReadDirResult implements IResult {
    private final List<ReadDirEntry> entries;

    public ReadDirResult(List<ReadDirEntry> entries) {
      this.entries = entries;
    }

    public byte[] toByteArray() throws IOException {
      if (entries != null) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(entries.size() * READ_DIR_ENTRY_LENGTH + LONG_CAPACITY);
        try {
          out.write(BinaryUtils.longToBytesBE(entries.size()));
          for (ReadDirEntry entry : entries) {
            out.write(entry.toByteArray());
          }
          return out.toByteArray();
        } finally {
          out.close();
        }
      }
      return null;
    }

  }

  private static class ReadDirEntry {
    private final long aFileSize;
    private final long bModifiedTime;
    private final boolean cIsDirectory;
    private final char[] dName;

    public ReadDirEntry(long fileSize, long modifiedTime, boolean isDirectory, String name) {
      this.aFileSize = fileSize;
      this.bModifiedTime = modifiedTime;
      this.cIsDirectory = isDirectory;
      int length = Math.min(name.length(), MAX_FILE_NAME_LENGTH);
      this.dName = new char[MAX_FILE_NAME_LENGTH];
      for (int i = 0; i < length; i++) {
        this.dName[i] = name.charAt(i);
      }
    }

    public byte[] toByteArray() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(READ_DIR_ENTRY_LENGTH);
      try {
        out.write(BinaryUtils.longToBytesBE(this.aFileSize));
        out.write(BinaryUtils.longToBytesBE(this.bModifiedTime));
        out.write(cIsDirectory ? 1 : 0);
        out.write(BinaryUtils.charArrayToByteArray(dName));
        return out.toByteArray();
      } finally {
        out.close();
      }
    }
  }

  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    List<ReadDirEntry> entries = new ArrayList<>();
    Set<String> addedNames = new java.util.HashSet<>();
    Set<IFile> directories = ctx.getFile();
    if (directories != null) {
      for (IFile file : directories) {
        if (entries.size() == MAX_ENTRIES)
          break;
        if (file != null && file.isDirectory()) {
          IFile[] files = file.listFiles();
          for (IFile f : files) {
            if (entries.size() == MAX_ENTRIES)
              break;
            String fileName = f.getName() != null ? f.getName() : "";
            // If file already added (by name and type), skip it.
            // We use a simple Key format: "NAME|IS_DIR" to distinguish if needed,
            // but typically if a file exists with same name in multiple folders, we just
            // take the first one found.
            if (!addedNames.contains(fileName + f.isDirectory())) {
              ReadDirEntry entry = new ReadDirEntry(f.isDirectory() ? EMPTY_SIZE : f.length(),
                  f.lastModified() / MILLISECONDS_IN_SECOND, f.isDirectory(), fileName);
              entries.add(entry);
              addedNames.add(fileName + f.isDirectory());
            }
          }
        }
      }
    }
    if (entries.isEmpty()) {
      send(BinaryUtils.longToBytesBE(EMPTY_SIZE));
    } else {
      send(new ReadDirResult(entries));
    }
    ctx.setFile(null);
  }
}
