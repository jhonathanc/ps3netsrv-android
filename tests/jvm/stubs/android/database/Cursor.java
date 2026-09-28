package android.database;
public interface Cursor {
  boolean moveToNext();
  String getString(int index);
  long getLong(int index);
  void close();
}
