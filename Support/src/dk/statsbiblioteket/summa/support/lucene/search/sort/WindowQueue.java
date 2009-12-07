/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
    private Comparator<T> comparator;

    private ResourceTracker<T> resourceTracker;

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
        this.comparator = comparator;
        if (comparator == null) {
            this.comparator = new Comparator<T>() {
                public int compare(T o1, T o2) {
                    return o1.compareTo(o2);
                }
            };
        }
    }

    @Override
    public T insert(T value) {
        if (lowerBound != null && comparator.compare(lowerBound, value) >= 0) {
            return null;
        }
        if (upperBound != null && comparator.compare(upperBound, value) <= 0) {
            return null;
        }
        resourceTracker.add(value);
        T bumped = super.insert(value);
        if (bumped != null) {
            resourceTracker.remove(bumped);
        }
        // Trim
        while (getSize() > 0 && resourceTracker.isOverflowing()) {
            removeMin(); // Lost without a trace
        }
        return bumped;
    }

    @Override
    public T removeMin() {
        T min = super.removeMin();
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
