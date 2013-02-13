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
import java.util.List;

/**
 * Large contiguous memory allocations on the heap are not recommended as heap
 * fragmentation might result in an OOM event though the reported amount of
 * available heap is large enough.
 * </p><p>
 * The ChunkedLongArray maintains a list of long chunks internally and allows
 * for dynamic expansion by adding chunks. This allows for O(chunk_size) time
 * penalty for expansion with amortized time O(1).
 */
public class ChunkedLongArray {
  public static final int DEFAULT_CHUNK_BITS = 20; // 1MB

  private final int chunkBits;
  private final int offsetMask;
  private final int chunkSize;
  private final List<long[]> chunks = new ArrayList<long[]>();
  private int size = 0;

  public ChunkedLongArray() {
    this(DEFAULT_CHUNK_BITS);
  }

  public ChunkedLongArray(int chunkBits) {
    this.chunkBits = chunkBits;
    chunkSize = (int) Math.pow(2, chunkBits);
    offsetMask = ~((~0) << chunkBits);
  }

  public void add(long value) {
    set(size++, value);
  }

  private void set(int index, long value) {
    ensureSpace(index);
    chunks.get(index >>> chunkBits)[index &  offsetMask] = value;
  }

  private long get(int index) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException(
          "Index " + index + " requested with array length " + size);
    }
    return chunks.get(index >>> chunkBits)[index &  offsetMask];
  }

  private void ensureSpace(final int index) {
    while (chunks.size() <= (index >>> chunkBits)) {
      chunks.add(new long[chunkSize]);
    }
  }

  public void set(ChunkedLongArray src, int srcPos, int destPos, int length) {
    // TODO: Improve this by doing array copys
    for (int i = 0; i < length ; i++) {
      set(destPos+i, src.get(srcPos+i));
    }
  }
}
