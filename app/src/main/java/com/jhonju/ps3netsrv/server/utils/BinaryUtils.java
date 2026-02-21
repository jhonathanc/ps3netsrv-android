package com.jhonju.ps3netsrv.server.utils;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;
import com.jhonju.ps3netsrv.server.io.PS3RegionInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BinaryUtils {

  public static final int SHORT_CAPACITY = 2;
  public static final int INT_CAPACITY = 4;
  public static final int LONG_CAPACITY = 8;
  public static final short SECTOR_SIZE = 2048;
  public static final String DOT_STR = ".";
  public static final String DKEY_EXT = ".dkey";
  public static final String PS3ISO_FOLDER_NAME = "PS3ISO";
  public static final String REDKEY_FOLDER_NAME = "REDKEY";
  public static final String ISO_EXTENSION = ".iso";
  public static final String READ_ONLY_MODE = "r";
  public static final int _3K3Y_KEY_OFFSET = 0xF80;
  public static final int _3K3Y_WATERMARK_OFFSET = 0xF70;
  public static final int ENCRYPTION_KEY_SIZE = 16;
  public static final int BUFFER_SIZE = 4 * 1048576; // 4MB
  
  // 3k3y watermarks: "Encrypted 3K BLD" and "Dncrypted 3K BLD"
  public static final byte[] _3K3Y_ENCRYPTED_WATERMARK = {
      0x45, 0x6E, 0x63, 0x72, 0x79, 0x70, 0x74, 0x65, 
      0x64, 0x20, 0x33, 0x4B, 0x20, 0x42, 0x4C, 0x44
  };
  public static final byte[] _3K3Y_DECRYPTED_WATERMARK = {
      0x44, 0x6E, 0x63, 0x72, 0x79, 0x70, 0x74, 0x65, 
      0x64, 0x20, 0x33, 0x4B, 0x20, 0x42, 0x4C, 0x44
  };
  
  // Keys for D1 to decryption key conversion
  public static final byte[] _3K3Y_D1_KEY = {
      0x38, 0x0B, (byte) 0xCF, 0x0B, 0x53, 0x45, 0x5B, 0x3C, 
      0x78, 0x17, (byte) 0xAB, 0x4F, (byte) 0xA3, (byte) 0xBA, (byte) 0x90, (byte) 0xED
  };
  public static final byte[] _3K3Y_D1_IV = {
      0x69, 0x47, 0x47, 0x72, (byte) 0xAF, 0x6F, (byte) 0xDA, (byte) 0xB3, 
      0x42, 0x74, 0x3A, (byte) 0xEF, (byte) 0xAA, 0x18, 0x62, (byte) 0x87
  };

  public static byte[] charArrayToByteArray(char[] chars) {
    CharBuffer charBuffer = CharBuffer.wrap(chars);
    ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
    return bytes;
  }

  public static byte[] longToBytesBE(final long value) {
    ByteBuffer bb = ByteBuffer.allocate(LONG_CAPACITY).order(ByteOrder.BIG_ENDIAN);
    bb.putLong(value);
    return bb.array();
  }

  public static byte[] intToBytesBE(final int value) {
    ByteBuffer bb = ByteBuffer.allocate(INT_CAPACITY).order(ByteOrder.BIG_ENDIAN);
    bb.putInt(value);
    return bb.array();
  }

  public static byte[] shortToBytesBE(final short value) {
    ByteBuffer bb = ByteBuffer.allocate(SHORT_CAPACITY).order(ByteOrder.BIG_ENDIAN);
    bb.putShort(value);
    return bb.array();
  }

  public static boolean isByteArrayEmpty(byte[] byteArray) {
    return (byteArray.length == 0 || Arrays.equals(byteArray, new byte[byteArray.length]));
  }

  public static ByteBuffer readCommandData(InputStream in, int size) throws IOException {
    byte[] data = new byte[size];
    int bytesRead = 0;
    while (bytesRead < size) {
      int result = in.read(data, bytesRead, size - bytesRead);
      if (result == -1) {
        if (bytesRead == 0) return null;
        break;
      }
      bytesRead += result;
    }
    return ByteBuffer.wrap(data);
  }

  public static int BytesBEToInt(byte[] value) {
    return ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getInt();
  }

  public static PS3RegionInfo[] getRegionInfos(byte[] sec0sec1) {
    int regionCount = BinaryUtils.BytesBEToInt(Arrays.copyOf(sec0sec1, 4)) * 2 - 1;
    PS3RegionInfo[] regionInfos = new PS3RegionInfo[regionCount];
    for (int i = 0; i < regionCount; ++i) {
      int offset = 12 + (i * INT_CAPACITY);
      long lastAddr = (BinaryUtils.BytesBEToInt(Arrays.copyOfRange(sec0sec1, offset, offset + INT_CAPACITY))
          - (i % 2 == 1 ? 1L : 0L)) * SECTOR_SIZE + SECTOR_SIZE - 1L;
      regionInfos[i] = new PS3RegionInfo(i % 2 == 1, i == 0 ? 0L : regionInfos[i - 1].getLastAddress() + 1L, lastAddr);
    }
    return regionInfos;
  }

  public static void decryptData(SecretKeySpec key, byte[] iv, byte[] data, int dataOffset, int sectorCount, long startLBA)
      throws IOException {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      for (int i = 0; i < sectorCount; ++i) {
        IvParameterSpec ivParams = new IvParameterSpec(resetIV(iv, startLBA + i));
        cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
        int offset = dataOffset + (SECTOR_SIZE * i);
        byte[] decryptedSector = cipher.doFinal(data, offset, SECTOR_SIZE);
        System.arraycopy(decryptedSector, 0, data, offset, SECTOR_SIZE);
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private static byte[] resetIV(byte[] iv, long lba) {
    Arrays.fill(iv, (byte) 0);
    // Use unsigned right shift (>>>) and mask after shift to avoid sign extension
    // issues
    iv[12] = (byte) ((lba >>> 24) & 0xFF);
    iv[13] = (byte) ((lba >>> 16) & 0xFF);
    iv[14] = (byte) ((lba >>> 8) & 0xFF);
    iv[15] = (byte) (lba & 0xFF);
    return iv;
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    if (bytes == null)
      return "";
    char[] hexChars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      hexChars[i * 2] = HEX_ARRAY[v >>> 4];
      hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Check if the sec0sec1 data contains the 3k3y encrypted watermark at offset 0xF70.
   * This watermark is "Encrypted 3K BLD".
   */
  public static boolean has3K3YEncryptedWatermark(byte[] sec0sec1) {
    if (sec0sec1 == null || sec0sec1.length < _3K3Y_KEY_OFFSET + ENCRYPTION_KEY_SIZE) {
      return false;
    }
    for (int i = 0; i < ENCRYPTION_KEY_SIZE; i++) {
      if (sec0sec1[_3K3Y_WATERMARK_OFFSET + i] != _3K3Y_ENCRYPTED_WATERMARK[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if the sec0sec1 data contains the 3k3y decrypted watermark at offset 0xF70.
   * This watermark is "Dncrypted 3K BLD" (already decrypted, no key needed).
   */
  public static boolean has3K3YDecryptedWatermark(byte[] sec0sec1) {
    if (sec0sec1 == null || sec0sec1.length < _3K3Y_WATERMARK_OFFSET + ENCRYPTION_KEY_SIZE) {
      return false;
    }
    for (int i = 0; i < ENCRYPTION_KEY_SIZE; i++) {
      if (sec0sec1[_3K3Y_WATERMARK_OFFSET + i] != _3K3Y_DECRYPTED_WATERMARK[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Extract the D1 key from sec0sec1 data and convert it to the actual decryption key.
   * The D1 value at offset 0xF80 needs to be encrypted with a specific key/IV to get the real key.
   */
  public static byte[] convertD1ToKey(byte[] sec0sec1) throws IOException {
    if (sec0sec1 == null || sec0sec1.length < _3K3Y_KEY_OFFSET + ENCRYPTION_KEY_SIZE) {
      return null;
    }
    
    // Extract D1 from offset 0xF80
    byte[] d1 = new byte[ENCRYPTION_KEY_SIZE];
    System.arraycopy(sec0sec1, _3K3Y_KEY_OFFSET, d1, 0, ENCRYPTION_KEY_SIZE);
    
    // Convert D1 to decryption key using AES encryption with the magic key/IV
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(_3K3Y_D1_KEY, "AES");
      IvParameterSpec ivSpec = new IvParameterSpec(_3K3Y_D1_IV);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
      return cipher.doFinal(d1);
    } catch (Exception e) {
      throw new IOException("Failed to convert D1 to key", e);
    }
  }

}
