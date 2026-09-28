package android.net;
public class Uri {
  private final String value;
  private Uri(String value) { this.value = value; }
  public static Uri parse(String value) { return new Uri(value); }
  public String toString() { return value; }
}
