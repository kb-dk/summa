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

import java.util.Comparator;

/**
 * Provides sorting of a list with indirection: The true value of a list element
 * is determined by lookup from an int value.
 * </p><p>
 * The underlying algorithm for this sorter is merging. This works well for
 * large lists where the indirect lookups are backed by a finite cache, such as
 * a disk-based lookup.
 * </p><p>
 * Terminology:
 * index     the position in the list.
 * reference the pointer that the underlying list use to resolve a value.
 * value     what the list contains.
 * references and values are linket to the same index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "This is a copy of IndirectLongSorter")
public abstract class IndirectIntSorter<V> {

    /**
     * Get the value for the given reference.
     * @param reference a reference to a value.
     * @return a value corresponding to the given reference.
     */
    protected abstract V getValue(int reference);


    /**
     * Sort the given array of references according to the comparator of values.
     * This used the method {@link #getValue} to resolve values for the
     * references in the list.
     * </p><p>
     * The article http://en.wikipedia.org/wiki/Merge_sort was used as base for
     * this code.
     * @param references the references to sort.
     * @param c          the comparator method for values.
     * @return the references array after sort, suitable for chaining.
     */
    public int[] sort(int[] references, Comparator<? super V> c) {
        return sort(references, 0, references.length, c);
    }

    /**
     * Sort the given array of references according to the comparator of values.
     * This used the method {@link #getValue} to resolve values for the
     * references in the list.
     * </p><p>
     * The article http://en.wikipedia.org/wiki/Merge_sort was used as base for
     * this code.
     * </p><p>
     * The given array of references is not changed by the sorting.
     * @param references the references to sort.
     * @param start the index of the first reference to sort (inclusive).
     * @param end   the index of the last reference to sort (exclusive).
     * @param c     the comparator method for values.
     * @return the references array after sort, suitable for chaining.
     */
    public int[] sort(int[] references, int start, int end,
                       Comparator<? super V> c) {
        if (end - start <= 1) {
            return references;
        }

        int middle = (end - start) / 2 - 1 + start;
        sort(references, start, middle + 1, c);
        sort(references, middle + 1, end, c);
        merge(references, start, middle + 1, end, c);
        return references;
    }

    /**
     * Expects the references from start (inclusive) to middle (exclusive) to
     * be sorted and the references from middle (inclusive) to end (exclusive)
     * to be sorted and performs a merge, with the result assigned to references
     * starting from start.
     * @param references the references to mere.
     * @param start  the start of the references to merge.
     * @param middle the middle of the references to merge.
     * @param end    the end of the references to merge.
     * @param c      the comparator method for values.
     */
    private void merge(int[] references, int start, int middle, int end,
                       Comparator<? super V> c) {
        if (middle - start == 0 || end - middle == 0) {
            return;
        }
        int[] result = new int[end - start];
        int iLeft = start;
        int iRight = middle;
        int iResult = 0;
        while (iLeft < middle && iRight < end) {
            V vLeft = getValue(references[iLeft]);
            V vRight = getValue(references[iRight]);
            if (c.compare(vLeft, vRight) <= 0) {
                result[iResult++] = references[iLeft++];
            } else {
                result[iResult++] = references[iRight++];
            }
        }
        while (iLeft < middle) {
            result[iResult++] = references[iLeft++];
        }
        while (iRight < end) {
            result[iResult++] = references[iRight++];
        }
        System.arraycopy(result, 0 , references, start, result.length);
    }
}
