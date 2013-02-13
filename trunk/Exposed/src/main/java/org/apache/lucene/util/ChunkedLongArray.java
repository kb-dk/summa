/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package org.apache.lucene.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  public static final int DEFAULT_CHUNK_BITS = 20; // 1MB

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
    offsetMask = ~((~0) << chunkBits);
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
  }

  public long get(int index) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException(
          "Index " + index + " requested with array length " + size);
    }
    return chunks.get(index >>> chunkBits)[index &  offsetMask];
  }

  public int size() {
    return size;
  }

  private void ensureSpace(final int index) {
    while (chunks.size() <= (index >>> chunkBits)) {
      chunks.add(new long[chunkLength]);
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
      System.arraycopy(
          src, srcPos, chunks.get(destChunk), destOffset, subLength);
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

}

