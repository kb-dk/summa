/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.RandomAccess;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * {@link Collections#sort} copies the content of a given list to an Array
 * before sorting. This does not work properly for persistent storage based
 * lists. The ListSorter sorts by swapping elements directly in the list.
 * For memory-based Lists, this is slower than Collections.sort by about a
 * factor 4, so it should only be used when memory-use is a concern.
 * </p><p>
 * A HeapSort is used, which takes O(n*log(n)) time and n space. The HeapSort
 * uses {@link #swap} only to modify the list.
 * @see {@url http://en.wikibooks.org/wiki/Wikiversity:Data_Structures#Heaps}.
 * @see {@url http://www.personal.kent.edu/~rmuhamma/Algorithms/MyAlgorithms/Sorting/heapSort.htm}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ListSorter {
    public <T> void sort(List<T> list, Comparator<? super T> c) {
/*        if (!(list instanceof RandomAccess)) {
            Collections.sort(list, c);
            return;
        }*/
        for (int position = list.size() / 2 - 1 ; position >= 0 ; position--) {
            siftDown(list, position, list.size(), c);
        }
        for (int i = list.size() ; i > 0 ; i--) {
            sortSingle(list, i, c);
        }
    }

    /**
     * Default implementation of swap. If the List-structure is capable of
     * swapping elements faster or better than by using get, setand set,
     * this method should be overwritten to support the better swap.
     * </p><p>
     * The sorter uses this method only to modify the order of elements in
     * the list.
     * @param list the list with the elements to swap.
     * @param pos1 the index of the first value.
     * @param pos2 the index of the second value.
     */
    protected <T> void swap(List<T> list, int pos1, int pos2) {
        T t = list.get(pos2);
        list.set(pos2, list.get(pos1));
        list.set(pos1, t);
    }

    private <T> void siftDown(List<T> list, int startPosition, int heapSize,
                                Comparator<? super T> c) {
        int position = startPosition;
        while (firstChild(position) < heapSize) {
            int kid = firstChild(position);
            if (kid < heapSize-1 &&
                c.compare(list.get(kid), list.get(kid+1)) < 0) {
                kid++;
            }
            if (c.compare(list.get(position), list.get(kid)) > 0) {
                break;
            } else {
                swap(list, kid, position);
                position = kid;
            }
        }
    }
    private int firstChild(int element) {
        return 2*element+1;
    }
    private <T> void sortSingle(List<T> list, int heapSize,
                               Comparator<? super T> c) {
        swap(list, 0, heapSize-1);
        heapSize--;
        siftDown(list, 0, heapSize, c);
    }
}
