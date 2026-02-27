package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.MAX_ISO_PARTS;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.MULTIPART_ISO_SUFFIX;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.READ_ONLY_MODE;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.REDKEY_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.SECTOR_SIZE;

import com.jhonju.ps3netsrv.server.enums.EEncryptionType;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;

public class FileCustom implements IFile {

  private final File file;
  private final SecretKeySpec decryptionKey;
  private final EEncryptionType encryptionType;
  private final RandomAccessFile randomAccessFile;

  private final PS3RegionInfo[] regionInfos;
  private final byte[] iv = new byte[16];

  // Multipart ISO fields
  private final boolean isMultipart;
  private final RandomAccessFile[] parts;
  private final int partCount;
  private final long partSize;
  private final long totalSize;

  public File getRealFile() {
    return file;
  }

  @SuppressWarnings("")
  public FileCustom(File file) throws IOException {
    this.file = file;
    byte[] encryptionKey = null;
    EEncryptionType detectedEncryptionType = EEncryptionType.NONE;
    RandomAccessFile randomAccessFile = null;
    PS3RegionInfo[] regionInfos = null;
    byte[] sec0sec1 = null;

    // Multipart ISO detection
    boolean multipart = false;
    RandomAccessFile[] multiParts = null;
    int multiPartCount = 0;
    long multiPartSize = 0;
    long multiTotalSize = 0;

    long fileSize;
    boolean isRegularFile;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
      fileSize = basicFileAttributes.size();
      isRegularFile = basicFileAttributes.isRegularFile();
    } else {
      fileSize = file.length();
      isRegularFile = file.isFile();
    }
    if (isRegularFile) {
      randomAccessFile = new RandomAccessFile(file, READ_ONLY_MODE);

      // Check for multipart ISO (.iso.0)
      String fileName = file.getName();
      if (fileName.toLowerCase().endsWith(MULTIPART_ISO_SUFFIX)) {
        multipart = true;
        multiParts = new RandomAccessFile[MAX_ISO_PARTS];
        multiParts[0] = randomAccessFile;
        multiPartSize = fileSize; // All parts except the last must be this size
        multiPartCount = 1;
        multiTotalSize = fileSize;

        String basePath = file.getAbsolutePath();
        String baseIsoPath = basePath.substring(0, basePath.length() - 1); // Remove trailing "0"

        for (int i = 1; i < MAX_ISO_PARTS; i++) {
          File partFile = new File(baseIsoPath + i);
          if (!partFile.exists() || !partFile.isFile())
            break;
          multiParts[i] = new RandomAccessFile(partFile, READ_ONLY_MODE);
          multiTotalSize += partFile.length();
          multiPartCount++;
        }
        // Multipart ISOs skip encryption
      } else {
        boolean isInPS3ISOFolder = file.getParentFile() != null
            && file.getParentFile().getName().equalsIgnoreCase(PS3ISO_FOLDER_NAME);

        // For PS3ISO files, read sec0sec1 early to check for watermarks and region info
        int sec0Sec1Length = SECTOR_SIZE * 2;
        if (isInPS3ISOFolder && fileSize >= sec0Sec1Length) {
          sec0sec1 = new byte[sec0Sec1Length];
          randomAccessFile.seek(0);
          if (randomAccessFile.read(sec0sec1) != sec0Sec1Length) {
            sec0sec1 = null;
          }
        }

        // First try to get Redump key from external .dkey file
        encryptionKey = getRedumpKey(file.getParentFile(), file.getAbsolutePath(), file.getName());
        if (encryptionKey != null) {
          detectedEncryptionType = EEncryptionType.REDUMP;
        } else if (BinaryUtils.has3K3YEncryptedWatermark(sec0sec1)) {
          // If no Redump key, check for 3k3y watermark and extract key if found
          encryptionKey = BinaryUtils.convertD1ToKey(sec0sec1);
          if (encryptionKey != null) {
            detectedEncryptionType = EEncryptionType._3K3Y;
          }
        }

        // Parse region info from sec0sec1 if we have encryption
        if (encryptionKey != null && sec0sec1 != null) {
          regionInfos = BinaryUtils.getRegionInfos(sec0sec1);
        }
      }
    }

    this.randomAccessFile = randomAccessFile;
    this.isMultipart = multipart;
    this.parts = multiParts;
    this.partCount = multiPartCount;
    this.partSize = multiPartSize;
    this.totalSize = multipart ? multiTotalSize : 0;

    if (encryptionKey != null) {
      this.decryptionKey = new SecretKeySpec(encryptionKey, "AES");
      this.encryptionType = detectedEncryptionType;
      Arrays.fill(encryptionKey, (byte) 0);
    } else {
      this.decryptionKey = null;
      this.encryptionType = EEncryptionType.NONE;
    }
    this.regionInfos = regionInfos != null ? regionInfos : new PS3RegionInfo[0];

    if (sec0sec1 != null) {
      Arrays.fill(sec0sec1, (byte) 0);
    }
  }

  private static byte[] getRedumpKey(File parent, String path, String fileName) throws IOException {
    byte[] decryptionKey = null;
    if (parent != null && parent.getName().equalsIgnoreCase(PS3ISO_FOLDER_NAME)) {
      int pos = path.lastIndexOf(DOT_STR);
      if (pos >= 0 && path.substring(pos).equalsIgnoreCase(ISO_EXTENSION)) {
        File decryptionKeyFile = new File(path.substring(0, pos) + DKEY_EXT);
        if (!decryptionKeyFile.exists() || decryptionKeyFile.isDirectory()) {
          File redKeyFolder = new File(parent.getParentFile(), REDKEY_FOLDER_NAME);
          if (redKeyFolder.exists() && redKeyFolder.isDirectory()) {
            decryptionKeyFile = new File(redKeyFolder, fileName.substring(0, fileName.lastIndexOf(DOT_STR)) + DKEY_EXT);
          }
        }
        if (decryptionKeyFile.exists() && decryptionKeyFile.isFile()) {
          decryptionKey = getKeyFromDocumentFile(decryptionKeyFile);
        }
      }
    }
    return decryptionKey;
  }

  private static byte[] getKeyFromDocumentFile(File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    try {
      return EncryptionKeyHelper.parseKeyFromStream(fis);
    } finally {
      fis.close();
    }
  }

  @Override
  public boolean exists() {
    return this.file.exists();
  }

  @Override
  public boolean isFile() {
    return this.file.isFile();
  }

  @Override
  public boolean isDirectory() {
    return file.isDirectory();
  }

  @Override
  public boolean delete() {
    return file.delete();
  }

  @Override
  public long length() {
    if (isMultipart) {
      return totalSize;
    }
    return file.length();
  }

  @Override
  public IFile[] listFiles() throws IOException {
    File[] filesAux = file.listFiles();
    IFile[] files = null;
    if (filesAux != null) {
      files = new IFile[filesAux.length];
      int i = 0;
      for (File fileAux : filesAux) {
        files[i] = new FileCustom(fileAux);
        i++;
      }
    }
    return files;
  }

  @Override
  public long lastModified() {
    return file.lastModified();
  }

  @Override
  public String getName() {
    return file.getName();
  }

  @Override
  public String[] list() {
    return file.list();
  }

  @Override
  public IFile findFile(String fileName) throws IOException {
    return new FileCustom(new File(file.getCanonicalPath() + "/" + fileName));
  }

  @Override
  public int read(byte[] buffer, long position) throws IOException {
    return read(buffer, 0, buffer.length, position);
  }

  @Override
  public int read(byte[] buffer, int offset, int length, long position) throws IOException {
    if (isMultipart) {
      return readMultipart(buffer, offset, length, position);
    }

    randomAccessFile.seek(position);
    int bytesRead = randomAccessFile.read(buffer, offset, length);

    if (encryptionType == EEncryptionType.NONE) {
      return bytesRead;
    }

    for (PS3RegionInfo regionInfo : regionInfos) {
      if ((position >= regionInfo.getFirstAddress()) && (position <= regionInfo.getLastAddress())) {
        if (!regionInfo.isEncrypted()) {
          return bytesRead;
        }
        BinaryUtils.decryptData(decryptionKey, iv, buffer, offset, bytesRead / SECTOR_SIZE, position / SECTOR_SIZE);
        return bytesRead;
      }
    }
    return bytesRead;
  }

  private int readMultipart(byte[] buffer, int offset, int length, long position) throws IOException {
    int partIndex = (int) (position / partSize);
    long posInPart = position % partSize;

    if (partIndex >= partCount) {
      return -1;
    }

    parts[partIndex].seek(posInPart);
    int bytesRead = parts[partIndex].read(buffer, offset, length);

    // Check if read spans into the next part
    if (bytesRead < length && (partIndex + 1) < partCount) {
      int remaining = length - bytesRead;
      int nextIndex = partIndex + 1;
      parts[nextIndex].seek(0);
      int bytesRead2 = parts[nextIndex].read(buffer, offset + bytesRead, remaining);
      if (bytesRead2 > 0) {
        bytesRead += bytesRead2;
      }
    }

    return bytesRead;
  }

  @Override
  public void close() throws IOException {
    if (isMultipart) {
      for (int i = 0; i < partCount; i++) {
        if (parts[i] != null) {
          parts[i].close();
        }
      }
    } else if (randomAccessFile != null) {
      randomAccessFile.close();
    }
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    FileOutputStream fos = new java.io.FileOutputStream(file);
    try {
      fos.write(buffer);
    } finally {
      fos.close();
    }
  }

  @Override
  public boolean createDirectory(String name) {
    return new File(file, name).mkdir();
  }

  @Override
  public boolean createFile(String name) {
    try {
      return new File(file, name).createNewFile();
    } catch (IOException e) {
      return false;
    }
  }
}
