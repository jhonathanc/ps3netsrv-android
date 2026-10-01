package com.jhonju.ps3netsrv.server.commands;

import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.LONG_CAPACITY;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.exceptions.PS3NetSrvException;
import com.jhonju.ps3netsrv.server.io.IFile;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
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
 * @author JCorrêa
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
        ByteBuffer out = ByteBuffer.allocate(
            entries.size() * READ_DIR_ENTRY_LENGTH + LONG_CAPACITY);
        out.putLong(entries.size());
        for (ReadDirEntry entry : entries) {
          entry.writeTo(out);
        }
        return out.array();
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
    private final byte[] dName;

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
      byte[] encoded = name.getBytes(StandardCharsets.UTF_8);
      if (encoded.length >= MAX_FILE_NAME_LENGTH || name.indexOf('\0') >= 0) {
        throw new IllegalArgumentException("File name does not fit the protocol field");
      }
      this.dName = new byte[MAX_FILE_NAME_LENGTH];
      System.arraycopy(encoded, 0, dName, 0, encoded.length);
    }

    /**
     * Converts entry to binary format.
     * Format: [size (8)] [mtime (8)] [is_dir (1)] [UTF-8 name (512)]
     *
     * @param out Destination buffer for the 529-byte entry
     */
    public void writeTo(ByteBuffer out) {
      out.putLong(aFileSize).putLong(bModifiedTime)
          .put((byte) (cIsDirectory ? 1 : 0)).put(dName);
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

    if (directories != null) {
      for (IFile file : directories) {
        if (entries.size() >= MAX_ENTRIES) {
          FileLogger.logWarning("Maximum entries limit reached in directory listing");
          break;
        }

        if (file != null && file.isDirectory()) {
          IFile[] files = null;
          try {
            files = file.listFiles();
            if (files != null) {
              for (IFile f : files) {
                if (entries.size() >= MAX_ENTRIES) {
                  FileLogger.logWarning("Maximum entries limit reached");
                  break;
                }

                String fileName = f.getName();
                if (fileName == null || fileName.indexOf('\0') >= 0
                    || fileName.getBytes(StandardCharsets.UTF_8).length >= MAX_FILE_NAME_LENGTH) {
                  // Do not advertise a truncated alias that the client cannot open.
                  FileLogger.logWarning("Skipping file name that cannot be represented in READ_DIR");
                  continue;
                }
                if (addedNames.add(fileName)) {
                  ReadDirEntry entry = new ReadDirEntry(f.isDirectory() ? EMPTY_SIZE : f.length(),
                      f.lastModified() / MILLISECONDS_IN_SECOND, f.isDirectory(), fileName);
                  entries.add(entry);
                }
              }
            }
          } catch (IOException e) {
            FileLogger.logError("Error reading directory contents", e);
            // Continue with next directory rather than failing completely
          } finally {
            if (files != null) {
              for (IFile child : files) {
                Context.closeFile(child);
              }
            }
          }
        }
      }
    }
    FileLogger.logInfo("Directory listing completed: " + entries.size() + " entries found");
    send(new ReadDirResult(entries));
  }
}
