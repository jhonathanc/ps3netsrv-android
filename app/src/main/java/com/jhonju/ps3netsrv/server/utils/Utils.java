package com.jhonju.ps3netsrv.server.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Utils {

    private static final String osName = System.getProperty("os.name");
    public static final boolean isWindows = osName.toLowerCase().startsWith("windows");
    public static final boolean isOSX = osName.toLowerCase().contains("os x");
    public static final boolean isSolaris = osName.toLowerCase().contains("sunos");

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
        for (Object o : list) {
            singleObjectToByteArray(o, out);
        }
    }

    private static void singleObjectToByteArray(Object obj, ByteArrayOutputStream out) throws Exception {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(obj);
            if (value != null) {
                if (value instanceof Boolean) {
                    out.write(((Boolean) value) ? 1 : 0);
                } else if (value instanceof Long) {
                    out.write(longToBytes((long) value));
                } else if (value instanceof Integer) {
                    out.write(intToBytes((int) value));
                } else if (value instanceof String) {
                    out.write(((String) value).getBytes(StandardCharsets.UTF_8));
                } else if (value instanceof char[]) {
                    out.write(charArrayToByteArray((char[]) value));
                }
            }
        }
    }

    private static byte[] charArrayToByteArray(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
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

    public static byte[] readCommandData(InputStream in, int size) throws IOException {
        byte[] data = new byte[size];
        if (in.read(data) < 0) return null;
        return data;
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
                        System.err.println("Could not parse date " + dateStr);
                    }
                }
            }
        }
        return null;
    }

    public static long[] getFileStats(File file) throws Exception {
        long[] stats = { 0, 0 };
        String filePath = file.getCanonicalPath();
        if (isWindows) {

        } else if (isOSX) {
            Process process = Runtime.getRuntime().exec(new String[] { "ls", "-laUT", filePath });
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                stats[0] = parseOSXDate(lines.iterator()).getTime();
            }

            process = Runtime.getRuntime().exec(new String[] { "ls", "-lauT", filePath });
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                stats[1] = parseOSXDate(lines.iterator()).getTime();
            }
        } else if (isSolaris) {
            Process process = Runtime.getRuntime().exec(new String[] { "ls", "-E", filePath, "| grep 'crtime=' | sed 's/^.*crtime=//'" });
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line == null) {
                System.err.println("Could not determine creation date for file: " + file.getName());
            } else {
                stats[0] = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(line).getTime();
            }

            process = Runtime.getRuntime().exec(new String[] { "ls", "-lauE", filePath });
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            line = reader.readLine();
            reader.close();
            if (line == null) {
                System.err.println("Could not determine last access date for file: " + file.getName());
            } else {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 8) {
                    String month = parts[5];

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new Date());
                    int year = cal.get(Calendar.YEAR);
                    int actualMonth = cal.get(Calendar.MONTH);

                    cal.setTime(new SimpleDateFormat("MMM").parse(month));
                    if (cal.get(Calendar.MONTH) > actualMonth) year--;

                    String dateString = month + " " + parts[6] + " " + year + " " + parts[7];
                    stats[1] = new SimpleDateFormat("MMM dd yyyy HH:mm").parse(dateString).getTime();
                }
            }
        } else {
            stats[0] = file.lastModified();

            Process process = Runtime.getRuntime().exec(new String[] {"stat", "-c", "%x", filePath});
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    stats[1] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS Z").parse(line).getTime();
                }
            }
        }
        return stats;
    }
}
