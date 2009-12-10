/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A space-efficient flexible-size 2-dimensional int array implementation.
 * int[][] works by having an array with pointers to int-arrays. The IntArray2D
 * works by having an int-array with pointers into another int-array with
 * values. The tradeoff is high insertion-time if the values are not added in
 * sequential order.
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

    private static final int OFFSET_MASK = 0x7FFFFFFF; // 011...111
    private static final int CLEAR_MASK =  0x80000000; // 100...000

    private double growthFactor = DEFAULT_GROWTH_FACTOR;

    // Invariant: offsets[size] == offsets[size-1]
    // As offsets cannot be negative, we use the first bit to indicate that the
    // values are cleared
    private int[] offsets;
    private int size = 0;
    private int[] values;

    public IntArray2D() {
        offsets = new int[1000];
        values = new int[1000];
    }

    /**
     * Initialize the array with the given number of positions.
     * @param initialLength the initial number of positions.
     */
    public IntArray2D(int initialLength) {
        offsets = new int[initialLength];
        values = new int[initialLength];
    }

    /**
     * Initialize the array with the given number of positions as well as the
     * factor used when growing the array.
     * @param initialLength the initial number of positions.
     * @param growthFactor  the factor for array-growth.
     */
    public IntArray2D(int initialLength, double growthFactor) {
        offsets = new int[initialLength];
        values = new int[initialLength];
        this.growthFactor = growthFactor;
    }

    private static final int[] EMPTY = new int[0];
    public int[] getValues(int position) {
        if (position >= size) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The primary array has size %d, while the requested "
                    + "position was %d", size, position));
        }
        if ((offsets[position] & CLEAR_MASK) != 0) {
            // Cleared
            return EMPTY;
        }
        final int from = offsets[position]   & OFFSET_MASK;
        final int to =   offsets[position+1] & OFFSET_MASK;
        int[] result = new int[to - from];
        for (int offset = from ; offset < to  ; offset++) {
            result[from - offset] = values[offset];
        }
        return result;
    }

    public void setValues(int position, int[] values) {

    }

    /**
     * Clear the values at the given position without freeing memory.
     * This operation is constant time with minimal overhead.
     * @param position the position to clear.
     */
    public void dirtyClear(int position) {
        if (position >= size) {
            throw new ArrayIndexOutOfBoundsException(String.format(
                    "The primary array has size %d, while the requested "
                    + "position was %d", size, position));
        }
        offsets[position] = offsets[position] | CLEAR_MASK;
    }
}
