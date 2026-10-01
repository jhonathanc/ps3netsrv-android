package com.jhonju.ps3netsrv;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.multidex.MultiDexApplication;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jhonju.ps3netsrv.server.io.FileCustom;
import com.jhonju.ps3netsrv.server.io.IFile;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.*;

/** Run on API 14, API 21 and a recent API to exercise the packaged DEX code. */
@RunWith(AndroidJUnit4.class)
public class DesugaringCompatibilityTest {
    @Test
    public void applicationAndCollectionApisLoad() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(context.getApplicationContext() instanceof MultiDexApplication);
        assertSame(context, Objects.requireNonNull(context));

        Map<String, List<String>> children = new HashMap<>();
        List<String> files = children.computeIfAbsent("root", key -> new ArrayList<>());
        files.addAll(Arrays.asList("z.iso", null, "A.iso", "b.iso"));
        files.sort(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        assertEquals(Arrays.asList(null, "A.iso", "b.iso", "z.iso"), children.get("root"));
        assertSame(files, children.computeIfAbsent("root", key -> new ArrayList<>()));

        files.sort(Comparator.comparingInt((String name) -> name == null ? 0 : name.length())
                .thenComparing(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        assertEquals(Arrays.asList(null, "A.iso", "b.iso", "z.iso"), files);
        assertEquals("content:folder/a+b c", Uri.decode("content:folder/a+b%20c"));
    }

    @Test
    public void defaultReadAndAutomaticCloseWork() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File path = File.createTempFile("desugaring", ".bin", context.getCacheDir());
        try {
            try (FileOutputStream out = new FileOutputStream(path)) {
                out.write(new byte[]{1, 2, 3});
            }
            FileCustom file = new FileCustom(path);
            try (IFile opened = file) {
                byte[] bytes = new byte[2];
                assertEquals(2, opened.read(bytes, 1));
                assertArrayEquals(new byte[]{2, 3}, bytes);
            }
            try {
                file.read(new byte[1], 0);
                fail("Closed files must reject reads");
            } catch (IOException expected) {
                assertEquals("File is closed", expected.getMessage());
            }
        } finally {
            assertTrue(path.delete());
        }
    }

    @Test
    public void closeFailurePreservesTheOriginalException() {
        IOException readFailure = new IOException("read");
        IOException closeFailure = new IOException("close");
        try (Closeable resource = () -> { throw closeFailure; }) {
            throw readFailure;
        } catch (IOException actual) {
            assertSame(readFailure, actual);
            // API 14 must retain the primary failure; suppression is public from API 19.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                assertArrayEquals(new Throwable[]{closeFailure}, actual.getSuppressed());
            }
        }
    }
}
