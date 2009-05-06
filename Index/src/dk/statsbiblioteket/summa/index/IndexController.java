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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;

/**
 * A controller for manipulating indexes. An index is a representation of
 * data in searcheable form and will normally include an Luxene index. In
 * addition it will normally include an IndexDescriptor and Facet information.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface IndexController extends ObjectFilter {
    /**
     * The name of the logger collecting Payloads that could not be indexed.
     * error-level: Used if some index manipulators handled the Payload okay
     *              but other manipulators failed. This means inconsistencies
     *              between the sub-indexes.<br />
     * warn-level:  Used if the fitst index manipulator fails on the Payload.
     *              Even though the Payload wasn't indexed, the sub-indexes
     *              are consistent between each other.<br />
     * debug-level: Logs the content fron the Records in the failed Payloads.
     */
    public static final String LOG_FAILED = "index.failed";

    /**
     * Add a manipulator to the list of manipulators. The order of addition is
     * significant: Manipulators will be called in order of addition.
     * @param manipulator the manipulator to add.
     */
    public void addManipulator(IndexManipulator manipulator);

    /**
     * Removea a manipulator from the list of manipulators.
     * @param manipulator the manipulator to remove.
     * @return if the manipulator was removed.
     */
    public boolean removeManipulator(IndexManipulator manipulator);

}



