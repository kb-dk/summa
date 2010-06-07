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
    private final Comparator<T> comparator; // Optional

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
        //noinspection unchecked
        heap = (T[])new Comparable[initialCapacity];
        this.maxCapacity = maxCapacity;
        this.comparator = fixComparator(comparator);
    }

    // Ensures that there is a Comparator (defaults to narutal order)
    private Comparator<T> fixComparator(Comparator<T> comparator) {
        if (comparator != null) {
            return comparator;
        }
        return new Comparator<T>() {
            public final int compare(final T o1, final T o2) {
                return o1.compareTo(o2);
            }
        };
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
            heap = (T[])new Comparable[Math.max(Math.min(
                    DEFAULT_INITIAL_CAPACITY, maxCapacity), size)];
            System.arraycopy(values, 0, heap, 0, size);
        }

        // This is O(n), although it looks like O(n*log(n))
        for (int position = size / 2 - 1 ; position  >= 0 ; position --) {
            siftDown(position);
        }
    }

    /**
     * Insert a value in the queue. Time complexity is O(log(n)).
     * If the insertion pushed out an existing value, that value is returned.
     * @param value the value to insert in the queue.
     * @return the old value, if the new value pushes one out. Else null
     */
    public T insert(final T value) {
        if (size == maxCapacity) { // Heap is full
            //noinspection unchecked
            final T heapZero = heap[0];
            if (comparator.compare(heapZero, value) > 0) {
                heap[0] = value;
                siftDown();
                return heapZero;
            }
        } else { // Insert at the end and sift up
            if (heap.length == size+1) { // Expand heap
                int newSize = Math.min(maxCapacity, heap.length*2);
                //noinspection unchecked
                T[] newQueue = (T[])new Comparable[newSize];
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
        final T result = getMin();
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
            final int parentPosition = parent(position);
            //noinspection unchecked
            if (comparator.compare(heap[parentPosition], heap[position]) < 0) {
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
    protected void siftDown(final int startPosition) {
        int position = startPosition;
        while (firstChild(position) < size) {
            int kid = firstChild(position);
            //noinspection unchecked
            if (kid < size-1 &&
                comparator.compare(heap[kid], heap[kid+1]) < 0) {
                kid++;
            }
            //noinspection unchecked
            if (comparator.compare(heap[position], heap[kid]) > 0) {
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
        final T temp = heap[element1];
        heap[element1] = heap[element2];
        heap[element2] = temp;
    }

    /**
     * @return the comparator used for this queue. If no explicit Comparator is
     * given on creation time, this will use natural ordering.
     */
    public Comparator<T> getComparator() {
        return comparator;
    }

    /**
     * Pretty print the heap.
     * @return string with all element and there placement in internal heap.
     */
    public String toString() {
        String str = "{";
        for(int i=0; i<size; i++) {
            str += heap[i] + ",";
        }
        return str + "}\n";
    }
}

