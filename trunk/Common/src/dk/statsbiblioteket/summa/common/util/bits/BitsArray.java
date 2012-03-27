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
     * This is one of the recommended getters as is is faster than the
     * auto-boxing variant but still with proper range-checking.
     * @param index the index for the value to get.
     * @return the value at the given position.
     */
    int getAtomic(int index);

    /**
     * The equivalent to {@link List#get(int)} but without auto-boxing
     * and fine-grained range-checking. This is one of the recommended getters
     * as it is the fastest at the cost of range-checking.
     * @param index the index for the value to get. This must be < size.
     * @return the value at the given position.
     */
    int fastGetAtomic(int index);

    /**
     * The equivalent to {@link List#set(int, Object)} but without
     * auto-boxing. This getter adjusts size properly.
     * @param index the index for the value to set.
     * @param value the value for the given position.
     */
    void set(int index, int value);

    /**
     * The equivalent to {@link List#set(int, Object)} but without
     * auto-boxing and adjustment of size. If this getter is used, only
     * {@link #fastGetAtomic(int)} can be used to get values.
     * @param index the index for the value to set.
     *        This must be < max(size, initial capacity)
     * @param value the value for the given position.
     */
    void fastSet(int index, int value);

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

