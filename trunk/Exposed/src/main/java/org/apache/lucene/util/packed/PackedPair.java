package org.apache.lucene.util.packed;

/**
 * Special purpose pair of PackedInts structures.
 */
public class PackedPair {
  private final PackedInts.Mutable primaries;
  private final PackedInts.Mutable secondaries;
  private final int secondaryOffset;
  private int size = 0;

  public PackedPair(
      int length, int primaryBPV, int secondaryBPV, int secondaryOffset) {
    primaries = PackedInts.getMutable(length, primaryBPV, 0);
    secondaries = PackedInts.getMutable(length, secondaryBPV, 0);
    this.secondaryOffset = secondaryOffset;
  }

  public void add(long primary, long secondary) {
    primaries.set(size, primary);
    secondaries.set(size++, secondary);
  }

  public void countUniquePrimaries(int[] counters) {
    for (int i = 0 ; i < primaries.size() ; i++) {
      counters[(int)primaries.get(i)]++;
    }
  }

  public void assignSecondaries(int[] offsets, PackedInts.Mutable destination) {
    for (int i = 0 ; i < size; i++) {
      destination.set(offsets[(int)primaries.get(i)]++,
                      secondaryOffset + secondaries.get(i));
    }
  }

  public PackedInts.Mutable getPrimaries() {
    return primaries;
  }

  public PackedInts.Mutable getSecondaries() {
    return secondaries;
  }


}
