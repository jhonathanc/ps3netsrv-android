package com.jhonju.ps3netsrv.server.io;

import android.content.ContentResolver;
import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

public class VirtualIsoFile implements IFile {

  private static final int SECTOR_SIZE = 2048;

  private final IFile rootFile;
  private final String volumeName;

  private ByteBuffer fsBuf;
  private int fsBufSize;
  private long totalSize;

  private DirList rootList;
  private List<FileEntry> allFiles;

  private static class FileEntry {
    String name;
    long size;
    int rlba;
    IFile file; // Abstraction
    long startOffset;
    long endOffset;
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
    this.rootFile = rootDir;
    this.volumeName = rootDir.getName() != null ? rootDir.getName().toUpperCase() : "PS3VOLUME";

    if (!build()) {
      throw new IOException("Failed to build virtual ISO structure");
    }
  }

  private boolean build() throws IOException {
    allFiles = new ArrayList<>();
    rootList = new DirList();
    rootList.name = "";
    rootList.parent = rootList;
    rootList.idx = 1;

    List<DirList> allDirs = new ArrayList<>();
    allDirs.add(rootList);

    scanDirectory(rootFile, rootList, allDirs);

    // 2. Calculate sizes and assign Relative LBAs for files
    int currentFileSectorOffset = 0;
    for (DirList dir : allDirs) {
      for (FileEntry file : dir.files) {
        file.rlba = currentFileSectorOffset;
        int sectors = (int) ((file.size + SECTOR_SIZE - 1) / SECTOR_SIZE);
        currentFileSectorOffset += sectors;
        allFiles.add(file);
      }
    }
    int filesSizeSectors = currentFileSectorOffset;

    // 4. Generate Path Tables
    Collections.sort(allDirs, new Comparator<DirList>() {
      @Override
      public int compare(DirList o1, DirList o2) {
        int depth1 = getDepth(o1);
        int depth2 = getDepth(o2);
        if (depth1 != depth2)
          return Integer.compare(depth1, depth2);
        return Integer.compare(o1.parent.idx, o2.parent.idx);
      }
    });

    for (int i = 0; i < allDirs.size(); i++) {
      allDirs.get(i).idx = i + 1;
    }

    byte[] pathTableL = generatePathTable(allDirs, false);
    byte[] pathTableM = generatePathTable(allDirs, true);

    int pathTableSize = pathTableL.length;
    int pathTableSectors = (pathTableSize + SECTOR_SIZE - 1) / SECTOR_SIZE;

    // 5. Calculate Layout
    int lba = 16; // PVD
    lba++; // Terminator

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

    // 7. Build fsBuf
    fsBufSize = filesStartLba * SECTOR_SIZE;
    fsBuf = ByteBuffer.allocate(fsBufSize);
    fsBuf.order(ByteOrder.LITTLE_ENDIAN);

    Arrays.fill(fsBuf.array(), (byte) 0);

    writePVD(fsBuf, 16 * SECTOR_SIZE, pathTableSize, pathTableL_LBA, pathTableM_LBA, rootList, filesStartLba);

    // Terminator
    fsBuf.put(17 * SECTOR_SIZE, (byte) 255);
    fsBuf.put(17 * SECTOR_SIZE + 1, (byte) 'C');
    fsBuf.put(17 * SECTOR_SIZE + 2, (byte) 'D');
    fsBuf.put(17 * SECTOR_SIZE + 3, (byte) '0');
    fsBuf.put(17 * SECTOR_SIZE + 4, (byte) '0');
    fsBuf.put(17 * SECTOR_SIZE + 5, (byte) '1');
    fsBuf.put(17 * SECTOR_SIZE + 6, (byte) 1);

    fsBuf.position(pathTableL_LBA * SECTOR_SIZE);
    fsBuf.put(pathTableL);

    fsBuf.position(pathTableM_LBA * SECTOR_SIZE);
    fsBuf.put(pathTableM);

    for (DirList dir : allDirs) {
      fsBuf.position(dir.lba * SECTOR_SIZE);
      fsBuf.put(dir.content);
    }

    totalSize = (long) filesStartLba * SECTOR_SIZE + (long) filesSizeSectors * SECTOR_SIZE;

    // Update file entry cached offsets
    long filesAreaStartOffset = (long) filesStartLba * SECTOR_SIZE;
    for (FileEntry f : allFiles) {
      f.startOffset = filesAreaStartOffset + ((long) f.rlba * SECTOR_SIZE);
      f.endOffset = f.startOffset + f.size;
    }

    return true;
  }

  // Abstracted scanDirectory to use IFile
  private void scanDirectory(IFile dir, DirList dirEntry, List<DirList> allDirs) throws IOException {
    IFile[] files = dir.listFiles();
    if (files == null)
      return;

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

    for (IFile f : files) {
      String name = f.getName();
      if (name == null)
        continue;

      if (f.isDirectory()) {
        DirList child = new DirList();
        child.name = name;
        child.parent = dirEntry;
        allDirs.add(child);
        scanDirectory(f, child, allDirs);
      } else {
        FileEntry fe = new FileEntry();
        fe.name = name;
        fe.size = f.length();
        fe.file = f;
        dirEntry.files.add(fe);
      }
    }
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
    // ... Same logic as before ...
    ByteBuffer bb = ByteBuffer.allocate(dirs.size() * 32);
    bb.order(msb ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

    for (DirList d : dirs) {
      String name = d.name;
      if (d == rootList)
        name = "\0";
      else
        name = name.toUpperCase();

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
        bb.put(name.getBytes(StandardCharsets.US_ASCII));

      if (len_di % 2 != 0)
        bb.put((byte) 0);
    }
    byte[] res = new byte[bb.position()];
    bb.position(0);
    bb.get(res);
    return res;
  }

  private byte[] generateDirectoryContent(DirList dir, List<DirList> allDirs, int filesStartLba) {
    // ... Same logic as before ...
    List<DirList> subDirs = new ArrayList<>();
    for (DirList d : allDirs) {
      if (d.parent == dir && d != dir && d != rootList)
        subDirs.add(d);
    }

    ByteBuffer bb = ByteBuffer.allocate(64 * 1024);
    bb.order(ByteOrder.LITTLE_ENDIAN);

    writeDirRecord(bb, dir, ".", 0);
    writeDirRecord(bb, dir.parent, "..", 0);

    for (DirList sub : subDirs)
      writeDirRecord(bb, sub, sub.name, 0);
    for (FileEntry f : dir.files)
      writeFileRecord(bb, f, filesStartLba);

    byte[] res = new byte[bb.position()];
    bb.position(0);
    bb.get(res);
    return res;
  }

  private void writeDirRecord(ByteBuffer bb, DirList target, String name, int flags) {
    // ... Same logic as before ...
    int nameLen = name.equals(".") || name.equals("..") ? 1 : name.length();
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
      bb.put(name.toUpperCase().getBytes(StandardCharsets.US_ASCII));

    if ((33 + nameLen) % 2 != 0)
      bb.put((byte) 0);
  }

  private void writeFileRecord(ByteBuffer bb, FileEntry f, int filesStartLba) {
    // ... Same logic as before ...
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
    putBothEndianInt(bb, lba);

    putBothEndianInt(bb, (int) f.size);

    putDate(bb);

    bb.put((byte) 0);
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
      int filesStartLba) {
    // ... Same logic as before ...
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

    int volSize = (int) (totalSize / SECTOR_SIZE);
    putBothEndianInt(bb, totalSize > 0 ? volSize : 0);

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

    // 1. Read from metadata buffer
    if (position < fsBufSize) {
      int toRead = (int) Math.min(fsBufSize - position, remaining);
      // Safety check
      if (position + toRead > fsBuf.capacity())
        toRead = fsBuf.capacity() - (int) position;

      fsBuf.position((int) position);
      fsBuf.get(buffer, bufOffset, toRead);

      remaining -= toRead;
      r += toRead;
      bufOffset += toRead;
      position += toRead;
    }

    if (remaining == 0 || position >= totalSize)
      return r;

    // 2. Read from files
    for (FileEntry f : allFiles) {
      if (position < f.startOffset) {
        continue;
      }

      long fileAreaEnd = f.startOffset + ((f.size + SECTOR_SIZE - 1) / SECTOR_SIZE) * SECTOR_SIZE;

      if (position < fileAreaEnd) {
        // In this file's allocated area
        if (position < f.endOffset) {
          // Real data
          long offsetInFile = position - f.startOffset;
          int toRead = (int) Math.min(f.endOffset - position, remaining);

          // Read from abstracted IFile into temp buffer
          // We use a temp buffer because IFile.read(buffer, pos) might not support
          // reading into an offset of the buffer
          byte[] tempBuf = new byte[toRead];
          int readCount = f.file.read(tempBuf, offsetInFile);

          if (readCount > 0) {
            System.arraycopy(tempBuf, 0, buffer, bufOffset, readCount);
            r += readCount;
            remaining -= readCount;
            bufOffset += readCount;
            position += readCount;
          }
        }

        // Padding
        if (remaining > 0 && position < fileAreaEnd) {
          int pad = (int) Math.min(fileAreaEnd - position, remaining);
          Arrays.fill(buffer, bufOffset, bufOffset + pad, (byte) 0);
          r += pad;
          remaining -= pad;
          bufOffset += pad;
          position += pad;
        }

        if (remaining == 0)
          return r;
      }
    }

    return r;
  }

  @Override
  public void close() throws IOException {
    fsBuf = null;
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
