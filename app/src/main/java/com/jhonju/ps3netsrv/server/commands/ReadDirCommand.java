package com.jhonju.ps3netsrv.server.commands;

import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.LONG_CAPACITY;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ReadDirCommand handles directory listing requests from PS3 clients.
 * 
 * This command:
 * 1. Retrieves directory contents from configured root directories
 * 2. Respects folder priority (first folder takes precedence)
 * 3. Avoids duplicate entries across multiple directories
 * 4. Formats results as binary protocol response
 * 5. Enforces maximum entry limits for protocol compliance
 * 
 * @author PS3NetSrv Android Contributors
 */
public class ReadDirCommand extends AbstractCommand {
  private static final long MAX_ENTRIES = 4096;
  private static final short MAX_FILE_NAME_LENGTH = 512;
  private static final int READ_DIR_ENTRY_LENGTH = 529;

  /**
   * Constructs a ReadDirCommand for the given context.
   * 
   * @param ctx The server context with connection and configuration info
   */
  public ReadDirCommand(Context ctx) {
    super(ctx);
  }

  /**
   * Internal result container for directory listing data.
   * Formats entries as binary protocol response.
   */
  private static class ReadDirResult implements IResult {
    private final List<ReadDirEntry> entries;

    public ReadDirResult(List<ReadDirEntry> entries) {
      this.entries = entries;
    }

    /**
     * Converts directory entries to binary format.
     * Format: [entry count (8 bytes)] + [entries...]
     * 
     * @return Byte array containing binary protocol response
     * @throws IOException If writing to stream fails
     */
    public byte[] toByteArray() throws IOException {
      if (entries != null) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(
            entries.size() * READ_DIR_ENTRY_LENGTH + LONG_CAPACITY);
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

  /**
   * Internal representation of a single directory entry.
   */
  private static class ReadDirEntry {
    private final long aFileSize;
    private final long bModifiedTime;
    private final boolean cIsDirectory;
    private final char[] dName;

    /**
     * Constructs a directory entry.
     * 
     * @param fileSize Size of the file in bytes
     * @param modifiedTime Last modification time (milliseconds since epoch)
     * @param isDirectory true if entry is a directory
     * @param name File/directory name
     */
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

    /**
     * Converts entry to binary format.
     * Format: [size (8)] [mtime (8)] [is_dir (1)] [name (1024)]
     * 
     * @return Byte array (529 bytes) representing the entry
     * @throws IOException If writing to stream fails
     */
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

  /**
   * Executes the directory listing task.
   * 
   * Algorithm:
   * 1. Iterate through configured root directories in order
   * 2. For each directory, list its contents
   * 3. Track seen entries to avoid duplicates
   * 4. Respect folder priority (first folder wins for duplicate names)
   * 5. Enforce maximum entries limit (4096)
   * 6. Format and send binary response
   * 
   * @throws IOException If socket communication or file operations fail
   * @throws PS3NetSrvException If protocol or server errors occur
   */
  @Override
  public void executeTask() throws IOException, PS3NetSrvException {
    List<ReadDirEntry> entries = new ArrayList<>();
    Set<String> addedNames = new HashSet<>();
    Set<IFile> directories = ctx.getFile();
    
    try {
      if (directories != null) {
        for (IFile file : directories) {
          if (entries.size() >= MAX_ENTRIES) {
            FileLogger.logWarning("Maximum entries limit reached in directory listing");
            break;
          }
          
          if (file != null && file.isDirectory()) {
            try {
              IFile[] files = file.listFiles();
              if (files != null) {
                for (IFile f : files) {
                  if (entries.size() >= MAX_ENTRIES) {
                    FileLogger.logWarning("Maximum entries limit reached");
                    break;
                  }
                  
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
            } catch (IOException e) {
              FileLogger.logError("Error reading directory contents", e);
              // Continue with next directory rather than failing completely
            }
          }
        }
      }
      FileLogger.logInfo("Directory listing completed: " + entries.size() + " entries found");
      send(new ReadDirResult(entries));
    } catch (Exception e) {
      FileLogger.logError("Error executing ReadDirCommand", e);
      throw new PS3NetSrvException("Directory listing failed: " + e.getMessage());
    }
  }
}