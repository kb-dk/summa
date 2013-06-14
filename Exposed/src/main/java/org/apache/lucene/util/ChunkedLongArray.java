/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.lucene.util;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Large contiguous memory allocations on the heap are not recommended as heap
 * fragmentation might result in an OOM event though the reported amount of
 * available heap is large enough.
 * </p><p>
 * The ChunkedLongArray maintains a list of long chunks internally and allows
 * for dynamic expansion by adding chunks. This allows for O(chunk_size) time
 * penalty for expansion with amortized time O(1).
 * </p><p>
 * The array provides optimized methods for emulating the System method
 * arrayCopy as sorting. Note that sorting uses in-place QuickSort which
 * has virtually no memory overhead. QuickSort has worst case O(n*n) running
 * time, although in practise the speed should be comparable to (n*log(n)).
 * // TODO: Perform more tests on actual performance
 */
public class ChunkedLongArray {
  public static final int DEFAULT_CHUNK_BITS = 20; // 1M entries = 8MB

  private final int chunkBits;
  private final int offsetMask;
  private final int chunkLength;
  private final List<long[]> chunks = new ArrayList<long[]>();
  private int size = 0;

  public ChunkedLongArray() {
    this(DEFAULT_CHUNK_BITS);
  }

  public ChunkedLongArray(int chunkBits) {
    this.chunkBits = chunkBits;
    chunkLength = (int) Math.pow(2, chunkBits);
    offsetMask = ~(~0 << chunkBits);
  }

  private ChunkedLongArray(List<long[]> chunks, int size, int chunkBits) {
    this(chunkBits);
    this.chunks.addAll(chunks);
    this.size = size;
  }

  public void add(long value) {
    set(size++, value);
  }

  public void set(int index, long value) {
    ensureSpace(index);
    chunks.get(index >>> chunkBits)[index &  offsetMask] = value;
    size = Math.max(size, index+1);
  }

  public long get(int index) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException("Index " + index + " requested with array length " + size);
    }
    return chunks.get(index >>> chunkBits)[index &  offsetMask];
  }

  public int size() {
    return size;
  }

  private void ensureSpace(final int index) {
    while (chunks.size() <= (index >>> chunkBits)) {
      try {
        chunks.add(new long[chunkLength]);
      } catch (OutOfMemoryError e) {
        throw new OutOfMemoryError(String.format(
            "OOM (%s) while allocating long[%d] (%dMB) in addition to the existing %d chunks (%d entries, %dMB). %s",
            e.toString(), chunkLength, 1L*chunkLength*8/1048576, chunks.size(), size(),
            1L*chunks.size()*chunkLength*8/1048576, memStats()));
      }
    }
  }

  public void set(ChunkedLongArray src, int srcPos, int destPos, int length) {
    while (length > 0) {
      int srcChunk = srcPos >>> src.chunkBits;
      int srcOffset = srcPos & src.offsetMask;
      if (src.chunks.get(srcChunk).length - srcOffset >= length) {
        set(src.chunks.get(srcChunk), srcOffset, destPos, length);
        return;
      }
      int subLength = src.chunkLength - srcOffset;
      set(src.chunks.get(srcChunk), srcOffset, destPos, subLength);
      length -= subLength;
      srcPos += subLength;
      destPos += subLength;
      size = Math.max(size, destPos+1);
    }
  }

  public void set(long[] src, int srcPos, int destPos, int length) {
    ensureSpace(destPos+length);
    while (length > 0) {
      int destChunk = destPos >>> chunkBits;
      int destOffset = destPos & offsetMask;
      if (chunkLength - destOffset >= length) {
        System.arraycopy(
            src, srcPos, chunks.get(destChunk), destOffset, length);
        return;
      }
      int subLength = chunkLength - destOffset;
      System.arraycopy(src, srcPos, chunks.get(destChunk), destOffset, subLength);
      length -= subLength;
      srcPos += subLength;
      destPos += subLength;
    }
  }

  private void swap(int index1, int index2) {
    long tmp = get(index1);
    set(index1, get(index2));
    set(index2, tmp);
  }

  public void sort() {
    quickSort();
  }

  // http://en.wikipedia.org/wiki/Quicksort#In-place_version
  private void quickSort() {
    quickSort(this, 0, size-1);
  }

  private void quickSort(ChunkedLongArray src, int left, int right) {
    // If the list has < 2 items
    if (left >= right) {
      return;
    }
    // Simple & dumb middle
    int pivotIndex = left + (right-left)/2;

    // Get lists of bigger and smaller items and final position of pivot
    int pivotNewIndex = partition(src, left, right, pivotIndex);

    // Recursively sort elements smaller than the pivot
    quickSort(src, left, pivotNewIndex - 1);

    // Recursively sort elements at least as big as the pivot
    quickSort(src, pivotNewIndex + 1, right);
  }

  // left is the index of the leftmost element of the array
  // right is the index of the rightmost element of the array (inclusive)
  // number of elements in subarray = right-left+1
  private int partition(
      ChunkedLongArray src, int left, int right, int pivotIndex) {
    long pivotValue = src.get(pivotIndex);
    src.swap(pivotIndex, right);  // Move pivot to end
    int storeIndex = left;
    for (int i = left ; i <= right-1 ; i++) {  // left ≤ i < right
      if (src.get(i) < pivotValue) {
        src.swap(i, storeIndex);
        storeIndex++;
      }
    }
    src.swap(storeIndex, right);  // Move pivot to its final place
    return storeIndex;
  }

  /*
   * Wrapper for {@link Arrays#binarySearch(int[], int)}. JavaDoc for return is
   * taken from Array's JavaDoc.
   * @param value the value to search for.
   * @return index of the search key, if it is contained in the array within the
   * specified range; otherwise, <tt>(-(insertion point) - 1)</tt>.
   * The insertion point is defined as the point at which the key would be
   * inserted into the array: the index of the first element in the range
   * greater than the key, or toIndex if all elements in the range are less than
   * the specified key. Note that this guarantees that the return value will be
   * <tt>&gt;= 0</tt> if and only if the key is found.
   */
  private int binarySearch(int start, int end, long value) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
/*  private void sortIndividualChunks() {
    for (int i = 0 ; i < chunks.size() ; i++) {
      if ((i+1)*chunkLength < size) {
        Arrays.sort(chunks.get(i));
      } else {
        Arrays.sort(chunks.get(i), 0, size-i*chunkLength);
      }
    }
  }

  private void mergeChunks() {
    if (chunks.size() <= 1) {
      return;
    }
    int bundleSize = 1;
    while (bundleSize < chunks.size()) {
      for (int i = 0 ; i < chunks.size() ; i+= 2*bundleSize) {
        ChunkedLongArray bundle1 = new ChunkedLongArray(
            chunks.subList(i, i+bundleSize), bundleSize*chunkLength, chunkBits);
          int pendingSize = size - (i*bundleSize+bundleSize);
        int rightLength = pendingSize > bundleSize*chunkLength ?
            bundleSize*chunkLength :
        if (i+bundleSize)
      }
    }
    throw new UnsupportedOperationException("Not implemented yet");
  }  */

  /**
   * Note: Capacity auto-increases.
   * @return the current capacity.
   */
  public int capacity() {
    return chunks.size()*chunkLength;
  }

  private static final Locale locale = new Locale("en");
  public static String memStats() {
      Runtime r = Runtime.getRuntime();
      return String.format(
          locale,
          "Allocated memory: %s, Allocated unused memory: %s, Heap memory used: %s, Max memory: %s",
          reduce(r.totalMemory()), reduce(r.freeMemory()),
          reduce(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()), reduce(r.maxMemory())
      );
  }
  private static String reduce(long bytes) {
      return bytes / 1048576 + "MB";
  }

  @Override
  public String toString() {
    return "ChunkedLongArray(" + size + " entries, " + chunks.size()
           + " chunks, " + 1L*chunks.size()*chunkLength*8/1047576 + "MB)";
  }
}

