package androidx.documentfile.provider;
import android.content.*;
import android.net.Uri;
import java.io.*;
public class DocumentFile {
  private final File file;
  public DocumentFile(File file) { this.file=file; }
  public static DocumentFile fromTreeUri(Context ctx,Uri uri) { return new DocumentFile(ContentResolver.file(uri)); }
  public static DocumentFile fromSingleUri(Context ctx,Uri uri) { return fromTreeUri(ctx,uri); }
  public boolean exists() { return file.exists(); }
  public boolean isFile() { return file.isFile(); }
  public boolean isDirectory() { return file.isDirectory(); }
  public boolean delete() { return file.delete(); }
  public long length() { return file.length(); }
  public long lastModified() { return file.lastModified(); }
  public String getName() { return file.getName(); }
  public Uri getUri() { return Uri.parse(file.getAbsolutePath()); }
  public DocumentFile getParentFile() { return file.getParentFile()==null ? null : new DocumentFile(file.getParentFile()); }
  public DocumentFile findFile(String name) { File child=new File(file,name); return child.exists() ? new DocumentFile(child) : null; }
  public DocumentFile[] listFiles() {
    File[] files=file.listFiles(); if(files==null)return new DocumentFile[0];
    DocumentFile[] result=new DocumentFile[files.length];
    for(int i=0;i<files.length;i++)result[i]=new DocumentFile(files[i]); return result;
  }
  public DocumentFile createDirectory(String name) { File child=new File(file,name); return child.mkdir() ? new DocumentFile(child) : null; }
  public DocumentFile createFile(String mime,String name) {
    File child=new File(file,name);
    try { return child.createNewFile() ? new DocumentFile(child) : null; } catch(IOException e) { return null; }
  }
}
