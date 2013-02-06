/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package org.apache.lucene.util;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Arrays;

/**
 * Specialized high performance expandable list of integer pairs that can be sorted on both primary and secondary key.
 * </p><p>
 * This implementation is not thread safe.
 */
public class DoubleIntArrayList {
    private static Log log = LogFactory.getLog(DoubleIntArrayList.class);

    /**
     * The factor used when expanding the list.
     */
    private final double expandFactor;

    // TODO: consider using lists of lists to avoid array copy on expansion.
    private long[] pairs;
    private int size;

    public DoubleIntArrayList(int initialCapacity) {
        this(initialCapacity, 0.5d);
    }

    public DoubleIntArrayList(int initialCapacity, double expandFactor) {
        pairs = new long[initialCapacity];
        this.expandFactor = expandFactor;
    }

    /**
     * Adds the given pair to the list. When the internal list capacity is exceeded, it is expanded, which involves
     * an array copy.
     * @param primary   primary value.
     * @param secondary secondary value.
     */
    public void add(int primary, int secondary) {
        if (pairs.length == size) {
            expand();
        }
        pairs[size++] = ((long)primary) << 32 | (long)secondary;
    }

    private void expand() {
        int newCapacity = (int) (pairs.length * expandFactor);
        if (newCapacity == pairs.length) {
            newCapacity += 10;
        }
        long[] newPairs = new long[newCapacity];
        System.arraycopy(pairs, 0, newPairs, 0, size);
        pairs = newPairs;
    }

    public void sortByPrimaries() {
        Arrays.sort(pairs, 0, size);
    }

    public void sortBySecondaries() {
        secondarySort(pairs, 0, size);
    }

    /**
     * Classic merge sort with memory overhead equal to the full input.
     * </p><p>
     * The article http://en.wikipedia.org/wiki/Merge_sort was used as base for
     * this code.
     * @param references the references to sort.
     * @param start      the index of the first reference to sort (inclusive).
     * @param end        the index of the last reference to sort (exclusive).
     */
    private void secondarySort(long[] references, int start, int end) {
        if (end - start <= 1) {
            return;
        }

        int middle = (end - start) / 2 - 1 + start;
        secondarySort(references, start, middle + 1);
        secondarySort(references, middle + 1, end);
        secondaryMerge(references, start, middle + 1, end);
    }

    /**
     * Expects the references from start (inclusive) to middle (exclusive) to
     * be sorted and the references from middle (inclusive) to end (exclusive)
     * to be sorted and performs a merge, with the result assigned to references
     * starting from start.
     *
     * @param pairs  the pairs to merge.
     * @param start  the start of the pairs to merge.
     * @param middle the middle of the pairs to merge.
     * @param end    the end of the pairs to merge.
     */
    private void secondaryMerge(long[] pairs, int start, int middle, int end) {
        if (middle - start == 0 || end - middle == 0) {
            return;
        }
        long[] result = new long[end - start];
        int iLeft = start;
        int iRight = middle;
        int iResult = 0;
        while (iLeft < middle && iRight < end) {
            int vLeft = (int) pairs[iLeft];
            int vRight = (int) pairs[iRight];
            result[iResult++] = vLeft < vRight ? pairs[iLeft++] : pairs[iRight++];
        }
        while (iLeft < middle) {
            result[iResult++] = pairs[iLeft++];
        }
        while (iRight < end) {
            result[iResult++] = pairs[iRight++];
        }
        System.arraycopy(result, 0, pairs, start, result.length);
    }

    public int getPrimary(int index) {
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException("Requested value @ index " + index + " with array length " + size);
        }
        return (int) (pairs[index] >>> 32);
    }

    public int getSecondary(int index) {
        if (index >= size) {
            throw new ArrayIndexOutOfBoundsException("Requested value @ index " + index + " with array length " + size);
        }
        return (int) pairs[index]; // Discards the upper 32 bit
    }

    /**
     * Extracts the added primary values. Note that the full list is traversed and a new integer array is allocated.
     * The time complexity is O(n).
     * @return all primary values.
     */
    public int[] getPrimaries() {
        final int[] result = new int[size];
        for (int i = 0 ; i < size ; i++) {
            result[i] = (int) (pairs[i] >>> 32);
        }
        return result;
    }

    /**
     * Extracts the added secondary values. Note that the full list is traversed and a new integer array is allocated.
     * The time complexity is O(n).
     * @return all secondary values.
     */
    public int[] getSecondaries() {
        final int[] result = new int[size];
        for (int i = 0 ; i < size ; i++) {
            result[i] = (int) pairs[i]; // Discards the upper 32 bit
        }
        return result;
    }

    public int getSize() {
        return size;
    }
}
