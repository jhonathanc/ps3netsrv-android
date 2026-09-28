package android.content;
import java.io.File;
import java.util.*;
public class Context {
  private final Map<String,MemoryPreferences> prefs = new HashMap<>();
  private final ContentResolver resolver = new ContentResolver();
  public SharedPreferences getSharedPreferences(String key,int mode) {
    if (!prefs.containsKey(key)) prefs.put(key,new MemoryPreferences());
    return prefs.get(key);
  }
  public void clear() { for (MemoryPreferences p : prefs.values()) p.clear(); }
  public android.content.res.Resources getResources() { return new android.content.res.Resources(); }
  public File getExternalFilesDir(String type) { return new File("external-default"); }
  public File getFilesDir() { return new File("internal-default"); }
  public String getString(int id,Object... args) { return "resource-" + id; }
  public ContentResolver getContentResolver() { return resolver; }
}
