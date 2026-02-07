package com.jhonju.ps3netsrv.server.io;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

public class VirtualIsoFile implements IFile {

  private static final int SECTOR_SIZE = 2048;
  private static final int MAX_DIRECTORY_BUFFER_SIZE = 4 * 1024 * 1024; // 4MB to handle large directories(STREAMS has 3689 files)
  private static final int PATH_TABLE_ENTRY_ESTIMATE = 32;

  // Multi-extent support - max size per extent (~4GB, sector-aligned)
  private static final long MULTIEXTENT_PART_SIZE = 0xFFFFF800L;

  // ISO 9660 file flags
  private static final byte ISO_FILE = 0x00;
  private static final byte ISO_DIRECTORY = 0x02;
  private static final byte ISO_MULTIEXTENT = (byte) 0x80;

  // Multipart file pattern (.66600, .66601, etc.)
  private static final String MULTIPART_SUFFIX_PATTERN = ".66600";

  private final IFile rootFile;
  private final String volumeName;

  // PS3 Mode fields
  private final boolean ps3Mode;
  private final String titleId;

  private ByteBuffer fsBuf;
  private int fsBufSize;
  private long totalSize;

  private DirList rootList;
  private List<FileEntry> allFiles;

  // Lock object for thread-safe access to fsBuf
  private final Object fsBufLock = new Object();

  private static class FileEntry {
    String name;
    long size;
    int rlba;
    long startOffset;
    long endOffset;

    // Multipart support - list of file parts
    List<IFile> fileParts = new ArrayList<>();
    boolean isMultipart;

    // Multi-extent support - number of extent parts for files > 4GB
    int extentParts = 1;
  }

  private static class DirList {
    String name;
    DirList parent;
    int idx;
    int lba;
    int sizeBytes;
    byte[] content;
    List<FileEntry> files = new ArrayList<>();
  }

  public VirtualIsoFile(IFile rootDir) throws IOException {
    try {
      this.rootFile = rootDir;
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile ctor start for: " + (rootDir != null ? rootDir.getName() : "null"));

      // Detect PS3 mode by checking for PS3_GAME/PARAM.SFO
      String detectedTitleId = ParamSfoParser.getTitleId(rootDir);
      this.ps3Mode = (detectedTitleId != null);
      this.titleId = detectedTitleId;
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile ps3Mode: " + ps3Mode + ", titleId: " + titleId);

      if (ps3Mode) {
        this.volumeName = "PS3VOLUME";
      } else {
        this.volumeName = rootDir.getName() != null ? rootDir.getName().toUpperCase() : "DVDVIDEO";
      }

      build();
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile ctor end");
    } catch (Throwable e) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logError(new RuntimeException("Error in VirtualIsoFile ctor", e));
      throw new IOException("Error creating VirtualIsoFile", e);
    }
  }

  private void build() throws IOException {
    try {
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: build() start");
    allFiles = new ArrayList<>();
    rootList = new DirList();
    rootList.name = "";
    rootList.parent = rootList;
    rootList.idx = 1;

    List<DirList> allDirs = new ArrayList<>();
    allDirs.add(rootList);

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: scanDirectory start");
    scanDirectory(rootFile, rootList, allDirs);
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: scanDirectory end. Dirs found: " + allDirs.size());

    // 2. Calculate sizes and assign Relative LBAs for files
    int currentFileSectorOffset = 0;
    for (DirList dir : allDirs) {
      for (FileEntry file : dir.files) {
        file.rlba = currentFileSectorOffset;
        int sectors = (int) ((file.size + SECTOR_SIZE - 1) / SECTOR_SIZE);
        currentFileSectorOffset += sectors;
        allFiles.add(file);

        // Calculate extent parts for multi-extent support
        if (file.size > MULTIEXTENT_PART_SIZE) {
          file.extentParts = (int) ((file.size + MULTIEXTENT_PART_SIZE - 1) / MULTIEXTENT_PART_SIZE);
        }
      }
    }
    int filesSizeSectors = currentFileSectorOffset;
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: Sizes calculated. Files found: " + allFiles.size());

    // 4. Generate Path Tables
    
    // ISO 9660 requires Path Table to be sorted by:
    // 1. Parent Directory Number (Ascending)
    // 2. Directory Identifier (Ascending)
    // To achieve this, we must traverse the directory tree in Level Order (BFS),
    // and sort siblings by name.
    
    // First, map parents to their children
    java.util.Map<DirList, List<DirList>> childrenMap = new java.util.HashMap<>();
    for (DirList dir : allDirs) {
        if (dir.parent != null && dir != rootList) {
            List<DirList> children = childrenMap.get(dir.parent);
            if (children == null) {
                children = new ArrayList<>();
                childrenMap.put(dir.parent, children);
            }
            children.add(dir);
        }
    }
    
    // Now perform BFS to rebuild allDirs in correct order
    List<DirList> sortedDirs = new ArrayList<>();
    
    // Queue for BFS
    List<DirList> queue = new ArrayList<>();
    queue.add(rootList);
    
    // Initial root setup
    rootList.idx = 1;
    sortedDirs.add(rootList);
    
    // We use a simple pointer instead of actual Queue to avoid auto-boxing overhead/complexity
    int queuePtr = 0;
    while(queuePtr < queue.size()) {
       DirList parent = queue.get(queuePtr++);
       
       List<DirList> children = childrenMap.get(parent);
       if (children != null) {
           // Sort siblings by name
           Collections.sort(children, new Comparator<DirList>() {
               @Override
               public int compare(DirList o1, DirList o2) {
                   String n1 = o1.name;
                   String n2 = o2.name;
                   if (n1 == null) return -1;
                   if (n2 == null) return 1;
                   return n1.compareToIgnoreCase(n2);
               }
           });
           
           for (DirList child : children) {
               // Assign index based on position in the FINAL sorted list
               // (sortedDirs size + 1 because ISO indices are 1-based)
               child.idx = sortedDirs.size() + 1;
               sortedDirs.add(child);
               queue.add(child);
           }
       }
    }
    
    // Replace the original list with the sorted one
    allDirs.clear();
    allDirs.addAll(sortedDirs);

    byte[] pathTableL = generatePathTable(allDirs, false);
    byte[] pathTableM = generatePathTable(allDirs, true);

    int pathTableSize = pathTableL.length;
    int pathTableSectors = (pathTableSize + SECTOR_SIZE - 1) / SECTOR_SIZE;
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: Path tables generated. Size: " + pathTableSize);

    // 5. Calculate Layout
    int lba = 16; // PVD
    lba++; // Terminator
    lba++; // Skip Terminator sector (17) so Path Table starts at 18

    int pathTableL_LBA = lba;
    lba += pathTableSectors;

    int pathTableM_LBA = lba;
    lba += pathTableSectors;

    for (DirList dir : allDirs) {
      dir.lba = lba;
      byte[] content = generateDirectoryContent(dir, allDirs, 0);
      dir.content = content;
      dir.sizeBytes = content.length;
      int sectors = (content.length + SECTOR_SIZE - 1) / SECTOR_SIZE;
      lba += sectors;
    }

    int filesStartLba = lba;

    // 6. Fixup Directory Records
    for (DirList dir : allDirs) {
      dir.content = generateDirectoryContent(dir, allDirs, filesStartLba);
    }
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: Layout calculated. filesStartLba: " + filesStartLba);

    // Add padding at end (aligned to 0x20 sectors like C++ version)
    int volumeSize = filesStartLba + filesSizeSectors;
    int padSectors = 0x20;
    if ((volumeSize & 0x1F) != 0) {
      padSectors += (0x20 - (volumeSize & 0x1F));
    }

    // 7. Build fsBuf
    fsBufSize = filesStartLba * SECTOR_SIZE;
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: Allocating fsBuf: " + fsBufSize);
    fsBuf = ByteBuffer.allocate(fsBufSize);
    fsBuf.order(ByteOrder.LITTLE_ENDIAN);

    Arrays.fill(fsBuf.array(), (byte) 0);

    // Write PS3-specific sectors 0 and 1 if in PS3 mode
    if (ps3Mode) {
      writePS3Sectors(fsBuf, volumeSize + padSectors);
    }

    writePVD(fsBuf, 16 * SECTOR_SIZE, pathTableSize, pathTableL_LBA, pathTableM_LBA, rootList, volumeSize + padSectors);

    // Terminator
    fsBuf.put(17 * SECTOR_SIZE, (byte) 255);
    fsBuf.put(17 * SECTOR_SIZE + 1, (byte) 'C');
    fsBuf.put(17 * SECTOR_SIZE + 2, (byte) 'D');
    fsBuf.put(17 * SECTOR_SIZE + 3, (byte) '0');
    fsBuf.put(17 * SECTOR_SIZE + 4, (byte) '0');
    fsBuf.put(17 * SECTOR_SIZE + 5, (byte) '1');
    fsBuf.put(17 * SECTOR_SIZE + 6, (byte) 1);

    // CRITICAL FIX: Regenerate Path Tables now that LBAs are assigned (Phase 5)
    // The previous generation (Phase 4) used LBA=0 for all dirs.
    pathTableL = generatePathTable(allDirs, false);
    pathTableM = generatePathTable(allDirs, true);

    fsBuf.position(pathTableL_LBA * SECTOR_SIZE);
    fsBuf.put(pathTableL);

    fsBuf.position(pathTableM_LBA * SECTOR_SIZE);
    fsBuf.put(pathTableM);

    for (DirList dir : allDirs) {
      fsBuf.position(dir.lba * SECTOR_SIZE);
      fsBuf.put(dir.content);
    }

    totalSize = (long) (filesStartLba + filesSizeSectors + padSectors) * SECTOR_SIZE;
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: fsBuf populated. totalSize: " + totalSize);

    // Update file entry cached offsets
    long filesAreaStartOffset = (long) filesStartLba * SECTOR_SIZE;
    for (FileEntry f : allFiles) {
      f.startOffset = filesAreaStartOffset + ((long) f.rlba * SECTOR_SIZE);
      f.endOffset = f.startOffset + f.size;
    }
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "VirtualIsoFile: build() complete");
  } catch (IOException e) {
    com.jhonju.ps3netsrv.server.utils.FileLogger.logError(e);
    throw e;
  } catch (Exception e) {
    com.jhonju.ps3netsrv.server.utils.FileLogger.logError(new RuntimeException("Error building VirtualIsoFile", e));
    throw new IOException("Error building VirtualIsoFile", e);
  }
}

  /**
   * Write PS3-specific sectors 0 and 1.
   * Sector 0: DiscRangesSector - unencrypted sector ranges
   * Sector 1: DiscInfoSector - "PlayStation3" + product ID
   */
  private void writePS3Sectors(ByteBuffer bb, int volumeSizeSectors) {
    // Sector 0: DiscRangesSector
    bb.position(0);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putInt(1); // numRanges
    bb.putInt(0); // zero
    // Range 0: entire disc is unencrypted
    bb.putInt(0); // startSector
    bb.putInt(volumeSizeSectors - 1); // endSector

    // Sector 1: DiscInfoSector
    bb.position(SECTOR_SIZE);
    // Console ID: "PlayStation3\0\0\0\0"
    byte[] consoleId = "PlayStation3".getBytes(StandardCharsets.US_ASCII);
    bb.put(consoleId);
    for (int i = consoleId.length; i < 0x10; i++) {
      bb.put((byte) 0);
    }

    // Product ID: "XXXX-XXXXX " format (32 bytes, space-padded)
    byte[] productId = new byte[0x20];
    Arrays.fill(productId, (byte) ' ');
    if (titleId != null && titleId.length() >= 9) {
      // Format: "BCES-00104" -> "BCES-00104 "
      byte[] tid = titleId.getBytes(StandardCharsets.US_ASCII);
      // Copy first 4 chars
      System.arraycopy(tid, 0, productId, 0, Math.min(4, tid.length));
      productId[4] = '-';
      // Copy remaining chars (typically 5)
      if (tid.length > 4) {
        System.arraycopy(tid, 4, productId, 5, Math.min(tid.length - 4, 5));
      }
    }
    bb.put(productId);

    // Zeros (0x10 bytes)
    for (int i = 0; i < 0x10; i++) {
      bb.put((byte) 0);
    }

    // Info (0x1B0 bytes) - random data
    byte[] info = new byte[0x1B0];
    new SecureRandom().nextBytes(info);
    bb.put(info);

    // Hash (0x10 bytes) - random data (we don't compute real hash)
    byte[] hash = new byte[0x10];
    new SecureRandom().nextBytes(hash);
    bb.put(hash);

    bb.order(ByteOrder.LITTLE_ENDIAN);
  }

  /*
   * Abstracted scanDirectory to use IFile with DocumentFile optimization
   */
  private void scanDirectory(IFile dir, DirList dirEntry, List<DirList> allDirs) throws IOException {
    // Optimization for DocumentFileCustom to avoid slow listFiles()
    if (dir instanceof DocumentFileCustom) {
      scanDirectoryOptimized((DocumentFileCustom) dir, dirEntry, allDirs);
      return;
    }

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "scanDirectory: " + (dir != null ? dir.getName() : "null"));
    IFile[] files = dir.listFiles();
    if (files == null) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "scanDirectory: listFiles returned null for " + (dir != null ? dir.getName() : "null"));
      return;
    }
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "scanDirectory: found " + files.length + " files in " + dir.getName());

    Arrays.sort(files, new Comparator<IFile>() {
      @Override
      public int compare(IFile o1, IFile o2) {
        String n1 = o1.getName();
        String n2 = o2.getName();
        if (n1 == null)
          return -1;
        if (n2 == null)
          return 1;
        return n1.compareToIgnoreCase(n2);
      }
    });

    // Track multipart base names to avoid duplicates
    List<String> processedMultiparts = new ArrayList<>();

    for (IFile f : files) {
      processFile(f, f.getName(), dir, dirEntry, allDirs, processedMultiparts);
    }
  }

  private void processFile(IFile f, String name, IFile dir, DirList dirEntry, List<DirList> allDirs, List<String> processedMultiparts) throws IOException {
      if (name == null)
        return;

      if (f.isDirectory()) {
        DirList child = new DirList();
        child.name = name;
        child.parent = dirEntry;
        allDirs.add(child);
        scanDirectory(f, child, allDirs);
      } else {
        // Check for multipart files (.66600, .66601, etc.)
        if (isMultipartFile(name)) {
          // Skip non-first parts (.66601, .66602, etc.)
          if (!name.endsWith(MULTIPART_SUFFIX_PATTERN)) {
            return;
          }

          // This is the first part (.66600), process the whole set
          String baseName = name.substring(0, name.length() - 6); // Remove ".66600"
          if (processedMultiparts.contains(baseName)) {
            return;
          }
          processedMultiparts.add(baseName);

          FileEntry fe = createMultipartFileEntry(dir, baseName, f);
          if (fe != null) {
            dirEntry.files.add(fe);
          }
        } else {
          // Regular single file
          FileEntry fe = new FileEntry();
          fe.name = name;
          fe.size = f.length();
          fe.fileParts.add(f);
          fe.isMultipart = false;
          dirEntry.files.add(fe);
        }
      }
  }

  // Visited set prevents infinite loops
  private java.util.Set<String> visitedDocIds;

  private void scanDirectoryOptimized(DocumentFileCustom dir, DirList dirEntry, List<DirList> allDirs) throws IOException {
    if (visitedDocIds == null) {
      visitedDocIds = new java.util.HashSet<>();
    }
    
    // We assume dir.documentFile is a TreeDocumentFile
    android.net.Uri currentUri = dir.documentFile.getUri();
    String dirDocId = android.provider.DocumentsContract.getDocumentId(currentUri);
    
    if (visitedDocIds.contains(dirDocId)) {
        com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "scanDirectoryOptimized: Cycle detected for " + dir.getName() + " (" + dirDocId + ")");
        return;
    }
    visitedDocIds.add(dirDocId);

    // com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "scanDirectoryOptimized: Scanning " + dir.getName());
    
    android.content.Context context = com.jhonju.ps3netsrv.app.PS3NetSrvApp.getAppContext();
    android.content.ContentResolver resolver = context.getContentResolver();
    
    // Use the current URI as the tree base. 
    // DocumentFile.listFiles() uses its own mUri as the treeUri argument.
    android.net.Uri childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
        currentUri, 
        dirDocId
    );

    List<IFile> filesList = new ArrayList<>();
    
    String[] projection = new String[] {
        android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
        android.provider.DocumentsContract.Document.COLUMN_SIZE
    };

    try (android.database.Cursor c = resolver.query(childrenUri, projection, null, null, null)) {
      if (c != null) {
        while (c.moveToNext()) {
          String docId = c.getString(0);
          String name = c.getString(1);
          String mimeType = c.getString(2);
          long size = c.getLong(3);
          
          if (docId == null || name == null) continue;
           
          boolean isDir = android.provider.DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
          
          // Construct child URI using the CURRENT URI as tree base.
          // This ensures we stay within the same tree structure.
          android.net.Uri docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(currentUri, docId);
          
          androidx.documentfile.provider.DocumentFile docFile;
          // Use fromSingleUri even for directories because we are manually traversing them anyway.
          // fromTreeUri seems to act weirdly with nested tree URIs, resetting them to root.
          docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, docUri);
          
          if (docFile != null) {
              // Use lazy constructor to avoid I/O
              filesList.add(new DocumentFileCustom(docFile, resolver, name, size, isDir));
          }
        }
      }
    } catch (Exception e) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logError(new RuntimeException("Error in scanDirectoryOptimized", e));
    }

    // Sort files
    Collections.sort(filesList, new Comparator<IFile>() {
      @Override
      public int compare(IFile o1, IFile o2) {
        String n1 = o1.getName();
        String n2 = o2.getName();
        return (n1 == null) ? -1 : (n2 == null) ? 1 : n1.compareToIgnoreCase(n2);
      }
    });

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "scanDirectoryOptimized: found " + filesList.size() + " items in " + dir.getName());
    
    // Track multipart base names
    List<String> processedMultiparts = new ArrayList<>();

    for (IFile f : filesList) {
       processFile(f, f.getName(), dir, dirEntry, allDirs, processedMultiparts);
    }
  }

  /**
   * Check if a filename matches the multipart pattern (.666XX)
   */
  private boolean isMultipartFile(String name) {
    if (name == null || name.length() < 7) {
      return false;
    }
    // Check for .666XX pattern (XX = 00-99)
    int dotPos = name.length() - 6;
    if (name.charAt(dotPos) != '.' ||
        name.charAt(dotPos + 1) != '6' ||
        name.charAt(dotPos + 2) != '6' ||
        name.charAt(dotPos + 3) != '6') {
      return false;
    }
    char d1 = name.charAt(dotPos + 4);
    char d2 = name.charAt(dotPos + 5);
    return Character.isDigit(d1) && Character.isDigit(d2);
  }

  /**
   * Create a FileEntry for multipart files, merging all parts (.66600, .66601,
   * etc.)
   */
  private FileEntry createMultipartFileEntry(IFile dir, String baseName, IFile firstPart) throws IOException {
    FileEntry fe = new FileEntry();
    fe.name = baseName;
    fe.isMultipart = true;
    fe.size = 0;

    // Add first part
    fe.fileParts.add(firstPart);
    fe.size += firstPart.length();

    // Find and add subsequent parts
    for (int i = 1; i < 100; i++) {
      String partName = baseName + String.format(".666%02d", i);
      IFile part = dir.findFile(partName);
      if (part == null || !part.exists() || !part.isFile()) {
        break;
      }
      fe.fileParts.add(part);
      fe.size += part.length();
    }

    return fe;
  }

  private int getDepth(DirList d) {
    int depth = 0;
    while (d != d.parent) {
      depth++;
      d = d.parent;
    }
    return depth;
  }

  private byte[] generatePathTable(List<DirList> dirs, boolean msb) {
    ByteBuffer bb = ByteBuffer.allocate(dirs.size() * PATH_TABLE_ENTRY_ESTIMATE);
    bb.order(msb ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

    for (DirList d : dirs) {
      String name = d.name;
      if (d == rootList)
        name = "\0";
      else
        name = name.toUpperCase(java.util.Locale.US);

      int len_di = name.length();
      if (d == rootList)
        len_di = 1;

      bb.put((byte) len_di);
      bb.put((byte) 0);
      bb.putInt(d.lba);

      short parentIdx = (short) d.parent.idx;
      if (d == rootList)
        parentIdx = 1;
      bb.putShort(parentIdx);

      if (d == rootList)
        bb.put((byte) 0);
      else
        // Enforce US Locale
        bb.put(name.toUpperCase(java.util.Locale.US).getBytes(StandardCharsets.US_ASCII));

      if (len_di % 2 != 0)
        bb.put((byte) 0);
    }
    byte[] res = new byte[bb.position()];
    bb.position(0);
    bb.get(res);
    return res;
  }

  private byte[] generateDirectoryContent(DirList dir, List<DirList> allDirs, int filesStartLba) {
    List<DirList> subDirs = new ArrayList<>();
    for (DirList d : allDirs) {
      if (d.parent == dir && d != dir && d != rootList)
        subDirs.add(d);
    }

    ByteBuffer bb = ByteBuffer.allocate(MAX_DIRECTORY_BUFFER_SIZE);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    writeDirRecord(bb, dir, ".", 0);
    writeDirRecord(bb, dir.parent, "..", 0);

    // ISO 9660 requires ALL entries (files and directories) to be sorted alphabetically by name.
    // We must merge subdirectories and files into a single list and sort them.
    List<Object> entries = new ArrayList<>();
    entries.addAll(subDirs);
    entries.addAll(dir.files);

    Collections.sort(entries, new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            String n1 = getName(o1);
            String n2 = getName(o2);
            if (n1 == null) return -1;
            if (n2 == null) return 1;
            // Enforce US Locale for sorting
            return n1.toUpperCase(java.util.Locale.US).compareTo(n2.toUpperCase(java.util.Locale.US));
        }

        private String getName(Object o) {
            if (o instanceof DirList) return ((DirList) o).name;
            if (o instanceof FileEntry) return ((FileEntry) o).name;
            return "";
        }
    });

    for (Object entry : entries) {
        String name = getName(entry);
        if (entry instanceof DirList) {
            DirList sub = (DirList) entry;
            if (dir.name.equalsIgnoreCase("PS3_GAME") || dir.name.equalsIgnoreCase("USRDIR") || dir.name.equals("")) {
                 com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", 
                     String.format(java.util.Locale.US, "DIR_CONTENT [%s]: folder=%s LBA=%d", dir.name, name, sub.lba));
            }
            writeDirRecord(bb, sub, sub.name, 0);
        } else if (entry instanceof FileEntry) {
            FileEntry f = (FileEntry) entry;
            if (dir.name.equalsIgnoreCase("PS3_GAME") || dir.name.equalsIgnoreCase("USRDIR") || dir.name.equals("")) {
                 com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", 
                     String.format(java.util.Locale.US, "DIR_CONTENT [%s]: file=%s LBA=%d size=%d", dir.name, name, filesStartLba + f.rlba, f.size));
            }
            writeFileRecord(bb, f, filesStartLba);
        }
    }

    byte[] res = new byte[bb.position()];
    bb.position(0);
    bb.get(res);
    return res;
  }

  private void writeDirRecord(ByteBuffer bb, DirList target, String name, int flags) {
    // ISO 9660: '.' and '..' are 1 byte (0x00 and 0x01).
    int nameLen = (name.equals(".") || name.equals("..")) ? 1 : name.length();
    
    int recordLen = 33 + nameLen;
    if (recordLen % 2 != 0)
      recordLen++;

    int pos = bb.position();
    if ((pos % SECTOR_SIZE) + recordLen > (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE)) {
      int pad = (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE) - pos;
      for (int i = 0; i < pad; i++)
        bb.put((byte) 0);
    }

    bb.put((byte) recordLen);
    bb.put((byte) 0);

    int lba = target.lba;
    putBothEndianInt(bb, lba);

    int size = target.sizeBytes;
    putBothEndianInt(bb, size);

    putDate(bb);

    bb.put((byte) 2);
    bb.put((byte) 0);
    bb.put((byte) 0);

    putBothEndianShort(bb, (short) 1);

    bb.put((byte) nameLen);

    if (name.equals("."))
      bb.put((byte) 0);
    else if (name.equals(".."))
      bb.put((byte) 1);
    else
      // Enforce US ASCII for consistency
      bb.put(name.toUpperCase(java.util.Locale.US).getBytes(StandardCharsets.US_ASCII));

    if ((33 + nameLen) % 2 != 0)
      bb.put((byte) 0);
  }

  private void writeFileRecord(ByteBuffer bb, FileEntry f, int filesStartLba) {
    // For multi-extent files (>4GB), we need to write multiple records
    if (f.extentParts > 1) {
      writeMultiExtentFileRecords(bb, f, filesStartLba);
      return;
    }

    // Standard single-extent file record
    String name = f.name;
    int nameLen = name.length() + 2;
    int recordLen = 33 + nameLen;
    if (recordLen % 2 != 0)
      recordLen++;

    int pos = bb.position();
    if ((pos % SECTOR_SIZE) + recordLen > (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE)) {
      int pad = (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE) - pos;
      for (int i = 0; i < pad; i++)
        bb.put((byte) 0);
    }

    bb.put((byte) recordLen);
    bb.put((byte) 0);

    int lba = filesStartLba + f.rlba;
    
    // DEBUG: Log LBA for EBOOT.BIN
    if (f.name.equalsIgnoreCase("EBOOT.BIN")) {
        com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", 
            String.format(java.util.Locale.US, "writeFileRecord EBOOT.BIN: rlba=%d, filesStartLba=%d, finalLba=%d (0x%X), size=%d", 
                f.rlba, filesStartLba, lba, lba * 2048L, f.size));
    }
    
    putBothEndianInt(bb, lba);

    // Use int cast for size (works for files <= 4GB)
    putBothEndianInt(bb, (int) f.size);

    putDate(bb);

    bb.put(ISO_FILE);
    bb.put((byte) 0);
    bb.put((byte) 0);
    putBothEndianShort(bb, (short) 1);

    bb.put((byte) nameLen);
    bb.put(name.toUpperCase().getBytes(StandardCharsets.US_ASCII));
    bb.put((byte) ';');
    bb.put((byte) '1');

    if ((33 + nameLen) % 2 != 0)
      bb.put((byte) 0);
  }

  /**
   * Write multiple directory records for a multi-extent file (>4GB).
   * Each extent is limited to MULTIEXTENT_PART_SIZE bytes.
   */
  private void writeMultiExtentFileRecords(ByteBuffer bb, FileEntry f, int filesStartLba) {
    String name = f.name;
    int nameLen = name.length() + 2;
    int recordLen = 33 + nameLen;
    if (recordLen % 2 != 0)
      recordLen++;

    int lba = filesStartLba + f.rlba;
    long remainingSize = f.size;

    for (int part = 0; part < f.extentParts; part++) {
      int pos = bb.position();
      if ((pos % SECTOR_SIZE) + recordLen > (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE)) {
        int pad = (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE) - pos;
        for (int i = 0; i < pad; i++)
          bb.put((byte) 0);
      }

      bb.put((byte) recordLen);
      bb.put((byte) 0);

      putBothEndianInt(bb, lba);

      // Calculate size for this extent
      long extentSize;
      byte fileFlags;
      if (part == f.extentParts - 1) {
        // Last extent - use remaining size and normal file flag
        extentSize = remainingSize;
        fileFlags = ISO_FILE;
      } else {
        // Not last extent - use max extent size and multi-extent flag
        extentSize = MULTIEXTENT_PART_SIZE;
        fileFlags = ISO_MULTIEXTENT;
      }

      putBothEndianInt(bb, (int) extentSize);

      putDate(bb);

      bb.put(fileFlags);
      bb.put((byte) 0);
      bb.put((byte) 0);
      putBothEndianShort(bb, (short) 1);

      bb.put((byte) nameLen);
      bb.put(name.toUpperCase().getBytes(StandardCharsets.US_ASCII));
      bb.put((byte) ';');
      bb.put((byte) '1');

      if ((33 + nameLen) % 2 != 0)
        bb.put((byte) 0);

      // Update for next extent
      lba += (int) ((extentSize + SECTOR_SIZE - 1) / SECTOR_SIZE);
      remainingSize -= extentSize;
    }
  }

  // Helpers
  private void putBothEndianInt(ByteBuffer bb, int val) {
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(val);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putInt(val);
    bb.order(ByteOrder.LITTLE_ENDIAN);
  }

  private void putBothEndianShort(ByteBuffer bb, short val) {
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putShort(val);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putShort(val);
    bb.order(ByteOrder.LITTLE_ENDIAN);
  }

  private void putDate(ByteBuffer bb) {
    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    bb.put((byte) (c.get(Calendar.YEAR) - 1900));
    bb.put((byte) (c.get(Calendar.MONTH) + 1));
    bb.put((byte) c.get(Calendar.DAY_OF_MONTH));
    bb.put((byte) c.get(Calendar.HOUR_OF_DAY));
    bb.put((byte) c.get(Calendar.MINUTE));
    bb.put((byte) c.get(Calendar.SECOND));
    bb.put((byte) 0);
  }

  private void writePVD(ByteBuffer bb, int offset, int pathTableSize, int ptL, int ptM, DirList root,
      int volumeSizeSectors) {
    bb.position(offset);
    bb.put((byte) 1);
    bb.put("CD001".getBytes(StandardCharsets.US_ASCII));
    bb.put((byte) 1);
    bb.put((byte) 0);

    pad(bb, 32);

    byte[] volBytes = volumeName.getBytes(StandardCharsets.US_ASCII);
    bb.put(volBytes, 0, Math.min(volBytes.length, 32));
    pad(bb, 32 - Math.min(volBytes.length, 32));

    pad(bb, 8);

    putBothEndianInt(bb, volumeSizeSectors);

    pad(bb, 32);

    putBothEndianShort(bb, (short) 1);
    putBothEndianShort(bb, (short) 1);

    putBothEndianShort(bb, (short) SECTOR_SIZE);

    putBothEndianInt(bb, pathTableSize);

    bb.putInt(ptL);
    bb.putInt(0);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putInt(ptM);
    bb.putInt(0);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    ByteBuffer temp = ByteBuffer.allocate(256);
    temp.order(ByteOrder.LITTLE_ENDIAN);
    writeDirRecord(temp, root, ".", 0);
    byte[] rootRecord = new byte[temp.position()];
    temp.position(0);
    temp.get(rootRecord);
    bb.put(rootRecord);

    pad(bb, 2048 - bb.position() + offset);
  }

  private void pad(ByteBuffer bb, int count) {
    for (int i = 0; i < count; i++)
      bb.put((byte) 0);
  }

  /**
   * Binary search to find the file entry containing the given position.
   * Returns the index of the file entry, or -1 if not found in file data area.
   */
  private int findFileEntryIndex(long position) {
    if (allFiles == null || allFiles.isEmpty()) {
      return -1;
    }

    int low = 0;
    int high = allFiles.size() - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      FileEntry entry = allFiles.get(mid);
      long fileAreaEnd = entry.startOffset + ((entry.size + SECTOR_SIZE - 1) / SECTOR_SIZE) * SECTOR_SIZE;

      if (position < entry.startOffset) {
        high = mid - 1;
      } else if (position >= fileAreaEnd) {
        low = mid + 1;
      } else {
        // Position is within this file's allocated area
        return mid;
      }
    }

    return -1;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean delete() {
    return false;
  }

  @Override
  public long length() {
    return totalSize;
  }

  @Override
  public IFile[] listFiles() throws IOException {
    return null;
  }

  @Override
  public long lastModified() {
    return rootFile.lastModified();
  }

  @Override
  public String getName() {
    return rootFile.getName() + ".iso";
  }

  @Override
  public String[] list() {
    return null;
  }

  @Override
  public IFile findFile(String fileName) throws IOException {
    return null;
  }

  @Override
  public int read(byte[] buffer, long position) throws IOException {
    long remaining = buffer.length;
    int r = 0;
    int bufOffset = 0;

    if (position >= totalSize)
      return 0;

    // 1. Read from metadata buffer (thread-safe)
    if (position < fsBufSize) {
      int toRead = (int) Math.min(fsBufSize - position, remaining);
      // Safety check
      if (position + toRead > fsBuf.capacity())
        toRead = fsBuf.capacity() - (int) position;

      // Thread-safe read using synchronization
      synchronized (fsBufLock) {
        fsBuf.position((int) position);
        fsBuf.get(buffer, bufOffset, toRead);
      }

      remaining -= toRead;
      r += toRead;
      bufOffset += toRead;
      position += toRead;
    }

    if (remaining == 0 || position >= totalSize)
      return r;

    // 2. Read from files using binary search
    while (remaining > 0 && position < totalSize) {
      int fileIdx = findFileEntryIndex(position);
      if (fileIdx < 0) {
        // In padding area at end - return zeros
        int toRead = (int) Math.min(totalSize - position, remaining);
        Arrays.fill(buffer, bufOffset, bufOffset + toRead, (byte) 0);
        r += toRead;
        break;
      }

      FileEntry f = allFiles.get(fileIdx);
      long fileAreaEnd = f.startOffset + ((f.size + SECTOR_SIZE - 1) / SECTOR_SIZE) * SECTOR_SIZE;

      // DEBUG: Log ALL matches in the file area to track PS3 progress
      if (position == f.startOffset || position % (64 * 1024) == 0) {
          com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", 
              String.format(java.util.Locale.US, "FILE_MATCH: pos=0x%X -> %s (start=0x%X end=0x%X size=%d)", 
                  position, f.name, f.startOffset, f.endOffset, f.size));
      }

      // In this file's allocated area
      if (position < f.endOffset) {
        // Real data - read from file parts
        long offsetInFile = position - f.startOffset;
        int toRead = (int) Math.min(f.endOffset - position, remaining);

        int readCount;
        if (f.isMultipart) {
          readCount = readFromMultipartFile(f, offsetInFile, buffer, bufOffset, toRead);
        } else {
          // Single file - read directly
          byte[] readTarget = new byte[toRead];
          readCount = f.fileParts.get(0).read(readTarget, offsetInFile);
          if (readCount > 0) {
            System.arraycopy(readTarget, 0, buffer, bufOffset, readCount);
            
            // DIAGNOSTIC LOGGING FOR CRITICAL FILES
            if (f.name.equalsIgnoreCase("PARAM.SFO") || f.name.equalsIgnoreCase("EBOOT.BIN") || f.name.endsWith(".BIN")) {
                String hexSnippet = "";
                if (readCount > 0) {
                    int dumpLen = Math.min(readCount, 16);
                    StringBuilder sb = new StringBuilder();
                    for (int i=0; i<dumpLen; i++) sb.append(String.format("%02X ", readTarget[i]));
                    hexSnippet = sb.toString();
                }
                com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", 
                    String.format(java.util.Locale.US, "READ %s: off=%d len=%d read=%d | Data: %s", f.name, offsetInFile, toRead, readCount, hexSnippet));
            }
          }
        }

        if (readCount > 0) {
          r += readCount;
          remaining -= readCount;
          bufOffset += readCount;
          position += readCount;
        } else {
          // EOF or Read Error - break to avoid infinite loop
          if (readCount < 0) {
             com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "READ EOF/Error: file=" + f.name + " off=" + offsetInFile);
          }
          break;
        }
      }

      // Padding (zeros after file content to sector boundary)
      if (remaining > 0 && position >= f.endOffset && position < fileAreaEnd) {
        int pad = (int) Math.min(fileAreaEnd - position, remaining);
        Arrays.fill(buffer, bufOffset, bufOffset + pad, (byte) 0);
        r += pad;
        remaining -= pad;
        bufOffset += pad;
        position += pad;
      }
    }

    return r;
  }

  /**
   * Read from a multipart file (.66600, .66601, etc.), handling boundaries.
   */
  private int readFromMultipartFile(FileEntry f, long offsetInFile, byte[] buffer, int bufOffset, int toRead)
      throws IOException {
    int totalRead = 0;
    long currentOffset = offsetInFile;
    int currentBufOffset = bufOffset;
    int remainingToRead = toRead;

    // Find first part containing the offset
    long partStartOffset = 0;
    for (int partIdx = 0; partIdx < f.fileParts.size() && remainingToRead > 0; partIdx++) {
      IFile part = f.fileParts.get(partIdx);
      long partSize = part.length();
      long partEndOffset = partStartOffset + partSize;

      if (currentOffset >= partStartOffset && currentOffset < partEndOffset) {
        // Read from this part
        long offsetInPart = currentOffset - partStartOffset;
        int bytesToReadFromPart = (int) Math.min(partEndOffset - currentOffset, remainingToRead);

        byte[] readTarget = new byte[bytesToReadFromPart];
        int readCount = part.read(readTarget, offsetInPart);

        if (readCount > 0) {
          System.arraycopy(readTarget, 0, buffer, currentBufOffset, readCount);
          totalRead += readCount;
          currentOffset += readCount;
          currentBufOffset += readCount;
          remainingToRead -= readCount;
        } else {
          break; // Read error
        }
      }

      partStartOffset = partEndOffset;
    }

    return totalRead;
  }

  @Override
  public void close() throws IOException {
    synchronized (fsBufLock) {
      fsBuf = null;
    }

    // Close all nested file handles to prevent resource leaks
    if (allFiles != null) {
      for (FileEntry f : allFiles) {
        if (f.fileParts != null) {
          for (IFile part : f.fileParts) {
            try {
              part.close();
            } catch (IOException e) {
              // Log but continue closing other files
            }
          }
        }
      }
      allFiles = null;
    }
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    throw new IOException("ReadOnly");
  }

  @Override
  public boolean createDirectory(String name) {
    return false;
  }

  @Override
  public boolean createFile(String name) {
    return false;
  }
}
