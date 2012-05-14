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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Collection;

/**
 * Default memory-oriented implementation of ResourceTracker.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class ResourceTrackerImpl<T> implements ResourceTracker<T> {
    private long minCountLimit = 0;
    private long maxCountLimit = Integer.MAX_VALUE;
    private long memLimit = Integer.MAX_VALUE;

    private long count = 0;
    private long mem = 0;

    /**
     * Construct a basic tracker with the given limits. regardless of the given
     * values, the tracker will always allow at least one element.
     * @param maxCountLimit the maximum allowed number of elements.
     * @param memLimit      the maximum allowed bytes.
     */
    public ResourceTrackerImpl(long maxCountLimit, long memLimit) {
        this(1, maxCountLimit, memLimit);
    }

    /**
     * Construct a basic tracker with the given limits.
     * @param minCountLimit the minimum allowed number of elements.
     *                      This takes precedence over memLimit.
     * @param maxCountLimit the maximum allowed number of elements.
     * @param memLimit      the maximum allowed bytes.
     */
    public ResourceTrackerImpl(
            long minCountLimit, long maxCountLimit, long memLimit) {
        this.minCountLimit = minCountLimit;
        this.maxCountLimit = maxCountLimit;
        this.memLimit = memLimit;
    }

    /**
     * Calculate the aproximate memory usage for the given element.
     * @param element the element to calculate memory usage for.
     * @return approximate memory usage for the element.
     */
    public abstract long calculateBytes(T element);

    /**
     * @return the approximate number of bytes used by added objects.
     */
    public long getMem() {
        return mem;
    }

    public boolean hasRoom(T element) {
        return count < minCountLimit ||
               (count + 1 <= maxCountLimit &&
                calculateBytes(element) + mem < memLimit);
    }

    public boolean isOverflowing() {
        return count > minCountLimit 
               && (count > maxCountLimit || mem > memLimit);
    }

    public long getSize() {
        return count;
    }

    public void add(T element) {
        count++;
        mem += calculateBytes(element);
    }

    public void add(Collection<T> elements) {
        for (T e: elements) {
            add(e);
        }
    }

    public void remove(T element) {
        count--;
        mem -= calculateBytes(element);
    }

    public void remove(Collection<T> elements) {
        for (T e: elements) {
            remove(e);
        }
    }

    public void clear() {
        count = 0;
        mem = 0;
    }
}

