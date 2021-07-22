package com.jhonju.ps3netsrv.server.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static byte[] toByteArray(Object obj) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (obj instanceof List<?>) {
            listObjectToByteArray(obj, out);
        } else {
            singleObjectToByteArray(obj, out);
        }
        return out.toByteArray();
    }

    private static void listObjectToByteArray(Object obj, ByteArrayOutputStream out) throws Exception {
        List<?> list = (List<?>) obj;
        for(Object o : list) {
            singleObjectToByteArray(o, out);
        }
    }

    private static void singleObjectToByteArray(Object obj, ByteArrayOutputStream out) throws Exception {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field: fields) {
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value != null) {
                if (value.getClass() == Boolean.class) {
                    byte[] teste = new byte[]{(byte) ((boolean) value ? 1 : 0)};
                    out.write(teste);
                } else if (value.getClass() == Long.class) {
                    out.write(longToBytes((long) value));
                } else if (value.getClass() == Integer.class) {
                    out.write(intToBytes((int) value));
                } else if (value.getClass() == String.class) {
                    out.write(((String) value).getBytes());
                } else {
                    out.write(charToByteArray((char[]) value));
                }
            }
        }
    }

    private static byte[] charToByteArray(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public static byte[] longToBytes(final long i) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(i);
        return bb.array();
    }

    public static byte[] intToBytes(final int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(i);
        return bb.array();
    }

    public static boolean isByteArrayEmpty(byte[] byteArray) {
        if (byteArray.length == 0) {
            return true;
        }
        for (byte b : byteArray) {
            if (b != '\0') {
                return false;
            }
        }
        return true;
    }
}
