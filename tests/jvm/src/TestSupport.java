import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.commands.FileCommand;
import com.jhonju.ps3netsrv.server.io.*;

class TestSupport {
  static class MemorySocket extends Socket {
    final InputStream input;
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    MemorySocket(byte[] input) { this.input = new ByteArrayInputStream(input); }
    MemorySocket(String input) { this(input.getBytes(StandardCharsets.UTF_8)); }
    public InputStream getInputStream() { return input; }
    public OutputStream getOutputStream() { return output; }
    public void setSoTimeout(int timeout) {}
  }
  static class ProbeCommand extends FileCommand {
    ProbeCommand(Context ctx, String path) { super(ctx, (short)path.getBytes(StandardCharsets.UTF_8).length); }
    Set<IFile> resolve() throws Exception { return getFile(); }
    public void executeTask() {}
  }
  static Context context(MemorySocket socket, String... roots) {
    return new Context(socket, Arrays.asList(roots), new android.content.ContentResolver(), new android.content.Context());
  }
  static Object field(Object object, String name) throws Exception {
    java.lang.reflect.Field field = object.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(object);
  }
  static void check(boolean value, String message) {
    if (!value) throw new AssertionError(message);
    System.out.println("PASS " + message);
  }
  interface Throwing { void run() throws Exception; }
  static void fails(Class<? extends Throwable> type, Throwing action, String message) throws Exception {
    try { action.run(); } catch (Throwable error) {
      if (!type.isInstance(error)) throw new AssertionError(message, error);
      check(true, message); return;
    }
    throw new AssertionError(message + " (no exception)");
  }
  static Path directory(Path parent, String name) throws IOException {
    return Files.createDirectories(parent.resolve(name));
  }
}
