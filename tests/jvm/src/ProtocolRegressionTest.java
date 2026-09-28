import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import com.jhonju.ps3netsrv.server.Context;
import com.jhonju.ps3netsrv.server.ContextHandler;
import com.jhonju.ps3netsrv.server.commands.*;
import com.jhonju.ps3netsrv.server.io.*;
import com.jhonju.ps3netsrv.server.utils.BinaryUtils;

public class ProtocolRegressionTest extends TestSupport {
  static void socketFailure(Path base) throws Exception {
    try(ServerSocket listener=new ServerSocket(0);
        Socket client=new Socket("127.0.0.1",listener.getLocalPort());
        Socket accepted=listener.accept()) {
      client.setSoTimeout(3000);
      ContextHandler worker=new ContextHandler(accepted,Collections.singletonList(base.toString()),
          new android.content.ContentResolver(),(t,e)->{},new android.content.Context());
      check(worker.startIfCapacity(1),"worker reserves connection slot");
      byte[] path="/small.bin".getBytes("UTF-8");
      OutputStream out=client.getOutputStream();
      out.write(ByteBuffer.allocate(16).putShort((short)0x1224).putShort((short)path.length).array());
      out.write(path);out.flush();
      byte[] reply=new byte[16];new DataInputStream(client.getInputStream()).readFully(reply);
      check(ByteBuffer.wrap(reply).getLong()==3,"socket OPEN_FILE preserves length response");
      out.write(ByteBuffer.allocate(16).putShort((short)0x1225).putShort((short)0).putInt(8).putLong(0).array());out.flush();
      check(client.getInputStream().read()==-1,"incomplete critical reply closes actual TCP connection");
      worker.join(3000);
      check(!worker.isAlive()&&ContextHandler.getSimultaneousConnections()==0,"failed read releases worker slot");
    }
    MemorySocket socket=new MemorySocket("");
    ContextHandler worker=new ContextHandler(socket,null,new android.content.ContentResolver(),(t,e)->{},new android.content.Context());
    worker.start();worker.join(3000);
    check(socket.isClosed()&&ContextHandler.getSimultaneousConnections()==0,"context construction failure closes socket and releases slot");
  }
  public static void main(String[] args) throws Exception {
    byte[] packet = new byte[16]; Arrays.fill(packet, (byte)7);
    InputStream fragmented = new ByteArrayInputStream(packet) {
      public synchronized int read(byte[] b, int off, int len) { return super.read(b, off, Math.min(len, 3)); }
    };
    check(Arrays.equals(BinaryUtils.readCommandData(fragmented, 16).array(), packet), "fragmented command is reassembled");
    check(BinaryUtils.readCommandData(fragmented, 16) == null, "clean EOF ends session");
    fails(EOFException.class, () -> BinaryUtils.readCommandData(new ByteArrayInputStream(new byte[15]), 16), "partial header is rejected");
    for (int size : new int[]{-1, BinaryUtils.BUFFER_SIZE + 1}) {
      fails(IOException.class, () -> BinaryUtils.readCommandData(new ByteArrayInputStream(packet), size), "invalid allocation size " + size);
    }
    Path base = Files.createTempDirectory(Paths.get(args[0]), "protocol-");
    Path small = base.resolve("small.bin"); Files.write(small, new byte[]{1,2,3});
    MemorySocket socket = new MemorySocket("");
    try (Context ctx = context(socket)) {
      ctx.setFile(Collections.<IFile>singleton(new FileCustom(small.toFile())));
      Arrays.fill(ctx.getOutputBuffer(), (byte)99);
      fails(EOFException.class, () -> new ReadFileCriticalCommand(ctx, 8, 0).executeTask(), "critical EOF is fatal");
      check(socket.output.size() == 0, "critical EOF never sends stale buffer bytes");
      new ReadFileCriticalCommand(ctx, 3, 0).executeTask();
      check(Arrays.equals(socket.output.toByteArray(), new byte[]{1,2,3}), "critical reply has exact payload and no header");
      socket.output.reset(); new ReadFileCommand(ctx, 8, 0).executeTask();
      check(Arrays.equals(socket.output.toByteArray(), new byte[]{0,0,0,3,1,2,3}), "normal short read has actual length header");
      socket.output.reset(); new ReadFileCommand(ctx, 8, 3).executeTask();
      check(Arrays.equals(socket.output.toByteArray(), new byte[4]), "normal EOF returns zero length");
      socket.output.reset(); new ReadFileCommand(ctx, -1, 0).executeTask();
      check(ByteBuffer.wrap(socket.output.toByteArray()).getInt() == -1, "invalid normal read returns error header");
    }
    try (Context ctx = context(new MemorySocket(""))) {
      fails(IOException.class, () -> new ReadFileCriticalCommand(ctx, 1, 0).executeTask(), "critical read without file closes session");
    }
    int size = BinaryUtils.BUFFER_SIZE + 8192;
    byte[] large = new byte[size]; for (int i=0; i<size; i++) large[i]=(byte)(i%127);
    Path big = base.resolve("large.bin"); Files.write(big, large);
    socket = new MemorySocket("");
    try (Context ctx = context(socket)) {
      // Force short reads within every output chunk, exercising the fill loop.
      ctx.setFile(Collections.<IFile>singleton(new FileCustom(big.toFile()) {
        public int read(byte[] b, int off, int len, long pos) throws IOException {
          return super.read(b, off, Math.min(len, 511), pos);
        }
      }));
      new ReadFileCriticalCommand(ctx, size, 0).executeTask();
      check(Arrays.equals(socket.output.toByteArray(), large), "critical streaming above 4 MiB tolerates short reads");
    }
    com.jhonju.ps3netsrv.app.SettingsService.setReadOnly(false);
    byte[] partial = new byte[]{'/', 's','m','a','l','l','.','b','i','n',9,8};
    try (Context ctx = context(new MemorySocket(partial), base.toString())) {
      fails(EOFException.class, () -> new WriteFileCommand(ctx, (short)10, 8).executeTask(), "truncated write payload is rejected");
      check(Arrays.equals(Files.readAllBytes(small), new byte[]{1,2,3}), "truncated write preserves file contents");
    }
    socketFailure(base);
    byte[] complete=new byte[]{'/','s','m','a','l','l','.','b','i','n',9,8};
    MemorySocket writeSocket=new MemorySocket(complete);
    try(Context ctx=context(writeSocket,base.toString())) {
      new WriteFileCommand(ctx,(short)10,2).executeTask();
      check(ByteBuffer.wrap(writeSocket.output.toByteArray()).getInt()==2
          && Arrays.equals(Files.readAllBytes(small),new byte[]{9,8}),"complete write still stores payload and acknowledges length");
    }
  }
}
