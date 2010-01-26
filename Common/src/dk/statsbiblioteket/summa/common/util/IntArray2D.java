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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A space-efficient flexible-size 2-dimensional int array implementation.
 * Java's standard int[][] works by having an array with pointers to int-arrays.
 * The IntArray2D works by having an int-array with offsets into another
 * int-array with values. This eliminates the overhead of the second-dimenstion
 * arrays. The tradeoff is high insertion-time if the values are not added in
 * sequential order.
 * </p><p>
 * Allocated memory for the internal structures is never freed, even when all
 * values are set to 0.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IntArray2D {
    private static Log log = LogFactory.getLog(IntArray2D.class);

    /**
     * When the arrays are expanded, this factor if used to calculate the new
     * length.
     */
    public static final double DEFAULT_GROWTH_FACTOR = 1.2D;
    private static final int MAX_INCREMENT = 1000000;

    private static final int OFFSET_MASK = 0x7FFFFFFF; // 011...111
    private static final int CLEAR_MASK =  0x80000000; // 100...000

    private double growthFactor = DEFAULT_GROWTH_FACTOR;

    // Invariant: offsets[size] == last free entry in valueStore
    // As offsets cannot be negative, we use the first bit to indicate that the
    // valueStore are cleared
    private int[] offsets;
    private int size = 0;
    private int[] valueStore;

    public IntArray2D() {
        offsets = new int[1000];
        valueStore = new int[1000];
    }

    /**
     * Initialize the array with the given number of positions.
     * @param initialLength the initial number of positions.
     */
    public IntArray2D(int initialLength) {
        offsets = new int[initialLength];
        valueStore = new int[initialLength];
    }

    /**
     * Initialize the array with the given number of positions as well as the
     * factor used when growing the array.
     * @param initialLength the initial number of positions.
     * @param growthFactor  the factor for array-growth.
     */
    public IntArray2D(int initialLength, double growthFactor) {
        offsets = new int[initialLength];
        valueStore = new int[initialLength];
        this.growthFactor = growthFactor;
    }

    private static final int[] EMPTY = new int[0];

    /**
     * @param position where to get the values.
     * @return the values at the given position.
     */
    public int[] get(int position) {
        checkPos(position);
        if ((offsets[position] & CLEAR_MASK) != 0) {
            // Cleared
            return EMPTY;
        }
        final int from = offsets[position]   & OFFSET_MASK;
        final int to =   offsets[position+1] & OFFSET_MASK;
        int[] result = new int[to - from];
        for (int offset = from ; offset < to  ; offset++) {
            result[offset - from] = valueStore[offset];
        }
        return result;
    }

    /**
     * Set the values at the given position. This is fast if the position is
     * >= size, but slow for low positions vs. high size.
     * @param position where to store the values.
     * @param values the values to store.
     */
    public void set(int position, int[] values) {
        // Ensure offset-space
        offsets = ArrayUtil.makeRoom(
                offsets, position, growthFactor, MAX_INCREMENT, 2);
        if (position < size) {
            setValuesInside(position, values);
            return;
        }
        // Ensure that there is room
        valueStore = ArrayUtil.makeRoom(valueStore, offsets[size], growthFactor,
                                        MAX_INCREMENT, values.length);
        //noinspection ManualArrayCopy
        for (int i = 0 ; i <= position - size ; i++) { // Fill with 0 values
            offsets[size + i + 1] = offsets[size + i];
        }
        offsets[position + 1] = offsets[position + 1] + values.length;
        System.arraycopy(
                values,  0, valueStore, offsets[position], values.length);
        size = position + 1;
    }

    /**
     * Extend the array of values at the given position with the given value.
     * This is fast if the position is >= size, but slow for low positions vs.
     * high size.
     * @param position where to append the value.
     * @param value the values to append.
     */
    public void append(int position, int value) {
        if (position < size-1) { // Append inside (slow)
            int[] existing = get(position);
            int[] replacement = new int[existing.length+1];
            replacement[replacement.length-1] = value;
            set(position, replacement);
            return;
        }
        if (position > size-1) { // Plain new (somewhat slow)
            SINGLE[0] = value;
            set(position, SINGLE);
            return;
        }
        // position == size-1: Fast
        valueStore = ArrayUtil.makeRoom(
                valueStore, offsets[size], growthFactor, MAX_INCREMENT, 1);
        valueStore[offsets[position+1]] = value;
        offsets[position+1]++;
    }
    private static final int[] SINGLE = new int[1];

    private void setValuesInside(int position, int[] values) {
        final int existingLength = offsets[position+1] - offsets[position];
        int adjust = values.length - existingLength;
        if (adjust > 0) { // Expand capacity
            valueStore = ArrayUtil.makeRoom(
                    valueStore, size, growthFactor, MAX_INCREMENT, adjust);
        }
        if (adjust != 0) {
            // Move trailing values
            System.arraycopy(valueStore, offsets[position+1],          // From
                             valueStore, offsets[position+1] + adjust, // To
                             offsets[size] - offsets[position+1]);     // Length
            // Adjust trailing offsets
            for (int i = position + 1 ; i <= size ; i++) {
                offsets[i] += adjust;
            }
        }
        // Insert values
        System.arraycopy(
                values, 0, valueStore, offsets[position], values.length);
    }

    /**
     * Clear the values at the given position without moving values (no GC).
     * This operation is constant time with minimal overhead.
     * @param position the position to clear.
     */
    public void dirtyClear(int position) {
        checkPos(position);
        offsets[position] = offsets[position] | CLEAR_MASK;
    }

    /**
     * Remove all values at the given position.
     * Shorthand for calling {@link #set} with an empty array.
     * Note: This adjusts internal values and might be expensive, depending og
     * the existing structure. If reclaiming of freed space in the internal
     * structure is not a priority, consider using {@link #dirtyClear}.
     * @param position were to remove the values.
     */
    public void clear(int position) {
        set(position, EMPTY);
    }

    /**
     * @param position the position of an int-array.
     * @return true if the values at the given position is marked as cleared.
     */
    public boolean isCleared(int position) {
        checkPos(position);
        return (offsets[position] & CLEAR_MASK) != 0;
    }

    private void checkPos(final int position) {
        if (position >= size) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The primary array has size %d, while the requested "
                    + "position was %d", size, position));
        }
    }

    /**
     * @return the length of the primary array.
     */
    public int size() {
        return size;
    }
}

