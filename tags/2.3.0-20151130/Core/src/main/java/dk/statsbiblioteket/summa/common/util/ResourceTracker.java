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
 * Keeps track of the resource usage of a data structure. The tracker itself
 * does not hold the objects, only their consumed resources. The obvious
 * resource to track is memory, although things like file handles or open
 * connections are also candidates.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ResourceTracker<T> {

    /**
     * Determine whether there is room for the given Object if it is added,
     * within the constraints defined by the implementation.
     * @param element the potential Object to add to the tracker.
     * @return true if there is room within the given constraints, else false.
     */
    boolean hasRoom(T element);

    /**
     * @return true if the current content exceeds the given limits.
     */
    boolean isOverflowing();

    /**
     * @return the number of elements.
     */
    long getSize();

    /**
     * @param element the element to virtually add to the tracker.
     */
    void add(T element);

    /**
     * @param elements the elements to virtually add to the tracker.
     */
    void add(Collection<T> elements);

    /**
     * @param element the element to virtually remove from the tracker.
     */
    void remove(T element);

    /**
     * @param elements the elements to virtually remove from the tracker.
     */
    void remove(Collection<T> elements);

    /**
     * Clear all tracking data.
     */
    void clear();

}

