package org.apache.lucene.search.exposed;

import org.apache.lucene.util.packed.PackedInts;

public class ExposedSettings {
  /**
   * If the field does not support ordinals, a caching structure is created. A BytesRef and a TermState will be
   * created for every enumDivider enum.
   */
  public static int enumDivider = 128;
  /**
   * Whether or not sparse TagCollectors should be used. Sparse collectors are experimental.
   */
  public static boolean useSparseCollector = false;
  /**
   * if {@link #useSparseCollector} is true and forceSparseCollector is true, a sparse collector will always
   * be used, even when a multi collector would be better. Mainly intended for testing.
   */
  public static boolean forceSparseCollector = false;

  public static enum PRIORITY {memory, speed;
    public static PRIORITY fromString(String p) {
      if (memory.toString().equals(p)) {
        return memory;
      } else if (speed.toString().equals(p)) {
        return speed;
      }
      throw new IllegalArgumentException("The String '" + p + "' was not recognized as a priority");
    }
  }
  public static final PRIORITY DEFAULT_PRIORITY = PRIORITY.memory;

  /**
   * The priority determines what is most important: memory or speed.
   * The setting is used whenever a PackedInts-structure is created to determine
   * if rounding to byte, short, int or long representations should be used or
   * if the optimal space saving representation should be used (no rounding).
   * </p><p>
   * The speed gain depends very much on the actual index and the setup for
   * sorting, faceting and index lookup, but a rough rule of thumb is that
   * memory usage increases 25-50% and speed increases 25-50% for faceting and
   * initial index look ups when speed is chosen.
   */
  public static PRIORITY priority = DEFAULT_PRIORITY;

  /**
   * If true, debug messages will be written to stdout.
   */
  public static boolean debug = false;

  /**
   * Where applicable, this number of threads will be used for speedup
   */
  public static int threads = 2;

  /**
   * Construct a PackedInts.Mutable based on the given values and the overall
   * priority between memory usage and speed. If the maxValue is below 2^4 of
   * between 2^32 and 2^40, a standard PackedInts.Mutable is created, else the
   * maxValue is rounded up to 2^8-1, 2^16-1, 2^32-1 or 2^64-1;
   * @param valueCount the number of values that the mutator should hold.
   * @param maxValue the maximum value that will ever be inserted.
   * @return a mutable optimized for either speed or memory.
   */
  public static PackedInts.Mutable getMutable(int valueCount, long maxValue) {
    int bitsRequired = PackedInts.bitsRequired(maxValue);
    switch (priority) {
      case memory: return PackedInts.getMutable( valueCount, bitsRequired, PackedInts.COMPACT);
      case speed:
        return bitsRequired <= 4 || bitsRequired > 32 && bitsRequired < 40 ?
               PackedInts.getMutable(valueCount, bitsRequired, PackedInts.COMPACT) :
               PackedInts.getMutable(valueCount, bitsRequired, PackedInts.FASTEST);
      default: throw new IllegalArgumentException("The priority " + priority + " is unknown");
    }
  }
}
