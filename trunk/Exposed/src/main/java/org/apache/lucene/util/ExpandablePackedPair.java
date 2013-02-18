/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package org.apache.lucene.util;

import org.apache.lucene.util.packed.DoublePackedPair;
import org.apache.lucene.util.packed.PackedInts;

import java.util.ArrayList;
import java.util.List;

public class ExpandablePackedPair {
  public static final int DEFAULT_CHUNK_BITS = 20; // 1M entries

  private final int chunkBits;
  private final int chunkLength;
  private final List<DoublePackedPair> chunks =
      new ArrayList<DoublePackedPair>();
  private final int primaryBPV;
  private final int secondaryBPV;
  private long secondaryOffset; // Used for extraction only
  private int size = 0;

  public ExpandablePackedPair(
      int primaryBPV, int secondaryBPV, long secondaryOffset) {
    this(primaryBPV, secondaryBPV, secondaryOffset, DEFAULT_CHUNK_BITS);
  }

  public ExpandablePackedPair(int primaryBPV, int secondaryBPV,
                              long secondaryOffset, int chunkBits) {
    this.chunkBits = chunkBits;
    chunkLength = (int) Math.pow(2, chunkBits);
    this.primaryBPV = primaryBPV;
    this.secondaryBPV = secondaryBPV;
    this.secondaryOffset = secondaryOffset;
  }

  public void add(int primary, int secondary) {
    ensureSpace(size);
    chunks.get(size++ >>> chunkBits).add(primary, secondary);
  }

  public void countUniquePrimaries(int[] counters) {
    for (DoublePackedPair chunk: chunks) {
      chunk.countUniquePrimaries(counters);
    }
  }

  public void assignSecondaries(int[] offsets, PackedInts.Mutable destination) {
    for (DoublePackedPair chunk: chunks) {
      chunk.assignSecondaries(offsets, destination);
    }
  }

  public int size() {
    return size;
  }

  private void ensureSpace(final int index) {
    while (chunks.size() <= (index >>> chunkBits)) {
        chunks.add(new DoublePackedPair(
            chunkLength, primaryBPV, secondaryBPV, secondaryOffset));
        // TODO: Detailed OOM
    }
  }

  public void setSecondaryOffset(long secondaryOffset) {
    this.secondaryOffset = secondaryOffset;
    for (DoublePackedPair chunk: chunks) {
      chunk.setSecondaryOffset(secondaryOffset);
    }
  }

  /**
   * Note: Capacity auto-increases.
   * @return the current capacity.
   */
  public int capacity() {
    return chunks.size()*chunkLength;
  }

  @Override
  public String toString() {
    return "ChunkedPackedArray(" + size + " entries, " + chunks.size()
           + " chunks)";
  }
}

