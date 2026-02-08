package com.jhonju.ps3netsrv.server.io;

import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.DKEY_EXT;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.DOT_STR;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.ISO_EXTENSION;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.PS3ISO_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.READ_ONLY_MODE;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.REDKEY_FOLDER_NAME;
import static com.jhonju.ps3netsrv.server.utils.BinaryUtils.SECTOR_SIZE;


import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;
import com.jhonju.ps3netsrv.server.enums.EEncryptionType;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.crypto.spec.SecretKeySpec;

public class FileCustom implements IFile {

  private final File file;
  private final SecretKeySpec decryptionKey;
  private final EEncryptionType encryptionType;
  private final RandomAccessFile randomAccessFile;

  private final PS3RegionInfo[] regionInfos;
  private final byte[] iv = new byte[16];

  public File getRealFile() {
    return file;
  }

  public FileCustom(File file) throws IOException {
    this.file = file;
    byte[] encryptionKey = null;
    EEncryptionType detectedEncryptionType = EEncryptionType.NONE;
    RandomAccessFile randomAccessFile = null;
    PS3RegionInfo[] regionInfos = null;
    byte[] sec0sec1 = null;

    if (file != null && file.isFile()) {
      randomAccessFile = new RandomAccessFile(file, READ_ONLY_MODE);
      
      boolean isInPS3ISOFolder = file.getParentFile() != null 
          && file.getParentFile().getName().equalsIgnoreCase(PS3ISO_FOLDER_NAME);
      
      // For PS3ISO files, read sec0sec1 early to check for watermarks and region info
      int sec0Sec1Length = SECTOR_SIZE * 2;
      if (isInPS3ISOFolder && file.length() >= sec0Sec1Length) {
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
      } else if (sec0sec1 != null && BinaryUtils.has3K3YEncryptedWatermark(sec0sec1)) {
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

    this.randomAccessFile = randomAccessFile;

    if (encryptionKey != null) {
      this.decryptionKey = new SecretKeySpec(encryptionKey, "AES");
      this.encryptionType = detectedEncryptionType;
    } else {
      this.decryptionKey = null;
      this.encryptionType = EEncryptionType.NONE;
    }
    this.regionInfos = regionInfos != null ? regionInfos : new PS3RegionInfo[0];
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


  public boolean mkdir() {
    return file.mkdir();
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

  @Override
  public void close() throws IOException {
    if (randomAccessFile != null) {
      randomAccessFile.close();
    }
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
      fos.write(buffer);
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
