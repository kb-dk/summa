/* $Id: BitsArray.java 1960 2009-11-27 15:35:58Z toke-sb $
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

import java.util.Collection;
import java.util.List;

/**
 * A map from positive int positions to positive int values.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface BitsArray extends Collection<Integer>, List<Integer> {

    /**
     * The equivalent to {@link List#get(int)} but without auto-boxing.
     * This is the recommended getter as is is faster than the auto-boxing
     * variant.
     * @param index the index for the value to get.
     * @return the value at the given position.
     */
    int getAtomic(int index);

    void set(int position, int value);

    void assign(BitsArray other);
            /*
    @Override
    void clear();

    @Override
    Integer get(int index);

    @Override
    Integer set(int index, Integer element);

    @Override
    int size();
              */
    /**
     * @return approximate memory usage.
     */
    int getMemSize();

    /**
     * @return the maximum value to add before the internal representation needs
     *         to be updated. This will normally be 1, 3, 7, 15, 31 ... 2^32-1.
     */
    int getMaxValue();

    /**
     * The strategy for BitsArray-creation. This is used together with maxValue.
     */
    static enum PRIORITY {
        /**
         * Creates a memory-optimized BitsArray of the given length and with the
         * given maxValue. The maxValue defines which implementation is used, as
         * maxValues with bit-representations of length 1, 2, 4, 8, 16 and 31-32
         * allows for optimized implementations.
         * </p><p>
         * The bit-representations that are optimized translates to the numbers
         * 1, 3, 15, 255, 65535, and 4294967295 (in practise 2147483647).
         * </p><p>
         * Note: For high maxValues, such as 2^30-1, this will result in a
         * rather bad memory/performance-tradeof, as only 2/32 space is saved
         * at the cost of a very substantial performance hit (think factor 100).
         * Unless there is a specific reason, it is recommended to use the
         * priority mix instead.
         */
        memory,
        /**
         * Creates an array that is a mix between memory- and performance
         * optimized. For maxValues below 65535 (2^16-1), a BitsArrayAligned
         * is returned. For maxValues from 65536 (2^16) to 268435455 (2^28-1)
         * a BitsArrayPacked is returned and for values from 268435456 (2^28)
         * and above, BitsArrayInt is returned.
         */
        mix,
        /**
         * Creates either a BitsArrayAligned or BitsArrayInt array.
         * BitsArrayAligned is returned for maxValues below 65536.
         */
        speed}

    /**
     * The default priority when none is chosen.
     */
    PRIORITY DEFAULT_PRIORITY = PRIORITY.mix;
}
