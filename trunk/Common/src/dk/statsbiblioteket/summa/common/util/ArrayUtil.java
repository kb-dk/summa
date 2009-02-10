/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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

import java.util.Arrays;

/**
 * Small utilities for manipulating arrays.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ArrayUtil {
    //private static Log log = LogFactory.getLog(ArrayUtil.class);

    /**
     * Construct a single array containing the values from array1 and array2.
     * @param primary the first array.
     * @param additions the second array.
     * @param removeDuplicates if true any duplicates in the addition-array
     *                         are removed.
     * @param sort if true, the resulting array is sorted in natural order.
     * @return the result of the merge or the primary array if no additions has
     *         been performed. Note that if sort is true, the order of the
     *         elements in primary might have been changed.
     */
    public static int[] mergeArrays(int[] primary, int[] additions,
                                    boolean removeDuplicates, boolean sort) {
        int[] result;
        if (additions.length == 0) {
            result = primary;
        } else {
            result = new int[primary.length + additions.length];
            System.arraycopy(primary, 0, result, 0, primary.length);
            if (!removeDuplicates) {
                System.arraycopy(additions, 0, result, primary.length,
                                 additions.length);
            } else {
                int resultLength = primary.length;
                addLoop:
                for (int addition : additions) {
                    for (int j = 0; j < resultLength; j++) {
                        if (addition == result[j]) {
                            continue addLoop;
                        }
                    }
                    result[resultLength++] = addition;
                }
                if (resultLength != result.length) {
                    int newResult[] = new int[resultLength];
                    System.arraycopy(result, 0, newResult, 0, resultLength);
                    result = newResult;
                }
            }
        }
        if (sort) {
            Arrays.sort(result);
        }
        return result;
    }

    /**
     * Reverses the order of the elements in the array in-place.
     * @param array the array to reverse.
     */
    public static void reverse(int[] array) {
        for (int i = 0 ; i < array.length / 2 ; i++) {
            int temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
    }

    /**
     * Reverses the order of the elements in the array in-place.
     * @param array the array to reverse.
     */
    public static void reverse(Object[] array) {
        for (int i = 0 ; i < array.length / 2 ; i++) {
            Object temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
    }


}
