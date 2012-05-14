package org.apache.lucene.util.packed;

/**
 * Takes a reader with monotonic increasing or decreasing values and produces
 * a reduced memory reader with the same content. The amount of memory reduction
 * depends on the layout of the given Reader. With a uniform increment of
 * values the savings are substantial. If increments changes between very small
 * and very large increments, the MonotonicReader might use more memory.
 * </p><p>
 * The cost of reducing the memory requirements is a small increase in access
 * time.
 */
// TODO: Implement this
public class MonotonicReaderFactory {

  /**
   * Attempts to create a new reader with the same content but with reduced
   * memory requirements.
   * @param reader a monotonic increasing or decreasing reader.
   * @return a new Reader with the same content or the given reader if it was
   * not possible to produce a new reader with smaller memory requirements.
   */
  public static PackedInts.Reader reduce(PackedInts.Reader reader) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
