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
package dk.statsbiblioteket.summa.search;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Provides methods for controlling a SummaSearcher as part of a collection
 * of other SummaSearchers. Nodes should not open any indexes as part of the
 * construction-phase. Only {@link #open(String)} should trigger opening.
 * </p><p>
 * Nodes are not responsible for keeping track of running searches when open
 * or close is called. This is the responsibility of the user and is
 * implemented in {@link SearchNodeWrapper}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SearchNode extends SummaSearcher {
    /**
     * Opens the index at the given location. Implementations should ensure that
     * previously opened connections are closed before opening new.
     * It is not guaranteed that {@link #close()} will be called before open.
     * @param location     where the index can be found.
     * @throws IOException if the index could not be opened.
     */
    public void open(String location) throws IOException;

    /**
     * Closes all connections to index resources.
     */
    public void close();
}
