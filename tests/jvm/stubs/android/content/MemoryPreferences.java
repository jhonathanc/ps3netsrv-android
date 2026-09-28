package android.content;
import java.util.*;
public class MemoryPreferences implements SharedPreferences, SharedPreferences.Editor {
  private final Map<String,Object> data = new HashMap<>();
  public boolean contains(String k) { return data.containsKey(k); }
  public String getString(String k,String d) { return data.containsKey(k) ? (String)data.get(k) : d; }
  public int getInt(String k,int d) { return data.containsKey(k) ? (Integer)data.get(k) : d; }
  public boolean getBoolean(String k,boolean d) { return data.containsKey(k) ? (Boolean)data.get(k) : d; }
  @SuppressWarnings("unchecked")
  public Set<String> getStringSet(String k,Set<String> d) { return data.containsKey(k) ? (Set<String>)data.get(k) : d; }
  public Editor edit() { return this; }
  public Editor putString(String k,String v) { data.put(k,v); return this; }
  public Editor putInt(String k,int v) { data.put(k,v); return this; }
  public Editor putBoolean(String k,boolean v) { data.put(k,v); return this; }
  public Editor putStringSet(String k,Set<String> v) { data.put(k,new HashSet<>(v)); return this; }
  public void apply() {}
  public void clear() { data.clear(); }
}
