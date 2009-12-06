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
