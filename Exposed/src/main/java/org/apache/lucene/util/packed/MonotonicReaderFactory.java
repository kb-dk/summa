package org.apache.lucene.util.packed;

import org.apache.lucene.search.exposed.ExposedSettings;

/**
 * Takes a reader with monotonic increasing values and produces
 * a reduced memory reader with the same content. The amount of memory reduction
 * depends on the layout of the given Reader. With a uniform increment of
 * values, the savings are substantial. If increments changes between very small
 * and very large increments, the MonotonicReader might use more memory. In that
 * case, the original reader is returned.
 * </p><p>
 * The cost of reducing the memory requirements is a small increase in access
 * time.
 */
// TODO: Implement this
public class MonotonicReaderFactory {

  public static final int DEFAULT_BLOCK_BITS = 8;
  /**
   * The size of the reduced Reader must be <=
   * {@code MIN_SAVING*reader.ramBytesUsed()} in order to be created.
   */
  public static final double MIN_SAVING = 0.8d;

  /**
   * Attempts to create a new reader with the same content but with reduced
   * memory requirements.
   * @param reader a monotonic increasing or decreasing reader.
   * @return a new Reader with the same content or the given reader if it was
   * not possible to produce a new reader with smaller memory requirements.
   */
  public static PackedInts.Reader reduce(PackedInts.Reader reader) {
    return reduce(reader, DEFAULT_BLOCK_BITS);
  }

  /**
   * Attempts to create a new reader with the same content but with reduced
   * memory requirements.
   * @param reader a monotonic increasing or decreasing reader.
   * @param blockBits the block size is 2^blockBits.
   * @return a new Reader with the same content or the given reader if it was
   * not possible to produce a new reader with smaller memory requirements.
   */
  public static PackedInts.Reader reduce(
      PackedInts.Reader reader, int blockBits) {
    final long startTime = System.currentTimeMillis();
    final int step = (int) Math.pow(2, blockBits);
    if (reader.size() <= step) {
      return reader;
    }
    long maxDelta = Long.MIN_VALUE;
    long[] bases = new long[reader.size() / step + 1];
    for (int i = 0 ; i < reader.size() ; i += step) {
      bases[i/step] = reader.get(i);
      maxDelta = i == 0 ?
                 reader.get(i) :
                 Math.max(maxDelta, reader.get(i-1) - bases[(i-1)/step]);
    }

    long mem = bases.length*8
               + reader.size()*PackedInts.bitsRequired(maxDelta)/8;
    boolean proceed = mem * MIN_SAVING <= reader.ramBytesUsed();
    PackedInts.Reader result =
        proceed ? createMonotonic(reader, bases, blockBits, maxDelta) : reader;
    if (ExposedSettings.debug) {
      System.out.println(String.format(
          "MonotonicReaderFactory: reader(size=%d, bpv=%d, heap=%dKB)"
          + " -> monotonic(bpv=%d, heap=%dKB). %s%dms",
          reader.size(), reader.getBitsPerValue(), reader.ramBytesUsed()/1024,
          PackedInts.bitsRequired(maxDelta), mem/1024,
          proceed ? "Conversion performed in " : "conversion skipped in ",
          System.currentTimeMillis()-startTime));
    }
    return result;
  }

  private static PackedInts.Reader createMonotonic(
      PackedInts.Reader reader, long[] bases, int blockBits, long maxDelta) {
    PackedInts.Mutable backing = PackedInts.getMutable(
        reader.size(), PackedInts.bitsRequired(maxDelta), 0);
    for (int i = 0 ; i < reader.size() ; i++) {
      backing.set(i, reader.get(i) - bases[i >>> blockBits]);
    }
    return new Monotonic(backing, bases, blockBits, reader.getBitsPerValue());
  }

  private static class Monotonic extends PackedInts.ReaderImpl {
    private final PackedInts.Reader backing;
    // TODO: Make this a reader
    private final long[] bases;
    private final long blockBits;

    private Monotonic(PackedInts.Reader backing, long[] bases, long blockBits,
                      int originalBPV) {
      super(backing.size(), originalBPV);
      this.backing = backing;
      this.bases = bases;
      this.blockBits = blockBits;
    }

    @Override
    public long get(int index) {
      return backing.get(index) + bases[index >>> blockBits];
    }

    @Override
    public long ramBytesUsed() {
      return bases.length*8 + backing.ramBytesUsed();
    }
  }

}
