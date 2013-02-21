package org.apache.lucene.search.exposed;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RawCollationKey;
import org.apache.lucene.util.BytesRef;

import java.util.HashMap;
import java.util.Map;

public class ExposedUtil {
  public static Map<String, Collator> collators =
      new HashMap<String, Collator>();

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

  //          for (int j = 0 ; j < concat.length ; j++) {
    //            System.out.print((concat.bytes[concat.offset+j]  & 0xFF) + " ");
      //      }
//            System.out.println("(" + reuse.utf8ToString() + ")");

            return reuse;
        }
    }
//    System.out.println("No 0 in " + concat.utf8ToString());
    return concat; // Input did not contain a 0
  }

  public static Collator addCollator(String collatorID, Collator collator) {
    return collators.put(collatorID, collator);
  }

  /**
   * Adds the collator to the given collator resolver structure and assigns the
   * collatorID to the given field names in {@link ExposedCache}.
   * @param collatorID an unique ID for the collator.
   * @param collator   an ICU collator.
   * @param fields     the fields that uses the collator for concat.
   * @return the old collator for the given collatorID or null if no previous
   *         collator were assigned.
   */
  public static Collator addCollator(
      String collatorID, Collator collator, String... fields) {
    for (String field: fields) {
      ExposedCache.getInstance().addConcatField(field, collatorID);
    }
    return collators.put(collatorID, collator);
  }

  private final static RawCollationKey key = new RawCollationKey();
  public static synchronized BytesRef concat(
      String collatorID, BytesRef plain, BytesRef reuse) {
    Collator collator = collators.get(collatorID);
    if (collator == null) {
      throw new IllegalArgumentException(
          "No collator with ID '" + collatorID + "'. "
          + "Please add it with ExposedUtil.addCollator");
    }

    collator.getRawCollationKey(plain.utf8ToString(), key);
    int length = key.size + plain.length;
    if (reuse == null) {
      reuse = new BytesRef(length);
    } else if(reuse.bytes.length < length) {
      reuse.bytes = new byte[length];
    }
    System.arraycopy(
        key.bytes, 0, reuse.bytes, 0, key.size);
    System.arraycopy(
        plain.bytes, plain.offset, reuse.bytes, key.size, plain.length);
    reuse.offset = 0;
    reuse.length = length;
    return reuse;
  }
}
