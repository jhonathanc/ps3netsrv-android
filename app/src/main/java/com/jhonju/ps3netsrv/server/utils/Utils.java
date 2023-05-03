package com.jhonju.ps3netsrv.server.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import com.jhonju.ps3netsrv.app.PS3NetSrvApp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@SuppressLint("SimpleDateFormat")
public class Utils {

    public static final int SHORT_CAPACITY = 2;
    public static final int INT_CAPACITY = 4;
    public static final int LONG_CAPACITY = 8;
    private static final String osName = System.getProperty("os.name");
    public static final boolean isWindows = osName.toLowerCase().startsWith("windows");
    public static final boolean isOSX = osName.toLowerCase().contains("os x");
    public static final boolean isSolaris = osName.toLowerCase().contains("sunos");

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

    private static Date parseOSXDate(Iterator<String> it) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd hh:mm:ss yyyy", Locale.getDefault());
        while (it.hasNext()) {
            String line = it.next();
            if (line != null) {
                String[] dateStr = line.replaceAll("\\s+", " ").split(" ");
                if (dateStr.length > 10) {
                    try {
                        return sdf.parse(dateStr[5] + " " + dateStr[6] + " " + dateStr[7] + " " + dateStr[8]);
                    } catch (Exception e) {
                        System.err.printf("/nCould not parse date %s", Arrays.toString(dateStr));
                    }
                }
            }
        }
        return null;
    }

    private enum FileStat {
        CREATION_DATE
        , ACCESS_DATE
    }

    private static Date getFileStatsWindows(String filePath, FileStat fileStat) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm");
            String command = (fileStat == FileStat.CREATION_DATE ? "/TC" : "/TA");
            Process process = Runtime.getRuntime().exec(new String[]{"dir", command, filePath});
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] arr = line.split("\\s+");
                    String date = arr[0] + " " + arr[1];
                    try {
                        return sdf.parse(date);
                    } catch (ParseException ignored) {
                        System.err.printf("/nCould not parse date %s", date);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Date getFileStatsOSX(String filePath, FileStat fileStat) {
        try {
            String command = (fileStat == FileStat.CREATION_DATE ? "-laUT" : "-lauT");
            Process process = Runtime.getRuntime().exec(new String[]{"ls", command, filePath});
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return parseOSXDate(lines.iterator());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static long tryParseDate(String pattern, String value) {
        if (value != null) {
            try {
                return new SimpleDateFormat(pattern).parse(value).getTime();
            } catch (ParseException e) {
                System.err.printf("/nCould not parse date %s", value);
                return 0;
            }
        }
        System.err.println("Could not parse a null value");
        return 0;
    }
}
