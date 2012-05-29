package org.apache.lucene.util.packed;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A PackedInts that grows to accommodate any legal data that is added.
 * Deltas for both array length and values are used so memory-representation
 * is fairly compact.
 * </p><p>
 * The initial default value for a growing mutable is the given minValue or 0
 * if no minValue is specified. If a value smaller than minValue is added after
 * construction, the new default value will be that value and the old non-set
 * values will still be the old default value. To put it in code: {@code
 GrowingMutable g = new GrowingMutable(0, 5, 5, 10);
 assert(g.get(0) == 5);
 g.set(2, 4);
 assert(g.get(0) == 5);
 assert(g.get(1) == 4);
 }. To avoid error it is imperative to either specify a minValue that is
 guaranteed to be the minimum value for the life of the mutable or to ensure
 that all values are assigned explicitly.
 * </p><p>
 * The penalty for using this implementation compared to a plain PackedInts is
 * slower {@link #set}s as it needs to check boundaries and potentially expand
 * the internal array. There are no conditionals and very little extra overhead
 * for {@link #get}s so access is still fast.
 */
// TODO: Optimize the speed of array length growth
public class GrowingMutable implements PackedInts.Mutable {
  // Grow array length with this factor beyond the immediate need
  private static final double GROWTH_FACTOR = 1.2;

  private int indexMin = 0;
  private int indexMax = 0;
  private long valueMin = 0;
  private long valueMax = 0;
  private int size = 0; // Size is explicit to handle grow seamlessly
  private boolean optimizeSpeed = false;
  
  private PackedInts.Mutable values;

  /**
   * Create an empty array of size 0. If it is possible to estimate the amount
   * and rages of the values to add to the array, it is recommended to use
   * {@link #GrowingMutable(int,int,long,long,boolean)} instead.
   */
  public GrowingMutable() {
    values = PackedInts.getMutable(0, 1);
  }

  /**
   * Create an empty array with the given boundaries. The boundaries will be
   * expanded as needed. The default value is valueMin.
   * @param indexMin the minimum index for the array.
   * @param indexMax the maximum index for the array.
   * @param valueMin the minimum value for the array.
*                 Note that this can be negative.
   * @param valueMax the maximum value for the array. This must be higher than
*                 or equal to valueMin.
   * @param optimizeSpeed if true, the number of bit required to represent the
   *              values are rounded to 1, 2, 3, 4, 8, 16, 32-40 or 64.
   */
  public GrowingMutable(int indexMin, int indexMax,
                        long valueMin, long valueMax, boolean optimizeSpeed) {
    if (indexMin > indexMax) {
      throw new IllegalArgumentException(String.format(
        "indexMin(%d) must be <= indexMax(%d)", indexMin, indexMax));
    }
    if (valueMin > valueMax) {
      throw new IllegalArgumentException(String.format(
        "valueMin(%d) must be <= valueMax(%d)", valueMin, valueMax));
    }
    this.indexMin = indexMin;
    this.indexMax = indexMax;
    this.valueMin = valueMin;
    this.valueMax = valueMax;
    this.size = indexMax - indexMin + 1;
    this.optimizeSpeed = optimizeSpeed;
    values = getMutable(
        indexMax - indexMin + 1, PackedInts.bitsRequired(valueMax - valueMin));
  }

  private PackedInts.Mutable getMutable(int valueCount, int bitsPerValue) {
    if (!optimizeSpeed) {
      return PackedInts.getMutable(valueCount, bitsPerValue);
    }
    if (bitsPerValue <= 4 ||
          (bitsPerValue > 32 && bitsPerValue < 40)) { // TODO: Consider the 40
        return PackedInts.getMutable(valueCount, bitsPerValue);
    }
    return PackedInts.getMutable(valueCount, bitsPerValue);
  }

  // Make sure that there is room. Note: This might adjust the boundaries.
  private synchronized void checkValues(final int index, final long value) {
    if (index >= indexMin && index <= indexMax
        && value >= valueMin && value <= valueMax) {
      return;
    }
    // TODO: Just adjust max is within bitsPerValue 
    if (size() == 0) { // First addition
      indexMin = index;
      indexMax = index;
      valueMin = value;
      valueMax = value;
      GrowingMutable newGrowingMutable = new GrowingMutable(
            indexMin, indexMax, valueMin, valueMax, optimizeSpeed);
      size = 1;
      values = newGrowingMutable.values;
      return;
    }

    // indexes are grown with factor, values without
    int newIndexMin =
      index >= indexMin ? indexMin :
      indexMax - (int)Math.ceil((indexMax - index) * GROWTH_FACTOR);
    int newIndexMax =
      index <= indexMax ? indexMax :
      indexMin + (int)Math.ceil((index - indexMin) * GROWTH_FACTOR);
    long newValueMin = Math.min(value, valueMin);
    long newValueMax = Math.max(value, valueMax);

    GrowingMutable newGrowingMutable = new GrowingMutable(
          newIndexMin, newIndexMax, newValueMin, newValueMax, optimizeSpeed);
    for (int i = indexMin ; i <= indexMax ; i++) {
      newGrowingMutable.set(i, get(i));
    }

    values = newGrowingMutable.values;
    indexMin = newIndexMin;
    indexMax = newIndexMax;
    valueMin = newValueMin;
    valueMax = newValueMax;
    size = indexMax - indexMin; // Size is always minimum to accommodate index
  }

  public void set(int index, long value) {
    checkValues(index, value);
    values.set(index - indexMin, value - valueMin);
  }

  @Override
  public String toString() {
    final int DUMP_SIZE = 10;
    StringBuffer sb = new StringBuffer(DUMP_SIZE * 10);
    sb.append("GrowingMutable(");
    for (int i = indexMin ; i < indexMin + Math.min(size(), DUMP_SIZE) ; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append("i(").append(Integer.toString(i)).append(")=");
      sb.append(Long.toString(get(i)));
    }
    if (size > DUMP_SIZE) {
      sb.append(", ...");
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Clear only resets the array. It does not free memory or adjust size.
   */
  public void clear() {
    values.clear();
  }

  public long get(int index) {
    return values.get(index - indexMin) + valueMin;
  }

  public int getBitsPerValue() {
    return values.getBitsPerValue();
  }

  public int size() {
    return size;
  }

    @Override
    public Object getArray() {
        throw new UnsupportedOperationException("No direct array in GrowingMutable");
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    public int getIndexMin() {
    return indexMin;
  }

  public int getIndexMax() {
    return indexMax;
  }

  public long getValueMin() {
    return valueMin;
  }

  public long getValueMax() {
    return valueMax;
  }
}
