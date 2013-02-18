package org.apache.lucene.util.packed;

/**
 * Special purpose pair of PackedInts structures.
 */
public class DoublePackedPair {
  private final PackedInts.Mutable values;
  private final long secondaryOffset;
  private final int primaryShift;
  private final int secondaryMask;
  private int size = 0;

  public DoublePackedPair(
      int length, int primaryBPV, int secondaryBPV, long secondaryOffset) {
    values = PackedInts.getMutable(length, primaryBPV+secondaryBPV, 0);
    this.secondaryOffset = secondaryOffset;
    primaryShift = secondaryBPV;
    secondaryMask = ~(~0 << primaryShift);
  }

  public void add(long primary, long secondary) {
    values.set(size++, primary << primaryShift | secondary);
  }

  public void countUniquePrimaries(int[] counters) {
    for (int i = 0 ; i < size ; i++) {
      counters[(int)(values.get(i) >>> primaryShift)]++;
    }
  }

  public void assignSecondaries(int[] offsets, PackedInts.Mutable destination) {
    for (int i = 0 ; i < size; i++) {
      final long value = values.get(i);
      destination.set(offsets[(int)(value >>> primaryShift)]++,
                      secondaryOffset + value & secondaryMask);
    }
  }

}
