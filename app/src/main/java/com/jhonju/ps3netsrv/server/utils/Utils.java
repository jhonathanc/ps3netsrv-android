package com.jhonju.ps3netsrv.server.utils;

import com.jhonju.ps3netsrv.server.charset.StandardCharsets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;

public class Utils {

    public static final int SHORT_CAPACITY = 2;
    public static final int INT_CAPACITY = 4;
    public static final int LONG_CAPACITY = 8;
    public static final String DOT_STR = ".";
    public static final String DKEY_EXT = ".dkey";
    public static final String PS3ISO_FOLDER_NAME = "PS3ISO";
    public static final String REDKEY_FOLDER_NAME = "REDKEY";
    public static final String ISO_EXTENSION = ".iso";

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
}
