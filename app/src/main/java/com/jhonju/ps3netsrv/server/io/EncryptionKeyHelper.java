package com.jhonju.ps3netsrv.server.io;

import com.jhonju.ps3netsrv.R;
import com.jhonju.ps3netsrv.app.PS3NetSrvApp;

import java.io.IOException;
import java.io.InputStream;

public final class EncryptionKeyHelper {

  private EncryptionKeyHelper() {
    // Utility class, no instantiation
  }

  public static byte[] parseKeyFromStream(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[64];
    int bytesRead = inputStream.read(buffer);

    if (bytesRead < 0) {
      throw new IOException(PS3NetSrvApp.getAppContext().getString(R.string.error_redump_key_invalid));
    }

    if (bytesRead == 16) {
      // Raw 16-byte key
      byte[] key = new byte[16];
      System.arraycopy(buffer, 0, key, 0, 16);
      return key;
    } else if (bytesRead >= 32) {
      // Hex-encoded key
      String hexStr = new String(buffer, 0, bytesRead,
          com.jhonju.ps3netsrv.server.charset.StandardCharsets.US_ASCII).trim();
      if (hexStr.length() >= 32) {
        return hexStringToBytes(hexStr.substring(0, 32));
      } else {
        throw new IOException(
            PS3NetSrvApp.getAppContext().getString(R.string.error_redump_key_hex_short, hexStr.length()));
      }
    } else {
      throw new IOException(
          PS3NetSrvApp.getAppContext().getString(R.string.error_redump_key_size_invalid, bytesRead));
    }
  }

  private static byte[] hexStringToBytes(String hex) {
    byte[] bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      int index = i * 2;
      bytes[i] = (byte) ((Character.digit(hex.charAt(index), 16) << 4)
          + Character.digit(hex.charAt(index + 1), 16));
    }
    return bytes;
  }
}
