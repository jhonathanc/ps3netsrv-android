package com.jhonju.ps3netsrv.server.utils;

import android.annotation.SuppressLint;

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
import java.util.Objects;

@SuppressLint("SimpleDateFormat")
public class Utils {

    private static final String osName = Objects.requireNonNull(System.getProperty("os.name"));
    public static final boolean isWindows = osName.toLowerCase().startsWith("windows");
    public static final boolean isOSX = osName.toLowerCase().contains("os x");
    public static final boolean isSolaris = osName.toLowerCase().contains("sunos");

    public static byte[] toByteArray(Object obj) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (obj instanceof List<?>) {
                listObjectToByteArray((List<?>) obj, out);
            } else {
                singleObjectToByteArray(obj, out);
            }
            return out.toByteArray();
        }
    }

    private static void listObjectToByteArray(List<?> list, ByteArrayOutputStream out) throws Exception {
        for (Object o : list) {
            singleObjectToByteArray(o, out);
        }
    }

    private static void singleObjectToByteArray(Object obj, ByteArrayOutputStream out) throws Exception {
        Field[] fields = obj.getClass().getDeclaredFields();
        Arrays.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field field1, Field field2) {
                return field1.getName().compareTo(field2.getName());
            }
        });

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

    public static byte[] longToBytes(final long value) {
        byte[] result = new byte[8];
        for (int i = 0; i < 8; i++) {
            result[i] = (byte) (value >> (i * 8));
        }
        return result;
    }

    public static byte[] intToBytes(final int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static boolean isByteArrayEmpty(byte[] byteArray) {
        return (byteArray.length == 0 || Arrays.equals(byteArray, new byte[byteArray.length]));
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
                        System.err.println("Could not parse date " + Arrays.toString(dateStr));
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

    private static Date getFileStatsWindows(String filePath, FileStat fileStat) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm");
        String command = (fileStat == FileStat.CREATION_DATE ? "/TC" : "/TA");
        Process process = Runtime.getRuntime().exec(new String[] {"dir", command, filePath });
        process.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] arr = line.split("\\s+");
                String date = arr[0] + " " + arr[1];
                try {
                    return sdf.parse(date);
                } catch (ParseException ignored) {
                }
            }
        }
        return null;
    }

    private static Date getFileStatsOSX(String filePath, FileStat fileStat) throws Exception {
        String command = (fileStat == FileStat.CREATION_DATE ? "-laUT" : "-lauT");
        Process process = Runtime.getRuntime().exec(new String[] { "ls", command, filePath });
        process.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return parseOSXDate(lines.iterator());
        }
    }

    public static long[] getFileStats(File file) throws Exception {
        long[] stats = { 0, 0 };
        String filePath = file.getCanonicalPath();
        if (isWindows) {
            Date creationDate = getFileStatsWindows(filePath, FileStat.CREATION_DATE);
            if (creationDate != null) {
                stats[0] = creationDate.getTime();
            }

            Date accessDate = getFileStatsWindows(filePath, FileStat.ACCESS_DATE);
            if (accessDate != null) {
                stats[1] = accessDate.getTime();
            }
        } else if (isOSX) {
            Date creationDate = getFileStatsOSX(filePath, FileStat.CREATION_DATE);
            if (creationDate != null) {
                stats[0] = creationDate.getTime();
            }

            Date accessDate = getFileStatsOSX(filePath, FileStat.ACCESS_DATE);
            if (accessDate != null) {
                stats[1] = accessDate.getTime();
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
                stats[0] = Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(line)).getTime();
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

                    cal.setTime(Objects.requireNonNull(new SimpleDateFormat("MMM").parse(month)));
                    if (cal.get(Calendar.MONTH) > actualMonth) year--;

                    String dateString = month + " " + parts[6] + " " + year + " " + parts[7];
                    stats[1] = Objects.requireNonNull(new SimpleDateFormat("MMM dd yyyy HH:mm").parse(dateString)).getTime();
                }
            }
        } else {
            stats[0] = file.lastModified();

            Process process = Runtime.getRuntime().exec(new String[] {"stat", "-c", "%x", filePath});
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    stats[1] = Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS Z").parse(line)).getTime();
                }
            }
        }
        return stats;
    }
}
