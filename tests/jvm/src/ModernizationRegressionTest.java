import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.commands.GetDirSizeCommand;
import com.jhonju.ps3netsrv.server.commands.IResult;
import com.jhonju.ps3netsrv.server.io.*;

public class ModernizationRegressionTest extends TestSupport {
  static Object construct(String name, Class<?>[] types, Object... args) throws Exception {
    Constructor<?> constructor = Class.forName("com.jhonju.ps3netsrv.server.commands." + name)
        .getDeclaredConstructor(types);
    constructor.setAccessible(true);
    return constructor.newInstance(args);
  }

  static byte[] expected(long[] numbers, Short nameLength, Boolean directory, String name) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);
    for (long number : numbers) out.writeLong(number);
    if (nameLength != null) out.writeShort(nameLength);
    if (directory != null) out.writeBoolean(directory);
    if (name != null) out.write(name.getBytes(StandardCharsets.UTF_8));
    return bytes.toByteArray();
  }

  static void wireFormats() throws Exception {
    IResult opened = (IResult) construct("OpenFileCommand$OpenFileResult",
        new Class<?>[]{long.class, long.class}, 0x0102030405060708L, 1234567L);
    check(Arrays.equals(opened.toByteArray(), expected(new long[]{0x0102030405060708L, 1234567L}, null, null, null)),
        "OPEN_FILE preserves the exact 16-byte big-endian response");
    IResult missing = (IResult) construct("OpenFileCommand$OpenFileResult", new Class<?>[0]);
    check(Arrays.equals(missing.toByteArray(), expected(new long[]{-1, -1}, null, null, null)),
        "OPEN_FILE preserves failure sentinels");
    IResult stat = (IResult) construct("StatFileCommand$StatFileResult",
        new Class<?>[]{long.class, long.class, long.class, long.class, boolean.class},
        0x100000001L, 10L, 20L, 30L, false);
    check(Arrays.equals(stat.toByteArray(), expected(new long[]{0x100000001L, 10, 20, 30}, null, false, null)),
        "STAT_FILE preserves 64-bit sizes and its 33-byte response");

    String name = "ação.iso";
    IResult entry = (IResult) construct("ReadDirEntryCommand$ReadDirEntryResult",
        new Class<?>[]{long.class, short.class, boolean.class, String.class}, 7L, (short) name.length(), false, name);
    check(Arrays.equals(entry.toByteArray(), expected(new long[]{7}, (short) name.length(), false, name)),
        "READ_DIR_ENTRY preserves the header and UTF-8 payload without extra padding");
    IResult entryV2 = (IResult) construct("ReadDirEntryCommandV2$ReadDirEntryResultV2",
        new Class<?>[]{long.class, long.class, long.class, long.class, short.class, boolean.class, String.class},
        7L, 10L, 20L, 30L, (short) name.length(), true, name);
    check(Arrays.equals(entryV2.toByteArray(), expected(new long[]{7, 10, 20, 30}, (short) name.length(), true, name)),
        "READ_DIR_ENTRY_V2 preserves timestamps, directory flag and UTF-8 payload");
    IResult empty = (IResult) construct("ReadDirEntryCommand$ReadDirEntryResult", new Class<?>[0]);
    IResult emptyV2 = (IResult) construct("ReadDirEntryCommandV2$ReadDirEntryResultV2", new Class<?>[0]);
    check(Arrays.equals(empty.toByteArray(), new byte[11]) && Arrays.equals(emptyV2.toByteArray(), new byte[35]),
        "empty directory entry replies retain their original lengths");

    Object record = construct("ReadDirCommand$ReadDirEntry",
        new Class<?>[]{long.class, long.class, boolean.class, String.class}, 7L, 10L, false, name);
    IResult listing = (IResult) construct("ReadDirCommand$ReadDirResult",
        new Class<?>[]{List.class}, Collections.singletonList(record));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    bytes.write(expected(new long[]{1, 7, 10}, null, false, null));
    byte[] paddedName = new byte[512];
    byte[] encoded = name.getBytes(StandardCharsets.UTF_8);
    System.arraycopy(encoded, 0, paddedName, 0, encoded.length);
    bytes.write(paddedName);
    check(Arrays.equals(listing.toByteArray(), bytes.toByteArray()),
        "READ_DIR writes its count and fixed records directly without changing any bytes");
  }

  static class TrackedFile extends FileCustom {
    int closeCount;
    final Map<String, IFile> children = new HashMap<>();
    TrackedFile(File file) throws IOException { super(file); }
    public IFile findFile(String name) throws IOException {
      return children.containsKey(name) ? children.get(name) : super.findFile(name);
    }
    public void close() throws IOException { closeCount++; super.close(); }
  }

  static void sfoOwnership(Path base) throws Exception {
    Path game = directory(base, "sfo-game");
    Path ps3 = directory(game, "PS3_GAME");
    Path path = ps3.resolve("PARAM.SFO");
    byte[] data = new byte[55];
    ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(0, 0x46535000).putInt(8, 36).putInt(12, 45).putInt(16, 1).putInt(24, 10);
    System.arraycopy("TITLE_ID\0".getBytes(StandardCharsets.US_ASCII), 0, data, 36, 9);
    System.arraycopy("BCES00104\0".getBytes(StandardCharsets.US_ASCII), 0, data, 45, 10);
    for (boolean valid : new boolean[]{true, false}) {
      Files.write(path, valid ? data : new byte[12]);
      TrackedFile sfo = new TrackedFile(path.toFile());
      TrackedFile directory = new TrackedFile(ps3.toFile());
      directory.children.put("PARAM.SFO", sfo);
      try (TrackedFile root = new TrackedFile(game.toFile())) {
        root.children.put("PS3_GAME", directory);
        String title = ParamSfoParser.getTitleId(root);
        check(valid ? "BCES00104".equals(title) : title == null, "SFO parsing valid=" + valid);
        check(sfo.closeCount == 1 && directory.closeCount == 1 && root.closeCount == 0,
            "SFO parser closes owned handles and retains the caller's root, valid=" + valid);
      }
    }
    Files.write(path, data);
    try (IFile root = FileRegressionTest.document(game)) {
      check("BCES00104".equals(ParamSfoParser.getTitleId(root)), "SFO parsing works through SAF");
      check(android.os.ParcelFileDescriptor.openCount == 0, "SFO parser releases SAF descriptors immediately");
    }
  }

  static void directorySizesAndPaths(Path base) throws Exception {
    Path first = directory(base, "size-first"), second = directory(base, "size-second");
    Files.write(first.resolve("a.bin"), new byte[5]);
    Files.write(directory(first, "nested").resolve("b.bin"), new byte[7]);
    Files.write(second.resolve("c.bin"), new byte[3]);
    for (boolean saf : new boolean[]{false, true}) {
      String prefix = saf ? "content:" : "";
      MemorySocket socket = new MemorySocket("/");
      try (Context ctx = context(socket, prefix + first, prefix + second)) {
        new GetDirSizeCommand(ctx, (short) 1).executeTask();
        check(ByteBuffer.wrap(socket.output.toByteArray()).getLong() == 15,
            "directory size sums nested files and multiple roots, SAF=" + saf);
      }
      check(android.os.ParcelFileDescriptor.openCount == 0, "directory size releases SAF descriptors");
    }
    Files.write(first.resolve("shared.bin"), new byte[5]);
    Files.write(second.resolve("shared.bin"), new byte[3]);
    MemorySocket sizeSocket = new MemorySocket("/shared.bin");
    try (Context ctx = context(sizeSocket, first.toString(), second.toString())) {
      new GetDirSizeCommand(ctx, (short) 11).executeTask();
      check(ByteBuffer.wrap(sizeSocket.output.toByteArray()).getLong() == 8,
          "size traversal sums file matches instead of replacing the previous total");
    }
    for (String path : new String[]{"/a.bin\0\0", "/a.bin"}) {
      try (Context ctx = context(new MemorySocket(path), first.toString())) {
        Set<IFile> files = new ProbeCommand(ctx, path).resolve();
        ctx.setFile(files);
        check(files.size() == 1 && files.iterator().next().length() == 5,
            "path normalization accepts an optional trailing NUL suffix");
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Path base = Files.createTempDirectory(Paths.get(args[0]), "modernization-");
    wireFormats();
    sfoOwnership(base);
    directorySizesAndPaths(base);
  }
}
