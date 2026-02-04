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

public class Utils {

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
    public static final int ENCRYPTION_KEY_SIZE = 16;

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
        if (in.read(data) < 0) return null;
        return ByteBuffer.wrap(data);
    }

    public static int BytesBEToInt(byte[] value) {
        return ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public static PS3RegionInfo[] getRegionInfos(byte[] sec0sec1) {
        int regionCount = Utils.BytesBEToInt(Arrays.copyOf(sec0sec1, 4)) * 2 - 1;
        PS3RegionInfo[] regionInfos = new PS3RegionInfo[regionCount];
        for (int i = 0; i < regionCount; ++i) {
            int offset = 12 + (i * INT_CAPACITY);
            long lastAddr = (Utils.BytesBEToInt(Arrays.copyOfRange(sec0sec1, offset, offset + INT_CAPACITY)) - (i % 2 == 1 ? 1L : 0L)) * SECTOR_SIZE + SECTOR_SIZE - 1L;
            regionInfos[i] = new PS3RegionInfo(i % 2 == 1
                    , i == 0 ? 0L : regionInfos[i - 1].getLastAddress() + 1L
                    , lastAddr);
        }
        return regionInfos;
    }

    public static void decryptData(SecretKeySpec key, byte[] iv, byte[] data, int sectorCount, long startLBA) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            for (int i = 0; i < sectorCount; ++i) {
                IvParameterSpec ivParams = new IvParameterSpec(resetIV(iv, startLBA + i));
                cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
                int offset = SECTOR_SIZE * i;
                byte[] decryptedSector = cipher.doFinal(data, offset, SECTOR_SIZE);
                System.arraycopy(decryptedSector, 0, data, offset, SECTOR_SIZE);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static byte[] resetIV(byte[] iv, long lba) {
        Arrays.fill(iv, (byte) 0);
        // Use unsigned right shift (>>>) and mask after shift to avoid sign extension issues
        iv[12] = (byte) ((lba >>> 24) & 0xFF);
        iv[13] = (byte) ((lba >>> 16) & 0xFF);
        iv[14] = (byte) ((lba >>> 8) & 0xFF);
        iv[15] = (byte) (lba & 0xFF);
        return iv;
    }
}
