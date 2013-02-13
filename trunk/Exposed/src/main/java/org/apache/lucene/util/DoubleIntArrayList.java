package org.apache.lucene.util;

import org.apache.lucene.util.packed.PackedInts;

import java.util.Arrays;

/**
 * Specialized high performance expandable list of integer pairs that can be sorted on both primary and secondary key.
 * </p><p>
 * This implementation is not thread safe.
 */
public class DoubleIntArrayList {

  private ChunkedLongArray pairs;

  public DoubleIntArrayList(int initialCapacity) {
    // TODO: Make a clever guess of chunkBits to scale down
    pairs = new ChunkedLongArray();
  }

  /**
   * Adds the given pair to the list. When the internal list capacity is exceeded, it is expanded, which involves
   * an array copy.
   * @param primary   primary value.
s   * @param secondary secondary value.
   */
  public void add(int primary, int secondary) {
    pairs.add((long)primary << 32 | (long)secondary);
  }

  public void set(int index, int primary, int secondary) {
    pairs.set(index, (long)primary << 32 | (long)secondary);
  }

  /*
   * Wrapper for {@link Arrays#binarySearch(int[], int)}. JavaDoc for return is
   * taken from Array's JavaDoc.
   * @param primary the primary key to search.
   * @return index of the search key, if it is contained in the array within the
   * specified range; otherwise, <tt>(-(insertion point) - 1)</tt>.
   * The insertion point is defined as the point at which the key would be
   * inserted into the array: the index of the first element in the range
   * greater than the key, or toIndex if all elements in the range are less than
   * the specified key. Note that this guarantees that the return value will be
   * <tt>&gt;= 0</tt> if and only if the key is found.
   */
/*  public int searchPrimary(int primary) {
    return pairs.binarySearch(0, pairs.size(), (long)primary << 32);
  }*/

  public void sortByPrimaries() {
    pairs.sort();
  }

/*  public void sortBySecondaries() {
    secondarySort(pairs, 0, size);
  }*/

  /*
   * Classic merge sort with memory overhead equal to the full input.
   * </p><p>
   * The article http://en.wikipedia.org/wiki/Merge_sort was used as base for
   * this code.
   * @param references the references to sort.
   * @param start      the index of the first reference to sort (inclusive).
   * @param end        the index of the last reference to sort (exclusive).
   */
 /* private void secondarySort(long[] references, int start, int end) {
    if (end - start <= 1) {
      return;
    }

    int middle = (end - start) / 2 - 1 + start;
    secondarySort(references, start, middle + 1);
    secondarySort(references, middle + 1, end);
    secondaryMerge(references, start, middle + 1, end);
  }*/

  /*
   * Expects the references from start (inclusive) to middle (exclusive) to
   * be sorted and the references from middle (inclusive) to end (exclusive)
   * to be sorted and performs a merge, with the result assigned to references
   * starting from start.
   *
   * @param pairs  the pairs to merge.
   * @param start  the start of the pairs to merge.
   * @param middle the middle of the pairs to merge.
   * @param end    the end of the pairs to merge.
   */
 /* private void secondaryMerge(long[] pairs, int start, int middle, int end) {
    if (middle - start == 0 || end - middle == 0) {
      return;
    }
    long[] result = new long[end - start];
    int iLeft = start;
    int iRight = middle;
    int iResult = 0;
    while (iLeft < middle && iRight < end) {
      int vLeft = (int) pairs[iLeft];
      int vRight = (int) pairs[iRight];
      result[iResult++] = vLeft < vRight ? pairs[iLeft++] : pairs[iRight++];
    }
    while (iLeft < middle) {
      result[iResult++] = pairs[iLeft++];
    }
    while (iRight < end) {
      result[iResult++] = pairs[iRight++];
    }
    System.arraycopy(result, 0, pairs, start, result.length);
  }*/

  public int getPrimary(int index) {
    if (index >= pairs.size()) {
      throw new ArrayIndexOutOfBoundsException(
          "Requested value @ index " + index + " with array length "
          + pairs.size());
    }
    return (int) (pairs.get(index) >>> 32);
  }

  public int getSecondary(int index) {
    if (index >= pairs.size()) {
      throw new ArrayIndexOutOfBoundsException(
          "Requested value @ index " + index + " with array length "
          + pairs.size());
    }
    return (int) pairs.get(index); // Discards the upper 32 bit
  }

  public PackedInts.Mutable getPacked() {
    if (pairs.size() == 0) {
      return PackedInts.getMutable(0, 1, 0);
    }

    // Find max
    int maxPrimary = Integer.MIN_VALUE;
    int maxSecondary = Integer.MIN_VALUE;
    for (int i = 0 ; i < pairs.size() ; i++) {
      final int primary = getPrimary(i);
      final int secondary = getSecondary(i);
      if (maxSecondary < secondary) {
        maxSecondary = secondary;
      }
      if (maxPrimary < primary) {
        maxPrimary = primary;
      }
    }
    PackedInts.Mutable result = PackedInts.getMutable(
        maxPrimary+1, PackedInts.bitsRequired(maxSecondary), 0);
    for (int i = 0 ; i < pairs.size() ; i++) {
      result.set(getPrimary(i), getSecondary(i));
    }
    return result;
  }

  public PackedInts.Mutable getPrimariesPacked() {
    if (pairs.size() == 0) {
      return PackedInts.getMutable(0, 1, 0);
    }

    int max = Integer.MIN_VALUE;
    for (int i = 0 ; i < pairs.size() ; i++) {
      max = Math.max(max, getPrimary(i));
    }
    PackedInts.Mutable result = PackedInts.getMutable(
        pairs.size(), PackedInts.bitsRequired(max), 0);
    for (int i = 0 ; i < pairs.size() ; i++) {
      result.set(i, getPrimary(i));
    }
    return result;
  }

  public PackedInts.Mutable getSecondariesPacked() {
    if (pairs.size() == 0) {
      return PackedInts.getMutable(0, 1, 0);
    }

    int max = Integer.MIN_VALUE;
    for (int i = 0 ; i < pairs.size() ; i++) {
      max = Math.max(max, getSecondary(i));
    }
    PackedInts.Mutable result;
    try {
      result = PackedInts.getMutable(
          pairs.size(), PackedInts.bitsRequired(max), 0);
    } catch (OutOfMemoryError e) {
      throw new OutOfMemoryError(String.format(
          "OOM (%s) while calling PackedInts.getMutable(%d, %d) with estimated"
          + " heap requirement %dMB. %s. %s",
          e.toString(), pairs.size(), PackedInts.bitsRequired(max),
          1L*pairs.size()*PackedInts.bitsRequired(max)/8/1048576,
          pairs, ChunkedLongArray.memStats()));
    }
    for (int i = 0 ; i < pairs.size() ; i++) {
      result.set(i, getSecondary(i));
    }
    return result;
  }

  /**
   * Extracts the added primary values. Note that the full list is traversed and a new integer array is allocated.
   * The time complexity is O(n).
   * @return all primary values.
   */
  public int[] getPrimaries() {
    final int[] result = new int[pairs.size()];
    for (int i = 0 ; i < pairs.size() ; i++) {
      result[i] = (int) (pairs.get(i) >>> 32);
    }
    return result;
  }

  /**
   * Extracts the added secondary values. Note that the full list is traversed and a new integer array is allocated.
   * The time complexity is O(n).
   * @return all secondary values.
   */
  public int[] getSecondaries() {
    final int[] result = new int[pairs.size()];
    for (int i = 0 ; i < pairs.size() ; i++) {
      result[i] = (int) pairs.get(i); // Discards the upper 32 bit
    }
    return result;
  }

  public int size() {
    return pairs.size();
  }

  /**
   * Note: Capacity auto-increases.
   * @return the current capacity.
   */
  public int capacity() {
    return pairs.capacity();
  }

  @Override
  public String toString() {
    return "DoubleIntArrayList(" + pairs + ")";
  }
}
