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

import android.content.ContentResolver;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.server.enums.EEncryptionType;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;
import com.jhonju.ps3netsrv.server.utils.FileLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

public class DocumentFileCustom implements IFile {

  public final DocumentFile documentFile;
  private SecretKeySpec decryptionKey;
  private EEncryptionType encryptionType;
  private final ContentResolver contentResolver;
  private ParcelFileDescriptor pfd;
  private FileInputStream fis;
  private FileChannel fileChannel;
  private PS3RegionInfo[] regionInfos;
  private final byte[] iv = new byte[16];

  private String cachedName;
  private Long cachedSize;
  private Boolean cachedIsDir;
  private boolean isInitialized = false;
  private IFile[] cachedListFiles = null;
  private String[] cachedList = null;

  // Multipart ISO fields
  private boolean isMultipart = false;
  private FileChannel[] partChannels;
  private ParcelFileDescriptor[] partPfds;
  private FileInputStream[] partStreams;
  private int partCount = 0;
  private long partSize = 0;
  private long totalSize = 0;

  private final android.content.Context androidContext;

  public DocumentFileCustom(DocumentFile documentFile, ContentResolver contentResolver, android.content.Context context)
      throws IOException {
    this.documentFile = documentFile;
    this.contentResolver = contentResolver;
    this.androidContext = context;
    init();
  }

  public DocumentFileCustom(DocumentFile documentFile, ContentResolver contentResolver, android.content.Context context,
      String name, long size, boolean isDir) {
    this(documentFile, contentResolver, context, name, size, isDir, false);
  }

  public DocumentFileCustom(DocumentFile documentFile, ContentResolver contentResolver, android.content.Context context,
      String name, long size,
      boolean isDir, boolean initNow) {
    this.documentFile = documentFile;
    this.contentResolver = contentResolver;
    this.cachedName = name;
    this.cachedSize = size;
    this.cachedIsDir = isDir;
    this.androidContext = context;
    this.isInitialized = false;
    if (initNow) {
      try {
        init();
      } catch (IOException e) {
        FileLogger.logError(e);
      }
    }
  }

  private void init() throws IOException {
    if (isInitialized)
      return;

    byte[] encryptionKey = null;
    EEncryptionType detectedEncryptionType = EEncryptionType.NONE;
    PS3RegionInfo[] regions = null;
    byte[] sec0sec1 = null;

    if (documentFile != null && documentFile.isFile()) {
      this.pfd = contentResolver.openFileDescriptor(documentFile.getUri(), READ_ONLY_MODE);
      this.fis = new FileInputStream(pfd.getFileDescriptor());
      this.fileChannel = fis.getChannel();

      // Check for multipart ISO (.iso.0)
      String fileName = getName();
      if (fileName != null && fileName.toLowerCase().endsWith(MULTIPART_ISO_SUFFIX)) {
        initMultipart();
        // Multipart ISOs skip encryption
      } else {
        boolean isInPS3ISOFolder = documentFile.getParentFile() != null
            && documentFile.getParentFile().getName() != null
            && documentFile.getParentFile().getName().equalsIgnoreCase(PS3ISO_FOLDER_NAME);

        // For PS3ISO files, read sec0sec1 early to check for watermarks and region info
        int sec0Sec1Length = SECTOR_SIZE * 2;
        if (isInPS3ISOFolder && documentFile.length() >= sec0Sec1Length) {
          sec0sec1 = new byte[sec0Sec1Length];
          fileChannel.position(0);
          if (fileChannel.read(ByteBuffer.wrap(sec0sec1)) != sec0Sec1Length) {
            sec0sec1 = null;
          }
        }

        // First try to get Redump key from external .dkey file
        encryptionKey = getRedumpKey(documentFile.getParentFile(), documentFile.getName());
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
          regions = BinaryUtils.getRegionInfos(sec0sec1);
        }
      }
    }

    if (encryptionKey != null) {
      this.decryptionKey = new SecretKeySpec(encryptionKey, "AES");
      this.encryptionType = detectedEncryptionType;
      Arrays.fill(encryptionKey, (byte) 0);
    } else {
      this.decryptionKey = null;
      this.encryptionType = EEncryptionType.NONE;
    }
    this.regionInfos = regions != null ? regions : new PS3RegionInfo[0];
    this.isInitialized = true;

    if (sec0sec1 != null) {
      Arrays.fill(sec0sec1, (byte) 0);
    }
  }

  private void initMultipart() {
    isMultipart = true;
    partChannels = new FileChannel[MAX_ISO_PARTS];
    partPfds = new ParcelFileDescriptor[MAX_ISO_PARTS];
    partStreams = new FileInputStream[MAX_ISO_PARTS];

    // Part 0 is the already-opened file
    partChannels[0] = fileChannel;
    partPfds[0] = pfd;
    partStreams[0] = fis;
    partSize = documentFile.length();
    partCount = 1;
    totalSize = partSize;

    // Find sibling parts (.iso.1, .iso.2, ...) in the parent directory
    DocumentFile parent = documentFile.getParentFile();
    if (parent == null)
      return;

    String fileName = getName();
    // Base name without the trailing "0" -> e.g. "game.iso."
    String baseName = fileName.substring(0, fileName.length() - 1);

    for (int i = 1; i < MAX_ISO_PARTS; i++) {
      String partName = baseName + i;
      DocumentFile partDoc = parent.findFile(partName);
      if (partDoc == null || !partDoc.exists() || !partDoc.isFile())
        break;

      try {
        ParcelFileDescriptor partPfd = contentResolver.openFileDescriptor(partDoc.getUri(), READ_ONLY_MODE);
        FileInputStream partFis = new FileInputStream(partPfd.getFileDescriptor());
        FileChannel partChannel = partFis.getChannel();

        partPfds[i] = partPfd;
        partStreams[i] = partFis;
        partChannels[i] = partChannel;
        totalSize += partDoc.length();
        partCount++;
      } catch (IOException e) {
        FileLogger.logError("Error opening multipart ISO part: " + partName, e);
        break;
      }
    }

    // Update cached size to reflect total multipart size
    cachedSize = totalSize;
  }

  private byte[] getRedumpKey(DocumentFile parent, String fileName) throws IOException {
    byte[] decryptionKey = null;
    if (parent != null) {
      String parentName = parent.getName();
      if (parentName != null && parentName.equalsIgnoreCase(PS3ISO_FOLDER_NAME)) {
        int pos = fileName == null ? -1 : fileName.lastIndexOf(DOT_STR);
        if (pos >= 0 && fileName.substring(pos).equalsIgnoreCase(ISO_EXTENSION)) {
          DocumentFile documentFileAux = parent.findFile(fileName.substring(0, pos) + DKEY_EXT);
          if (documentFileAux == null || documentFileAux.isDirectory()) {
            documentFileAux = parent.getParentFile();
            if (documentFileAux != null) {
              documentFileAux = documentFileAux.findFile(REDKEY_FOLDER_NAME);
              if (documentFileAux != null && documentFileAux.isDirectory()) {
                documentFileAux = documentFileAux
                    .findFile(fileName.substring(0, fileName.lastIndexOf(DOT_STR)) + DKEY_EXT);
              }
            }
          }
          if (documentFileAux != null && documentFileAux.isFile()) {
            decryptionKey = getKeyFromDocumentFile(documentFileAux);
          }
        }
      }
    }
    return decryptionKey;
  }

  private byte[] getKeyFromDocumentFile(DocumentFile file) throws IOException {
    InputStream is = contentResolver.openInputStream(file.getUri());
    try {
      return EncryptionKeyHelper.parseKeyFromStream(is);
    } finally {
      is.close();
    }
  }

  @Override
  public boolean exists() {
    return documentFile != null && documentFile.exists();
  }

  @Override
  public boolean isFile() {
    if (cachedIsDir != null)
      return !cachedIsDir;
    return documentFile.isFile();
  }

  @Override
  public boolean isDirectory() {
    if (cachedIsDir != null)
      return cachedIsDir;
    return documentFile.isDirectory();
  }

  @Override
  public boolean delete() {
    return documentFile.delete();
  }

  @Override
  public long length() {
    if (isMultipart) {
      return totalSize;
    }
    if (cachedSize != null)
      return cachedSize;
    return documentFile.length();
  }

  @Override
  public IFile[] listFiles() throws IOException {
    if (cachedListFiles != null)
      return cachedListFiles;
    init(); // Ensure initialized for listFiles (though we try to avoid using this method)
    DocumentFile[] filesAux = documentFile.listFiles();
    IFile[] files = new IFile[filesAux.length];
    int i = 0;
    for (DocumentFile fileAux : filesAux) {
      files[i] = new DocumentFileCustom(fileAux, contentResolver, androidContext);
      i++;
    }
    cachedListFiles = files;
    return files;
  }

  @Override
  public long lastModified() {
    return documentFile.lastModified();
  }

  @Override
  public String getName() {
    if (cachedName != null)
      return cachedName;
    return documentFile.getName();
  }

  @Override
  public String[] list() {
    if (cachedList != null)
      return cachedList;
    DocumentFile[] filesAux = documentFile.listFiles();
    String[] files = new String[filesAux.length];
    int i = 0;
    for (DocumentFile fileAux : filesAux) {
      files[i] = fileAux.getName();
      i++;
    }
    cachedList = files;
    return files;
  }

  @Override
  public IFile findFile(String fileName) throws IOException {
    return new DocumentFileCustom(documentFile.findFile(fileName), contentResolver, androidContext);
  }

  @Override
  public int read(byte[] buffer, long position) throws IOException {
    return read(buffer, 0, buffer.length, position);
  }

  @Override
  public int read(byte[] buffer, int offset, int length, long position) throws IOException {
    if (!isInitialized)
      init();

    if (isMultipart) {
      return readMultipart(buffer, offset, length, position);
    }

    fileChannel.position(position);
    int bytesRead = fileChannel.read(ByteBuffer.wrap(buffer, offset, length));
    if (encryptionType != EEncryptionType.NONE) {
      for (PS3RegionInfo regionInfo : regionInfos) {
        if ((position >= regionInfo.getFirstAddress()) && (position <= regionInfo.getLastAddress())) {
          if (!regionInfo.isEncrypted()) {
            return bytesRead;
          }
          BinaryUtils.decryptData(decryptionKey, iv, buffer, offset, bytesRead / SECTOR_SIZE, position / SECTOR_SIZE);
          return bytesRead;
        }
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

    partChannels[partIndex].position(posInPart);
    int bytesRead = partChannels[partIndex].read(ByteBuffer.wrap(buffer, offset, length));

    // Check if read spans into the next part
    if (bytesRead < length && (partIndex + 1) < partCount) {
      int remaining = length - bytesRead;
      int nextIndex = partIndex + 1;
      partChannels[nextIndex].position(0);
      int bytesRead2 = partChannels[nextIndex].read(ByteBuffer.wrap(buffer, offset + bytesRead, remaining));
      if (bytesRead2 > 0) {
        bytesRead += bytesRead2;
      }
    }

    return bytesRead;
  }

  @Override
  public void close() throws IOException {
    if (isMultipart) {
      // Close all multipart handles (part 0 is the main file's channel/pfd/fis)
      for (int i = 0; i < partCount; i++) {
        try {
          if (partChannels[i] != null)
            partChannels[i].close();
        } catch (Exception e) {
          FileLogger.logError("Error closing part channel " + i, e);
        }
        try {
          if (partStreams[i] != null)
            partStreams[i].close();
        } catch (Exception e) {
          FileLogger.logError("Error closing part stream " + i, e);
        }
        try {
          if (partPfds[i] != null)
            partPfds[i].close();
        } catch (Exception e) {
          FileLogger.logError("Error closing part pfd " + i, e);
        }
      }
      // Nullify the main references since they were closed above as part 0
      fileChannel = null;
      fis = null;
      pfd = null;
    } else {
      try {
        if (fileChannel != null)
          fileChannel.close();
      } catch (Exception e) {
        FileLogger.logError("Error closing fileChannel", e);
      } finally {
        fileChannel = null;
      }

      try {
        if (fis != null)
          fis.close();
      } catch (Exception e) {
        FileLogger.logError("Error closing fis", e);
      } finally {
        fis = null;
      }

      try {
        if (pfd != null)
          pfd.close();
      } catch (Exception e) {
        FileLogger.logError("Error closing pfd", e);
      } finally {
        pfd = null;
      }
    }
  }

  @Override
  public void write(byte[] buffer) throws IOException {
    if (documentFile != null && documentFile.isFile()) {
      OutputStream os = contentResolver.openOutputStream(documentFile.getUri());
      try {
        if (os != null) {
          os.write(buffer);
        } else {
          throw new IOException(androidContext.getString(R.string.error_open_output_stream));
        }
      } finally {
        if (os != null) {
          os.close();
        }
      }
    } else {
      throw new IOException(androidContext.getString(R.string.error_file_not_writable));
    }
  }

  @Override
  public boolean createDirectory(String name) {
    return documentFile != null && documentFile.createDirectory(name) != null;
  }

  @Override
  public boolean createFile(String name) {
    return documentFile != null && documentFile.createFile(null, name) != null;
  }
}
