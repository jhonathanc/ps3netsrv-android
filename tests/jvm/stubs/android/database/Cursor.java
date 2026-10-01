package android.database;
public interface Cursor extends java.io.Closeable {
  boolean moveToNext();
  String getString(int index);
  long getLong(int index);
  void close();
}
