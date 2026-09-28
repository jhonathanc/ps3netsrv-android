package android.os;
import java.io.*;
public class ParcelFileDescriptor implements Closeable {
  public static int openCount;
  private final RandomAccessFile file;
  private boolean closed;
  public ParcelFileDescriptor(File path) throws IOException { file=new RandomAccessFile(path,"r"); openCount++; }
  public FileDescriptor getFileDescriptor() {
    try { return file.getFD(); } catch(IOException e) { throw new IllegalStateException(e); }
  }
  public void close() throws IOException { if(!closed) { closed=true; openCount--; file.close(); } }
}
