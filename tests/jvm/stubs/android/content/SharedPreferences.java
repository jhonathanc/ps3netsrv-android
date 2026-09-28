package android.content;
import java.util.Set;
public interface SharedPreferences {
  boolean contains(String key);
  String getString(String key, String fallback);
  int getInt(String key, int fallback);
  boolean getBoolean(String key, boolean fallback);
  Set<String> getStringSet(String key, Set<String> fallback);
  Editor edit();
  interface Editor {
    Editor putString(String key, String value);
    Editor putInt(String key, int value);
    Editor putBoolean(String key, boolean value);
    Editor putStringSet(String key, Set<String> value);
    void apply();
  }
}
