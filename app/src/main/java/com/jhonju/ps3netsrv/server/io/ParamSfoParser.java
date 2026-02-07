package com.jhonju.ps3netsrv.server.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Parser for PS3 PARAM.SFO files.
 * Extracts metadata like TITLE_ID from PS3 game folders.
 */
public class ParamSfoParser {

  private static final int SFO_MAGIC = 0x46535000; // "PSF\0" (00 50 53 46) read as Little Endian int
  private static final String TITLE_ID_KEY = "TITLE_ID";

  /**
   * Get the TITLE_ID from a PS3 game directory.
   * Looks for PS3_GAME/PARAM.SFO in the given directory.
   *
   * @param gameDir The root directory of the PS3 game
   * @return The TITLE_ID (e.g., "BCES00104") or null if not found
   */
  public static String getTitleId(IFile gameDir) {
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser.getTitleId called for: " + (gameDir != null ? gameDir.getName() : "null"));
    if (gameDir == null || !gameDir.isDirectory()) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: gameDir is invalid");
      return null;
    }

    try {
      // Try to find PS3_GAME/PARAM.SFO
      IFile ps3GameDir = gameDir.findFile("PS3_GAME");
      if (ps3GameDir == null || !ps3GameDir.exists() || !ps3GameDir.isDirectory()) {
        com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: PS3_GAME directory not found");
        return null;
      }

      IFile paramSfo = ps3GameDir.findFile("PARAM.SFO");
      if (paramSfo == null || !paramSfo.exists() || !paramSfo.isFile()) {
        com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: PARAM.SFO not found");
        return null;
      }

      String titleId = parseTitleId(paramSfo);
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Parsed TITLE_ID: " + titleId);
      return titleId;
    } catch (IOException e) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logError(e);
      return null;
    } catch (Exception e) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logError(new RuntimeException("Error in ParamSfoParser", e));
      return null;
    }
  }

  /**
   * Parse the PARAM.SFO file and extract the TITLE_ID field.
   */
  private static String parseTitleId(IFile sfoFile) throws IOException {
    long length = sfoFile.length();
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: parseTitleId file length: " + length);
    if (length < 20 || length > 65536) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Invalid SFO file size");
      return null;
    }

    byte[] data = new byte[(int) length];
    int bytesRead = sfoFile.read(data, 0);
    if (bytesRead != length) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Read mismatch. Expected " + length + ", got " + bytesRead);
      return null;
    }

    ByteBuffer buf = ByteBuffer.wrap(data);
    buf.order(ByteOrder.LITTLE_ENDIAN);

    // Check magic
    int magic = buf.getInt(0);
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Magic: " + Integer.toHexString(magic));
    if (magic != SFO_MAGIC) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Invalid magic");
      return null;
    }

    int keyTableOffset = buf.getInt(0x08);
    int dataTableOffset = buf.getInt(0x0C);
    int entryCount = buf.getInt(0x10);
    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: entryCount: " + entryCount + ", keyTableOffset: " + keyTableOffset);

    if (entryCount <= 0 || entryCount > 255) {
      com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Invalid entry count");
      return null;
    }

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
      // com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Key[" + i + "]: " + keyName); // Verbose

      if (TITLE_ID_KEY.equals(keyName)) {
        // Found TITLE_ID, read the value
        int dataAbsOffset = dataTableOffset + dataOffset;
        if (dataAbsOffset + dataLength > data.length) {
          com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: TITLE_ID value out of bounds");
          return null;
        }
        String val = readNullTerminatedString(data, dataAbsOffset);
        com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: Found TITLE_ID: " + val);
        return val;
      }
    }

    com.jhonju.ps3netsrv.server.utils.FileLogger.logPath("DEBUG", "ParamSfoParser: TITLE_ID not found in keys");
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
