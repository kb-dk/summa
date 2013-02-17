package org.apache.lucene.util.packed;

/**
 * Special purpose pair of PackedInts structures.
 */
public class PackedPair {
  private final PackedInts.Mutable primaries;
  private final PackedInts.Mutable secondaries;
  private final long secondaryOffset;
  private int size = 0;

  public PackedPair(
      int length, int primaryBPV, int secondaryBPV, long secondaryOffset) {
    primaries = PackedInts.getMutable(length, primaryBPV, 0);
    secondaries = PackedInts.getMutable(length, secondaryBPV, 0);
    this.secondaryOffset = secondaryOffset;
  }

  public void add(long primary, long secondary) {
    primaries.set(size, primary);
    secondaries.set(size++, secondary);
  }

  public void countUniquePrimaries(int[] counters) {
    for (int i = 0 ; i < size ; i++) {
      counters[(int)primaries.get(i)]++;
    }
  }

  public void assignSecondaries(int[] offsets, PackedInts.Mutable destination) {
    for (int i = 0 ; i < size; i++) {
      try {
        destination.set(offsets[(int)primaries.get(i)]++,
                        secondaryOffset + secondaries.get(i));
      } catch (ArrayIndexOutOfBoundsException e) {
        String primariesGet;
        try {
          primariesGet = Long.toString(primaries.get(i));
        } catch (ArrayIndexOutOfBoundsException e2) {
          primariesGet = "N/A";
        }
        String secondariesGet;
        try {
          secondariesGet = Long.toString(secondaries.get(i));
        } catch (ArrayIndexOutOfBoundsException e2) {
          secondariesGet = "N/A";
        }
        throw new ArrayIndexOutOfBoundsException(String.format(
            "ArrayIndexOutOfBounds in destination{size=%d}.set(offsets" +
            "{length=%d}[(int)primaries{size=%d}.get(i{=%d}){=%s}]++, " +
            "secondaryOffset{=%d} + secondaries{size=%d}.get(i{=%d}){=%s})",
            destination.size(), offsets.length, primaries.size(), i,
            primariesGet, secondaryOffset, secondaries.size(), i,
            secondariesGet));
      }
    }
  }

  public PackedInts.Mutable getPrimaries() {
    return primaries;
  }

  public PackedInts.Mutable getSecondaries() {
    return secondaries;
  }


}
