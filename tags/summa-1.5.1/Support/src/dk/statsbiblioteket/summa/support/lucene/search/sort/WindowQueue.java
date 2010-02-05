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
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.PriorityQueue;
import dk.statsbiblioteket.summa.common.util.ResourceTracker;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Comparator;

/**
 * Holds a limited amount of elements in sorted order, meant for sliding-window
 * usage with lower and upper bounds for insertion acceptance.
 * </p><p>
 * The window-size is controlled by a
 * {@link dk.statsbiblioteket.summa.common.util.ResourceTracker}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class WindowQueue<T extends Comparable<? super T>> 
                                                      extends PriorityQueue<T> {
    // If defined, only elements higher than this will be accepted
    private T lowerBound = null;
    // If defined, only elements lower than this will be accepted
    private T upperBound = null;

    /**
     * If no comparator is given, a default comparator is created, which will
     * use the elements natural order.
     */
    private final Comparator<T> comparator;

    private final ResourceTracker<T> resourceTracker;

    /**
     * Create a queue with the given constraints.
     * @param comparator      determines the order. If null, natural ordering
     *                        is used.
     * @param lowerBound      elements lower than or equal to this won't be
     *                        inserted in the queue. Ignored if null.
     * @param upperBound      elements higher than or equal to this won't be
     *                        inserted in the queue. Ignored if null.
     * @param resourceTracker determines the queue size.
     */
    public WindowQueue(
            Comparator<T> comparator,
            T lowerBound, T upperBound, ResourceTracker<T> resourceTracker) {
        super(comparator, 10, Integer.MAX_VALUE); // resourceTracker controls
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.resourceTracker = resourceTracker;
        this.comparator = getComparator();
    }

    @Override
    public T insert(final T value) {
/*        if ("mB2".equals(value.toString())) {
            System.out.println("Inserting problematic term " + value);
        }*/
        if (lowerBound != null && comparator.compare(lowerBound, value) >= 0) {
            return null;
        }
        if (upperBound != null && comparator.compare(upperBound, value) <= 0) {
            return null;
        }
        resourceTracker.add(value);
        T bumped = super.insert(value);
        if (bumped != null) { // Normally null as resourceTracker handles limits
            resourceTracker.remove(bumped);
        }
        // Trim
        while (getSize() > 0 && resourceTracker.isOverflowing()) {
            bumped = removeMin(); // Reuse the last one
        }
        return bumped;
    }

    @Override
    public T removeMin() {
        final T min = super.removeMin();
        if (min != null) {
            resourceTracker.remove(min);
        }
        return min;
    }

    public T getLowerBound() {
        return lowerBound;
    }

    /**
     * Note: Changing the lower bound only affects future inserts.
     * @param lowerBound the lower bound for accepted valued.
     */
    public void setLowerBound(T lowerBound) {
        this.lowerBound = lowerBound;
    }

    public T getUpperBound() {
        return upperBound;
    }

    /**
     * Note: Changing the upper bound only affects future inserts.
     * @param upperBound the upper bound for accepted valued.
     */
    public void setUpperBound(T upperBound) {
        this.upperBound = upperBound;
    }

}

