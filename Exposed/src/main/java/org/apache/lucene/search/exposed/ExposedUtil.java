package org.apache.lucene.search.exposed;

public class ExposedUtil {
  private ExposedUtil() {
    // Disables all constructors
  }

  public static String time(String type, long num, long ms) {
    if (ms == 0) {
      return num + " " + type + " in 0 ms";
    }
    return num + " " + type + " in " + ms + " ms: ~= "
        + num/ms + " " + type + "/ms";
  }
}
