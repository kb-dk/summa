/* $Id: PriorityQueue.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:20 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: PriorityQueue.java,v 1.2 2007/10/04 13:28:20 te Exp $
 */
package dk.statsbiblioteket.summa.common.util;

import java.util.ArrayList;
import java.util.Comparator;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A generic Priority Queue. This Queue supports a max capacity.
 * If an element is inserted and the max capacity is exceeded, the least
 * significant value is discarded and returned.
 * The queue optionally takes a Comparator and uses the natural order from
 * the Comparable objects if none is specified.
 * insert:    O(log(n))
 * removeMin: O(log(n))
 * getMin:    O(1)
 * clear:     O(1)
 * Note: This queue is not thread-safe.
 *
 * see http://en.wikibooks.org/wiki/Wikiversity:Data_Structures#Heaps
 * and http://www.personal.kent.edu/~rmuhamma/Algorithms/MyAlgorithms/Sorting/heapSort.htm
 * </p><p>
 * Note: This class creates arrays of generics by using the same cast-hack as
 *       {@link ArrayList}.
 *       See http://forums.sun.com/thread.jspa?threadID=564355&forumID=316 for
 *       a discussion on the subject.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class PriorityQueue<T extends Comparable<? super T>> {
    private T[] heap;
    private int size = 0;
    private int maxCapacity;
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;
    private Comparator<T> comparator; // Optional

    public PriorityQueue() {
        this(Integer.MAX_VALUE);
    }
    public PriorityQueue(int maxCapacity) {
        this(null, Math.min(DEFAULT_INITIAL_CAPACITY, maxCapacity),
             maxCapacity);
    }
    public PriorityQueue(Comparator<T> comparator, int maxCapacity) {
        this(comparator, Math.min(DEFAULT_INITIAL_CAPACITY,
                                  maxCapacity), maxCapacity);
    }
    public PriorityQueue(int initialCapacity, int maxCapacity) {
        this(null, initialCapacity, maxCapacity);
    }
    public PriorityQueue(Comparator<T> comparator, int initialCapacity,
                         int maxCapacity) {
        if (maxCapacity < initialCapacity) {
            throw new IllegalArgumentException(String.format(
                    "The max capacity (%d) must be larger than or equal to the "
                    + "initial capacity (%d)", maxCapacity, initialCapacity));
        }
        new ArrayList<Integer>(10);
        //noinspection unchecked
        heap = (T[])new Object[initialCapacity];
        this.maxCapacity = maxCapacity;
        this.comparator = comparator;
    }

    /**
     * Assign all the given values to the queue, replacing all existing values.
     * Time complexity is O(n).
     * @param values      the values to assign to the queue.
     * @param size        the number of values to assign. This must be equal to
     *                    or smaller than the number of values.
     * @param reuseArray  if true, the array with the values is used for the
     *                    internal heap-representation.
     * @param maxCapacity the maximum capacity for this queue. The maximum must
     *                    be equal to or greater than the size.
     */
    public void setValues(T[] values, int size, boolean reuseArray,
                          int maxCapacity) {
        if (size > values.length) {
            throw new IllegalArgumentException(String.format(
                    "The size (%d) must be equal to or smaller than the length "
                    + "of values (%d)", size, values.length));
        }
        if (maxCapacity < size) {
            throw new IllegalArgumentException(String.format(
                    "The maxCapacity (%d) must be equal to or greater than "
                    + "size (%d)", maxCapacity, size));
        }
        this.maxCapacity = maxCapacity;
        this.size = size;
        if (reuseArray) {
            heap = values;
        } else {
            //noinspection unchecked
            heap = (T[])new Object[Math.max(Math.min(
                    DEFAULT_INITIAL_CAPACITY, maxCapacity), size)];
            System.arraycopy(values, 0, heap, 0, size);
        }

        // This is O(n), although it looks like O(n*log(n))
        for (int position = size / 2 - 1 ; size >= 0 ; size--) {
            siftDown(position);
        }
    }

    /**
     * Insert a value in the queue. Time complexity is O(log(n)).
     * If the insertion pushed out an existing value, that value is returned.
     * @param value the value to insert in the queue.
     * @return the old value, if the new value pushes one out. Else null
     */
    public T insert(T value) {
        if (size == maxCapacity) { // Heap is full
            //noinspection unchecked
            T heapZero = heap[0];
            if ((comparator != null ? comparator.compare(heapZero, value)
                 : heapZero.compareTo(value)) > 0) {
                heap[0] = value;
                siftDown();
                return heapZero;
            }
        } else { // Insert at the end and sift up
            if (heap.length == size+1) { // Expand heap
                int newSize = Math.min(maxCapacity, heap.length*2);
                //noinspection unchecked
                T[] newQueue = (T[])new Object[newSize];
                System.arraycopy(heap, 0, newQueue, 0, size);
                heap = newQueue;
            }
            heap[size++] = value;
            siftUp();
        }
        return null;
    }

    /**
     * Remove the smallest value on the queue Time complexity is O(log(n)).
     * @return the smallest value on the queue.
     */
    public T removeMin() {
        T result = getMin();
        heap[0] = heap[size-1];
        size--;
        siftDown();
        return result;
    }

    /**
     * Get the smallest value on the queue. Time complexity is O(1).
     * @return the smallest value on the queue.
     */
    public T getMin() {
        if (size == 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "No values left on the heap");
        }
        //noinspection unchecked
        return heap[0];
    }

    /**
     * @return the size of the queue.
     */
    public int getSize() {
        return size;
    }

    /**
     * Assumes that the last element in the queue is positioned wrong and
     * balances the heap accordingly.
     */
    protected void siftUp() {
        int position = size-1;
        while (position > 0) {
            int parentPosition = parent(position);
            //noinspection unchecked
            if ((comparator != null
                 ? comparator.compare(heap[parentPosition],
                                      heap[position])
                 : (heap[parentPosition]).compareTo(heap[position])) < 0) {
                swap(parentPosition, position);
            } else {
                break;
            }
            position = parentPosition;
        }
    }

    /**
     * Assumes that the first element (the root) in the queue is positioned
     * wrong and balances the heap accordingly.
     */
    protected void siftDown() {
        siftDown(0);
    }
    protected void siftDown(int startPosition) {
        int position = startPosition;
        while (firstChild(position) < size) {
            int kid = firstChild(position);
            //noinspection unchecked
            if (kid < size-1 &&
                (comparator != null
                 ? comparator.compare(heap[kid], heap[kid+1])
                 : (heap[kid]).compareTo(heap[kid+1])) < 0) {
                kid++;
            }
            //noinspection unchecked
            if ((comparator != null
                 ? comparator.compare(heap[position], heap[kid])
                 : (heap[position]).compareTo(heap[kid])) > 0) {
                break;
            } else {
                swap(kid, position);
                position = kid;
            }
        }
    }

    /**
     * @param element the position of an element.
     * @return the position of the parent of the element.
     */
    protected int parent(int element) {
      return (element-1) / 2;
    }

    /**
     * @param element the position of an element.
     * @return the position of the first child of the element. Note that this
     *         might be outside of the heap.
     */
    protected int firstChild(int element) {
        return 2*element+1;
    }

    private void swap(int element1, int element2) {
        T temp = heap[element1];
        heap[element1] = heap[element2];
        heap[element2] = temp;
    }
}
