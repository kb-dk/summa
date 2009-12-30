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
package dk.statsbiblioteket.summa.common.util.bits;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Arrays;

/**
 * The BitsArrayFactory is the recommended way of constructing BitsArrays, as
 * the optimal BitsArray-implementation is automatically created.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BitsArrayFactory {
//    private static Log log = LogFactory.getLog(BitsArrayFactory.class);

    // BitArrays
    private static enum BAS {packed, aligned, direct}

    private static int[] ALIGNED_BITS = {1, 2, 4, 8, 16};
    /**
     * Creates a BitsArray where the implementation is chosen from maxValue and
     * priority. See the JavaDoc for
     * {@link dk.statsbiblioteket.summa.common.util.bits.BitsArray.PRIORITY} for
     * details.
     * @param length the initial internal allocation of elements for the array.
     * @param maxValue the initial maximum value for elements in the BitsArray.
     * @param priority the memory/speed priority for the array.
     * @return a BitsArray optimized for the given values.
     */
    public static BitsArray createArray(
            int length, int maxValue, BitsArray.PRIORITY priority) {
        BAS bas = selectArray(maxValue, priority);
        switch (bas) {
            // TODO: Make persistent priority
            case aligned: return new BitsArrayAligned(length, maxValue);
            case packed: return new BitsArrayPacked(length, maxValue);
            case direct:  return new BitsArrayInt(length);
            default: {
                throw new IllegalArgumentException(String.format(
                        "Unknown BitsArray enum '%s'", bas));
            }
        }
    }

    private static BAS selectArray(int maxValue, BitsArray.PRIORITY priority) {
        if (priority == null) {
            priority = BitsArray.DEFAULT_PRIORITY;
        }
        int bits = (int)Math.ceil(Math.log(maxValue+1)/Math.log(2));
        if (bits == 31  | bits == 32) { // 31-32 is always perfect
            return BAS.direct;
        }
        // For everything else, there is masterswitch
        switch (priority) {
            case memory: {
                if (Arrays.binarySearch(ALIGNED_BITS, bits) >= 0) {
                    return BAS.aligned;
                }
                return BAS.packed;
            }
            case mix: {
                if (bits <= 16) {
                    return BAS.aligned;
                } else if (bits <= 28) {
                    return BAS.packed;
                }
                return BAS.direct;
            }
            case speed: {
                if (bits <= 16) {
                    return BAS.aligned;
                }
                return BAS.direct;
            }
            default: {
                throw new IllegalArgumentException(String.format(
                        "Unknown PRIORITY '%s'", priority));
            }
        }
    }

    /**
     * Creates a BitsArray based on the given maxValue and the default PRIORITY.
     * @param length the initial internal allocation of elements for the array.
     * @param maxValue the initial maximum value for elements in the BitsArray.
     * @return a BitsArray optimized for the given values.
     */
    public static BitsArray createArray(int length, int maxValue) {
        return createArray(length, maxValue, BitsArray.DEFAULT_PRIORITY);
    }

    /**
     * Creates a BitsArray based on the given maxValue and the default PRIORITY.
     * @param maxValue the initial maximum value for elements in the BitsArray.
     * @return a BitsArray optimized for the given values.
     */
    public static BitsArray createArray(int maxValue) {
        return createArray(10, maxValue, BitsArray.DEFAULT_PRIORITY);
    }

    /**
     * Creates a BitsArray.
     * @return a BitsArrayAligned optimized for single bit.
     */
    public static BitsArray createArray() {
        return createArray(10, 1, BitsArray.DEFAULT_PRIORITY);
    }

    /**
     * Ensures that the optimal BitsArray implementation is chosen. This might
     * involve changing the implementing class or shrinking the internal
     * representation.
     * @param bitsArray an existing BitsArray.
     * @param priority the strategy for selecting the optimal BitsArray.
     *        If null, {@link BitsArray#DEFAULT_PRIORITY} is used.
     * @param thorough if true, the given bitsArray is iterated to find the
     *        real maxValue. If false, the maxValue is the stated maxValue.
     *        Setting this to true ensures that the optimal BitsArray is
     *        truly optimal, at the cost of an iteration.
     * @return an optimized BitsArray. This might be the given bitsArray.
     */
/*    public static BitsArray optimize(
            BitsArray bitsArray, BitsArray.PRIORITY priority,
            boolean thorough) {
        int maxValue = bitsArray.getMaxValue();
        if (thorough) {
            maxValue = 1;
            for (int value: bitsArray) {
                maxValue = Math.max(maxValue, value);
            }
        }
        maxValue = (int)(Math.pow(2, bits(maxValue))-1);
        BAS bas = selectArray(maxValue, priority);

    }*/
}
