package com.jhonju.ps3netsrv.server.io;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * VirtualIsoFile constructs and serves a virtual ISO 9660 filesystem image
 * on-the-fly from a normal directory structure. It acts as an IFile
 * implementation,
 * rendering transparent ISO sector streams upon read requests and fully
 * supporting
 * multipart or segmented content mapping.
 */
public class VirtualIsoFile implements IFile {

  private static final int SECTOR_SIZE = 2048;

  private static final int PATH_TABLE_ENTRY_ESTIMATE = 32;

  // Multi-extent support - max size per extent (~4GB, sector-aligned)
  private static final long MULTIEXTENT_PART_SIZE = 0xFFFFF800L;

  // ISO 9660 file flags
  private static final byte ISO_FILE = 0x00;

  private static final byte ISO_MULTIEXTENT = (byte) 0x80;

  // Multipart file pattern (.66600, .66601, etc.)
  private static final String MULTIPART_SUFFIX_PATTERN = ".66600";

  private final IFile rootFile;

  private final android.content.Context context;
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

  /**
   * Represents a mapped file within the virtual ISO filesystem.
   * Encapsulates physical file details, tracking multipart components where
   * applicable,
   * alongside logical block addressing (rlba) properties.
   */
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

  /**
   * Represents a directory list within the ISO 9660 hierarchy.
   * Tracks structural tree pointers such as parent nodes, hierarchy index,
   * LBA positions, and its internal lists of child file entries.
   */
  private static class DirList {
    String name;
    DirList parent;
    int idx;
    int lba;
    int sizeBytes;
    byte[] content;
    List<FileEntry> files = new ArrayList<>();
  }

  public VirtualIsoFile(IFile rootDir, android.content.Context context) throws IOException {
    if (rootDir == null) {
      throw new IOException("root dir should not be null");
    }
    this.context = context;
    this.rootFile = rootDir;

    // Detect PS3 mode by checking for PS3_GAME/PARAM.SFO
    String detectedTitleId = ParamSfoParser.getTitleId(rootDir);
    this.ps3Mode = (detectedTitleId != null);
    this.titleId = detectedTitleId;

    if (ps3Mode) {
      this.volumeName = "PS3VOLUME";
    } else {
      this.volumeName = rootDir.getName() != null ? rootDir.getName().toUpperCase() : "DVDVIDEO";
    }
    build();
  }

  private void build() throws IOException {
    allFiles = new ArrayList<>();
    rootList = new DirList();
    rootList.name = "";
    rootList.parent = rootList;
    rootList.idx = 1;

    List<DirList> allDirs = new ArrayList<>();
    allDirs.add(rootList);

    scanDirectory(rootFile, rootList, allDirs);

    // 1.5. Map parents to children for DFS/BFS traversals
    // This is needed for both DFS file layout (Phase 2) and BFS Path Table (Phase
    // 4)
    Map<DirList, List<DirList>> childrenMap = new HashMap<>();
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

    // 2. Calculate sizes and assign Relative LBAs for files
    // Use DFS Traversal to match C++ makeps3iso behavior (recursive walk)
    // This ensures physical file order matches the reference implementation.
    // NOTE: passing 'filesStartLba' not possible here as it is not calculated yet.
    // However, rlba must be aligned relative to filesStartLba.
    // We can assume filesStartLba will be aligned to 32 sectors (Phase 5).
    // So if we align rlba to 32 sectors, the absolute lba will also be aligned.
    int currentFileSectorOffset = 0;
    allFiles = new ArrayList<>();
    currentFileSectorOffset = scanFilesDFS(rootList, currentFileSectorOffset, allFiles, childrenMap);

    int filesSizeSectors = currentFileSectorOffset;

    // 4. Generate Path Tables

    // ISO 9660 requires Path Table to be sorted by:
    // 1. Parent Directory Number (Ascending)
    // 2. Directory Identifier (Ascending)
    // To achieve this, we must traverse the directory tree in Level Order (BFS),
    // and sort siblings by name.

    // childrenMap is already built in Phase 1.5

    // Now perform BFS to rebuild allDirs in correct order
    List<DirList> sortedDirs = new ArrayList<>();

    // Queue for BFS
    List<DirList> queue = new ArrayList<>();
    queue.add(rootList);

    // Initial root setup
    rootList.idx = 1;
    sortedDirs.add(rootList);

    // We use a simple pointer instead of actual Queue to avoid auto-boxing
    // overhead/complexity
    int queuePtr = 0;
    while (queuePtr < queue.size()) {
      DirList parent = queue.get(queuePtr++);

      List<DirList> children = childrenMap.get(parent);
      if (children != null) {
        // Sort siblings by name
        Collections.sort(children, (o1, o2) -> {
          String n1 = o1.name;
          String n2 = o2.name;
          if (n1 == null)
            return -1;
          if (n2 == null)
            return 1;
          return n1.compareToIgnoreCase(n2);
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
    //byte[] pathTableM = generatePathTable(allDirs, true);
    byte[] pathTableM;

    int pathTableSize = pathTableL.length;
    int pathTableSectors = (pathTableSize + SECTOR_SIZE - 1) / SECTOR_SIZE;

    // 5. Calculate Layout
    // PS3 ISO Layout (based on makeps3iso.c):
    // 0-15: System Area / Unused
    // 16: PVD
    // 17-19: Unused / Terminator (17)
    // 20: Path Table L
    // ..: Path Table M
    // >=32: Directories

    int lba = 20; // Force Path Table L at LBA 20
    int pathTableL_LBA = lba;
    lba += pathTableSectors;

    // Path Table M follows immediately
    int pathTableM_LBA = lba;
    lba += pathTableSectors;

    // Directories start after Path Tables, but at least at LBA 32
    if (lba < 32) {
      lba = 32;
    }

    for (DirList dir : allDirs) {
      dir.lba = lba;
      byte[] content = generateDirectoryContent(dir, allDirs, 0);
      dir.content = content;
      dir.sizeBytes = content.length;
      int sectors = (content.length + SECTOR_SIZE - 1) / SECTOR_SIZE;
      lba += sectors;
    }

    int filesStartLba = lba;

    // 7. Fixup Directory Records
    for (DirList dir : allDirs) {
      dir.content = generateDirectoryContent(dir, allDirs, filesStartLba);
    }

    int volumeSize = filesStartLba + filesSizeSectors;
    int padSectors = 0x20;
    if ((volumeSize & 0x1F) != 0) {
      padSectors += (0x20 - (volumeSize & 0x1F));
    }

    // Ensure total size is a multiple of 32
    int finalVolumeSize = volumeSize + padSectors;
    if ((finalVolumeSize & 0x1F) != 0) {
      finalVolumeSize = (finalVolumeSize + 0x1F) & ~0x1F;
    }

    // 7. Build fsBuf
    fsBufSize = filesStartLba * SECTOR_SIZE;
    fsBuf = ByteBuffer.allocate(fsBufSize);
    fsBuf.order(ByteOrder.LITTLE_ENDIAN);

    Arrays.fill(fsBuf.array(), (byte) 0);

    // Write PS3-specific sectors 0 and 1 if in PS3 mode
    if (ps3Mode) {
      writePS3Sectors(fsBuf, finalVolumeSize);
    }

    writePVD(fsBuf, pathTableSize, pathTableL_LBA, pathTableM_LBA, rootList, finalVolumeSize);

    // Terminator
    fsBuf.put(17 * SECTOR_SIZE, (byte) 255);
    fsBuf.put(17 * SECTOR_SIZE + 1, (byte) 'C');
    fsBuf.put(17 * SECTOR_SIZE + 2, (byte) 'D');
    fsBuf.put(17 * SECTOR_SIZE + 3, (byte) '0');
    fsBuf.put(17 * SECTOR_SIZE + 4, (byte) '0');
    fsBuf.put(17 * SECTOR_SIZE + 5, (byte) '1');
    fsBuf.put(17 * SECTOR_SIZE + 6, (byte) 1);

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

    totalSize = (long) finalVolumeSize * SECTOR_SIZE;

    // Update file entry cached offsets
    long filesAreaStartOffset = (long) filesStartLba * SECTOR_SIZE;
    for (FileEntry f : allFiles) {
      f.startOffset = filesAreaStartOffset + ((long) f.rlba * SECTOR_SIZE);
      f.endOffset = f.startOffset + f.size;
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      scanDirectoryOptimized((DocumentFileCustom) dir, dirEntry, allDirs);
      return;
    }

    IFile[] files = dir.listFiles();
    if (files == null) {
      return;
    }

    Arrays.sort(files, (o1, o2) -> {
      String n1 = o1.getName();
      String n2 = o2.getName();
      if (n1 == null)
        return -1;
      if (n2 == null)
        return 1;
      return n1.compareToIgnoreCase(n2);
    });

    // Track multipart base names to avoid duplicates
    List<String> processedMultiparts = new ArrayList<>();

    for (IFile f : files) {
      processFile(f, f.getName(), dir, dirEntry, allDirs, processedMultiparts);
    }
  }

  private void processFile(IFile f, String name, IFile dir, DirList dirEntry, List<DirList> allDirs,
      List<String> processedMultiparts) throws IOException {
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
        dirEntry.files.add(fe);
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

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void scanDirectoryOptimized(DocumentFileCustom dir, DirList dirEntry, List<DirList> allDirs)
      throws IOException {
    if (visitedDocIds == null) {
      visitedDocIds = new java.util.HashSet<>();
    }

    // We assume dir.documentFile is a TreeDocumentFile
    android.net.Uri currentUri = dir.documentFile.getUri();
    String dirDocId = android.provider.DocumentsContract.getDocumentId(currentUri);

    if (visitedDocIds.contains(dirDocId)) {
      return;
    }
    visitedDocIds.add(dirDocId);

    android.content.ContentResolver resolver = context.getContentResolver();

    // Use the current URI as the tree base.
    // DocumentFile.listFiles() uses its own mUri as the treeUri argument.
    android.net.Uri childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
        currentUri,
        dirDocId);

    List<IFile> filesList = new ArrayList<>();

    String[] projection = new String[] {
        android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
        android.provider.DocumentsContract.Document.COLUMN_SIZE
    };

    android.database.Cursor c = resolver.query(childrenUri, projection, null, null, null);
    try {
      if (c != null) {
        while (c.moveToNext()) {
          String docId = c.getString(0);
          String name = c.getString(1);
          String mimeType = c.getString(2);
          long size = c.getLong(3);

          if (docId == null || name == null)
            continue;

          boolean isDir = android.provider.DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);

          // Construct child URI using the CURRENT URI as tree base.
          // This ensures we stay within the same tree structure.
          android.net.Uri docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(currentUri, docId);

          androidx.documentfile.provider.DocumentFile docFile;
          // Use fromSingleUri even for directories because we are manually traversing
          // them anyway.
          // fromTreeUri seems to act weirdly with nested tree URIs, resetting them to
          // root.
          docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, docUri);

          if (docFile != null) {
            // Use lazy constructor to avoid I/O
            filesList.add(new DocumentFileCustom(docFile, resolver, context, name, size, isDir));
          }
        }
      }
    } catch (Exception e) {
      FileLogger.logError("Error in scanDirectoryOptimized", e);
    } finally {
      if (c != null) {
        c.close();
      }
    }

    // Sort files
    Collections.sort(filesList, (o1, o2) -> {
      String n1 = o1.getName();
      String n2 = o2.getName();
      return (n1 == null) ? -1 : (n2 == null) ? 1 : n1.compareToIgnoreCase(n2);
    });

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
      String partName = baseName + String.format(Locale.US, ".666%02d", i);
      IFile part = dir.findFile(partName);
      if (part == null || !part.exists() || !part.isFile()) {
        break;
      }
      fe.fileParts.add(part);
      fe.size += part.length();
    }

    return fe;
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
    ByteBuffer bb = ByteBuffer.allocate(BinaryUtils.BUFFER_SIZE);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    writeDirRecord(bb, dir, ".");
    writeDirRecord(bb, dir.parent, "..");

    // ISO 9660: Strict Alphabetical Order (Files and Dirs mixed)
    // PS3 likely relies on this for binary search.
    // Previous "Files First" attempt violated this for EBOOT.BIN (E) vs DOWNLOADED
    // (D).

    List<Object> allEntries = new ArrayList<>(dir.files);
    for (DirList d : allDirs) {
      if (d.parent == dir && d != dir && d != rootList)
        allEntries.add(d);
    }

    Collections.sort(allEntries, (o1, o2) -> {
      String n1 = (o1 instanceof FileEntry) ? ((FileEntry) o1).name : ((DirList) o1).name;
      String n2 = (o2 instanceof FileEntry) ? ((FileEntry) o2).name : ((DirList) o2).name;
      return n1.toUpperCase(Locale.US).compareTo(n2.toUpperCase(Locale.US));
    });

    for (Object o : allEntries) {
      if (o instanceof FileEntry) {
        FileEntry f = (FileEntry) o;
        writeFileRecord(bb, f, filesStartLba);
      } else {
        DirList sub = (DirList) o;
        writeDirRecord(bb, sub, sub.name);
      }
    }

    // Pad the directory content to the nearest sector boundary (2048 bytes)
    // makeps3iso sets directory size to a multiple of 2048. PS3 likely expects
    // this.
    int size = bb.position();
    int remainder = size % SECTOR_SIZE;
    if (remainder != 0) {
      int pad = SECTOR_SIZE - remainder;
      for (int i = 0; i < pad; i++) {
        bb.put((byte) 0);
      }
    }

    byte[] res = new byte[bb.position()];
    bb.position(0);
    bb.get(res);
    return res;
  }

  /**
   * Recursive helper to populate allFiles in DFS order and assign LBAs.
   */
  private int scanFilesDFS(DirList dir, int currentSectorOffset, List<FileEntry> fileList,
      java.util.Map<DirList, List<DirList>> childrenMap) {
    // Process Files in THIS directory first (alphabetical order)
    List<FileEntry> sortedFiles = new ArrayList<>(dir.files);

    Collections.sort(sortedFiles, (o1, o2) -> {
      if (o1.name == null) return -1;
      if (o2.name == null) return 1;
      return o1.name.compareToIgnoreCase(o2.name);
    });

    for (FileEntry file : sortedFiles) {
      file.rlba = currentSectorOffset;
      int sectors = (int) ((file.size + SECTOR_SIZE - 1) / SECTOR_SIZE);

      currentSectorOffset += sectors;

      fileList.add(file);

      if (file.size > MULTIEXTENT_PART_SIZE) {
        file.extentParts = (int) ((file.size + MULTIEXTENT_PART_SIZE - 1) / MULTIEXTENT_PART_SIZE);
      }
    }

    // Then recurse into subdirectories (alphabetical order)
    List<DirList> children = childrenMap.get(dir);
    if (children != null) {
      List<DirList> sortedChildren = new ArrayList<>(children);
      Collections.sort(sortedChildren, (o1, o2) -> {
        if (o1.name == null) return -1;
        if (o2.name == null) return 1;
        return o1.name.compareToIgnoreCase(o2.name);
      });

      for (DirList child : sortedChildren) {
        currentSectorOffset = scanFilesDFS(child, currentSectorOffset, fileList, childrenMap);
      }
    }

    return currentSectorOffset;
  }

  private void writeDirRecord(ByteBuffer bb, DirList target, String name) {
    // ISO 9660: '.' and '..' are 1 byte (0x00 and 0x01).
    int nameLen = (name.equals(".") || name.equals("..")) ? 1 : name.length();

    // makeps3iso compatibility: +6 bytes overhead
    int padOverhead = 6;
    int recordLen = 33 + nameLen + padOverhead;
    if (recordLen % 2 != 0) {
      recordLen++;
    }

    int pos = bb.position();
    // Strict sector boundary check: if (current_offset + record_len > 2048)
    if ((pos % SECTOR_SIZE) + recordLen > SECTOR_SIZE) {
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

    // Fill remaining bytes with 0 padding
    int bytesWritten = 33 + nameLen;
    while (bytesWritten < recordLen) {
      bb.put((byte) 0);
      bytesWritten++;
    }
  }

  private void writeFileRecord(ByteBuffer bb, FileEntry f, int filesStartLba) {
    // For multi-extent files (>4GB), we need to write multiple records
    if (f.extentParts > 1) {
      writeMultiExtentFileRecords(bb, f, filesStartLba);
      return;
    }

    // Standard single-extent file record
    String name = f.name;
    // PS3 compatibility: makeps3iso logic.
    int nameLen = name.length();
    int nameLenWithSuffix = nameLen + 2; // +2 for ";1"

    // makeps3iso compatibility: +6 bytes overhead
    int padOverhead = 6;
    int recordLen = 33 + nameLenWithSuffix + padOverhead;

    if (recordLen % 2 != 0) {
      recordLen++;
    }

    int pos = bb.position();
    // Strict sector boundary check
    if ((pos % SECTOR_SIZE) + recordLen > SECTOR_SIZE) {
      int pad = (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE) - pos;
      for (int i = 0; i < pad; i++)
        bb.put((byte) 0);
    }

    bb.put((byte) recordLen);
    bb.put((byte) 0);

    int lba = filesStartLba + f.rlba;

    putBothEndianInt(bb, lba);

    // Use int cast for size (works for files <= 4GB)
    putBothEndianInt(bb, (int) f.size);

    putDate(bb);

    bb.put(ISO_FILE);
    bb.put((byte) 0);
    bb.put((byte) 0);
    putBothEndianShort(bb, (short) 1);

    // Length of File Identifier includes suffix
    bb.put((byte) nameLenWithSuffix);
    bb.put(name.toUpperCase().getBytes(StandardCharsets.US_ASCII));
    bb.put((byte) ';');
    bb.put((byte) '1');

    // Fill remaining bytes with 0 padding
    int bytesWritten = 33 + nameLenWithSuffix;
    while (bytesWritten < recordLen) {
      bb.put((byte) 0);
      bytesWritten++;
    }
  }

  /**
   * Write multiple directory records for a multi-extent file (>4GB).
   * Each extent is limited to MULTIEXTENT_PART_SIZE bytes.
   */
  private void writeMultiExtentFileRecords(ByteBuffer bb, FileEntry f, int filesStartLba) {
    String name = f.name;
    int nameLenWithSuffix = name.length() + 2;

    // makeps3iso logic (+6 byte overhead)
    int padOverhead = 6;
    int recordLen = 33 + nameLenWithSuffix + padOverhead;

    if (recordLen % 2 != 0) {
      recordLen++;
    }

    int lba = filesStartLba + f.rlba;
    long remainingSize = f.size;

    for (int part = 0; part < f.extentParts; part++) {
      long currentSize = Math.min(remainingSize, MULTIEXTENT_PART_SIZE);

      // Alignment check (same as single record)
      int pos = bb.position();
      // Strict sector boundary check
      if ((pos % SECTOR_SIZE) + recordLen > SECTOR_SIZE) {
        int pad = (((pos / SECTOR_SIZE) + 1) * SECTOR_SIZE) - pos;
        for (int i = 0; i < pad; i++)
          bb.put((byte) 0);
      }

      bb.put((byte) recordLen);
      bb.put((byte) 0);

      putBothEndianInt(bb, lba);
      putBothEndianInt(bb, (int) currentSize);
      putDate(bb);

      byte flags = ISO_FILE;
      if (part < f.extentParts - 1) {
        flags |= ISO_MULTIEXTENT;
      }

      bb.put(flags);
      bb.put((byte) 0);
      bb.put((byte) 0);
      putBothEndianShort(bb, (short) 1);

      bb.put((byte) nameLenWithSuffix);
      bb.put(name.toUpperCase().getBytes(StandardCharsets.US_ASCII));
      bb.put((byte) ';');
      bb.put((byte) '1');

      // Fill remaining bytes with 0 padding
      int bytesWritten = 33 + nameLenWithSuffix;
      while (bytesWritten < recordLen) {
        bb.put((byte) 0);
        bytesWritten++;
      }

      lba += (int) ((currentSize + SECTOR_SIZE - 1) / SECTOR_SIZE);
      remainingSize -= currentSize;
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

  private void writePVD(ByteBuffer bb, int pathTableSize, int ptL, int ptM, DirList root, int volumeSizeSectors) {
    bb.position(32768);
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
    writeDirRecord(temp, root, ".");
    byte[] rootRecord = new byte[temp.position()];
    temp.position(0);
    temp.get(rootRecord);
    bb.put(rootRecord);

    pad(bb, 2048 - bb.position() + 32768);
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
  public IFile[] listFiles() {
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
  public IFile findFile(String fileName) {
    return null;
  }

  @Override
  public int read(byte[] buffer, long position) throws IOException {
    return read(buffer, 0, buffer.length, position);
  }

  @Override
  public int read(byte[] buffer, int offset, int length, long position) throws IOException {
    long remaining = length;
    int r = 0;
    int bufOffset = offset;

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

      // In this file's allocated area
      if (position < f.endOffset) {
        // Real data - read from file parts
        long offsetInFile = position - f.startOffset;
        int toRead = (int) Math.min(f.endOffset - position, remaining);

        int readCount;
        if (f.isMultipart) {
          readCount = readFromMultipartFile(f, offsetInFile, buffer, bufOffset, toRead);
        } else {
          // Single file - read directly using zero-copy
          readCount = f.fileParts.get(0).read(buffer, bufOffset, toRead, offsetInFile);
        }

        if (readCount > 0) {
          r += readCount;
          remaining -= readCount;
          bufOffset += readCount;
          position += readCount;
        } else {
          // EOF or Read Error - break to avoid infinite loop
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

        // Direct zero-copy read
        int readCount = part.read(buffer, currentBufOffset, bytesToReadFromPart, offsetInPart);

        if (readCount > 0) {
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
