package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parser for PS3 PARAM.SFO files.
 * Extracts metadata like TITLE_ID from PS3 game folders.
 */
public class ParamSfoParser {

  private static final int SFO_MAGIC = 0x00505346; // "PSF\0" in little-endian
  private static final String TITLE_ID_KEY = "TITLE_ID";

  /**
   * Get the TITLE_ID from a PS3 game directory.
   * Looks for PS3_GAME/PARAM.SFO in the given directory.
   *
   * @param gameDir The root directory of the PS3 game
   * @return The TITLE_ID (e.g., "BCES00104") or null if not found
   */
  public static String getTitleId(IFile gameDir) {
    if (gameDir == null || !gameDir.isDirectory()) {
      return null;
    }

    try {
      // Try to find PS3_GAME/PARAM.SFO
      IFile ps3GameDir = gameDir.findFile("PS3_GAME");
      if (ps3GameDir == null || !ps3GameDir.exists() || !ps3GameDir.isDirectory()) {
        return null;
      }

      IFile paramSfo = ps3GameDir.findFile("PARAM.SFO");
      if (paramSfo == null || !paramSfo.exists() || !paramSfo.isFile()) {
        return null;
      }

      return parseTitleId(paramSfo);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Parse the PARAM.SFO file and extract the TITLE_ID field.
   */
  private static String parseTitleId(IFile sfoFile) throws IOException {
    long length = sfoFile.length();
    if (length < 20 || length > 65536) {
      return null; // Invalid SFO file size
    }

    byte[] data = new byte[(int) length];
    int bytesRead = sfoFile.read(data, 0);
    if (bytesRead != length) {
      return null;
    }

    ByteBuffer buf = ByteBuffer.wrap(data);
    buf.order(ByteOrder.LITTLE_ENDIAN);

    // Check magic
    int magic = buf.getInt(0);
    if (magic != SFO_MAGIC) {
      return null;
    }

    // SFO header structure:
    // 0x00: magic (4 bytes)
    // 0x04: version (4 bytes)
    // 0x08: key table offset (4 bytes)
    // 0x0C: data table offset (4 bytes)
    // 0x10: entry count (4 bytes)
    int keyTableOffset = buf.getInt(0x08);
    int dataTableOffset = buf.getInt(0x0C);
    int entryCount = buf.getInt(0x10);

    if (entryCount <= 0 || entryCount > 255) {
      return null;
    }

    // Index table starts at offset 0x14
    // Each entry is 16 bytes:
    // 0x00: key offset (2 bytes, relative to key table)
    // 0x02: data format (2 bytes)
    // 0x04: data length (4 bytes)
    // 0x08: data max length (4 bytes)
    // 0x0C: data offset (4 bytes, relative to data table)

    int indexTableOffset = 0x14;

    for (int i = 0; i < entryCount; i++) {
      int entryOffset = indexTableOffset + (i * 16);

      if (entryOffset + 16 > data.length) {
        break;
      }

      int keyOffset = buf.getShort(entryOffset) & 0xFFFF;
      int dataLength = buf.getInt(entryOffset + 0x04);
      int dataOffset = buf.getInt(entryOffset + 0x0C);

      // Read key name
      int keyAbsOffset = keyTableOffset + keyOffset;
      if (keyAbsOffset >= data.length) {
        continue;
      }

      String keyName = readNullTerminatedString(data, keyAbsOffset);
      if (TITLE_ID_KEY.equals(keyName)) {
        // Found TITLE_ID, read the value
        int dataAbsOffset = dataTableOffset + dataOffset;
        if (dataAbsOffset + dataLength > data.length) {
          return null;
        }
        return readNullTerminatedString(data, dataAbsOffset);
      }
    }

    return null;
  }

  private static String readNullTerminatedString(byte[] data, int offset) {
    int end = offset;
    while (end < data.length && data[end] != 0) {
      end++;
    }
    return new String(data, offset, end - offset);
  }
}
