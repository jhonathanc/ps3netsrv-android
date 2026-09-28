package android.content;
import android.net.Uri;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import java.io.*;
public class ContentResolver {
  public static File file(Uri uri) {
    String name=uri.toString(); return new File(name.startsWith("content:") ? name.substring(8) : name);
  }
  public ParcelFileDescriptor openFileDescriptor(Uri uri,String mode) throws IOException { return new ParcelFileDescriptor(file(uri)); }
  public InputStream openInputStream(Uri uri) throws IOException { return new FileInputStream(file(uri)); }
  public OutputStream openOutputStream(Uri uri) throws IOException { return new FileOutputStream(file(uri)); }
  public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order) {
    final File[] children=file(uri).listFiles();
    return new Cursor() {
      int index=-1;
      public boolean moveToNext() { return children!=null && ++index<children.length; }
      public String getString(int column) {
        File child=children[index];
        if(column==0) return child.getAbsolutePath();
        if(column==1) return child.getName();
        return child.isDirectory() ? "directory" : "application/octet-stream";
      }
      public long getLong(int column) { return children[index].length(); }
      public void close() {}
    };
  }
}
