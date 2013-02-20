package org.apache.lucene.search.exposed;

import org.apache.lucene.util.BytesRef;

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

  /**
   * Iterates the given term until 0 is found, then returns a new BytesRef with
   * everything after 0.
   * @param concat a term containing a collator key followed by a human readable
   *               value (normally an UTF-8 String).
   * @param reuse  optional destination for the human readable part.
   * @return the human readable part of the concat.
   */
  public static BytesRef deConcat(BytesRef concat, BytesRef reuse) {
    for (int i = 0 ; i < concat.length ; i++) {
        if (concat.bytes[concat.offset+i] == 0) {
            if (reuse == null) {
                reuse = new BytesRef(new byte[concat.length-i+1]); // +1 to handle empty String
            } else if (reuse.bytes.length < concat.length-i+1) {
                reuse.bytes = new byte[concat.length-i+1];
            }
            System.arraycopy(concat.bytes, concat.offset+i+1,
                             reuse.bytes, 0, concat.length-i-1);
            reuse.offset = 0;
            reuse.length = concat.length-i-1;
            return reuse;
        }
    }
    return concat; // Input did not contain a 0

  }
}
