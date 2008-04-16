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

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

/**
 * A manipulator is responsible for updating part of an index. This could be a
 * Lucene index, the persistent structure for Facets or SearchDescriptor.
 * If the specific part of the index is not initialized upon start, the
 * manipulator is responsible for the initialization.
 * </p><p>
 * Note that it is expected that readers of the index can connect to the index
 * at any time, so manipulators /must/ ensure that the persistent files are
 * kept in a readable state at all time.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface IndexManipulator extends Configurable {
    /**
     * Clear the index, preparing for a fresh start.
     * @throws IOException in case of I/O problems.
     */
    public void clear() throws IOException;

    /**
     * Update the given payload in the index. The nature of the update can be
     * determined by the Record in the payload and spans new, changed and
     * deleted.
     * </p><p>
     * Manipulators are required to preserve the order of added payloads in the
     * index-updating precess, but in the event of changed or deleted payloads,
     * ordering can be ignored. This is due to Lucene being slow at deletes.
     * @param payload the parload containing a Document to add, change or
     *                delete in the index.
     * @throws IOException in case of I/O problems.
     */
    public void update(Payload payload) throws IOException;

    /**
     * Commit any cached structures to persistent storage and ensure that new
     * searchers will open the commited index parts. It is strongly recommended
     * to optimize this call for time, so that commits are cheap. This ensures
     * short turn-around times for index updated.
     * @throws IOException in case of I/O problems.
     */
    public void commit() throws IOException;

    /**
     * Perform clean-up and optimization of index parts. For Lucene indexes,
     * this will most probably correspond to a call to optimize(). This call
     * might take some time to perform and will normally be called as part
     * of a batch clean-up at times with light workload.
     * @throws IOException in case of I/O problems.
     */
    public void consolidate() throws IOException;

    /**
     * Ensure that all content is flushed to persistent storage and close down
     * any open connections. No action should be performed on manipulators
     * after close has been called.
     */
    public void close();
}
