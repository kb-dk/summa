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

import org.apache.lucene.util.packed.DoublePackedPair;
import org.apache.lucene.util.packed.PackedInts;

import java.util.ArrayList;
import java.util.List;

public class ExpandablePackedPair {
  public static final int DEFAULT_CHUNK_BITS = 20; // 1M entries

  private final int chunkBits;
  private final int chunkLength;
  private final List<DoublePackedPair> chunks = new ArrayList<DoublePackedPair>();
  private final int primaryBPV;
  private final int secondaryBPV;
  private long secondaryOffset; // Used for extraction only
  private int size = 0;

  public ExpandablePackedPair(int primaryBPV, int secondaryBPV, long secondaryOffset) {
    this(primaryBPV, secondaryBPV, secondaryOffset, DEFAULT_CHUNK_BITS);
  }

  public ExpandablePackedPair(int primaryBPV, int secondaryBPV, long secondaryOffset, int chunkBits) {
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
        chunks.add(new DoublePackedPair(chunkLength, primaryBPV, secondaryBPV, secondaryOffset));
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
    return "ChunkedPackedArray(" + size + " entries, " + chunks.size() + " chunks)";
  }
}

